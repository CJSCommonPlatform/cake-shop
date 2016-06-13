package uk.gov.justice.services.example.cakeshop.query.view;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.example.cakeshop.query.view.response.RecipesView;
import uk.gov.justice.services.example.cakeshop.query.view.service.RecipeService;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@ServiceComponent(Component.QUERY_VIEW)
public class RecipesQueryView {

    static final String NAME_RESPONSE_RECIPE = "cakeshop.findRecipe-response";
    static final String NAME_RESPONSE_RECIPE_LIST = "cakeshop.recipes-response";
    private static final Logger LOGGER = getLogger(RecipesQueryView.class);
    private static final String FIELD_RECIPE_ID = "recipeId";
    private static final String FIELD_NAME = "name";
    private static final String PAGESIZE = "pagesize";
    private static final String FIELD_GLUTEN_FREE = "glutenFree";

    private final RecipeService recipeService;
    private final Enveloper enveloper;

    @Inject
    public RecipesQueryView(RecipeService recipeService, Enveloper enveloper) {
        this.recipeService = recipeService;
        this.enveloper = enveloper;
    }

    @Handles("cakeshop.get-recipe")
    public JsonEnvelope findRecipe(final JsonEnvelope query) {
        LOGGER.info("=============> Inside findRecipe Query View. RecipeId: " + query.payloadAsJsonObject().getString(FIELD_RECIPE_ID));

        return enveloper.withMetadataFrom(query, NAME_RESPONSE_RECIPE).apply(
                recipeService.findRecipe(query.payloadAsJsonObject().getString(FIELD_RECIPE_ID)));
    }

    @Handles("cakeshop.search-recipes")
    public JsonEnvelope listRecipes(final JsonEnvelope query) {
        LOGGER.info("=============> Inside listRecipes Query View ");

        return enveloper.withMetadataFrom(query, NAME_RESPONSE_RECIPE_LIST).apply(fetchRecipes(query));
    }

    private RecipesView fetchRecipes(final JsonEnvelope query) {
        final JsonObject queryObject = query.payloadAsJsonObject();
        return recipeService.getRecipes(
                queryObject.getInt(PAGESIZE),
                getString(queryObject, FIELD_NAME),
                getBoolean(queryObject, FIELD_GLUTEN_FREE));
    }
}
