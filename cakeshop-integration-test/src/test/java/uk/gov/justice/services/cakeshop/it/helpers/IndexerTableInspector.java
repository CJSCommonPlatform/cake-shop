package uk.gov.justice.services.cakeshop.it.helpers;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

public class IndexerTableInspector {

    private final DataSource viewStoreDataSource;

    public IndexerTableInspector(final DataSource viewStoreDataSource) {
        this.viewStoreDataSource = viewStoreDataSource;
    }

    public int countNumberOfCreatedIndexes() {

        final String sql = "SELECT COUNT (*) FROM index";

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(sql);
             final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();

            return resultSet.getInt(1);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }

}
