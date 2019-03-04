package uk.gov.justice.services.example.cakeshop.query.view;


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.example.cakeshop.query.view.request.SearchCakeOrder;
import uk.gov.justice.services.example.cakeshop.query.view.response.CakeOrderView;
import uk.gov.justice.services.example.cakeshop.query.view.service.CakeOrderService;
import uk.gov.justice.services.messaging.Envelope;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CakeOrdersQueryViewTest {

    @Mock
    private CakeOrderService service;

    @InjectMocks
    private CakeOrdersQueryView queryView;

    @Test
    public void shouldHaveCorrectHandlerMethod() throws Exception {
        assertThat(queryView, isHandler(QUERY_VIEW)
                .with(method("findOrder").thatHandles("example.get-order")));
    }

    @Test
    public void shouldReturnPojoOrder() {

        final UUID orderId = UUID.randomUUID();
        final UUID recipeId = UUID.randomUUID();
        final ZonedDateTime deliveryDate = ZonedDateTime.now();

        final SearchCakeOrder searchCakeOrder = new SearchCakeOrder(orderId, recipeId, deliveryDate);

        final Envelope<SearchCakeOrder> query = envelopeFrom(metadataWithDefaults(), searchCakeOrder);

        when(service.findOrder(orderId.toString())).thenReturn(new CakeOrderView(orderId, recipeId, deliveryDate));

        final Envelope<CakeOrderView> response = queryView.findOrder(query);

        assertThat(response.payload().getOrderId(), equalTo(orderId));
        assertThat(response.payload().getRecipeId(), equalTo(recipeId));
    }
}
