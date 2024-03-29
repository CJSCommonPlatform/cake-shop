package uk.gov.justice.services.cakeshop.query.view.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.cakeshop.persistence.CakeRepository;
import uk.gov.justice.services.cakeshop.persistence.entity.Cake;
import uk.gov.justice.services.cakeshop.query.view.response.CakesView;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CakeServiceTest {

    @InjectMocks
    private CakeService cakeService;

    @Mock
    private CakeRepository cakeRepository;


    @Test
    public void shouldReturnCakes() throws Exception {
        final UUID id = randomUUID();
        final UUID id2 = randomUUID();
        final String name = "Xmass Cake";
        final String name2 = "Easter Cake";

        when(cakeRepository.findAll()).thenReturn(asList(new Cake(id, name), new Cake(id2, name2)));

        final CakesView cakes = cakeService.cakes();
        assertThat(cakes.getCakes().get(0).getId(), is(id));
        assertThat(cakes.getCakes().get(0).getName(), is(name));

        assertThat(cakes.getCakes().get(1).getId(), is(id2));
        assertThat(cakes.getCakes().get(1).getName(), is(name2));

    }
}
