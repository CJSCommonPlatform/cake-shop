package uk.gov.justice.services.cakeshop.provider;


import static java.util.Collections.emptyList;

import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;

import java.util.List;

public class CakshopCustomApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return "CUSTOM_API";
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        return emptyList();
    }
}
