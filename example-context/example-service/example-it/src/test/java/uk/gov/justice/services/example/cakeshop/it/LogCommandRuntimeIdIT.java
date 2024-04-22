package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.management.ping.commands.LogRuntimeIdCommand.LOG_RUNTIME_ID;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import java.util.UUID;

import org.junit.Test;

public class LogCommandRuntimeIdIT {

    private static final String HOST = getHost();
    private static final int PORT = parseInt(getProperty("random.management.port"));
    private static final String CONTEXT_NAME = "example";

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            final UUID commandRuntimeId = randomUUID();
            systemCommanderClient
                    .getRemote(CONTEXT_NAME)
                    .callWithRuntimeId(LOG_RUNTIME_ID, commandRuntimeId);
        }
    }
}
