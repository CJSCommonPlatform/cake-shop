package uk.gov.justice.services.example.cakeshop.it.helpers;

import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.PublishedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

public class PublishedEventInserter {

    private final DataSource eventStoreDataSource;

    public PublishedEventInserter(final DataSource eventStoreDataSource) {
        this.eventStoreDataSource = eventStoreDataSource;
    }

    public void insert(final PublishedEvent publishedEvent) {

        final String sql = "INSERT INTO published_event (" +
                "id," +
                "stream_id," +
                "position_in_stream," +
                "name," +
                "payload," +
                "metadata," +
                "date_created," +
                "event_number," +
                "previous_event_number) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        try(final Connection connection = eventStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, publishedEvent.getId());
            preparedStatement.setObject(2, publishedEvent.getStreamId());
            preparedStatement.setLong(3, publishedEvent.getPositionInStream());
            preparedStatement.setString(4, publishedEvent.getName());
            preparedStatement.setString(5, publishedEvent.getPayload());
            preparedStatement.setString(6, publishedEvent.getMetadata());
            preparedStatement.setTimestamp(7, toSqlTimestamp(publishedEvent.getCreatedAt()));
            preparedStatement.setLong(8, publishedEvent.getEventNumber().orElse(null));
            preparedStatement.setLong(9, publishedEvent.getPreviousEventNumber());

            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the event store", e);
        }
    }
}
