package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcResultSetStreamer;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.test.utils.persistence.SettableEventStoreDataSourceProvider;

import javax.sql.DataSource;

public class EventStreamJdbsRepositoryFactory {

    public EventStreamJdbcRepository getEventStreamJdbcRepository(final DataSource dataSource) {

        final EventStreamJdbcRepository eventStreamJdbcRepository = new EventStreamJdbcRepository();

        final SettableEventStoreDataSourceProvider eventStoreDataSourceProvider = new SettableEventStoreDataSourceProvider();
        eventStoreDataSourceProvider.setDataSource(dataSource);

        setField(eventStreamJdbcRepository, "jdbcResultSetStreamer", new JdbcResultSetStreamer());
        setField(eventStreamJdbcRepository, "preparedStatementWrapperFactory", new PreparedStatementWrapperFactory());
        setField(eventStreamJdbcRepository, "eventStoreDataSourceProvider", eventStoreDataSourceProvider);
        setField(eventStreamJdbcRepository, "clock", new UtcClock());

        return eventStreamJdbcRepository;
    }
}
