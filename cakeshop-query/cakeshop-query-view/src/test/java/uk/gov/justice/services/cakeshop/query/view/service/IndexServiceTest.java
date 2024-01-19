package uk.gov.justice.services.cakeshop.query.view.service;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.cakeshop.persistence.IndexRepository;
import uk.gov.justice.services.cakeshop.persistence.entity.Index;
import uk.gov.justice.services.cakeshop.query.view.response.IndexView;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
