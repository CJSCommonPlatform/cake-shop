package uk.gov.justice.services.example.cakeshop.it.helpers;

public class PositionInStreamIterator {

    private long position = 0;

    public synchronized long nextPosition() {
        position++;

        return position;
    }
}
