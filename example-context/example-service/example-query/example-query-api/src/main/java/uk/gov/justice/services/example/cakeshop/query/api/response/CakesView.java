package uk.gov.justice.services.example.cakeshop.query.api.response;

import uk.gov.justice.services.example.cakeshop.query.api.request.SearchCake;

import java.util.List;

public class CakesView {

    private final List<SearchCake> cakes;

    public CakesView(final List<SearchCake> cakes) {
        this.cakes = cakes;
    }

    public List<SearchCake> getCakes() {
        return cakes;
    }
}
