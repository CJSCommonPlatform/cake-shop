package uk.gov.justice.services.cakeshop.cakeshop.event.listener.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.cakeshop.cakeshop.domain.event.RecipeAdded;
import uk.gov.justice.services.cakeshop.cakeshop.persistence.entity.Ingredient;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecipeAddedToIngredientsConverterTest {

    private static final String NAME = "ingredientName";

    @Mock
    private RecipeAdded recipeAdded;

    private RecipeAddedToIngredientsConverter converter;

    @BeforeEach
    public void setup() {
        when(recipeAdded.getIngredients()).thenReturn(Collections.singletonList(new uk.gov.justice.services.cakeshop.cakeshop.domain.Ingredient(NAME, 2)));
        converter = new RecipeAddedToIngredientsConverter();
    }

    @Test
    public void shouldConvertRecipeAddedEvent() {
        List<Ingredient> ingredients = converter.convert(recipeAdded);

        assertThat(ingredients.size(), equalTo(1));
        assertThat(ingredients.get(0).getName(), equalTo(NAME));
    }
}