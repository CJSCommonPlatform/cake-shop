package uk.gov.justice.services.example.cakeshop.event.listener.provider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import java.util.List;

import org.junit.Test;

public class ExampleEventListenerInterceptorChainProviderTest {

    @Test
    public void shouldCreateInterceptorChainEntriesWithSubscriptionEventInterceptor() {

        final List<InterceptorChainEntry> interceptorChainEntries = new ExampleEventListenerInterceptorChainProvider().interceptorChainTypes();

        assertThat(interceptorChainEntries.size(), is(1));

        final InterceptorChainEntry interceptorChainEntry = interceptorChainEntries.get(0);
        assertThat(interceptorChainEntry.getInterceptorType().getName(), is("uk.gov.justice.services.event.source.subscriptions.interceptors.SubscriptionEventInterceptor"));
        assertThat(interceptorChainEntry.getPriority(), is(1000));
    }

    @Test
    public void shouldReturnComponentName() {
        assertThat(new ExampleEventListenerInterceptorChainProvider().component(), is("EVENT_LISTENER"));
    }
}