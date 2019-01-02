package uk.gov.justice.services.example.cakeshop.it.helpers;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.event.buffer.core.repository.subscription.StreamStatusJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryHelper;

import javax.sql.DataSource;

public class StandaloneStreamStatusJdbcRepositoryFactory {

    public StreamStatusJdbcRepository getSnapshotSubscriptionJdbcRepository(final DataSource dataSource) {
        final StreamStatusJdbcRepository snapshotJdbcRepository = new StreamStatusJdbcRepository();

        setField(snapshotJdbcRepository, "dataSource", dataSource);
        setField(snapshotJdbcRepository, "jdbcRepositoryHelper", new JdbcRepositoryHelper());

        return snapshotJdbcRepository;
    }
}
