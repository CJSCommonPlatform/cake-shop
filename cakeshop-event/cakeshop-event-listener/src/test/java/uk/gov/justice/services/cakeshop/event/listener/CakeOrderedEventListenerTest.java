package uk.gov.justice.services.cakeshop.event.listener;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.cakeshop.persistence.CakeOrderRepository;
import uk.gov.justice.services.cakeshop.persistence.entity.CakeOrder;
import uk.gov.justice.services.messaging.Envelope;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CakeOrderedEventListenerTest {

    @Mock
    private CakeOrderRepository repository;

    @InjectMocks
    private CakeOrderedEventListener listener;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSaveEvent() throws Exception {

        final Envelope<CakeOrder> envelope = mock(Envelope.class);
        final CakeOrder cakeOrderObject = new CakeOrder(UUID.randomUUID(), UUID.randomUUID(), ZonedDateTime.now());
        when(envelope.payload()).thenReturn(cakeOrderObject);

        listener.handle(envelope);

        verify(repository).save(cakeOrderObject);

    }
}
