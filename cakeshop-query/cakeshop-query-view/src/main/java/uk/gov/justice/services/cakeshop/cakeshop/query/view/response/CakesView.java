package uk.gov.justice.services.cakeshop.cakeshop.query.view.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CakesView {

    private final List<CakeView> cakes;

    @JsonCreator
    public CakesView(@JsonProperty final List<CakeView> cakes) {
        this.cakes = cakes;
    }

    public List<CakeView> getCakes() {
        return cakes;
    }
}
