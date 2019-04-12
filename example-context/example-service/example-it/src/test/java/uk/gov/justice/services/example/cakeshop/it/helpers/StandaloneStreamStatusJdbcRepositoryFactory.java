package uk.gov.justice.services.example.cakeshop.it.helpers;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.event.buffer.core.repository.subscription.StreamStatusJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryHelper;

import javax.sql.DataSource;

public class StandaloneStreamStatusJdbcRepositoryFactory {

    public StreamStatusJdbcRepository getStreamStatusJdbcRepository(final DataSource dataSource) {
        final StreamStatusJdbcRepository streamStatusJdbcRepository = new StreamStatusJdbcRepository();

        setField(streamStatusJdbcRepository, "dataSource", dataSource);
        setField(streamStatusJdbcRepository, "jdbcRepositoryHelper", new JdbcRepositoryHelper());

        return streamStatusJdbcRepository;
    }
}
