package uk.gov.justice.services.example.cakeshop.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.fromString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopMediaTypes.CONTEXT_NAME;

import uk.gov.justice.services.event.buffer.core.repository.subscription.Subscription;
import uk.gov.justice.services.example.cakeshop.it.helpers.ApiResponse;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeShopRepositoryManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFinder;
import uk.gov.justice.services.example.cakeshop.it.helpers.Querier;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;

import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CakeShopReplayEventsIT {

    private static final CakeShopRepositoryManager CAKE_SHOP_REPOSITORY_MANAGER = new CakeShopRepositoryManager();

    private final EventFactory eventFactory = new EventFactory();
    private final EventFinder eventFinder = new EventFinder(CAKE_SHOP_REPOSITORY_MANAGER);
    private final CommandFactory commandFactory = new CommandFactory();

    private Client client;
    private Querier querier;

    @BeforeClass
    public static void beforeClass() throws Exception {
        CAKE_SHOP_REPOSITORY_MANAGER.initialise();
    }

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();
        querier = new Querier(client);
    }

    @After
    public void cleanup() {
        client.close();
    }

    @Test
    public void shouldFindReplayedRecipesInViewStore() {

        final String recipeId_1 = "489c5e3b-8c0c-4e26-855f-34592604bd98";
        final String recipeId_2 = "8440bcc3-a4d6-4bd1-817c-ab89ffd307ae";

        final ApiResponse response_1 = querier.queryForRecipe(recipeId_1);
        final ApiResponse response_2 = querier.queryForRecipe(recipeId_2);

        with(response_1.body())
                .assertThat("$.id", equalTo(recipeId_1))
                .assertThat("$.name", equalTo("Turnip Cake"));

        with(response_2.body())
                .assertThat("$.id", equalTo(recipeId_2))
                .assertThat("$.name", equalTo("Rock Cake"));

        assertThat(subscription(recipeId_1).getPosition(), is(2L));
        assertThat(subscription(recipeId_2).getPosition(), is(2L));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Subscription subscription(final String recipeId) {
        return CAKE_SHOP_REPOSITORY_MANAGER
                .getSubscriptionJdbcRepository()
                .findByStreamIdAndSource(fromString(recipeId), CONTEXT_NAME)
                .get();
    }
}
