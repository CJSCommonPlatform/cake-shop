package uk.gov.justice.services.example.cakeshop.it.helpers;

public class MBeanClientException extends RuntimeException {

    public MBeanClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
