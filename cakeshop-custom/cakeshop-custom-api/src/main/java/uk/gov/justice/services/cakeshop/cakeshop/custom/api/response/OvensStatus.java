package uk.gov.justice.services.cakeshop.cakeshop.custom.api.response;

import java.util.List;

public class OvensStatus {

    private final List<OvenStatus> ovens;

    public OvensStatus(final List<OvenStatus> ovens) {
        this.ovens = ovens;
    }

    public List<OvenStatus> getOvens() {
        return ovens;
    }
}
