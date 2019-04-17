package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcResultSetStreamer;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;

import javax.sql.DataSource;

public class EventStreamJdbsRepositoryFactory {

    public EventStreamJdbcRepository getEventStreamJdbcRepository(final DataSource dataSource) {

        final EventStreamJdbcRepository eventStreamJdbcRepository = new EventStreamJdbcRepository(
                new JdbcResultSetStreamer(),
                new PreparedStatementWrapperFactory(),
                dataSource,
                new UtcClock());

        setField(eventStreamJdbcRepository, "dataSource", dataSource);

        return eventStreamJdbcRepository;
    }
}
