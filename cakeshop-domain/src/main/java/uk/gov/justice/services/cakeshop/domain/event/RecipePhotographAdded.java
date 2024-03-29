package uk.gov.justice.services.cakeshop.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("cakeshop.events.recipe-photograph-added")
public class RecipePhotographAdded {

    private final UUID recipeId;
    private final UUID photoId;

    public RecipePhotographAdded(final UUID recipeId, final UUID photoId) {
        this.recipeId = recipeId;
        this.photoId = photoId;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public UUID getPhotoId() {
        return photoId;
    }
}
