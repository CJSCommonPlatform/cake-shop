package uk.gov.justice.services.eventsourcing.repository.jdbc.event;

import uk.gov.justice.services.eventsourcing.publishedevent.jdbc.DatabaseTableTruncator;

import java.sql.SQLException;

import javax.sql.DataSource;

public class PublishedEventTableTruncator {

    private final DataSource datasource;
    private final DatabaseTableTruncator databaseTableTruncator;

    public PublishedEventTableTruncator(final DataSource datasource) {
        this.datasource = datasource;
        this.databaseTableTruncator = new DatabaseTableTruncator();
    }

    public void truncate() throws SQLException {
        databaseTableTruncator.truncate("published_event", datasource);
    }
}
