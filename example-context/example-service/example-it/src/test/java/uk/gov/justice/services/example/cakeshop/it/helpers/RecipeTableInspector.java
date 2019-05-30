package uk.gov.justice.services.example.cakeshop.it.helpers;


import static java.lang.String.format;

import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

public class RecipeTableInspector {

    private final DataSource viewStoreDataSource;

    public RecipeTableInspector(final DataSource viewStoreDataSource) {
        this.viewStoreDataSource = viewStoreDataSource;
    }

    public void truncateViewstoreTables() {

        final List<String> tableNames = new ArrayList<>();

        tableNames.add("ingredient");
        tableNames.add("recipe");
        tableNames.add("cake");
        tableNames.add("cake_order");
        tableNames.add("processed_event");
        tableNames.add("stream_buffer");
        tableNames.add("stream_status");

        tableNames.forEach(this::truncateTable);
    }

    private void truncateTable(final String tableName) {

        final String sql = "DELETE FROM " + tableName;

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }

    public int countNumberOfRecipes() {

        final String sql = "SELECT COUNT (*) FROM recipe";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            final ResultSet resultSet = preparedStatement.executeQuery()) {

            resultSet.next();

            return resultSet.getInt(1);
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }

    public List<Recipe> getAllRecipes() {
        final String sql = "SELECT id, name, gluten_free, photo_id FROM recipe";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql);
            final ResultSet resultSet = preparedStatement.executeQuery()) {


            final List<Recipe> recipes = new ArrayList<>();
            while (resultSet.next()) {
                final UUID id = (UUID) resultSet.getObject("id");
                final String name = resultSet.getString("name");
                final boolean glutenFree = resultSet.getBoolean("gluten_free");
                final UUID photoId = (UUID) resultSet.getObject("photo_id");

                final Recipe recipe = new Recipe(id, name, glutenFree, photoId);

                recipes.add(recipe);
            }

            return recipes;
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the view store", e);
        }
    }

    public long countEventsPerStream(final UUID streamId, final String componentName) {

        final String sql = "SELECT position FROM stream_status WHERE stream_id = ? AND component = ?";

        try(final Connection connection = viewStoreDataSource.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, streamId);
            preparedStatement.setString(2, componentName);

            try(final ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    return resultSet.getLong(1);
                } else {
                    throw new RuntimeException(format("No results returned from query '%s'", sql));
                }
            }

        } catch (final SQLException e) {
            throw new RuntimeException("Failed to run query '" + sql + "' against the event store", e);
        }
    }
}
