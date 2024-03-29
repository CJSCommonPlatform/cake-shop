package uk.gov.justice.services.cakeshop.domain.aggregate;

import static uk.gov.justice.domain.aggregate.condition.Precondition.assertPrecondition;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.cakeshop.domain.Ingredient;
import uk.gov.justice.services.cakeshop.domain.event.CakeMade;
import uk.gov.justice.services.cakeshop.domain.event.RecipeAdded;
import uk.gov.justice.services.cakeshop.domain.event.RecipePhotographAdded;
import uk.gov.justice.services.cakeshop.domain.event.RecipeRemoved;
import uk.gov.justice.services.cakeshop.domain.event.RecipeRenamed;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Recipe aggregate.
 */
public class Recipe implements Aggregate {

    private static final long serialVersionUID = 7047097583627086738L;

    private UUID recipeId;
    private String name;

    public Stream<Object> addRecipe(final UUID recipeId, final String name, final Boolean glutenFree, final List<Ingredient> ingredients) {
        assertPrecondition(this.recipeId == null).orElseThrow("Recipe already added");
        return apply(Stream.of(new RecipeAdded(recipeId, name, glutenFree, ingredients)));
    }

    public Stream<Object> removeRecipe() {
        assertPrecondition(this.recipeId != null).orElseThrow("Recipe not available");

        return apply(Stream.of(new RecipeRemoved(this.recipeId)));
    }

    public Stream<Object> makeCake(final UUID cakeId) {
        assertPrecondition(this.recipeId != null).orElseThrow("Recipe not available");
        return apply(Stream.of(new CakeMade(cakeId, this.name)));
    }

    public Stream<Object> renameRecipe(final String name) {
        assertPrecondition(this.recipeId != null).orElseThrow("Recipe not available");

        return apply(Stream.of(new RecipeRenamed(this.recipeId, name)));
    }

    public Stream<Object> addPhotograph(final UUID photoId) {
        assertPrecondition(this.recipeId != null).orElseThrow("Recipe not available");

        return apply(Stream.of(new RecipePhotographAdded(this.recipeId, photoId)));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(RecipeRemoved.class).apply(x -> recipeId = null),
                when(RecipeAdded.class).apply(x -> {
                    recipeId = x.getRecipeId();
                    name = x.getName();
                }),
                when(RecipeRenamed.class).apply(x -> name = x.getName()),
                when(CakeMade.class).apply(x -> doNothing()),
                when(RecipePhotographAdded.class).apply(x -> doNothing()));
    }
}
