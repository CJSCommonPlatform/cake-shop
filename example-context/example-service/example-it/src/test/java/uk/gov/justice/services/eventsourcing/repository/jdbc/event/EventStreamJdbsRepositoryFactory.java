package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.DefaultJdbcDataSourceProvider;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryHelper;

import javax.sql.DataSource;

public class EventStreamJdbsRepositoryFactory {

    public EventStreamJdbcRepository getEventStreamJdbcRepository(final DataSource dataSource) {

        final EventStreamJdbcRepository eventStreamJdbcRepository = new EventStreamJdbcRepository(
                new JdbcRepositoryHelper(),
                new DefaultJdbcDataSourceProvider(),
                new UtcClock(),
                null,
                getLogger(EventStreamJdbcRepository.class));

        setField(eventStreamJdbcRepository, "dataSource", dataSource);

        return eventStreamJdbcRepository;
    }
}
