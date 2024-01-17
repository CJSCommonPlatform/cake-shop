package uk.gov.justice.services.example.cakeshop.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.example.cakeshop.persistence.entity.Index;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class IndexRepositoryIT extends BaseTransactionalJunit4Test {

    @Inject
    private IndexRepository indexRepository;


    @Test
    public void shouldStoreIndex() {
        final UUID indexId = UUID.randomUUID();
        final ZonedDateTime deliveryDate = ZonedDateTime.of(2014, 5, 13, 4, 12, 12, 0, ZoneId.of("UTC"));

        final Index index = new Index(indexId, deliveryDate);
        indexRepository.save(index);

        final Index indexResult = indexRepository.findBy(indexId);

        assertThat(indexResult, is(notNullValue()));
        assertThat(indexResult.getIndexId(), equalTo(indexId));
        assertThat(indexResult.getDeliveryDate(), equalTo(deliveryDate));
    }
}
