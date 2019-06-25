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
import uk.gov.justice.services.eventstore.management.indexer.commands.IndexerCatchupCommand;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeshopEventGenerator;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.IndexerTableInspector;
import uk.gov.justice.services.example.cakeshop.it.helpers.PositionInStreamIterator;
import uk.gov.justice.services.example.cakeshop.it.helpers.ProcessedEventCounter;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexerCatchUpIT {
    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final IndexerTableInspector indexerTableInspector = new IndexerTableInspector(viewStoreDataSource);
    private final ProcessedEventCounter processedEventCounter = new ProcessedEventCounter(viewStoreDataSource);

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
    public void shouldReplayAndFindIndexesCreated() throws Exception {

        final int numberOfStreams = 1000;

        final List<UUID> streamIds = addEventsToEventLog(numberOfStreams);

        if (streamIds.size()==numberOfStreams) {
            System.out.println("Inserted " + numberOfStreams + " events");
        } else {
            fail("Failed to insert " + numberOfStreams + " events");
        }

        final Optional<Integer> numberOfEvents = longPoller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();
            if (eventCount == numberOfStreams) {
                return of(eventCount);
            }

            return empty();
        });

        if (numberOfEvents.isPresent()) {
            System.out.println("Inserted " + numberOfEvents.get() + " events");
        } else {
            fail("Failed to insert " + numberOfStreams + " events");
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

        final Optional<Integer> numberOfIndexesCreatedOptional = checkExpectedNumberOfIndexesCreated(numberOfStreams);

        if (!numberOfIndexesCreatedOptional.isPresent()) {
            fail();
        }
    }

    private List<UUID> addEventsToEventLog(final int numberOfStreams) throws InvalidPositionException {

        final CakeshopEventGenerator cakeshopEventGenerator = new CakeshopEventGenerator();

        final List<UUID> streamIds = new ArrayList<>();
        for (int i = 0; i < numberOfStreams; i++) {

            final PositionInStreamIterator positionInStreamIterator = new PositionInStreamIterator();

            final Event cakeOrderedEvent = cakeshopEventGenerator.createCakeOrderedEvent(positionInStreamIterator);
            final UUID cakeId = cakeOrderedEvent.getStreamId();

            streamIds.add(cakeId);

            eventStreamJdbcRepository.insert(cakeId);
            eventJdbcRepository.insert(cakeOrderedEvent);
        }
        return streamIds;

    }

    private void runCatchup() throws Exception {

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(HOST, PORT)) {

            systemCommanderClient.getRemote().call(new IndexerCatchupCommand());
        }
    }

    private Optional<Integer> checkExpectedNumberOfIndexesCreated(final int numberOfStreams) {
        return longPoller.pollUntilFound(() -> {
            final int numberOfIndexes = indexerTableInspector.countNumberOfCreatedIndexes();

            if (numberOfIndexes == numberOfStreams) {
                return of(numberOfIndexes);
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
                "index"
        );

        databaseCleaner.cleanStreamBufferTable(contextName);
        databaseCleaner.cleanStreamStatusTable(contextName);
    }
}
