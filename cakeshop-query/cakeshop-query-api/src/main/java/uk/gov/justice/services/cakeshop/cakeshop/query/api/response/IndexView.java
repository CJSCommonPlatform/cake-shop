package uk.gov.justice.services.cakeshop.cakeshop.query.api.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public class IndexView {

    private UUID indexId;
    private ZonedDateTime deliveryDate;

    public IndexView(final UUID indexId, final ZonedDateTime deliveryDate) {
        this.indexId = indexId;
        this.deliveryDate = deliveryDate;
    }

    public UUID getIndexId() {
        return indexId;
    }

    public ZonedDateTime getDeliveryDate() {
        return deliveryDate;
    }
}
