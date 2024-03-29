package uk.gov.justice.services.cakeshop.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.cakeshop.persistence.entity.CakeOrder;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CakeOrderRepositoryIT extends BaseTransactionalJunit4Test {

    @Inject
    private CakeOrderRepository cakeOrderRepository;


    @Test
    public void shouldStoreOrder() {
        final UUID orderId = UUID.randomUUID();
        final UUID recipeId = UUID.randomUUID();
        final ZonedDateTime deliveryDate = ZonedDateTime.of(2014, 5, 13, 4, 12, 12, 0, ZoneId.of("UTC"));

        CakeOrder cakeOrderA = new CakeOrder(orderId, recipeId, deliveryDate);
        cakeOrderRepository.save(cakeOrderA);

        CakeOrder cakeOrder = cakeOrderRepository.findBy(orderId);

        assertThat(cakeOrder, is(notNullValue()));
        assertThat(cakeOrder.getOrderId(), equalTo(orderId));
        assertThat(cakeOrder.getRecipeId(), equalTo(recipeId));
        assertThat(cakeOrder.getDeliveryDate(), is(deliveryDate));
        assertThat(cakeOrder.getDeliveryDate().getZone(), is(ZoneId.of("UTC")));
    }
}
