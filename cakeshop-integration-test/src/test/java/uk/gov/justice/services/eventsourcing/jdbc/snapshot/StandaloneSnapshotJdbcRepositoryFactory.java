package uk.gov.justice.services.eventsourcing.jdbc.snapshot;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.test.utils.persistence.SettableEventStoreDataSourceProvider;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StandaloneSnapshotJdbcRepositoryFactory {

    public SnapshotJdbcRepository getSnapshotJdbcRepository(final DataSource dataSource) {

        final SnapshotJdbcRepository snapshotJdbcRepository = new SnapshotJdbcRepository();

        final SettableEventStoreDataSourceProvider eventStoreDataSourceProvider = new SettableEventStoreDataSourceProvider();
        final Logger logger = LoggerFactory.getLogger(SnapshotJdbcRepository.class);

        eventStoreDataSourceProvider.setDataSource(dataSource);

        setField(snapshotJdbcRepository, "eventStoreDataSourceProvider", eventStoreDataSourceProvider);
        setField(snapshotJdbcRepository, "logger", logger);

        return snapshotJdbcRepository;
    }
}
