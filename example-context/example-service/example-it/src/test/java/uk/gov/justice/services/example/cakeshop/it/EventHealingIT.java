package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopMediaTypes.ADD_RECIPE_MEDIA_TYPE;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopUris.RECIPES_RESOURCE_URI;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.matchers.HttpStatusCodeMatcher.isStatus;

import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventJdbcRepository;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.event.EventStreamJdbsRepositoryFactory;
import uk.gov.justice.services.eventsourcing.repository.jdbc.eventstream.EventStreamJdbcRepository;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.DatabaseManager;
import uk.gov.justice.services.example.cakeshop.it.helpers.ProcessedEventCounter;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.jmx.api.command.EventCatchupCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EventHealingIT {

    private static final String CONTEXT_NAME = "example";

    private final DataSource eventStoreDataSource = new DatabaseManager().initEventStoreDb();
    private final DataSource viewStoreDataSource = new DatabaseManager().initViewStoreDb();
    private final EventJdbcRepository eventJdbcRepository = new EventRepositoryFactory().getEventJdbcRepository(eventStoreDataSource);

    private final EventStreamJdbsRepositoryFactory eventStreamJdbcRepositoryFactory = new EventStreamJdbsRepositoryFactory();
    private final EventStreamJdbcRepository eventStreamJdbcRepository = eventStreamJdbcRepositoryFactory.getEventStreamJdbcRepository(eventStoreDataSource);

    private final ProcessedEventCounter processedEventCounter = new ProcessedEventCounter(viewStoreDataSource);

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));

    private final CommandFactory commandFactory = new CommandFactory();

    private final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SequenceSetter sequenceSetter = new SequenceSetter();

    private final Poller poller = new Poller();

    private Client client;

    @Before
    public void before() {
        client = new RestEasyClientFactory().createResteasyClient();

        final String contextName = "framework";

        databaseCleaner.cleanEventStoreTables(contextName);
        cleanViewstoreTables();
        databaseCleaner.cleanSystemTables(contextName);

        sequenceSetter.setSequenceTo(1L, "event_sequence_seq", eventStoreDataSource);
    }

    @After
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

        poller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();
            if (eventCount == numberOfRecipes) {
                return of(eventCount);
            }

            return empty();
        });

        removeRecipesFromViewStore(3, findRecipeIdForEventNumber(3));
        removeRecipesFromViewStore(5, findRecipeIdForEventNumber(5));

        runCatchup();

        final Optional<Integer> numberOfEventsInProcessedEventTable = poller.pollUntilFound(() -> {
            final int eventCount = processedEventCounter.countProcessedEvents();

            System.out.println("Number of events in processed_event: " + eventCount);
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

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters)) {

            systemCommanderClient.getRemote(CONTEXT_NAME).call(new EventCatchupCommand());
        }
    }

    private void cleanViewstoreTables() {

        final String contextName = "framework";

        databaseCleaner.cleanViewStoreTables(contextName,
                "ingredient",
                "recipe",
                "cake",
                "cake_order",
                "processed_event"
        );

        databaseCleaner.cleanStreamBufferTable(contextName);
        databaseCleaner.cleanStreamStatusTable(contextName);
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
