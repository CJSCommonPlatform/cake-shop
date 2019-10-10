package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventStreamJdbsRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.exception.InvalidPositionException;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeshopEventGenerator;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.PositionInStreamIterator;
import uk.gov.justice.services.example.cakeshop.it.helpers.ProcessedEventCounter;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.api.command.EventCatchupCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatchupPerformanceIT {

    private static final String CONTEXT_NAME = "example";

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final ProcessedEventCounter processedEventCounter = new ProcessedEventCounter(viewStoreDataSource);

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));

    private final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    private final Poller longPoller = new Poller(1200, 1000L);

    private Client client;

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();

        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();
        databaseCleaner.cleanSystemTables(contextName);
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

        addEventsToEventLog(numberOfStreams, numberOfEventsPerStream);

        final Optional<Integer> numberOfEvents = longPoller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();
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

        cleanViewstoreTables();

        longPoller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();
            if (eventCount == 0) {
                return of(eventCount);
            }

            return empty();
        });

        try (final Connection connection = eventStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT (*) FROM publish_queue");
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            int eventCount = -1;
            if (resultSet.next()) {
              eventCount = resultSet.getInt(1);
            }

            assertThat(eventCount, is(0));
        }

        runCatchup();

        final Optional<Integer> numberOfReplayedEvents = longPoller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();
            if (eventCount == totalEvents) {
                return of(eventCount);
            }

            return empty();
        });

        if (numberOfReplayedEvents.isPresent()) {
            System.out.println("Successfully caught up " + numberOfEvents.get() + " events");
        } else {
            fail("Failed to catchup " + totalEvents + " events.");
        }
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

    private void runCatchup() throws Exception {

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters)) {

            systemCommanderClient.getRemote(CONTEXT_NAME).call(new EventCatchupCommand());
        }
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
