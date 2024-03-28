package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.management.ping.commands.LogRuntimeIdCommand.LOG_RUNTIME_ID;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.api.mbean.SystemCommanderMBean;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import java.util.UUID;

import org.junit.Test;

public class TestLogRuntimeIdCommandIT {

    private static final String HOST = getHost();
    private static final int PORT = parseInt(getProperty("random.management.port"));
    private static final String CONTEXT_NAME = "example";

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    @Test
    public void shouldCallLogRuntimeIdCommand() throws Exception {

        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();
        final UUID uuid = randomUUID();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            final SystemCommanderMBean systemCommanderMBean = systemCommanderClient
                    .getRemote(CONTEXT_NAME);

            systemCommanderMBean.callWithRuntimeId(LOG_RUNTIME_ID, uuid);
        }
    }
}
