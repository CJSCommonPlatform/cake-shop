package uk.gov.justice.services.cakeshop.cakeshop.it.helpers;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.Event;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;

import java.util.List;
import java.util.stream.Stream;

public class EventFinder {

    private final EventJdbcRepository eventJdbcRepository;

    public EventFinder(final EventJdbcRepository eventJdbcRepository) {
        this.eventJdbcRepository = eventJdbcRepository;
    }

    public List<Event> eventsWithPayloadContaining(final String string) {
        try (final Stream<Event> events = eventJdbcRepository.findAll().filter(e -> e.getPayload().contains(string))) {
            return events.collect(toList());
        }
    }
}
