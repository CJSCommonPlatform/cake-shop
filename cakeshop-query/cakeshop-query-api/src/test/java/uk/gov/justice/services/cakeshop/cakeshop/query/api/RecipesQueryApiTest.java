package uk.gov.justice.services.cakeshop.cakeshop.query.api;


import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;

public class RecipesQueryApiTest {

    @Test
    public void shouldBeQueryApiThatHasRequesterPassThroughMethods() throws Exception {
        assertThat(RecipesQueryApi.class, isHandlerClass(QUERY_API)
                .with(allOf(
                        method("searchRecipes")
                                .thatHandles("example.search-recipes")
                                .withRequesterPassThrough(),
                        method("getRecipe")
                                .thatHandles("example.get-recipe")
                                .withRequesterPassThrough(),
                        method("queryRecipes")
                                .thatHandles("example.query-recipes")
                                .withRequesterPassThrough(),
                        method("getRecipePhotograph")
                                .thatHandles("example.get-recipe-photograph")
                                .withRequesterPassThrough()
                )));
    }
}
