package uk.gov.justice.services.cakeshop.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("cakeshop.events.recipe-renamed")
public class RecipeRenamed {

    private final UUID recipeId;
    private final String name;

    public RecipeRenamed(final UUID recipeId, final String name) {
        this.recipeId = recipeId;
        this.name = name;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public String getName() {
        return name;
    }
}
