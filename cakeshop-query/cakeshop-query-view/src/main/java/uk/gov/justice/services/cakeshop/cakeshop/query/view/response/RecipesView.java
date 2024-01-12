package uk.gov.justice.services.cakeshop.cakeshop.query.view.response;

import uk.gov.justice.services.cakeshop.cakeshop.persistence.entity.Recipe;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * View representation of a list of {@link  Recipe}'s.
 */
public class RecipesView {

    private final List<RecipeView> recipes;

    @JsonCreator
    public RecipesView(@JsonProperty final List<RecipeView> recipes) {
        this.recipes = recipes;
    }

    public List<RecipeView> getRecipes() {
        return recipes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipesView that = (RecipesView) o;
        return Objects.equals(getRecipes(), that.getRecipes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRecipes());
    }
}
