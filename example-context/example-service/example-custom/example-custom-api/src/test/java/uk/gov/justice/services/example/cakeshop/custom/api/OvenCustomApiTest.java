package uk.gov.justice.services.example.cakeshop.custom.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isCustomHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.example.cakeshop.custom.api.response.OvenStatus;
import uk.gov.justice.services.example.cakeshop.custom.api.response.OvensStatus;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OvenCustomApiTest {

    @InjectMocks
    private OvenCustomApi ovenCustomApi;

    @Test
    public void shouldHandleOvenStatus() throws Exception {
        assertThat(OvenCustomApi.class, isCustomHandlerClass("CUSTOM_API")
                .with(method("status").thatHandles("example.ovens-status")));
    }

    @Test
    public void shouldReturnStatusOfAllOvens() throws Exception {
        final JsonEnvelope query = envelope().with(
                metadataOf(randomUUID(), "example.ovens-status"))
                .build();

        final Envelope<OvensStatus> status = ovenCustomApi.status(query);

        final List<OvenStatus> ovens = status.payload().getOvens();

        assertThat(ovens.size(), is(2));
        assertThat(ovens.get(0).getId(), notNullValue());
        assertThat(ovens.get(0).getName(), is("Big Oven"));
        assertThat(ovens.get(0).getTemperature(), is(250));
        assertThat(ovens.get(0).isActive(), is(true));

        assertThat(ovens.get(1).getId(), notNullValue());
        assertThat(ovens.get(1).getName(), is("Large Oven"));
        assertThat(ovens.get(1).getTemperature(), is(0));
        assertThat(ovens.get(1).isActive(), is(false));
    }
}