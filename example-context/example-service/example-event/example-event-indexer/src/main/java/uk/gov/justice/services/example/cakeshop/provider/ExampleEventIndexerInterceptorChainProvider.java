package uk.gov.justice.services.example.cakeshop.provider;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;
import uk.gov.justice.services.event.source.subscriptions.interceptors.SubscriptionEventInterceptor;

import java.util.ArrayList;
import java.util.List;

public class ExampleEventIndexerInterceptorChainProvider implements InterceptorChainEntryProvider {

    private final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<InterceptorChainEntry>();

    public ExampleEventIndexerInterceptorChainProvider() {
        interceptorChainEntries.add(new InterceptorChainEntry(1000, SubscriptionEventInterceptor.class));
    }

    @Override
    public String component() {
        return "EVENT_INDEXER";
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        return interceptorChainEntries;
    }
}
