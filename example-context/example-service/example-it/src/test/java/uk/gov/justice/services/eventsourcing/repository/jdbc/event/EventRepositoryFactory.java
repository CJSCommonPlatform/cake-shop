package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.eventsourcing.repository.jdbc.AnsiSQLEventLogInsertionStrategy;
import uk.gov.justice.services.jdbc.persistence.JdbcResultSetStreamer;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.test.utils.persistence.TestEventStoreDataSourceFactory;

import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;

public class EventRepositoryFactory {

    public EventJdbcRepository getEventJdbcRepository(final DataSource dataSource) throws SQLException, LiquibaseException {
        final EventJdbcRepository eventJdbcRepository = new EventJdbcRepository(
                new AnsiSQLEventLogInsertionStrategy(),
                new JdbcResultSetStreamer(),
                new PreparedStatementWrapperFactory(),
                new TestEventStoreDataSourceFactory().createDataSource("frameworkeventstore"),
                getLogger(EventJdbcRepository.class)
        );

        setField(eventJdbcRepository, "dataSource", dataSource);

        return eventJdbcRepository;
    }
}
