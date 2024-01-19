package uk.gov.justice.services.cakeshop.query.api.request;

import java.util.UUID;

public class SearchCake {

    private final UUID id;
    private final String name;

    public SearchCake(final UUID id, final String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
