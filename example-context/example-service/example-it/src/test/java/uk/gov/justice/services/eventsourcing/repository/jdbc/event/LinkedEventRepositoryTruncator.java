package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import java.sql.SQLException;

import javax.sql.DataSource;

public class LinkedEventRepositoryTruncator {

    private final DataSource datasource;
    private final PublishedEventInserter publishedEventInserter;

    public LinkedEventRepositoryTruncator(final DataSource datasource) {
        this.datasource = datasource;
        this.publishedEventInserter = new PublishedEventInserter();
    }

    public void truncate() throws SQLException {
        publishedEventInserter.truncate(datasource.getConnection());
    }
}
