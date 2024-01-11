package uk.gov.justice.services.example.cakeshop.query.view.request;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SearchIndex {

    private UUID indexId;
    private ZonedDateTime deliveryDate;

    public SearchIndex(final UUID indexId, final ZonedDateTime deliveryDate) {
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
