package uk.gov.justice.services.example.cakeshop.dummy;

import uk.gov.justice.services.unifiedsearch.UnifiedSearchIndexer;
import uk.gov.justice.services.unifiedsearch.UnifiedSearchName;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@ApplicationScoped
public class DummyUnifiedSearchIndexerProducer {

    @Inject
    private DummyUnifiedSearchIndexer dummyUnifiedSearchIndexer;

    @Produces
    @UnifiedSearchName
    public UnifiedSearchIndexer unifiedSearchClient(final InjectionPoint injectionPoint) {
        return dummyUnifiedSearchIndexer;
    }
}
