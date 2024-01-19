package uk.gov.justice.services.cakeshop.it.helpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

public class PublishedEventCounter {

    private final DataSource eventStoreDataSource;

    public PublishedEventCounter(final DataSource eventStoreDataSource) {
        this.eventStoreDataSource = eventStoreDataSource;
    }


    public int countPublishedEvents() {

        final String sql = "SELECT COUNT (*) FROM published_event";

        try(final Connection connection = eventStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();

            return resultSet.getInt(1);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the event store", e);
        }
    }
}
