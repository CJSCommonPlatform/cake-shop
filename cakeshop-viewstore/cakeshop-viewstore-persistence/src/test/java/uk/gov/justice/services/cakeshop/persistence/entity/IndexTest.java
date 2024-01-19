package uk.gov.justice.services.cakeshop.persistence.entity;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class IndexTest {

    private final static UUID ID = randomUUID();
    private final static ZonedDateTime TIME = now();

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "squid:S00122"})
    @Test
    public void equalsAndHashCode() {
        final Index item1 = new Index(ID, TIME);
        final Index item2 = new Index(ID, TIME);
        final Index item3 = new Index(randomUUID(), TIME);

        new EqualsTester()
                .addEqualityGroup(item1, item2)
                .addEqualityGroup(item3)
                .testEquals();
    }


}
