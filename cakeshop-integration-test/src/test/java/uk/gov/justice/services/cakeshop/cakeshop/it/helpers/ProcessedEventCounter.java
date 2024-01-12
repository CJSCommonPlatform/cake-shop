package uk.gov.justice.services.cakeshop.cakeshop.it.helpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

public class ProcessedEventCounter {

    private final DataSource viewStoreDataSource;

    public ProcessedEventCounter(final DataSource viewStoreDataSource) {
        this.viewStoreDataSource = viewStoreDataSource;
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
}
