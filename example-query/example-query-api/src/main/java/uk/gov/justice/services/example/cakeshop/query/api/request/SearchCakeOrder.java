package uk.gov.justice.services.example.cakeshop.query.api.request;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SearchCakeOrder {
    private UUID orderId;

    private UUID recipeId;

    private ZonedDateTime deliveryDate;


    public SearchCakeOrder(final UUID orderId, final UUID recipeId, final ZonedDateTime deliveryDate) {
        this.orderId = orderId;
        this.recipeId = recipeId;
        this.deliveryDate = deliveryDate;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public ZonedDateTime getDeliveryDate() {
        return deliveryDate;
    }
}
