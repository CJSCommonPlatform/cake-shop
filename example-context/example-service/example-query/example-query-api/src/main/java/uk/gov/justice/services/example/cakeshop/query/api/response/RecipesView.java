package uk.gov.justice.services.example.cakeshop.query.api.response;

import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;

import java.util.List;
import java.util.Objects;

/**
 * View representation of a list of {@link  Recipe}'s.
 */
public class RecipesView {

    private final List<RecipeView> recipes;

    public RecipesView(final List<RecipeView> recipes) {
        this.recipes = recipes;
    }

    public List<RecipeView> getRecipes() {
        return recipes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RecipesView that = (RecipesView) o;
        return Objects.equals(recipes, that.getRecipes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipes);
    }
}
