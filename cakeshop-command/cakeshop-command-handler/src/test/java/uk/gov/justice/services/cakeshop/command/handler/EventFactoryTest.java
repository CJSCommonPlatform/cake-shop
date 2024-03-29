package uk.gov.justice.services.cakeshop.command.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.cakeshop.domain.event.CakeOrdered;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EventFactoryTest {

    private EventFactory eventFactory;

    @BeforeEach
    public void setUp() throws Exception {
        eventFactory = new EventFactory();
        eventFactory.objectMapper = new ObjectMapperProducer().objectMapper();
    }

    @Test
    public void shouldCreateCakeOrderedEvent() throws Exception {

        final CakeOrdered cakeOrdered = eventFactory.cakeOrderedEventFrom(envelope().with(metadataWithDefaults())
                .withPayloadOf("163af847-effb-46a9-96bc-32a0f7526f22", "orderId")
                .withPayloadOf("163af847-effb-46a9-96bc-32a0f7526f23", "recipeId")
                .withPayloadOf("163af847-effb-46a9-96bc-32a0f7526f22", "orderId")
                .withPayloadOf("2016-01-14T22:15:03.000000123+04:00", "deliveryDate").build());

        assertThat(cakeOrdered.getOrderId(), is(UUID.fromString("163af847-effb-46a9-96bc-32a0f7526f22")));
        assertThat(cakeOrdered.getRecipeId(), is(UUID.fromString("163af847-effb-46a9-96bc-32a0f7526f23")));
        assertThat(cakeOrdered.getDeliveryDate(), is(ZonedDateTime.of(2016, 01, 14, 18, 15, 3, 123, ZoneId.of("UTC"))));

    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldThrowIllegalStateExceptionOnMapperIOException() throws IOException {
        final ObjectMapper mockedMapper = Mockito.mock(ObjectMapper.class);
        when(mockedMapper.readValue(Mockito.anyString(), Mockito.eq(CakeOrdered.class))).thenThrow(new JsonMappingException("Error"));
        eventFactory.objectMapper = mockedMapper;

        assertThrows(IllegalStateException.class, () -> eventFactory.cakeOrderedEventFrom(envelope().with(metadataWithDefaults()).build()));
    }
}
