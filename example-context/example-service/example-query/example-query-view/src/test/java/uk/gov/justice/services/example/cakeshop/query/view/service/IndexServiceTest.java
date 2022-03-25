package uk.gov.justice.services.example.cakeshop.query.view.service;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.example.cakeshop.persistence.IndexRepository;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Index;
import uk.gov.justice.services.example.cakeshop.query.view.response.IndexView;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexServiceTest {

    @Mock
    private IndexRepository repository;

    @InjectMocks
    private IndexService service;

    @Test
    public void shouldReturnIndexView() throws Exception {

        final UUID indexId = randomUUID();
        final ZonedDateTime deliveryDate = now();

        when(repository.findBy(indexId)).thenReturn(new Index(indexId, deliveryDate));

        final IndexView view = service.findIndexBy(indexId.toString());

        assertThat(view.getIndexId(), is(indexId));
        assertThat(view.getDeliveryDate(), is(deliveryDate));
    }

    @Test
    public void shouldReturnNullIfIndexNotFoundInRepo() throws Exception {

        when(repository.findBy(any(UUID.class))).thenReturn(null);

        final IndexView view = service.findIndexBy(randomUUID().toString());

        assertThat(view, nullValue());
    }

}
