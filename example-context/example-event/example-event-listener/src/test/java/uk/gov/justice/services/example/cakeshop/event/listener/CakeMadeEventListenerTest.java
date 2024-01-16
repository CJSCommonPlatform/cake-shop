package uk.gov.justice.services.example.cakeshop.event.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.example.cakeshop.persistence.CakeRepository;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Cake;
import uk.gov.justice.services.messaging.Envelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CakeMadeEventListenerTest {

    @Mock
    private CakeRepository cakeRepository;

    @InjectMocks
    private CakeMadeEventListener cakeMadeEventListener = new CakeMadeEventListener();

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSaveCake() {
        final Envelope<Cake> envelope = mock(Envelope.class);
        final Cake cake = mock(Cake.class);
        when(envelope.payload()).thenReturn(cake);
        cakeMadeEventListener.handle(envelope);

        verify(cakeRepository).save(cake);
    }

}
