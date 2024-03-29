package uk.gov.justice.services.cakeshop.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("cakeshop.events.recipe-removed")
public class RecipeRemoved {

    private final UUID recipeId;

    public RecipeRemoved(final UUID recipeId) {
        this.recipeId = recipeId;
      }

    public UUID getRecipeId() {
        return recipeId;
    }

}
