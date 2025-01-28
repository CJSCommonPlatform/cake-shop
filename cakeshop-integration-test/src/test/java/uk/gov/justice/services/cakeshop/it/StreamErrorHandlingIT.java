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
import uk.gov.justice.services.test.utils.core.messaging.Poller;

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

    private final Poller poller = new Poller(20, 1000L);

    private Client client;

    @BeforeEach
    public void before() throws Exception {
        client = new RestEasyClientFactory().createResteasyClient();
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
            assertThat(streamError.exceptionClassName(), is("javax.persistence.PersistenceException"));
            assertThat(streamError.exceptionMessage(), is("org.hibernate.exception.ConstraintViolationException: could not execute statement"));
            assertThat(streamError.causeClassName(), is(of("org.postgresql.util.PSQLException")));
            assertThat(streamError.causeMessage().get(), startsWith("ERROR: null value in column \"name\" violates not-null constraint"));
            assertThat(streamError.javaClassname(), is("uk.gov.justice.services.persistence.EntityManagerFlushInterceptor"));

            final Optional<StreamStatus> streamStatus = poller.pollUntilFound(() -> findStreamStatus(streamError.streamId()));
            if (streamStatus.isPresent()) {
                assertThat(streamStatus.get().streamErrorId(), is(streamError.id()));
                assertThat(streamStatus.get().streamErrorPosition(), is(streamError.positionInStream()));
                assertThat(streamStatus.get().streamId(), is(streamError.streamId()));

            } else {
                fail("Could not find stream status for streamId: " + streamError.streamId());
            }
        } else {
            fail("Failed to find stream error for event named '" + eventName + "' in stream_error table");
        }
    }

    private Optional<StreamError> findStreamError(final String eventName) {

        final String SELECT_SQL = """
                    SELECT
                    id,
                    hash,
                    exception_classname,
                    exception_message,
                    cause_classname,
                    cause_message,
                    java_classname,
                    java_method,
                    java_line_number,
                    event_name,
                    event_id,
                    stream_id,
                    position_in_stream,
                    date_created,
                    full_stack_trace
                FROM stream_error
                WHERE event_name = ?""";

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setString(1, eventName);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    final UUID id = (UUID) resultSet.getObject("id");
                    final String hash = resultSet.getString("hash");
                    final String javaClassName = resultSet.getString("java_classname");
                    final String exceptionClassname = resultSet.getString("exception_classname");
                    final String exceptionMessage = resultSet.getString("exception_message");
                    final Optional<String> causeClassName = ofNullable(resultSet.getString("cause_classname"));
                    final Optional<String> causeMessage = ofNullable(resultSet.getString("cause_message"));
                    final String javaMethod = resultSet.getString("java_method");
                    final int javaLineNumber = resultSet.getInt("java_line_number");
                    final String theEventName = resultSet.getString("event_name");
                    final UUID eventId = (UUID) resultSet.getObject("event_id");
                    final UUID streamId = (UUID) resultSet.getObject("stream_id");
                    final Long positionInStream = resultSet.getLong("position_in_stream");
                    final ZonedDateTime dateCreated = ZonedDateTimes.fromSqlTimestamp(resultSet.getTimestamp("date_created"));
                    final String stackTrace = resultSet.getString("full_stack_trace");

                    final StreamError streamError = new StreamError(
                            id,
                            hash,
                            exceptionClassname,
                            exceptionMessage,
                            causeClassName,
                            causeMessage,
                            javaClassName,
                            javaMethod,
                            javaLineNumber,
                            theEventName,
                            eventId,
                            streamId,
                            positionInStream,
                            dateCreated,
                            stackTrace
                    );

                    return of(streamError);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to read from stream error table", e);
        }

        return empty();
    }

    private Optional<StreamStatus> findStreamStatus(final UUID streamId) {

        final String SELECT_SQL = """
                    SELECT
                    stream_id,
                    stream_error_id,
                    stream_error_position
                FROM stream_status
                WHERE stream_id = ?""";

        try (final Connection connection = viewStoreDataSource.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
            preparedStatement.setObject(1, streamId);

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

