package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
import uk.gov.justice.services.example.cakeshop.it.helpers.PublishedEventCounter;
import uk.gov.justice.services.example.cakeshop.it.helpers.RecipeTableInspector;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatchupPerformanceIT {

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final RecipeTableInspector recipeTableInspector = new RecipeTableInspector(viewStoreDataSource);
    private final PublishedEventCounter publishedEventCounter = new PublishedEventCounter(eventStoreDataSource);

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));


    private final SystemCommanderClientFactory systemCommanderClientFactory = new SystemCommanderClientFactory();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    private final Poller longPoller = new Poller(1200, 1000L);

    private Client client;

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();

        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();
    }

    @After
    public void cleanup() {
        client.close();
    }

    @Test
    public void shouldReplayAndFindRecipesInViewStore() throws Exception {

        final int numberOfStreams = 10;
        final int numberOfEventsPerStream = 100;
        final int totalEvents = numberOfStreams * numberOfEventsPerStream;

        final List<UUID> streamIds = addEventsToEventLog(numberOfStreams, numberOfEventsPerStream);

        final Optional<Integer> numberOfEvents = longPoller.pollUntilFound(() -> {
            final int eventCount = publishedEventCounter.countPublishedEvents();
            if (eventCount == totalEvents) {
                return of(eventCount);
            }

            return empty();
        });

        if (numberOfEvents.isPresent()) {
            System.out.println("Inserted " + numberOfEvents.get() + " events");
        } else {
            fail("Failed to insert " + totalEvents + " events");
        }

        for (final UUID streamId : streamIds) {

            final Optional<Long> eventCount = longPoller.pollUntilFound(() -> {
                final long eventsPerStream = recipeTableInspector.countEventsPerStream(streamId);
                if (eventsPerStream == numberOfEventsPerStream) {
                    return of(eventsPerStream);
                }

                return empty();
            });

            if (!eventCount.isPresent()) {
                fail("Expected " + numberOfEventsPerStream + " events but found " + recipeTableInspector.countEventsPerStream(streamId) + " in stream " + streamId);
            }
        }

        cleanViewstoreTables();

        runCatchup();

        final Optional<Integer> numberOfReplayedRecipesOptional = checkExpectedNumberOfRecipes(numberOfStreams);

        if (!numberOfReplayedRecipesOptional.isPresent()) {
            fail();
        }


        for (final UUID streamId : streamIds) {

            final Optional<Long> eventCount = longPoller.pollUntilFound(() -> {
                final long eventsPerStream = recipeTableInspector.countEventsPerStream(streamId);
                if (eventsPerStream == numberOfEventsPerStream) {
                    return of(eventsPerStream);
                }

                return empty();
            });

            if (!eventCount.isPresent()) {
                fail();
            }
        }

        publishedEventCounter.truncatePublishQueue();
    }

    private List<UUID> addEventsToEventLog(final int numberOfStreams, final int numberOfEventsPerStream) throws InvalidPositionException {

        final CakeshopEventGenerator cakeshopEventGenerator = new CakeshopEventGenerator();

        final List<UUID> streamIds = new ArrayList<>();

        for (int seed = 0; seed < numberOfStreams; seed++) {

            final PositionInStreamIterator positionInStreamIterator = new PositionInStreamIterator();

            final Event recipeAddedEvent = cakeshopEventGenerator.createRecipeAddedEvent(seed, positionInStreamIterator);
            final UUID recipeId = recipeAddedEvent.getStreamId();

            streamIds.add(recipeId);

            eventStreamJdbcRepository.insert(recipeId);
            eventJdbcRepository.insert(recipeAddedEvent);

            for (int renameNumber = 1; renameNumber < numberOfEventsPerStream; renameNumber++) {
                final Event recipeRenamedEvent = cakeshopEventGenerator.createRecipeRenamedEvent(recipeId, seed, renameNumber, positionInStreamIterator);
                eventJdbcRepository.insert(recipeRenamedEvent);
            }
        }

        return streamIds;
    }

    private void runCatchup() throws Exception {

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(HOST, PORT)) {

            systemCommanderClient.getRemote().call(new CatchupCommand());
        }
    }

    private Optional<Integer> checkExpectedNumberOfRecipes(final int numberOfStreams) {
        return longPoller.pollUntilFound(() -> {
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
                "processed_event"
        );

        databaseCleaner.cleanStreamBufferTable(contextName);
        databaseCleaner.cleanStreamStatusTable(contextName);
    }
}
