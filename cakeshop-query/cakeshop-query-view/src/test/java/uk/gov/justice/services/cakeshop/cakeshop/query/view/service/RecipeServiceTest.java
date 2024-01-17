package uk.gov.justice.services.cakeshop.cakeshop.query.view.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;

import uk.gov.justice.services.cakeshop.cakeshop.persistence.RecipeRepository;
import uk.gov.justice.services.cakeshop.cakeshop.persistence.entity.Recipe;
import uk.gov.justice.services.cakeshop.cakeshop.query.view.response.PhotoView;
import uk.gov.justice.services.cakeshop.cakeshop.query.view.response.RecipeView;
import uk.gov.justice.services.cakeshop.cakeshop.query.view.response.RecipesView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecipeServiceTest {

    private static final String NAME = "name";
    private static final UUID RECIPE_ID = UUID.randomUUID();
    private static final UUID NON_EXISTENT_ID = UUID.randomUUID();
    private static final boolean GLUTEN_FREE = true;
    private static final UUID PHOTO_ID = UUID.randomUUID();
    @InjectMocks
    private RecipeService service;

    @Mock
    private RecipeRepository recipeRepository;

    @Test
    public void shouldReturnRecipeById() {
        given(recipeRepository.findBy(RECIPE_ID)).willReturn(new Recipe(RECIPE_ID, NAME, GLUTEN_FREE, PHOTO_ID));

        RecipeView foundPerson = service.findRecipe(RECIPE_ID.toString());

        assertThat(foundPerson.getId(), equalTo(RECIPE_ID));
        assertThat(foundPerson.getName(), equalTo(NAME));
    }

    @Test
    public void shouldReturnNullWhenRecipeNotFound() {
        given(recipeRepository.findBy(NON_EXISTENT_ID)).willReturn(null);

        assertNull(service.findRecipe(NON_EXISTENT_ID.toString()));
    }

    @Test
    public void shouldGetRecipes() {
        int pageSize = 20;
        Optional<String> nameQueryParam = Optional.of("name123");
        Optional<Boolean> glutenFreeQueryParam = Optional.of(false);
        given(recipeRepository.findBy(pageSize, nameQueryParam, glutenFreeQueryParam))
                .willReturn(singletonList(new Recipe(RECIPE_ID, NAME, GLUTEN_FREE, PHOTO_ID)));
        RecipesView recipes = service.getRecipes(pageSize, nameQueryParam, glutenFreeQueryParam);

        List<RecipeView> firstRecipe = recipes.getRecipes();
        assertThat(firstRecipe, hasSize(1));
        assertThat(firstRecipe.get(0).getId(), equalTo(RECIPE_ID));
        assertThat(firstRecipe.get(0).getName(), equalTo(NAME));
        assertThat(firstRecipe.get(0).isGlutenFree(), is(GLUTEN_FREE));
    }

    @Test
    public void shouldGetRecipes2() {
        int pageSize = 10;

        Optional<String> nameQueryParam = Optional.of("other name");
        Optional<Boolean> glutenFreeQueryParam = Optional.empty();

        given(recipeRepository.findBy(pageSize, nameQueryParam, glutenFreeQueryParam))
                .willReturn(singletonList(new Recipe(RECIPE_ID, NAME, GLUTEN_FREE, PHOTO_ID)));

        RecipesView recipes = service.getRecipes(pageSize, nameQueryParam, glutenFreeQueryParam);

        List<RecipeView> firstRecipe = recipes.getRecipes();
        assertThat(firstRecipe, hasSize(1));
        assertThat(firstRecipe.get(0).getId(), equalTo(RECIPE_ID));
        assertThat(firstRecipe.get(0).getName(), equalTo(NAME));
        assertThat(firstRecipe.get(0).isGlutenFree(), is(GLUTEN_FREE));
    }

    @Test
    public void shouldGetRecipePhoto() throws Exception {
        given(recipeRepository.findBy(RECIPE_ID)).willReturn(new Recipe(RECIPE_ID, NAME, GLUTEN_FREE, PHOTO_ID));

        final PhotoView recipePhoto = service.findRecipePhoto(RECIPE_ID.toString());

        assertThat(recipePhoto.getFileId(), is(PHOTO_ID));
    }

    @Test
    public void shouldReturnNullWhenFetchingPhotoIfRecipeNotFound() {
        given(recipeRepository.findBy(NON_EXISTENT_ID)).willReturn(null);

        assertNull(service.findRecipePhoto(NON_EXISTENT_ID.toString()));
    }

    @Test
    public void shouldReturnNullWhenPhotoIdNull() {
        given(recipeRepository.findBy(RECIPE_ID)).willReturn(new Recipe(RECIPE_ID, NAME, GLUTEN_FREE, null));

        assertNull(service.findRecipePhoto(RECIPE_ID.toString()));
    }

}