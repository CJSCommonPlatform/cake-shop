package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.eventstore.management.commands.ReplayEventToEventIndexerCommand.REPLAY_EVENT_TO_EVENT_INDEXER;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.ProcessedEventFinder;
import uk.gov.justice.services.example.cakeshop.it.helpers.PublishedEventInserter;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.subscription.ProcessedEvent;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Temporarily ignoring until the command is implemented in event-store. 10 April 2024")
public class SendSingleEventToEventIndexerIT {

    private static final String HOST = getHost();
    private static final int PORT = parseInt(getProperty("random.management.port"));
    private static final String CONTEXT_NAME = "example";

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final PublishedEventInserter publishedEventInserter = new PublishedEventInserter(eventStoreDataSource);
    private final ProcessedEventFinder processedEventFinder = new ProcessedEventFinder(viewStoreDataSource);
    private final Poller poller = new Poller();

    @Before
    public void cleanDatabases() {
        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();
        databaseCleaner.cleanSystemTables(contextName);
    }

    @Test
    public void shouldReplaySingleEventToEventIndexerUsingTheReplayEventToEventIndexerJmxCommand() throws Exception {

        final PublishedEvent publishedEvent = createPublishedEvent();
        publishedEventInserter.insert(publishedEvent);

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            final UUID commandRuntimeId = publishedEvent.getId();
            systemCommanderClient
                    .getRemote(CONTEXT_NAME)
                    .callWithRuntimeId(REPLAY_EVENT_TO_EVENT_INDEXER, commandRuntimeId);
        }

        final Optional<ProcessedEvent> processedEvent = poller.pollUntilFound(
                () -> processedEventFinder.findProcessedEvent(publishedEvent.getId())
        );

        if (processedEvent.isPresent()) {
            assertThat(processedEvent.get().getEventId(), is(publishedEvent.getId()));
            assertThat(processedEvent.get().getComponentName(), is("EVENT_INDEXER"));
        } else {
            fail();
        }
    }

    private PublishedEvent createPublishedEvent() {
        final UUID eventId = fromString("19adc152-89f7-4829-b41d-8e880d552b14");
        final UUID streamId = fromString("bf1f11c9-9164-4a36-bfac-a037b1ee5775");
        final Long positionInStream = 1L;
        final String eventName = "example.events.recipe-added";
        final String payload = "{\"recipeId\":\"bf1f11c9-9164-4a36-bfac-a037b1ee5775\",\"name\":\"Vanilla cake\",\"ingredients\":[{\"name\":\"vanilla\",\"quantity\":2}],\"glutenFree\":false}";
        final String metadata = "{\"createdAt\":\"2024-04-04T09:16:14.0Z\",\"id\":\"19adc152-89f7-4829-b41d-8e880d552b14\",\"name\":\"example.events.recipe-added\",\"causation\":[\"6020eb91-b02e-4224-aaf9-b58d56397980\",\"f79890bd-80a8-47df-acf0-a0ca3f84c315\"],\"stream\":{\"id\":\"bf1f11c9-9164-4a36-bfac-a037b1ee5775\",\"version\":1},\"source\":\"example\",\"event\":{\"eventNumber\":1,\"previousEventNumber\":0}}";
        final ZonedDateTime createdAt = ZonedDateTime.of(2024, 4, 4, 9, 16, 14, 0, UTC);
        final Long eventNumber = 1L;
        final Long previousEventNumber = 0L;

        return new PublishedEvent(
                eventId,
                streamId,
                positionInStream,
                eventName,
                metadata,
                payload,
                createdAt,
                eventNumber,
                previousEventNumber
        );
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
