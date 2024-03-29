package uk.gov.justice.services.cakeshop.query.view.service;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.cakeshop.persistence.CakeOrderRepository;
import uk.gov.justice.services.cakeshop.persistence.entity.CakeOrder;
import uk.gov.justice.services.cakeshop.query.view.response.CakeOrderView;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

;

@ExtendWith(MockitoExtension.class)
public class CakeOrderServiceTest {

    @Mock
    private CakeOrderRepository repository;

    @InjectMocks
    private CakeOrderService service;

    @Test
    public void shouldReturnOrderView() throws Exception {

        final UUID id = UUID.randomUUID();
        final UUID recipeId = UUID.randomUUID();
        final ZonedDateTime deliveryDate = ZonedDateTime.now();

        when(repository.findBy(id)).thenReturn(new CakeOrder(id, recipeId, deliveryDate));

        CakeOrderView view = service.findOrder(id.toString());

        assertThat(view.getOrderId(), is(id));
        assertThat(view.getDeliveryDate(), is(deliveryDate));
        assertThat(view.getRecipeId(), is(recipeId));

    }

    @Test
    public void shouldReturnNullIfOrderNotFoundInRepo() throws Exception {

        when(repository.findBy(any(UUID.class))).thenReturn(null);

        CakeOrderView view = service.findOrder(UUID.randomUUID().toString());

        assertThat(view, nullValue());
    }

    }
