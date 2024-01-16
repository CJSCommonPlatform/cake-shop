package uk.gov.justice.services.example.cakeshop.query.view;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.example.cakeshop.query.view.request.SearchIndex;
import uk.gov.justice.services.example.cakeshop.query.view.response.IndexView;
import uk.gov.justice.services.example.cakeshop.query.view.service.IndexService;
import uk.gov.justice.services.messaging.Envelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_VIEW)
public class IndexQueryView {

    @Inject
    private IndexService indexService;

    @Handles("example.get-index")
    public Envelope<IndexView> findIndex(final Envelope<SearchIndex> query) {
        final String indexId = query.payload().getIndexId().toString();

        return envelop(indexService.findIndexBy(indexId))
                .withName("example.get-index")
                .withMetadataFrom(query);
    }
}
