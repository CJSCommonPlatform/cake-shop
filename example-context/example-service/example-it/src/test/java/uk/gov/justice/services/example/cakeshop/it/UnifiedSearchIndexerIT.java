package uk.gov.justice.services.example.cakeshop.it;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopMediaTypes.ORDER_CAKE_MEDIA_TYPE;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopUris.ORDERS_RESOURCE_URI;
import static uk.gov.justice.services.test.utils.core.matchers.HttpStatusCodeMatcher.isStatus;

import uk.gov.justice.services.example.cakeshop.it.helpers.ApiResponse;
import uk.gov.justice.services.example.cakeshop.it.helpers.Querier;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UnifiedSearchIndexerIT {

    private final Client client = new RestEasyClientFactory().createResteasyClient();
    private final Querier querier = new Querier(client);

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Before
    public void before() throws Exception {

        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();
    }

    @After
    public void cleanup() throws Exception {
        client.close();
    }

    @Test
    public void shouldIndexData() {

        final UUID recipeId = randomUUID();

        final Response commandResponse =
                client.target(ORDERS_RESOURCE_URI + randomUUID().toString()).request()
                        .post(entity(
                                createObjectBuilder()
                                        .add("recipeId", recipeId.toString())
                                        .add("deliveryDate", "2016-01-21T23:42:03.522+07:00")
                                        .build().toString(),
                                ORDER_CAKE_MEDIA_TYPE));

        assertThat(commandResponse.getStatus(), isStatus(ACCEPTED));

        await().until(() -> querier.queryForIndex(recipeId.toString()).httpCode() == OK.getStatusCode());

        final ApiResponse queryResponse = querier.queryForIndex(recipeId.toString());

        System.out.println(queryResponse.body());

        with(queryResponse.body())
                .assertThat("$.indexId", equalTo(recipeId.toString()))
                .assertThat("$.deliveryDate", equalTo("2016-01-21T16:42:03.522Z"));
    }

    private void cleanViewstoreTables() {

        final String contextName = "framework";

        databaseCleaner.cleanViewStoreTables(contextName,
                "ingredient",
                "recipe",
                "cake",
                "cake_order",
                "processed_event",
                "shuttered_command_store"
        );

        databaseCleaner.cleanStreamBufferTable(contextName);
        databaseCleaner.cleanStreamStatusTable(contextName);
    }
}
