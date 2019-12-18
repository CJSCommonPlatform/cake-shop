package uk.gov.justice.services.example.cakeshop.it;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.example.cakeshop.it.helpers.EventErrorLogRepositoryFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.EventSender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.system.domain.EventError;
import uk.gov.justice.services.system.persistence.EventErrorLogRepository;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

public class EventErrorHandlingIT {

    private static final String TOPIC_NAME = "example.event";

    private final EventSender eventSender = new EventSender();

    private final DataSource systemDataSource = new TestJdbcDataSourceProvider().getSystemDataSource("framework");
    private final EventErrorLogRepositoryFactory eventErrorLogRepositoryFactory = new EventErrorLogRepositoryFactory();

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    private final Poller poller = new Poller();

    @Before
    public void cleanDatabase() {
        databaseCleaner.cleanSystemTables("framework");
    }

    @Test
    public void shouldStoreEventProcessingErrorsInTheDatabase() throws Exception {

        final UUID recipeId = randomUUID();
        final UUID eventId = randomUUID();
        final String eventName = "example.events.recipe-added";

        final JsonObject jsonObject = createObjectBuilder()
                .add("recipeId", recipeId.toString())
                .add("name", "Chocolate muffin")
                .add("glutenFree", true)
                .add("ingredients", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("name", "someIngredient")
                                .add("quantity", 1)
                        ).build()
                ).build();

        final JsonEnvelope eventWithMissingEventNumberEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(eventId)
                        .withName(eventName)
                        .withStreamId(recipeId)
                        .withPosition(1L)
                        .build(),
                jsonObject);

        assertThat(eventWithMissingEventNumberEnvelope.metadata().eventNumber(), is(empty()));

        eventSender.sendToTopic(eventWithMissingEventNumberEnvelope, TOPIC_NAME);

        final EventErrorLogRepository eventErrorLogRepository = eventErrorLogRepositoryFactory.create(systemDataSource);

        final Optional<EventError> eventError = poller.pollUntilFound(() -> {
            final List<EventError> eventErrors = eventErrorLogRepository.findAll();

            if (eventErrors.size() > 0) {
                return of(eventErrors.get(0));
            }

            return empty();
        });

        if (eventError.isPresent()) {
            assertThat(eventError.get().getEventId(), is(eventId));
            assertThat(eventError.get().getEventName(), is(eventName));
            assertThat(eventError.get().getEventNumber(), is(empty()));
            assertThat(eventError.get().getComponent(), is("EVENT_LISTENER"));
        } else {
            fail();
        }
    }
}
