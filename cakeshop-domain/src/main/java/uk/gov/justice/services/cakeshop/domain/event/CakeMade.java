package uk.gov.justice.services.cakeshop.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("cakeshop.events.cake-made")
public class CakeMade {

    private final UUID cakeId;
    private final String name;

    public CakeMade(final UUID cakeId, final String name) {
        this.cakeId = cakeId;
        this.name = name;
    }

    public UUID getCakeId() {
        return cakeId;
    }


    public String getName() {
        return name;
    }
}
