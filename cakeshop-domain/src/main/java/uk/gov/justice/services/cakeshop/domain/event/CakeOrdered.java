package uk.gov.justice.services.cakeshop.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

@Event("cakeshop.events.cake-ordered")
public class CakeOrdered {

    private final UUID orderId;
    private final UUID recipeId;
    private final ZonedDateTime deliveryDate;

    public CakeOrdered(UUID orderId, UUID recipeId, ZonedDateTime deliveryDate) {
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
