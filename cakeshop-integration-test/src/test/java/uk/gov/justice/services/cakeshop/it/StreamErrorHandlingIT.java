package uk.gov.justice.services.cakeshop.it;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.cakeshop.it.params.CakeShopUris.RECIPES_RESOURCE_URI;

import uk.gov.justice.services.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.cakeshop.it.helpers.EventFactory;
import uk.gov.justice.services.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.event.buffer.core.repository.streamerror.StreamError;
import uk.gov.justice.services.event.buffer.core.repository.streamerror.StreamErrorDetails;
import uk.gov.justice.services.event.buffer.core.repository.streamerror.StreamErrorHash;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StreamErrorHandlingIT {

    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventFactory eventFactory = new EventFactory();
    final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    private final Poller poller = new Poller(20, 1000L);

    private Client client;

    @BeforeEach
    public void before() throws Exception {
        client = new RestEasyClientFactory().createResteasyClient();

        databaseCleaner.cleanEventStoreTables("framework");

        databaseCleaner.cleanStreamBufferTable("framework");
        databaseCleaner.cleanStreamStatusTable("framework");
        databaseCleaner.cleanViewStoreTables(
                "framework",
                "stream_error_hash",
                "stream_error",
                "cake",
                "cake_order",
                "recipe",
                "ingredient",
                "processed_event");
    }

    @AfterEach
    public void cleanup() throws Exception {
        client.close();
    }


    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void shouldAddRowInStreamErrorTableOnEventProcessingFailure() throws Exception {
        final String recipeId = "6a710473-2af4-44a9-99f1-4c27632d5b23";
        final String recipeName = "DELIBERATELY_FAIL";
        final String eventName = "cakeshop.events.recipe-added";

        final Entity<String> recipeEntity = eventFactory.recipeEntity(recipeName, false);
        final Invocation.Builder request = client.target(RECIPES_RESOURCE_URI + recipeId).request();
        try (final Response response = request.post(recipeEntity)) {
            assertThat(response.getStatus(), is(202));
        }

        final Optional<StreamError> streamErrorOptional = poller.pollUntilFound(() -> findStreamError(eventName));

        if (streamErrorOptional.isPresent()) {
            final StreamError streamError = streamErrorOptional.get();
            final StreamErrorDetails streamErrorDetails = streamError.streamErrorDetails();
            final StreamErrorHash streamErrorHash = streamError.streamErrorHash();

            assertThat(streamErrorHash.exceptionClassName(), is("javax.persistence.PersistenceException"));
            assertThat(streamErrorHash.causeClassName(), is(of("org.postgresql.util.PSQLException")));
            assertThat(streamErrorHash.javaClassName(), is("uk.gov.justice.services.persistence.EntityManagerFlushInterceptor"));

            assertThat(streamErrorDetails.exceptionMessage(), is("org.hibernate.exception.ConstraintViolationException: could not execute statement"));
            assertThat(streamErrorDetails.causeMessage().get(), startsWith("ERROR: null value in column \"name\" violates not-null constraint"));

            final Optional<StreamStatus> streamStatus = poller.pollUntilFound(() -> findStreamStatus(streamErrorDetails.streamId(), "cakeshop", "EVENT_LISTENER"));
            if (streamStatus.isPresent()) {
                assertThat(streamStatus.get().streamErrorId(), is(streamErrorDetails.id()));
                assertThat(streamStatus.get().streamErrorPosition(), is(streamErrorDetails.positionInStream()));
                assertThat(streamStatus.get().streamId(), is(streamErrorDetails.streamId()));

            } else {
                fail("Could not find stream status for streamId: " + streamErrorDetails.streamId());
            }
        } else {
            fail("Failed to find stream error for event named '" + eventName + "' in stream_error table");
        }
    }

    private Optional<StreamError> findStreamError(final String eventName) {

        final Optional<StreamErrorDetails> streamErrorDetails = findStreamErrorDetails(eventName);

        if (streamErrorDetails.isPresent()) {
            final Optional<StreamErrorHash> streamErrorHash = findStreamErrorHash(streamErrorDetails.get().hash());

            if (streamErrorHash.isPresent()) {
                return of(new StreamError(streamErrorDetails.get(), streamErrorHash.get()));
            }
        }

        return empty();
    }

    private Optional<StreamErrorDetails> findStreamErrorDetails(final String eventName) {
        final String SELECT_SQL = """
                    SELECT
                    id,
                    hash,
                    exception_message,
                    cause_message,
                    event_id,
                    stream_id,
                    position_in_stream,
                    date_created,
                    full_stack_trace,
                    component_name,
                    source
                FROM stream_error
                WHERE event_name = ?""";

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setString(1, eventName);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final UUID id = (UUID) resultSet.getObject("id");
                    final String hash = resultSet.getString("hash");
                    final String exceptionMessage = resultSet.getString("exception_message");
                    final Optional<String> causeMessage = ofNullable(resultSet.getString("cause_message"));
                    final UUID eventId = (UUID) resultSet.getObject("event_id");
                    final UUID streamId = (UUID) resultSet.getObject("stream_id");
                    final Long positionInStream = resultSet.getLong("position_in_stream");
                    final ZonedDateTime dateCreated = ZonedDateTimes.fromSqlTimestamp(resultSet.getTimestamp("date_created"));
                    final String stackTrace = resultSet.getString("full_stack_trace");
                    final String componentName = resultSet.getString("component_name");
                    final String source = resultSet.getString("source");

                    final StreamErrorDetails streamError = new StreamErrorDetails(
                            id,
                            hash,
                            exceptionMessage,
                            causeMessage,
                            eventName,
                            eventId,
                            streamId,
                            positionInStream,
                            dateCreated,
                            stackTrace,
                            componentName,
                            source
                    );

                    return of(streamError);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to read from stream error table", e);
        }

        return empty();
    }

    private Optional<StreamErrorHash> findStreamErrorHash(final String hash) {

        final String SELECT_SQL = """
                        SELECT
                            exception_classname,
                            cause_classname,
                            java_classname,
                            java_method,
                            java_line_number
                        FROM stream_error_hash
                        WHERE hash = ?
                """;

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setString(1, hash);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final String exceptionClassname = resultSet.getString("exception_classname");
                    final Optional<String> causeClassname = ofNullable(resultSet.getString("cause_classname"));
                    final String javaClassname = resultSet.getString("java_classname");
                    final String javaMethod = resultSet.getString("java_method");
                    final int javaLineNumber = resultSet.getInt("java_line_number");

                    final StreamErrorHash streamErrorHash = new StreamErrorHash(
                            hash,
                            exceptionClassname,
                            causeClassname,
                            javaClassname,
                            javaMethod,
                            javaLineNumber
                    );

                    return of(streamErrorHash);
                }

                return empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read from stream_error table", e);
        }
    }

    private Optional<StreamStatus> findStreamStatus(final UUID streamId, final String source, final String component) {

        final String SELECT_SQL = """
                    SELECT
                    stream_id,
                    stream_error_id,
                    stream_error_position
                FROM stream_status
                WHERE stream_id = ?
                AND source = ?
                AND component = ?""";

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setObject(1, streamId);
            preparedStatement.setString(2, source);
            preparedStatement.setString(3, component);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final StreamStatus streamStatus = new StreamStatus(
                            (UUID) resultSet.getObject("stream_id"),
                            (UUID) resultSet.getObject("stream_error_id"),
                            resultSet.getLong("stream_error_position")
                    );

                    return of(streamStatus);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to read from stream status table", e);
        }

        return empty();
    }
}

record StreamStatus(UUID streamId, UUID streamErrorId, Long streamErrorPosition) {

}

