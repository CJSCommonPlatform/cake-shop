package uk.gov.justice.services.cakeshop.it;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.cakeshop.it.helpers.JmxParametersFactory.buildJmxParameters;
import static uk.gov.justice.services.cakeshop.it.helpers.TestConstants.CONTEXT_NAME;
import static uk.gov.justice.services.cakeshop.it.helpers.TestConstants.DB_CONTEXT_NAME;
import static uk.gov.justice.services.cakeshop.it.params.CakeShopMediaTypes.ADD_RECIPE_MEDIA_TYPE;
import static uk.gov.justice.services.cakeshop.it.params.CakeShopUris.RECIPES_RESOURCE_URI;
import static uk.gov.justice.services.eventstore.management.commands.EventCatchupCommand.CATCHUP;
import static uk.gov.justice.services.jmx.api.mbean.CommandRunMode.GUARDED;
import static uk.gov.justice.services.jmx.api.parameters.JmxCommandRuntimeParameters.withNoCommandParameters;
import static uk.gov.justice.services.test.utils.core.matchers.HttpStatusCodeMatcher.isStatus;

import uk.gov.justice.services.cakeshop.it.helpers.CommandFactory;
import uk.gov.justice.services.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.cakeshop.it.helpers.JmxParametersFactory;
import uk.gov.justice.services.cakeshop.it.helpers.ProcessedEventFinder;
import uk.gov.justice.services.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.api.parameters.JmxCommandRuntimeParameters;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.SequenceSetter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventHealingIT {

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final ProcessedEventFinder processedEventFinder = new ProcessedEventFinder(viewStoreDataSource);

    private final CommandFactory commandFactory = new CommandFactory();

    private final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SequenceSetter sequenceSetter = new SequenceSetter();

    private final Poller poller = new Poller(50, 1000);

    private Client client;

    @BeforeEach
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();

        databaseCleaner.cleanEventStoreTables(DB_CONTEXT_NAME);
        cleanViewstoreTables();
        databaseCleaner.cleanSystemTables(DB_CONTEXT_NAME);

        sequenceSetter.setSequenceTo(1L, "event_sequence_seq", eventStoreDataSource);
    }

    @AfterEach
    public void cleanup() {
        client.close();
    }

    @Test
    public void shouldReplayAndFindRecipesInViewStore() throws Exception {


        final int numberOfRecipes = 10;

        final List<UUID> recipeIds = new ArrayList<>();

        for (int i = 0; i < numberOfRecipes; i++) {

            final UUID recipeId = randomUUID();

            recipeIds.add(recipeId);

            final Response response = client
                    .target(RECIPES_RESOURCE_URI + recipeId)
                    .request()
                    .post(entity(commandFactory.addRecipeCommandByName("recipe " + (i + 1)),
                            ADD_RECIPE_MEDIA_TYPE));

            assertThat(response.getStatus(), isStatus(ACCEPTED));
        }

        final Optional<Integer> result = poller.pollUntilFound(() -> {
            final int eventCount = processedEventFinder.countProcessedEventsForEventListener();
            System.out.printf("Polling processed_event table. Expected events count: %d, found: %d\n", numberOfRecipes, eventCount);
            if (eventCount == numberOfRecipes) {
                return of(eventCount);
            }

            return empty();
        });

        assertTrue(result.isPresent());

        removeRecipesFromViewStore(3, findRecipeIdForEventNumber(3));
        removeRecipesFromViewStore(5, findRecipeIdForEventNumber(5));
        removeRecipesFromViewStore(6, findRecipeIdForEventNumber(6));
        removeRecipesFromViewStore(7, findRecipeIdForEventNumber(7));

        runCatchup();

        final Optional<Integer> numberOfEventsInProcessedEventTable = poller.pollUntilFound(() -> {
            final int eventCount = processedEventFinder.countProcessedEventsForEventListener();
            System.out.printf("Polling processed_event table. Expected events count: %d, found: %d\n", numberOfRecipes, eventCount);
            if (eventCount == numberOfRecipes) {
                return of(eventCount);
            }

            return empty();
        });

        assertThat(numberOfEventsInProcessedEventTable, is(of(numberOfRecipes)));
    }

    private void removeRecipesFromViewStore(final int eventNumber, final UUID recipeId) throws Exception {

        final String deleteEventNumberSql = "DELETE FROM processed_event where event_number = ?";
        final String deleteRecipeSql = "DELETE FROM recipe where name = ?";
        final String deleteFromStreamBufferSql = "DELETE FROM stream_status where stream_id = ?";

        try (final Connection connection = viewStoreDataSource.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(deleteEventNumberSql)) {
                preparedStatement.setInt(1, eventNumber);
                preparedStatement.executeUpdate();
            }
            try (final PreparedStatement preparedStatement = connection.prepareStatement(deleteRecipeSql)) {
                preparedStatement.setString(1, "recipe " + eventNumber);
                preparedStatement.executeUpdate();
            }

            try (final PreparedStatement preparedStatement = connection.prepareStatement(deleteFromStreamBufferSql)) {
                preparedStatement.setObject(1, recipeId);
                preparedStatement.executeUpdate();
            }
        }
    }

    private void runCatchup() throws Exception {
        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(buildJmxParameters())) {

            final JmxCommandRuntimeParameters jmxCommandRuntimeParameters = withNoCommandParameters();
            systemCommanderClient.getRemote(CONTEXT_NAME).call(
                    CATCHUP,
                    jmxCommandRuntimeParameters.getCommandRuntimeId(),
                    jmxCommandRuntimeParameters.getCommandRuntimeString(),
                    GUARDED.isGuarded()
            );
        }
    }

    private void cleanViewstoreTables() {
        databaseCleaner.cleanViewStoreTables(DB_CONTEXT_NAME,
                "ingredient",
                "recipe",
                "cake",
                "cake_order",
                "processed_event",
                "stream_status"
        );
        databaseCleaner.cleanStreamBufferTable(DB_CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(DB_CONTEXT_NAME);
    }

    private UUID findRecipeIdForEventNumber(final int eventNumber) throws Exception {

        return poller.pollUntilFound(() -> pollForRecipeId(eventNumber))
                .orElseThrow(() -> new AssertionError("Failed to find event in event_log with event_number " + eventNumber));
    }

    private Optional<UUID> pollForRecipeId(final int eventNumber) {

        try {
            try(final Connection connection = eventStoreDataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("SELECT stream_id FROM event_log where event_number = ?")) {
                preparedStatement.setInt(1, eventNumber);

                try(final ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        final UUID recipeId = (UUID) resultSet.getObject(1);
                        return of(recipeId);
                    }
                }
            }

        } catch (final SQLException e) {
            fail(e.getMessage());
        }

        return empty();
    }
}
