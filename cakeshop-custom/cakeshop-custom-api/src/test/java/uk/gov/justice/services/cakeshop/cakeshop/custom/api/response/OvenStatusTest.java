package uk.gov.justice.services.cakeshop.cakeshop.custom.api.response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class OvenStatusTest {

    @Test
    public void shouldCreateOvenStatusWithGivenValues() throws Exception {
        final String name = "name";
        final UUID id = UUID.randomUUID();
        final int temperature = 200;
        final boolean active = true;
        final OvenStatus ovenStatus = new OvenStatus(id, name, temperature, active);

        assertThat(ovenStatus.getName(), is(name));
        assertThat(ovenStatus.getId(), is(id));
        assertThat(ovenStatus.getTemperature(), is(200));
        assertThat(ovenStatus.isActive(), is(active));
    }
}