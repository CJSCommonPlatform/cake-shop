package uk.gov.justice.services.cakeshop.event.listener.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.cakeshop.domain.Ingredient;
import uk.gov.justice.services.cakeshop.domain.event.RecipeAdded;
import uk.gov.justice.services.cakeshop.persistence.entity.Recipe;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecipeAddedToRecipeConverterTest {


    private RecipeAddedToRecipeConverter converter = new RecipeAddedToRecipeConverter();


    @Test
    public void shouldConvertRecipeAddedEvent() {
        final String name = "someName123";
        final UUID recipeId = UUID.randomUUID();
        final boolean glutenFree = true;
        final List<Ingredient> ingredients = Collections.singletonList(new Ingredient("sugar", 2));
        Recipe recipe = converter.convert(new RecipeAdded(recipeId, name, glutenFree, ingredients));

        assertThat(recipe.getName(), equalTo(name));
        assertThat(recipe.getId(), equalTo(recipeId));
        assertThat(recipe.isGlutenFree(), equalTo(glutenFree));

    }


}