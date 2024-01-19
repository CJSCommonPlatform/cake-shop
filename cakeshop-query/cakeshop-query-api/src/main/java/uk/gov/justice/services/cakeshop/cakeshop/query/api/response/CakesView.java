package uk.gov.justice.services.cakeshop.cakeshop.query.api.response;

import uk.gov.justice.services.cakeshop.cakeshop.query.api.request.SearchCake;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CakesView {

    private final List<SearchCake> cakes;

    @JsonCreator
    public CakesView(@JsonProperty final List<SearchCake> cakes) {
        this.cakes = cakes;
    }

    public List<SearchCake> getCakes() {
        return cakes;
    }
}
