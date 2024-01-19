package uk.gov.justice.services.cakeshop.cakeshop.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;

public class IndexQueryApiTest {

    @Test
    public void shouldHandleIndexQuery() throws Exception {
        assertThat(IndexQueryApi.class, isHandlerClass(QUERY_API)
                .with(method("getIndex")
                        .thatHandles("cakeshop.get-index")
                        .withRequesterPassThrough()));
    }


}
