package uk.gov.justice.services.example.cakeshop.it;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.core.matchers.HttpStatusCodeMatcher.isStatus;

import uk.gov.justice.services.example.cakeshop.it.helpers.ApiResponse;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeShopRepositoryManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandSender;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.MBeanHelper;
import uk.gov.justice.services.example.cakeshop.it.helpers.Querier;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.Shuttering;
import uk.gov.justice.services.jmx.ShutteringMBean;

import java.io.IOException;
import java.util.Optional;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

public class ShutteringIT {

    private static final CakeShopRepositoryManager CAKE_SHOP_REPOSITORY_MANAGER = new CakeShopRepositoryManager();

    private static final Logger logger = getLogger(ShutteringIT.class);
    private static final String MARBLE_CAKE = "Marble cake";
    private static final String CARROT_CAKE = "Carrot cake";

    private final EventFactory eventFactory = new EventFactory();

    private Client client;
    private Querier querier;
    private CommandSender commandSender;

    private MBeanHelper mBeanHelper;

    @BeforeClass
    public static void beforeClass() throws Exception {
        CAKE_SHOP_REPOSITORY_MANAGER.initialise();
    }

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();
        querier = new Querier(client);
        commandSender = new CommandSender(client, eventFactory);
        mBeanHelper = new MBeanHelper();
    }

    @After
    public void cleanup() throws MalformedObjectNameException, IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        client.close();

        //invoke unshuttering - Always ensure unshutter is invoked as we cannot guarantee order of execution for other Cakeeshop IT's
        invokeShuttering(false);
    }

    @Test
    public void shouldNotReturnRecipesAfterShuttering() throws MalformedObjectNameException, IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        //invoke shuttering
        invokeShuttering(true);

        //add 2 recipes
        final String recipeId = addRecipe(MARBLE_CAKE);
        final String recipeId2 = addRecipe(CARROT_CAKE);

        //check recipes have not been added due to shuttering
        verifyRecipeAdded(recipeId, recipeId2, null, null, false, NOT_FOUND);
    }

    @Test
    public void shouldQueryForRecipesAfterUnShuttering() throws MalformedObjectNameException, IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        //invoke shuttering
        invokeShuttering(true);

        //add more recipes
        final String recipeId = addRecipe(MARBLE_CAKE);
        final String recipeId2 = addRecipe(CARROT_CAKE);

        //check recipes have not been added due to shuttering
        verifyRecipeAdded(recipeId, recipeId2, null, null, false, NOT_FOUND);

        //invoke unshuttering
        invokeShuttering(false);

        ////check new recipes have been added successfully after unshuttering
        verifyRecipeAdded(recipeId, recipeId2, MARBLE_CAKE, CARROT_CAKE,true, OK);
    }

    private void verifyRecipeAdded(final String recipeId,
                                   final String recipeId2,
                                   final String recipeName,
                                   final String recipeName2,
                                   final boolean checkRecipeName,
                                   final Status status) {
        final Optional<String> recId = of(recipeId);
        await().until(() -> {
            if(checkRecipeName) {
                final ApiResponse response = verifyResponse(empty(), status);

                verifyResponseBody(recipeId, recipeId2, recipeName, recipeName2, response);
            } else {
                verifyResponse(recId, status);
            }
        });
    }

    private void verifyResponseBody(final String recipeId, final String recipeId2, final String recipeName, final String recipeName2, final ApiResponse response) {
        with(response.body())
                .assertThat("$.recipes[?(@.id=='" + recipeId + "')].name", hasItem(recipeName))
                .assertThat("$.recipes[?(@.id=='" + recipeId2 + "')].name", hasItem(recipeName2));
    }

    private ApiResponse verifyResponse(final Optional<String> recipeId, final Status status) {
        final ApiResponse response = recipeId.isPresent() ? querier.queryForRecipe(recipeId.get()):
                querier.recipesQueryResult();

        logger.info(format("Response: %s", response.httpCode()));
        assertThat(response.httpCode(), isStatus(status));

        return response;
    }

    private void invokeShuttering(final boolean isShutteringRequired) throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        try(final JMXConnector jmxConnector = mBeanHelper.getJMXConnector()){

            final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

            final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

            if(isShutteringRequired) {
                mBeanHelper.getMbeanProxy(connection, objectName, ShutteringMBean.class).doShutteringRequested();
            } else {
                mBeanHelper.getMbeanProxy(connection, objectName, ShutteringMBean.class).doUnshutteringRequested();
            }
        }
    }

    private String addRecipe(final String cakeName) {
        final String recipeId = randomUUID().toString();
        commandSender.addRecipe(recipeId, cakeName);
        return recipeId;
    }
}
