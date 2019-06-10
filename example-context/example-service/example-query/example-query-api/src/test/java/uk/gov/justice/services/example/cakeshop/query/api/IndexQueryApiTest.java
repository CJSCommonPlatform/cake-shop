package uk.gov.justice.services.example.cakeshop.query.api;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;

public class IndexQueryApiTest {

    @Test
    public void shouldHandleIndexQuery() throws Exception {
        assertThat(IndexQueryApi.class, isHandlerClass(QUERY_API)
                .with(method("getIndex")
                        .thatHandles("example.get-index")
                        .withRequesterPassThrough()));
    }


}
