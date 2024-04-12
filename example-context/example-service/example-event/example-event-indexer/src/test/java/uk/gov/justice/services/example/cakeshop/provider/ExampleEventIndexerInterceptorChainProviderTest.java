package uk.gov.justice.services.example.cakeshop.provider;

import org.junit.Test;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExampleEventIndexerInterceptorChainProviderTest {

    @Test
    public void shouldCreateInterceptorChainEntriesWithSubscriptionEventInterceptor() {

        final List<InterceptorChainEntry> interceptorChainEntries = new ExampleEventIndexerInterceptorChainProvider().interceptorChainTypes();

        assertThat(interceptorChainEntries.size(), is(1));

        final InterceptorChainEntry interceptorChainEntry = interceptorChainEntries.get(0);
        assertThat(interceptorChainEntry.getInterceptorType().getName(), is("uk.gov.justice.services.event.source.subscriptions.interceptors.SubscriptionEventInterceptor"));
        assertThat(interceptorChainEntry.getPriority(), is(1000));
    }

    @Test
    public void shouldReturnComponentName() {
        assertThat(new ExampleEventIndexerInterceptorChainProvider().component(), is("EVENT_INDEXER"));
    }
}