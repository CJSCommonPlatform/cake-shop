package uk.gov.justice.services.example.cakeshop.it.helpers;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class StreamGenerator {

    private final List<UUID> streams;
    private Random random = new Random();

    public StreamGenerator(final int numberOfStreams) {
        streams = range(0, numberOfStreams)
                .mapToObj(operand -> UUID.randomUUID())
                .collect(toList());
    }

    public UUID next() {
        return streams.get(random.nextInt(streams.size()));
    }
}
