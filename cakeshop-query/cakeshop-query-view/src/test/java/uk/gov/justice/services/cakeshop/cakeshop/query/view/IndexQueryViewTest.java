package uk.gov.justice.services.cakeshop.cakeshop.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.cakeshop.cakeshop.query.view.request.SearchIndex;
import uk.gov.justice.services.cakeshop.cakeshop.query.view.response.IndexView;
import uk.gov.justice.services.cakeshop.cakeshop.query.view.service.IndexService;
import uk.gov.justice.services.messaging.Envelope;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IndexQueryViewTest {

    @Mock
    private IndexService indexService;

    @InjectMocks
    private IndexQueryView indexQueryView;

    @Test
    public void shouldHaveCorrectHandlerMethod() throws Exception {
        assertThat(indexQueryView, isHandler(QUERY_VIEW)
                .with(method("findIndex").thatHandles("cakeshop.get-index")));
    }

    @Test
    public void shouldReturnPojoIndex() {

        final UUID indexId = randomUUID();
        final ZonedDateTime deliveryDate = ZonedDateTime.now();


        final SearchIndex searchIndex = new SearchIndex(indexId, deliveryDate);

        final Envelope<SearchIndex> query = envelopeFrom(metadataWithDefaults(), searchIndex);

        when(indexService.findIndexBy(indexId.toString())).thenReturn(new IndexView(indexId, deliveryDate));

        final Envelope<IndexView> response = indexQueryView.findIndex(query);

        assertThat(response.payload().getIndexId(), equalTo(indexId));
        assertThat(response.payload().getDeliveryDate(), equalTo(deliveryDate));
    }

}