package uk.gov.justice.services.example.cakeshop.query.api.request;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.example.cakeshop.query.view.CakesQueryView;
import uk.gov.justice.services.example.cakeshop.query.view.response.CakeView;
import uk.gov.justice.services.example.cakeshop.query.view.response.CakesView;
import uk.gov.justice.services.example.cakeshop.query.view.service.CakeService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CakesQueryViewTest {

    @Mock
    private CakeService service;

    @InjectMocks
    private CakesQueryView queryView;

    @Test
    public void shouldHaveCorrectHandlerMethod() throws Exception {
        assertThat(queryView, isHandler(QUERY_VIEW)
                .with(method("cakes").thatHandles("example.search-cakes")));
    }

    @Test
    public void shouldReturnCakes() throws Exception {
        final UUID id1 = randomUUID();
        final String name1 = "Chocolate cake";

        final JsonEnvelope query = envelope().with(metadataWithDefaults()).build();
        final UUID id2 = randomUUID();
        final String name2 = "Cheese cake";
        when(service.cakes()).thenReturn(new CakesView(asList(new CakeView(id1, name1), new CakeView(id2, name2))));

        final Envelope<CakesView> response = queryView.cakes(query);

        final List<CakeView> cakes = response.payload().getCakes();

        assertThat(cakes.size(), is(2));
        assertThat(cakes.get(0).getId(), is(id1));
        assertThat(cakes.get(0).getName(), is(name1));
        assertThat(cakes.get(1).getId(), is(id2));
        assertThat(cakes.get(1).getName(), is(name2));
    }
}