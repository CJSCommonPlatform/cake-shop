package uk.gov.justice.services.cakeshop.query.api;


import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;

public class CakeOrdersQueryApiTest {

    @Test
    public void shouldHandleCakeOrderQuery() throws Exception {
        assertThat(CakeOrdersQueryApi.class, isHandlerClass(QUERY_API)
                .with(method("getOrder")
                        .thatHandles("cakeshop.get-order")
                        .withRequesterPassThrough()));
    }
}
