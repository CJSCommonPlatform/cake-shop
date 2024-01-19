package uk.gov.justice.services.cakeshop.cakeshop.event.listener.provider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CakshopEventListenerInterceptorChainProviderTest {

    @Test
    public void shouldCreateInterceptorChainEntriesWithSubscriptionEventInterceptor() {

        final List<InterceptorChainEntry> interceptorChainEntries = new CakshopEventListenerInterceptorChainProvider().interceptorChainTypes();

        assertThat(interceptorChainEntries.size(), is(1));

        final InterceptorChainEntry interceptorChainEntry = interceptorChainEntries.get(0);
        assertThat(interceptorChainEntry.getInterceptorType().getName(), is("uk.gov.justice.services.event.source.subscriptions.interceptors.SubscriptionEventInterceptor"));
        assertThat(interceptorChainEntry.getPriority(), is(1000));
    }

    @Test
    public void shouldReturnComponentName() {
        assertThat(new CakshopEventListenerInterceptorChainProvider().component(), is("EVENT_LISTENER"));
    }
}