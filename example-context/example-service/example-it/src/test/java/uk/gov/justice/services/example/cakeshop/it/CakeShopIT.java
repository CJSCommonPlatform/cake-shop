package uk.gov.justice.services.example.cakeshop.it;

import static com.google.common.io.Resources.getResource;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.commons.io.IOUtils.contentEquals;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.event.buffer.core.repository.streamstatus.StandaloneStreamStatusJdbcRepositoryFactory.getSnapshotStreamStatusJdbcRepository;
import static uk.gov.justice.services.eventsourcing.jdbc.snapshot.StandaloneSnapshotJdbcRepositoryFactory.getSnapshotJdbcRepository;
import static uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventRepositoryFactory.getEventJdbcRepository;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.domain.snapshot.DefaultObjectInputStreamStrategy;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.exception.AggregateChangeDetectedException;
import uk.gov.justice.services.event.buffer.core.repository.streamstatus.StreamStatus;
import uk.gov.justice.services.event.buffer.core.repository.streamstatus.StreamStatusJdbcRepository;
import uk.gov.justice.services.eventsourcing.jdbc.snapshot.SnapshotJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;
import uk.gov.justice.services.example.cakeshop.domain.aggregate.Recipe;
import uk.gov.justice.services.example.cakeshop.it.util.ApiResponse;
import uk.gov.justice.services.example.cakeshop.it.util.TestProperties;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.HttpResponsePoller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CakeShopIT {

    private static final int OK = 200;
    private static final int ACCEPTED = 202;
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String RANDOM_HTTP_PORT = System.getProperty("random.http.port");
    private static final String RECIPES_RESOURCE_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-command-api/command/api/rest/cakeshop/recipes/";
    private static final String CAKES_RESOURCE_URI = RECIPES_RESOURCE_URI + "%s/cakes/%s";
    private static final String ORDERS_RESOURCE_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-command-api/command/api/rest/cakeshop/orders/";
    private static final String RECIPES_RESOURCE_QUERY_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-query-api/query/api/rest/cakeshop/recipes/";
    private static final String ORDERS_RESOURCE_QUERY_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-query-api/query/api/rest/cakeshop/orders/";
    private static final String CAKES_RESOURCE_QUERY_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-query-api/query/api/rest/cakeshop/cakes/";
    private static final String OVEN_RESOURCE_CUSTOM_URI = "http://localhost:" + RANDOM_HTTP_PORT + "/example-custom-api/custom/api/rest/cakeshop/ovens/";

    private static final String SOURCE = "example";

    private static final String ADD_RECIPE_MEDIA_TYPE = "application/vnd." + SOURCE + ".add-recipe+json";
    private static final String RENAME_RECIPE_MEDIA_TYPE = "application/vnd." + SOURCE + ".rename-recipe+json";
    private static final String REMOVE_RECIPE_MEDIA_TYPE = "application/vnd." + SOURCE + ".remove-recipe+json";
    private static final String MAKE_CAKE_MEDIA_TYPE = "application/vnd." + SOURCE + ".make-cake+json";
    private static final String ORDER_CAKE_MEDIA_TYPE = "application/vnd." + SOURCE + ".order-cake+json";
    private static final String QUERY_RECIPE_MEDIA_TYPE = "application/vnd." + SOURCE + ".recipe+json";
    private static final String QUERY_RECIPES_MEDIA_TYPE = "application/vnd." + SOURCE + ".recipes+json";
    private static final String QUERY_CAKES_MEDIA_TYPE = "application/vnd." + SOURCE + ".cakes+json";
    private static final String QUERY_ORDER_MEDIA_TYPE = "application/vnd." + SOURCE + ".order+json";

    private static final String JMS_USERNAME = "jmsuser";

    private static final String JMS_PASSWORD = "jms@user123";
    private static final String RANDOM_JMS_PORT = System.getProperty("random.jms.port");
    private static final String JMS_BROKER_URL = "tcp://localhost:" + RANDOM_JMS_PORT;

    private static final TestProperties TEST_PROPERTIES = new TestProperties("test.properties");

    private static EventJdbcRepository EVENT_LOG_REPOSITORY;
    private static StreamStatusJdbcRepository STREAM_STATUS_REPOSITORY;
    private static SnapshotJdbcRepository SNAPSHOT_REPOSITORY;
    private static ActiveMQConnectionFactory JMS_CONNECTION_FACTORY;
    private static DataSource CAKE_SHOP_DS;

    private Client client;

    private HttpResponsePoller httpResponsePoller;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final DataSource eventStoreDataSource = initEventStoreDb();
        EVENT_LOG_REPOSITORY = getEventJdbcRepository(eventStoreDataSource);
        JMS_CONNECTION_FACTORY = new ActiveMQConnectionFactory(JMS_BROKER_URL);

        final DataSource viewStoreDatasource = initViewStoreDb();
        STREAM_STATUS_REPOSITORY = getSnapshotStreamStatusJdbcRepository(viewStoreDatasource);
        SNAPSHOT_REPOSITORY = getSnapshotJdbcRepository(eventStoreDataSource);

        initFileServiceDb();

        Thread.sleep(300);
    }

    @Before
    public void before() throws Exception {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        final CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        client = new ResteasyClientBuilder().httpEngine(engine).build();
        httpResponsePoller = new HttpResponsePoller();
    }

    @After
    public void cleanup() throws Exception {
        client.close();
    }

    @Test
    public void shouldReturn202ResponseWhenAddingRecipe() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f88";
        final Response response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), ADD_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    @Test
    public void shouldReturn202ResponseWhenRemovingRecipe() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f25";
        Response response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), ADD_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));

        await().until(() -> eventsWithPayloadContaining(recipeId).size() == 1);

        response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), REMOVE_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    @Test
    public void shouldReturn400ResponseWhenJsonNotAdheringToSchemaIsSent() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f25";
        final Response response = sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity("{}", ADD_RECIPE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldRegisterRecipeAddedEvent() {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f99";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Vanilla cake")
                                .add("glutenFree", false)
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "vanilla")
                                                .add("quantity", 2)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).size() == 1);

        final Event event = eventsWithPayloadContaining(recipeId).get(0);
        assertThat(event.getName(), is("example.recipe-added"));
        with(event.getMetadata())
                .assertEquals("stream.id", recipeId)
                .assertEquals("stream.version", 1);
        final String eventPayload = event.getPayload();
        with(eventPayload)
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.name", equalTo("Vanilla cake"))
                .assertThat("$.glutenFree", equalTo(false))
                .assertThat("$.ingredients[0].name", equalTo("vanilla"))
                .assertThat("$.ingredients[0].quantity", equalTo(2));
    }

    @Test
    public final void concurrentAddAndRemoveRecipe() throws InterruptedException {
        final ExecutorService exec = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            exec.execute(this::shouldRegisterRecipeRemovedEvent);
        }
        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Test
    public void shouldRegisterRecipeRemovedEvent() {
        final String recipeId = randomUUID().toString();

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Vanilla cake")
                                .add("glutenFree", false)
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "vanilla")
                                                .add("quantity", 2)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).size() == 1);

        final Event event = eventsWithPayloadContaining(recipeId).get(0);
        assertThat(event.getName(), is("example.recipe-added"));
        with(event.getMetadata())
                .assertEquals("stream.id", recipeId)
                .assertEquals("stream.version", 1);
        final String eventPayload = event.getPayload();
        with(eventPayload)
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.name", equalTo("Vanilla cake"))
                .assertThat("$.glutenFree", equalTo(false))
                .assertThat("$.ingredients[0].name", equalTo("vanilla"))
                .assertThat("$.ingredients[0].quantity", equalTo(2));

        final String foundResponse = httpResponsePoller.pollUntilFound(RECIPES_RESOURCE_QUERY_URI + recipeId, QUERY_RECIPE_MEDIA_TYPE);
        assertThat(foundResponse, notNullValue());

        sendTo(RECIPES_RESOURCE_URI + recipeId).request().post(entity(jsonObject()
                .add("recipeId", recipeId).build().toString(), REMOVE_RECIPE_MEDIA_TYPE));

        await().until(() -> eventsWithPayloadContaining(recipeId).size() == 2);

        final String notFoundResponse = httpResponsePoller.pollUntilNotFound(RECIPES_RESOURCE_QUERY_URI + recipeId, QUERY_RECIPE_MEDIA_TYPE);
        assertThat(notFoundResponse, notNullValue());
    }

    @Test
    public void shouldReturn200WhenQueryingForRecipes() throws Exception {
        final Response response = sendTo(RECIPES_RESOURCE_QUERY_URI + "?pagesize=10").request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
        assertThat(response.getStatus(), is(OK));
    }

    @Test
    public void shouldReturn200WhenPostingQueryForRecipes() throws Exception {
        final Response response = sendTo("http://localhost:" + RANDOM_HTTP_PORT + "/example-query-api/query/api/rest/cakeshop/recipes")
                .request()
                .accept("application/vnd.example.recipes+json")
                .post(entity(
                        jsonObject()
                                .add("pagesize", 10)
                                .add("name", "Vanilla cake")
                                .add("glutenFree", false)
                                .build().toString(),
                        "application/vnd.example.query-recipes+json"));

        assertThat(response.getStatus(), is(OK));
    }

    @Test
    public void shouldReturn400WhenMandatoryQueryParamNotProvided() throws Exception {
        final Response response = sendTo(RECIPES_RESOURCE_QUERY_URI).request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn400WhenIncorrectMediaTypeInAccept() throws Exception {
        final Response response = sendTo(RECIPES_RESOURCE_QUERY_URI).request().accept("*/*").get();
        assertThat(response.getStatus(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn404IfRecipeDoesNotExist() {
        final ApiResponse response = queryForRecipe("163af847-effb-46a9-96bc-32a0f7526f00");
        assertThat(response.httpCode(), is(NOT_FOUND));
    }

    @Test
    public void shouldReturnRecipeOfGivenId() {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f22";
        final String recipeName = "Cheesy cheese cake";
        addRecipe(recipeId, recipeName);


        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        final ApiResponse response = queryForRecipe(recipeId);

        with(response.body())
                .assertThat("$.id", equalTo(recipeId))
                .assertThat("$.name", equalTo(recipeName));
    }

    @Test
    public void shouldFailTransactionOnDBFailureAndRedirectEventToDLQ() throws Exception {
        try (final Session jmsSession = jmsSession()) {
            try (final MessageConsumer dlqConsumer = queueConsumerOf(jmsSession, "DLQ")) {
                clear(dlqConsumer);

                //closing db to cause transaction error
                closeCakeShopDb();

                final String recipeId = "363af847-effb-46a9-96bc-32a0f7526f12";
                addRecipe(recipeId, "Cheesy cheese cake");

                final TextMessage messageFromDLQ = (TextMessage) dlqConsumer.receive();

                with(messageFromDLQ.getText())
                        .assertThat("$._metadata.name", equalTo("example.recipe-added"))
                        .assertThat("$.recipeId", equalTo(recipeId));
            }

            initViewStoreDb();
        }
    }

    @Test
    public void shouldReturnRecipes() {
        //adding 2 recipes
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e14";
        addRecipe(recipeId, "Cheesy cheese cake");

        final String recipeId2 = "163af847-effb-46a9-96bc-32a0f7526e15";
        addRecipe(recipeId2, "Chocolate muffin");


        await().until(() -> {
            final String responseBody = recipesQueryResult(singletonList(new BasicNameValuePair("pagesize", "30"))).body();
            return responseBody.contains(recipeId) && responseBody.contains(recipeId2);
        });

        final ApiResponse response = recipesQueryResult();
        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem("Cheesy cheese cake"))
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem("Chocolate muffin"));
    }

    @Test
    public void shouldReturnRecipeFromOtherEventListener() throws Exception {
        //adding 1 recipe as normal
        final UUID recipeId = UUID.randomUUID();
        addRecipe(recipeId.toString(), "Cheesy cheese cake");

        //adding 1 recipe as other event
        final UUID recipeId2 = UUID.randomUUID();

        try (final Session jmsSession = jmsSession()) {
            final Topic topic = jmsSession.createTopic("other.event");

            try (final MessageProducer producer = jmsSession.createProducer(topic);) {

                final JsonObject jsonObject = jsonObject()
                        .add("recipeId", recipeId2.toString())
                        .add("name", "Chocolate muffin")
                        .add("glutenFree", true)
                        .add("ingredients", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("name", "someIngredient")
                                        .add("quantity", 1)
                                ).build()
                        ).build();

                final JsonEnvelope jsonEnvelope = envelopeFrom(
                        metadataBuilder()
                                .withId(UUID.randomUUID())
                                .withName("other.recipe-added")
                                .withStreamId(recipeId2)
                                .withVersion(1L)
                                .build(),
                        jsonObject);

                @SuppressWarnings("deprecation") final String json = jsonEnvelope.toDebugStringPrettyPrint();
                final TextMessage message = jmsSession.createTextMessage();

                message.setText(json);
                message.setStringProperty("CPPNAME", "other.recipe-added");

                producer.send(message);
            }
        }

        await().until(() -> {
            final String responseBody = recipesQueryResult(singletonList(new BasicNameValuePair("pagesize", "30"))).body();
            return responseBody.contains(recipeId.toString()) && responseBody.contains(recipeId2.toString());
        });

        final ApiResponse response = recipesQueryResult();
        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem("Cheesy cheese cake"))
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem("Chocolate muffin"));
    }


    @Test
    public void shouldFilterRecipesUsingPageSize() {
        //adding 2 recipes
        final String recipeId = "263af847-effb-46a9-96bc-32a0f7526e44";
        addRecipe(recipeId, "Absolutely cheesy cheese cake");

        final String recipeId2 = "263af847-effb-46a9-96bc-32a0f7526e55";
        addRecipe(recipeId2, "Chocolate muffin");

        await().until(() -> recipesQueryResult().body().contains(recipeId));

        final ApiResponse response = recipesQueryResult(asList(new BasicNameValuePair("pagesize", "1")));
        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')]", emptyCollection())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem("Absolutely cheesy cheese cake"));
    }

    @Test
    public void shouldFilterGlutenFreeRecipes() {
        //adding 2 recipes
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e66";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity("Muffin", false));

        final String recipeId2 = "163af847-effb-46a9-96bc-32a0f7526e77";
        sendTo(RECIPES_RESOURCE_URI + recipeId2).request()
                .post(recipeEntity("Oat cake", true));

        await().until(() -> recipesQueryResult().body().contains(recipeId2));

        final ApiResponse response = recipesQueryResult(asList(
                new BasicNameValuePair("pagesize", "30"),
                new BasicNameValuePair("glutenFree", "true")));

        assertThat(response.httpCode(), is(OK));

        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')]", emptyCollection())
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem("Oat cake"));
    }

    @Test
    public void shouldReturn400WhenInvalidNumericParamPassed() {
        final ApiResponse response = recipesQueryResult(asList(
                new BasicNameValuePair("pagesize", "invalid")));

        assertThat(response.httpCode(), is(BAD_REQUEST));
    }

    @Test
    public void shouldReturn400WhenInvalidBooleanParamPassed() {
        final ApiResponse response = recipesQueryResult(asList(
                new BasicNameValuePair("pagesize", "30"),
                new BasicNameValuePair("glutenFree", "invalid")));

        assertThat(response.httpCode(), is(BAD_REQUEST));
    }


    @Test
    public void shouldReturnCakesWithNamesInheritedFromRecipe() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e51";
        final String cakeId = "163af847-effb-46a9-96bc-32a0f7526f01";
        final String cakeName = "Super cake";

        addRecipe(recipeId, cakeName);
        addRecipe("163af847-effb-46a9-96bc-32a0f7526f02", "cake");
        await().until(() -> recipesQueryResult().body().contains(recipeId));

        makeCake(recipeId, cakeId);

        await().until(() -> cakesQueryResult().body().contains(cakeId));

        //slightly contrived domain logic: when a cake is made, it gets a name of the recipe
        with(cakesQueryResult().body())
                .assertThat("$.cakes[?(@.id == '" + cakeId + "')].name", hasItem(cakeName));

    }

    @Test
    public void shouldUseSnapshotWhenMakingCakes() throws AggregateChangeDetectedException {

        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e52";
        final String cakeName = "Delicious cake";

        addRecipe(recipeId, cakeName);
        await().until(() -> recipesQueryResult().body().contains(recipeId));

        //cake made events belong to the recipe aggregate.
        //snapshot threshold is set to 3 in settings-test.xml so this should cause snapshot to be created
        final String cakeId1 = "b8b138a2-aee8-46ac-bc8d-a4e0b32de424";
        final String cakeId2 = "a7df425e-ba49-4b53-aaad-7b4b3f796dee";

        makeCake(recipeId, cakeId1);
        makeCake(recipeId, cakeId2);

        await().until(() -> recipeAggregateSnapshotOf(recipeId).isPresent());

        //tweaking recipe snapshot to change it's name and making cake afterwards that will use the new name
        final String newCakeName = "Tweaked cake";
        tweakRecipeSnapshotName(recipeId, newCakeName);

        final String lastCakeId = "7eba6ce5-70e7-452a-b9a9-84acc6011fdf";
        makeCake(recipeId, lastCakeId);

        await().until(() -> cakesQueryResult().body().contains(lastCakeId));

        with(cakesQueryResult().body())
                .assertThat("$.cakes[?(@.id == '" + lastCakeId + "')].name", hasItem(newCakeName));

    }

    @Test
    public void shouldNotPersistRecipeWhenIngredientPersistenceFailsDueToSharedTransaction() throws InterruptedException {
        final String recipeId = randomUUID().toString();

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(
                        jsonObject()
                                .add("name", "Transaction Failure Recipe Rollback Cake")
                                .add("ingredients", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("name", "ingredient-with-long-name-to-exceed-database-column-length")
                                                .add("quantity", 1)
                                        ).build()
                                ).build().toString(),
                        ADD_RECIPE_MEDIA_TYPE));

        Thread.sleep(500);

        assertThat(queryForRecipe(recipeId).httpCode(), is(NOT_FOUND));
    }

    @Test
    public void shouldPublishEventToPublicTopic() throws Exception {
        try (final Session jmsSession = jmsSession()) {
            try (final MessageConsumer publicTopicConsumer = topicConsumerOf(jmsSession, "public.event")) {

                final String recipeId = "163af847-effb-46a9-96bc-32a0f7526e13";
                sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                        .post(recipeEntity("Apple pie", false));

                final TextMessage message = (TextMessage) publicTopicConsumer.receive();
                with(message.getText())
                        .assertThat("$._metadata.name", equalTo("example.recipe-added"))
                        .assertThat("$._metadata.stream.id", equalTo(recipeId))
                        .assertThat("$.recipeId", equalTo(recipeId))
                        .assertThat("$.name", equalTo("Apple pie"));
            }
        }
    }

    @Test
    public void shouldUpdateRecipeWithNewName() throws Exception {
        final String recipeId = randomUUID().toString();
        final String recipeName = "Original Cheese Cake";

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity(recipeName));

        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .put(renameRecipeEntity("New Name"));

        await().until(() -> queryForRecipe(recipeId).body().contains("New Name"));
    }

    @Test
    public void shouldReturnOrderWithUTCOrderDate() {
        final UUID orderId = randomUUID();

        final Response commandResponse =
                sendTo(ORDERS_RESOURCE_URI + orderId.toString()).request()
                        .post(entity(
                                jsonObject()
                                        .add("recipeId", randomUUID().toString())
                                        .add("deliveryDate", "2016-01-21T23:42:03.522+07:00")
                                        .build().toString(),
                                ORDER_CAKE_MEDIA_TYPE));

        assertThat(commandResponse.getStatus(), is(ACCEPTED));

        await().until(() -> queryForOrder(orderId.toString()).httpCode() == OK);

        final ApiResponse queryResponse = queryForOrder(orderId.toString());

        with(queryResponse.body())
                .assertThat("$.orderId", equalTo(orderId.toString()))
                .assertThat("$.deliveryDate", equalTo("2016-01-21T16:42:03.522Z"));
    }

    @Test
    public void shouldSetDateCreatedTimestampInEventStore() {
        final UUID orderId = randomUUID();

        final Response commandResponse =
                sendTo(ORDERS_RESOURCE_URI + orderId.toString()).request()
                        .post(entity(
                                jsonObject()
                                        .add("recipeId", randomUUID().toString())
                                        .add("deliveryDate", "2016-01-21T23:42:03.522+07:00")
                                        .build().toString(),
                                ORDER_CAKE_MEDIA_TYPE));

        assertThat(commandResponse.getStatus(), is(ACCEPTED));

        await().until(() -> queryForOrder(orderId.toString()).httpCode() == OK);

        final Stream<Event> events = EVENT_LOG_REPOSITORY.findByStreamIdOrderByPositionAsc(orderId);
        final Event event = events.findFirst().get();

        assertThat(event.getCreatedAt(), is(notNullValue()));
        assertThat(event.getCreatedAt(), is(within(10L, SECONDS, new UtcClock().now())));
    }

    @Test
    public void shouldReturnCORSResponse() {
        final Response corsResponse =
                sendTo(ORDERS_RESOURCE_URI + "123")
                        .request()
                        .header("Origin", "http://foo.example")
                        .header("Access-Control-Request-Headers", "CPPCLIENTCORRELATIONID")
                        .options();

        assertThat(corsResponse.getStatus(), is(OK));
        final String allowedHeaders = corsResponse.getHeaderString("access-control-allow-headers");
        assertThat(allowedHeaders, not(nullValue()));
        assertThat(asList(allowedHeaders.split(", ")), hasItems("CJSCPPUID", "CPPSID", "CPPCLIENTCORRELATIONID"));

    }

    @Test
    public void shouldUpdateEventBufferStatus() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f89";
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(entity(addRecipeCommand(), ADD_RECIPE_MEDIA_TYPE));

        await().until(() -> streamStatus(recipeId, SOURCE).isPresent());
        assertThat(streamStatus(recipeId, SOURCE).get().getVersion(), is(1L));
    }

    @Test
    public void shouldRecoverAfterException() throws Exception {

        //The ExceptionThrowingInterceptor, in the event listener component, throws an exception on "Exceptional cake"
        //This triggers 4 exceptions to exhaust the connection pool if there's a connection leak
        for (int i = 0; i < 2; i++) {
            sendTo(RECIPES_RESOURCE_URI + randomUUID()).request()
                    .post(recipeEntity("Exceptional cake"));
        }
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f78";
        final String recipeName = "Non exceptional cake";
        addRecipe(recipeId, recipeName);

        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        final ApiResponse response = queryForRecipe(recipeId);

        with(response.body())
                .assertThat("$.id", equalTo(recipeId))
                .assertThat("$.name", equalTo(recipeName));

    }


    @Test
    public void shouldReturnStatusWhenQueryingForOvenStatus() throws Exception {
        final Response response = sendTo(OVEN_RESOURCE_CUSTOM_URI).request().accept("application/vnd.example.status+json").get();
        assertThat(response.getStatus(), is(OK));

        final String entity = response.readEntity(String.class);

        with(entity)
                .assertEquals("$.ovens[0].name", "Big Oven")
                .assertEquals("$.ovens[1].name", "Large Oven");
    }

    @Test
    public void shouldReturnAcceptedStatusAndCreatEventWhenPostingPhotographToMultipartEndpoint() throws Exception {

        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f22";
        final String fieldName = "photoId";
        final String filename = "croydon.jpg";
        final File file = new File(getResource(filename).getFile());

        addRecipe(recipeId, "Cheesy cheese cake");

        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        final HttpEntity httpEntity = MultipartEntityBuilder.create()
                .setMode(BROWSER_COMPATIBLE)
                .addBinaryBody(fieldName, file, APPLICATION_OCTET_STREAM, filename)
                .build();

        final HttpPost request = new HttpPost(RECIPES_RESOURCE_URI + recipeId + "/photograph");
        request.setEntity(httpEntity);

        final HttpResponse response = HttpClients.createDefault().execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED));

        await().until(() -> eventsWithPayloadContaining(recipeId).size() == 2);

        final Event event = eventsWithPayloadContaining(recipeId).get(1);
        assertThat(event.getName(), is("example.recipe-photograph-added"));
        with(event.getMetadata())
                .assertEquals("stream.id", recipeId)
                .assertEquals("stream.version", 2);
        with(event.getPayload())
                .assertThat("$.recipeId", equalTo(recipeId))
                .assertThat("$.photoId", notNullValue());
    }


    @Test
    public void shouldRetrieveRecipePhotograph() throws Exception {
        final String recipeId = "163af847-effb-46a9-96bc-32a0f7526f24";
        addRecipe(recipeId, "Easy Muffin");
        await().until(() -> queryForRecipe(recipeId).httpCode() == OK);

        assertThat(photographFor(recipeId).get().getStatus(), is(NOT_FOUND));

        final String filename = "croydon.jpg";
        appendFileToTheRecipe(recipeId, filename);

        await().until(() -> photographFor(recipeId).get().getStatus() == OK);

        final InputStream returnedStream = photographFor(recipeId).get(InputStream.class);

        assertThat(contentEquals(returnedStream, fileStreamOf(filename)), is(true));

    }

    private InputStream fileStreamOf(final String filename) {
        return this.getClass().getClassLoader().getResourceAsStream(filename);
    }

    private Invocation.Builder photographFor(final String recipeId) {
        return sendTo(format("%s%s/photograph", RECIPES_RESOURCE_QUERY_URI, recipeId))
                .request()
                .accept(APPLICATION_OCTET_STREAM_TYPE);
    }

    private void appendFileToTheRecipe(final String recipeId, final String filename) throws IOException {
        final File file = new File(getResource(filename).getFile());


        final HttpEntity httpEntity = MultipartEntityBuilder.create()
                .setMode(BROWSER_COMPATIBLE)
                .addBinaryBody("photoId", file, APPLICATION_OCTET_STREAM, filename)
                .build();

        final HttpPost request = new HttpPost(RECIPES_RESOURCE_URI + recipeId + "/photograph");
        request.setEntity(httpEntity);

        HttpClients.createDefault().execute(request);
    }

    private void tweakRecipeSnapshotName(final String recipeId, final String newRecipeName) throws AggregateChangeDetectedException {
        final AggregateSnapshot<Recipe> recipeAggregateSnapshot = recipeAggregateSnapshotOf(recipeId).get();
        final Recipe recipe = recipeAggregateSnapshot.getAggregate(new DefaultObjectInputStreamStrategy());
        setField(recipe, "name", newRecipeName);
        SNAPSHOT_REPOSITORY.removeAllSnapshots(recipeAggregateSnapshot.getStreamId(), Recipe.class);
        SNAPSHOT_REPOSITORY.storeSnapshot(new AggregateSnapshot(recipeAggregateSnapshot.getStreamId(), recipeAggregateSnapshot.getVersionId(), recipe));
    }

    private Optional<AggregateSnapshot<Recipe>> recipeAggregateSnapshotOf(final String recipeId) {
        return SNAPSHOT_REPOSITORY.getLatestSnapshot(UUID.fromString(recipeId), Recipe.class);
    }


    private void makeCake(final String recipeId, final String cakeId) {
        final Response response = sendTo(format(CAKES_RESOURCE_URI, recipeId, cakeId)).request()
                .post(entity("{}", MAKE_CAKE_MEDIA_TYPE));
        assertThat(response.getStatus(), is(ACCEPTED));
    }

    private void addRecipe(final String recipeId, final String cakeName) {
        sendTo(RECIPES_RESOURCE_URI + recipeId).request()
                .post(recipeEntity(cakeName));
    }

    private MessageConsumer topicConsumerOf(final Session session, final String topicName) throws JMSException {
        final Topic topic = session.createTopic(topicName);
        return session.createConsumer(topic);
    }

    private MessageConsumer queueConsumerOf(final Session session, final String queueName) throws JMSException {
        final Queue queue = session.createQueue(queueName);
        return session.createConsumer(queue);
    }

    private Session jmsSession() throws JMSException {
        final javax.jms.Connection connection = JMS_CONNECTION_FACTORY.createConnection(JMS_USERNAME, JMS_PASSWORD);
        connection.start();
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }


    private Optional<StreamStatus> streamStatus(final String recipeId, final String source) {
        return STREAM_STATUS_REPOSITORY.findByStreamIdAndSource(UUID.fromString(recipeId), source);
    }

    private static DataSource initViewStoreDb() throws Exception {
        CAKE_SHOP_DS = initDatabase("db.example.url", "db.example.userName", "db.example.password",
                "liquibase/view-store-db-changelog.xml", "liquibase/event-buffer-changelog.xml");
        return CAKE_SHOP_DS;
    }


    private static DataSource initEventStoreDb() throws Exception {
        return initDatabase("db.eventstore.url", "db.eventstore.userName",
                "db.eventstore.password", "liquibase/event-store-db-changelog.xml", "liquibase/snapshot-store-db-changelog.xml");
    }

    private static DataSource initFileServiceDb() throws Exception {
        return initDatabase("db.fileservice.url", "db.fileservice.userName",
                "db.fileservice.password", "liquibase/file-service-liquibase-db-changelog.xml");
    }

    private void closeCakeShopDb() throws SQLException {
        try (final Connection connection = CAKE_SHOP_DS.getConnection()) {
            final Statement statement = connection.createStatement();
            statement.execute("SHUTDOWN");
        }
    }

    private ApiResponse queryForRecipe(final String recipeId) {
        final Response jaxrsResponse = sendTo(RECIPES_RESOURCE_QUERY_URI + recipeId).request().accept(QUERY_RECIPE_MEDIA_TYPE).get();
        return ApiResponse.from(jaxrsResponse);
    }

    private ApiResponse queryForOrder(final String orderId) {
        final Response jaxrsResponse = sendTo(ORDERS_RESOURCE_QUERY_URI + orderId).request().accept(QUERY_ORDER_MEDIA_TYPE).get();
        return ApiResponse.from(jaxrsResponse);
    }

    private ApiResponse recipesQueryResult() {
        return recipesQueryResult(singletonList(new BasicNameValuePair("pagesize", "50")));
    }

    private ApiResponse recipesQueryResult(final List<NameValuePair> queryParams) {
        try {
            final URIBuilder uri = new URIBuilder(RECIPES_RESOURCE_QUERY_URI);
            uri.addParameters(queryParams);
            final Response jaxrRsResponse = sendTo(uri.toString()).request().accept(QUERY_RECIPES_MEDIA_TYPE).get();
            return ApiResponse.from(jaxrRsResponse);
        } catch (URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    private ApiResponse cakesQueryResult() {
        final Response jaxrsResponse = sendTo(CAKES_RESOURCE_QUERY_URI).request().accept(QUERY_CAKES_MEDIA_TYPE).get();
        assertThat(jaxrsResponse.getStatus(), is(200));
        return ApiResponse.from(jaxrsResponse);

    }

    private String addRecipeCommand() {
        return jsonObject()
                .add("name", "Chocolate muffin in six easy steps")
                .add("glutenFree", false)
                .add("ingredients", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("name", "chocolate")
                                .add("quantity", 1)
                        ).build())
                .build().toString();
    }

    private static DataSource initDatabase(final String dbUrlPropertyName,
                                           final String dbUserNamePropertyName,
                                           final String dbPasswordPropertyName,
                                           final String... liquibaseChangeLogXmls) throws Exception {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(H2_DRIVER);

        dataSource.setUrl(TEST_PROPERTIES.value(dbUrlPropertyName));
        dataSource.setUsername(TEST_PROPERTIES.value(dbUserNamePropertyName));
        dataSource.setPassword(TEST_PROPERTIES.value(dbPasswordPropertyName));
        boolean dropped = false;
        final JdbcConnection jdbcConnection = new JdbcConnection(dataSource.getConnection());

        for (String liquibaseChangeLogXml : liquibaseChangeLogXmls) {
            Liquibase liquibase = new Liquibase(liquibaseChangeLogXml,
                    new ClassLoaderResourceAccessor(), jdbcConnection);
            if (!dropped) {
                liquibase.dropAll();
                dropped = true;
            }
            liquibase.update("");
        }
        return dataSource;
    }

    private JsonObjectBuilder jsonObject() {
        return createObjectBuilder();
    }

    private List<Event> eventsWithPayloadContaining(final String string) {
        try (final Stream<Event> events = EVENT_LOG_REPOSITORY.findAll().filter(e -> e.getPayload().contains(string))) {
            return events.collect(toList());
        }
    }

    private WebTarget sendTo(String url) {
        return client.target(url);
    }

    private Entity<String> renameRecipeEntity(final String recipeName) {
        return entity(
                jsonObject()
                        .add("name", recipeName)
                        .build().toString(),
                RENAME_RECIPE_MEDIA_TYPE);
    }

    private Entity<String> recipeEntity(final String recipeName) {
        return recipeEntity(recipeName, false);
    }

    private Entity<String> recipeEntity(String recipeName, boolean glutenFree) {
        return entity(
                jsonObject()
                        .add("name", recipeName)
                        .add("glutenFree", glutenFree)
                        .add("ingredients", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("name", "someIngredient")
                                        .add("quantity", 1)
                                ).build()
                        ).build().toString(),
                ADD_RECIPE_MEDIA_TYPE);
    }

    private void clear(MessageConsumer msgConsumer) throws JMSException {
        while (msgConsumer.receiveNoWait() != null) {
        }
    }
}
