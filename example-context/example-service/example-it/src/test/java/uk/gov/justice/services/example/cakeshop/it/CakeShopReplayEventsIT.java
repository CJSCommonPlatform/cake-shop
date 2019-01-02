package uk.gov.justice.services.example.cakeshop.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopMediaTypes.CONTEXT_NAME;

import uk.gov.justice.services.event.buffer.core.repository.subscription.Subscription;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeShopRepositoryManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFinder;
import uk.gov.justice.services.example.cakeshop.it.helpers.Querier;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.util.Optional;

import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class CakeShopReplayEventsIT {

    private static final CakeShopRepositoryManager CAKE_SHOP_REPOSITORY_MANAGER = new CakeShopRepositoryManager();

    private final EventFactory eventFactory = new EventFactory();
    private final EventFinder eventFinder = new EventFinder(CAKE_SHOP_REPOSITORY_MANAGER);
    private final CommandFactory commandFactory = new CommandFactory();

    private final Poller poller = new Poller();


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

    @Ignore
    @Test
    public void shouldFindReplayedRecipesInViewStore() {

        final String recipeId_1 = "489c5e3b-8c0c-4e26-855f-34592604bd98";
        final String recipeId_2 = "8440bcc3-a4d6-4bd1-817c-ab89ffd307ae";

        final Optional<String> response_1 = poller.pollUntilFound(() -> getRecipe(recipeId_1));
        final Optional<String> response_2 = poller.pollUntilFound(() -> getRecipe(recipeId_2));
        assertThat(response_1.isPresent(), is(true));
        assertThat(response_2.isPresent(), is(true));

        with(response_1.get())
                .assertThat("$.id", equalTo(recipeId_1))
                .assertThat("$.name", equalTo("Turnip Cake"));

        with(response_2.get())
                .assertThat("$.id", equalTo(recipeId_2))
                .assertThat("$.name", equalTo("Rock Cake"));

        assertThat(subscription(recipeId_1).getPosition(), is(2L));
        assertThat(subscription(recipeId_2).getPosition(), is(2L));
    }

    private Optional<String> getRecipe(final String recipeId) {
        final String body = querier.queryForRecipe(recipeId).body();

        if (body == null || body.isEmpty()) {
            return empty();
        }
        return of(body);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Subscription subscription(final String recipeId) {
        return CAKE_SHOP_REPOSITORY_MANAGER
                .getStreamStatusJdbcRepository()
                .findByStreamIdAndSource(fromString(recipeId), CONTEXT_NAME)
                .get();
    }
}
