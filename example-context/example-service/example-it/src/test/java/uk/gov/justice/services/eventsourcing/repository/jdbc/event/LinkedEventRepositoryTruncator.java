package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import java.sql.SQLException;

import javax.sql.DataSource;

public class LinkedEventRepositoryTruncator {

    private final DataSource datasource;
    private final LinkedEventJdbcRepository linkedEventJdbcRepository;

    public LinkedEventRepositoryTruncator(final DataSource datasource) {
        this.datasource = datasource;
        this.linkedEventJdbcRepository = new LinkedEventJdbcRepository();
    }

    public void truncate() throws SQLException {
        linkedEventJdbcRepository.truncate(datasource.getConnection());
    }
}
