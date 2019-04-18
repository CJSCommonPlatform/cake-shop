package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcResultSetStreamer;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.test.utils.persistence.TestEventStoreDataSourceFactory;

import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;

public class EventStreamJdbsRepositoryFactory {

    public EventStreamJdbcRepository getEventStreamJdbcRepository(final DataSource dataSource) throws SQLException, LiquibaseException {

        final EventStreamJdbcRepository eventStreamJdbcRepository = new EventStreamJdbcRepository(
                new JdbcResultSetStreamer(),
                new PreparedStatementWrapperFactory(),
                new TestEventStoreDataSourceFactory().createDataSource("frameworkeventstore"),
                new UtcClock());

        setField(eventStreamJdbcRepository, "dataSource", dataSource);

        return eventStreamJdbcRepository;
    }
}
