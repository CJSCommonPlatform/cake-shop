package uk.gov.justice.services.cakeshop.persistence.entity;

import java.util.UUID;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class RecipeTest {

    private final static UUID ID = UUID.randomUUID();
    private final static String NAME = "Cake";

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "squid:S00122"})
    @Test
    public void equalsAndHashCode() {
        Recipe item1 = new Recipe(ID, NAME, false, null);
        Recipe item2 = new Recipe(ID, NAME, false, null);
        Recipe item3 = new Recipe(UUID.randomUUID(), NAME, true, null);
        Recipe item4 = new Recipe(ID, null, false, null);

        new EqualsTester()
                .addEqualityGroup(item1, item2)
                .addEqualityGroup(item3)
                .addEqualityGroup(item4)
                .testEquals();
    }

}