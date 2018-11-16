package uk.gov.justice.services.example.cakeshop.query.view;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.example.cakeshop.query.view.response.CakeOrderView;
import uk.gov.justice.services.example.cakeshop.query.view.service.CakeOrderService;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_VIEW)
public class CakeOrdersQueryView {

    @Inject
    CakeOrderService service;

    @Handles("example.get-order")
    public Envelope<CakeOrderView> findOrder(final JsonEnvelope query) {
        final String orderId = query.payloadAsJsonObject().getString("orderId");

        return envelop(service.findOrder(orderId))
                .withName("example.get-order")
                .withMetadataFrom(query);
    }
}
