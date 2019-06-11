package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventStreamJdbsRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.exception.InvalidPositionException;
import uk.gov.justice.services.eventstore.management.catchup.commands.CatchupCommand;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeshopEventGenerator;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.PositionInStreamIterator;
import uk.gov.justice.services.example.cakeshop.it.helpers.RecipeTableInspector;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CakeShopReplayEventsIT {


    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final RecipeTableInspector recipeTableInspector = new RecipeTableInspector(viewStoreDataSource);
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    private final Poller poller = new Poller(60, 1000L);

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));

    private final SystemCommanderClientFactory systemCommanderClientFactory = new SystemCommanderClientFactory();

    private Client client;

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();

        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();

        databaseCleaner.cleanStreamBufferTable(contextName);
        databaseCleaner.cleanStreamStatusTable(contextName);
    }

    @After
    public void cleanup() {
        client.close();
    }

    @Test
    public void shouldReplayAndFindRecipesInViewStore() throws Exception {

        final int numberOfStreams = 6;
        final int numberOfEventsPerStream = 2;

        addEventsToEventLog(numberOfStreams, numberOfEventsPerStream);

        System.out.println("Inserted " + numberOfStreams * numberOfEventsPerStream + " events");

        final Optional<Integer> numberOfRecipesOptional = checkExpectedNumberOfRecipes(numberOfStreams);

        if (!numberOfRecipesOptional.isPresent()) {
            fail();
        }

        final List<Recipe> originalRecipes = recipeTableInspector.getAllRecipes();

        cleanViewstoreTables();

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(HOST, PORT)) {
            systemCommanderClient.getRemote().call(new CatchupCommand());

        }

        final Optional<Integer> numberOfReplayedRecipesOptional = checkExpectedNumberOfRecipes(numberOfStreams);

        if (!numberOfReplayedRecipesOptional.isPresent()) {
            fail();
        }

        final List<Recipe> replayedRecipes = recipeTableInspector.getAllRecipes();


        assertThat(originalRecipes.size(), is(replayedRecipes.size()));


        originalRecipes.forEach(originalRecipe -> {

            final Optional<Recipe> recipe = replayedRecipes.stream()
                    .filter(replayedRecipe -> originalRecipe.getId().equals(replayedRecipe.getId()))
                    .findFirst();

            assertThat("Failed to replay recipe " + originalRecipe.getName(), recipe.isPresent(), is(true));
        });

    }

    private void addEventsToEventLog(final int numberOfStreams, final int numberOfEventsPerStream) throws InvalidPositionException {
        final CakeshopEventGenerator cakeshopEventGenerator = new CakeshopEventGenerator();


        for (int seed = 0; seed < numberOfStreams; seed++) {

            final PositionInStreamIterator positionInStreamIterator = new PositionInStreamIterator();

            final Event recipeAddedEvent = cakeshopEventGenerator.createRecipeAddedEvent(seed, positionInStreamIterator);
            final UUID recipeId = recipeAddedEvent.getStreamId();

            eventStreamJdbcRepository.insert(recipeId);
            eventJdbcRepository.insert(recipeAddedEvent);

            for (int renameNumber = 1; renameNumber < numberOfEventsPerStream; renameNumber++) {
                final Event recipeRenamedEvent = cakeshopEventGenerator.createRecipeRenamedEvent(recipeId, seed, renameNumber, positionInStreamIterator);
                eventJdbcRepository.insert(recipeRenamedEvent);
            }
        }
    }

    private Optional<Integer> checkExpectedNumberOfRecipes(final int numberOfStreams) {
        return poller.pollUntilFound(() -> {
            final int numberOfRecipes = recipeTableInspector.countNumberOfRecipes();

            if (numberOfRecipes == numberOfStreams) {
                return of(numberOfRecipes);
            }

            return empty();
        });
    }

    private void cleanViewstoreTables() {

        final String contextName = "framework";

        databaseCleaner.cleanViewStoreTables(contextName,
                "ingredient",
                "recipe",
                "cake",
                "cake_order",
                "processed_event",
                "shuttered_command_store",
                "stream_status"
        );
    }
}
