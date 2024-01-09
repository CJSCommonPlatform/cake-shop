package uk.gov.justice.services.example.cakeshop.event.listener.interceptor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExceptionThrowingInterceptorTest {

    private ExceptionThrowingInterceptor interceptor = new ExceptionThrowingInterceptor();

    @Mock
    private InterceptorChain interceptorChain;


    @Test
    public void shouldThrowExceptionWhenNameContainsExceptionalCake() throws Exception {

        final JsonEnvelope jsonEnvelope = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf("Exceptional cake", "name")
                .build();

        assertThrows(TestInterceptorException.class, () -> interceptor.process(interceptorContextWithInput(jsonEnvelope), interceptorChain));

    }

    @Test
    public void shouldNotThrowExceptionPayloadDoesNotContainExceptionalCake() {

        interceptor.process(interceptorContextWithInput(
                envelope().with(metadataWithDefaults()).withPayloadOf("Some cake", "name").build()), interceptorChain);

        interceptor.process(interceptorContextWithInput(
                envelope().with(metadataWithDefaults()).build()), interceptorChain);

    }
}