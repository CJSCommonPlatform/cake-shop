package uk.gov.justice.services.cakeshop.it.helpers;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import uk.gov.justice.services.subscription.ProcessedEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public class ProcessedEventFinder {

    private final DataSource viewStoreDataSource;

    public ProcessedEventFinder(final DataSource viewStoreDataSource) {
        this.viewStoreDataSource = viewStoreDataSource;
    }

    public Optional<ProcessedEvent> findProcessedEvent(final UUID eventId) {

        final String sql = "SELECT " +
                "event_number, " +
                "previous_event_number, " +
                "source," +
                "component " +
                "FROM processed_event " +
                "WHERE event_id = ?";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, eventId);

            try(final ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    final long eventNumber = resultSet.getLong("event_number");
                    final long previousEventNumber = resultSet.getLong("previous_event_number");
                    final String source = resultSet.getString("source");
                    final String component = resultSet.getString("component");

                    final ProcessedEvent processedEvent = new ProcessedEvent(
                            eventId,
                            previousEventNumber,
                            eventNumber,
                            source,
                            component);

                    return of(processedEvent);
                }
            }

        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }

        return empty();
    }

    public int countProcessedEvents() {

        final String sql = "SELECT COUNT (*) FROM processed_event";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();

            return resultSet.getInt(1);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }

    public int countProcessedEventsForEventListener() {

        final String sql = "SELECT COUNT (*) FROM processed_event where component = 'EVENT_LISTENER'";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();

            return resultSet.getInt(1);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }
}

