package uk.gov.justice.services.example.cakeshop.it;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.management.JMX.newMBeanProxy;
import static org.junit.Assert.fail;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventStreamJdbsRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.LinkedEventRepositoryTruncator;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.exception.InvalidPositionException;
import uk.gov.justice.services.example.cakeshop.it.helpers.CakeshopEventGenerator;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.MBeanHelper;
import uk.gov.justice.services.example.cakeshop.it.helpers.PositionInStreamIterator;
import uk.gov.justice.services.example.cakeshop.it.helpers.PublishedEventCounter;
import uk.gov.justice.services.example.cakeshop.it.helpers.RecipeTableInspector;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.StandaloneStreamStatusJdbcRepositoryFactory;
import uk.gov.justice.services.jmx.Catchup;
import uk.gov.justice.services.jmx.CatchupMBean;
import uk.gov.justice.services.jmx.Shuttering;
import uk.gov.justice.services.jmx.ShutteringMBean;
import uk.gov.justice.services.test.utils.core.messaging.Poller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatchupPerformanceIT {

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);
    private final LinkedEventRepositoryTruncator linkedEventRepositoryTruncator = new LinkedEventRepositoryTruncator(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final StandaloneStreamStatusJdbcRepositoryFactory standaloneStreamStatusJdbcRepositoryFactory = new StandaloneStreamStatusJdbcRepositoryFactory();

    private final RecipeTableInspector recipeTableInspector = new RecipeTableInspector(viewStoreDataSource);
    private final PublishedEventCounter publishedEventCounter = new PublishedEventCounter(eventStoreDataSource);

    private final Poller longPoller = new Poller(1200, 1000L);

    private Client client;
    private MBeanHelper mBeanHelper;

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();
        mBeanHelper = new MBeanHelper();
    }

    @After
    public void cleanup() {
        client.close();
    }

    @After
    public void unShutter() throws Exception {

        try (final JMXConnector jmxConnector = mBeanHelper.getJMXConnector()) {
            final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

            final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

            mBeanHelper.getMbeanProxy(connection, objectName, ShutteringMBean.class).doUnshutteringRequested();
        }
    }

    @Test
    public void shouldReplayAndFindRecipesInViewStore() throws Exception {

        final int numberOfStreams = 10;
        final int numberOfEventsPerStream = 100;
        final int totalEvents = numberOfStreams * numberOfEventsPerStream;

        shutter();

        truncateEventLog();
        recipeTableInspector.truncateViewstoreTables();

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
        }   else {
            fail("Failed to insert " + totalEvents + " events");
        }


        recipeTableInspector.truncateViewstoreTables();

        runCatchup();

        final Optional<Integer> numberOfReplayedRecipesOptional = checkExpectedNumberOfRecipes(numberOfStreams);

        if (!numberOfReplayedRecipesOptional.isPresent()) {
            fail();
        }


        for(final UUID streamId: streamIds) {

            final Optional<Long> eventCount = longPoller.pollUntilFound(() -> {
                final long eventsPerStream = recipeTableInspector.countEventsPerStream(streamId);
                if (eventsPerStream == numberOfEventsPerStream) {
                    return of(eventsPerStream);
                }

                return empty();
            });

            if (! eventCount.isPresent()) {
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

    private void truncateEventLog() throws SQLException {
        final Stream<Event> eventStream = eventJdbcRepository.findAll();
        eventStream.forEach(event -> eventJdbcRepository.clear(event.getStreamId()));

        linkedEventRepositoryTruncator.truncate();
    }

    private void shutter() throws Exception {

        try (final JMXConnector jmxConnector = mBeanHelper.getJMXConnector()) {
            final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

            final ObjectName objectName = new ObjectName("shuttering", "type", Shuttering.class.getSimpleName());

            mBeanHelper.getMbeanProxy(connection, objectName, ShutteringMBean.class).doShutteringRequested();
        }
    }

    private void runCatchup() throws Exception {

        try (final JMXConnector jmxConnector = mBeanHelper.getJMXConnector()) {
            final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

            final ObjectName objectName = new ObjectName("catchup", "type", Catchup.class.getSimpleName());

            mBeanHelper.getMbeanDomains(connection);

            mBeanHelper.getMbeanOperations(objectName, connection);

            final CatchupMBean catchupMBean = newMBeanProxy(connection, objectName, CatchupMBean.class, true);

            catchupMBean.doCatchupRequested();
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
}
