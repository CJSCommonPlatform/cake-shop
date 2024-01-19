package uk.gov.justice.services.cakeshop.event.listener.provider;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.justice.services.event.source.subscriptions.interceptors.SubscriptionEventInterceptor;

import java.util.ArrayList;
import java.util.List;

public class CakshopEventListenerInterceptorChainProvider implements InterceptorChainEntryProvider {

    private final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<InterceptorChainEntry>();

    public CakshopEventListenerInterceptorChainProvider() {
        interceptorChainEntries.add(new InterceptorChainEntry(1000, SubscriptionEventInterceptor.class));
    }

    @Override
    public String component() {
        return "EVENT_LISTENER";
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        return interceptorChainEntries;
    }
}
