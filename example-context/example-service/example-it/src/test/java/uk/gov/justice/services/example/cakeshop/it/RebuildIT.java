package uk.gov.justice.services.example.cakeshop.it;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;
import uk.gov.justice.services.eventstore.management.rebuild.commands.RebuildCommand;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandSender;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.SystemCommandMBeanClient;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.events.TestEventInserter;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.SequenceSetter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import org.junit.Before;
import org.junit.Test;

public class RebuildIT {

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final EventFactory eventFactory = new EventFactory();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final TestEventInserter testEventInserter = new TestEventInserter(eventStoreDataSource);

    private CommandSender commandSender;
    private final SequenceSetter sequenceSetter = new SequenceSetter();

    private final Poller poller = new Poller();

    private final SystemCommandMBeanClient systemCommandMBeanClient = new SystemCommandMBeanClient();

    @Before
    public void before() throws Exception {
        final Client client = new RestEasyClientFactory().createResteasyClient();
        commandSender = new CommandSender(client, eventFactory);

        databaseCleaner.cleanEventStoreTables("framework");
        databaseCleaner.cleanViewStoreTables("framework", "cake", "cake_order", "recipe", "ingredient", "processed_event");
    }

    @Test
    public void shouldRenumberTheEventLogTableAndRebuldPublishedEvents() throws Exception {

        final long startNumber = 1000L;
        sequenceSetter.setSequenceTo(startNumber, "event_sequence_seq", eventStoreDataSource);

        commandSender.addRecipe(randomUUID().toString(), "cake 1");
        commandSender.addRecipe(randomUUID().toString(), "cake 2");
        commandSender.addRecipe(randomUUID().toString(), "cake 3");

        final List<PublishedEvent> publishedEvents = getPublishedEvents();

        assertThat(publishedEvents.size(), is(3));
        assertThat(publishedEvents.get(0).getEventNumber(), is(of(startNumber)));

        assertThat(eventNumbersLinkedCorrectly(publishedEvents), is(true));

        final List<UUID> eventIds = publishedEvents.stream()
                .map(Event::getId)
                .collect(toList());

        invokeRebuild();

        final List<PublishedEvent> rebuiltEvents = getPublishedEvents();
        assertThat(rebuiltEvents.size(), is(3));

        final List<UUID> rebuiltEventIds = rebuiltEvents.stream()
                .map(Event::getId)
                .collect(toList());

        assertThat(rebuiltEvents.get(0).getEventNumber(), is(of(1L)));

        assertThat(rebuiltEventIds, hasItem(eventIds.get(0)));
        assertThat(rebuiltEventIds, hasItem(eventIds.get(1)));
        assertThat(rebuiltEventIds, hasItem(eventIds.get(2)));

        assertThat(eventNumbersLinkedCorrectly(rebuiltEvents), is(true));
    }

    private void invokeRebuild() throws Exception {
        systemCommandMBeanClient.getMbeanProxy().runCommand(new RebuildCommand());
    }

    private List<PublishedEvent> getPublishedEvents() {

        final Optional<List<PublishedEvent>> publishedEvents = poller.pollUntilFound(() -> {

            final List<PublishedEvent> events = doGetPublishedEvents();

            if (events.size() == 3) {
                return of(events);
            }

            return empty();
        });

        if (publishedEvents.isPresent()) {
            return publishedEvents.get();
        }

        fail();

        return new ArrayList<>();
    }

    private List<PublishedEvent> doGetPublishedEvents()  {
        try {
            return testEventInserter.findAllPublishedEventsOrderedByEventNumber();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get published events", e);
        }
    }

    private boolean eventNumbersLinkedCorrectly(final List<PublishedEvent> publishedEvents) {

        long previousEventNumber = 0L;

        for(final PublishedEvent publishedEvent: publishedEvents) {

            if(publishedEvent.getPreviousEventNumber() != previousEventNumber) {
                return false;
            }

            previousEventNumber = publishedEvent.getEventNumber().orElse(-1L);

            if (previousEventNumber == -1L) {

                return false;
            }
        }

        return true;
    }
}
