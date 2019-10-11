package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.parseInt;
import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.api.command.AddTriggerCommand;
import uk.gov.justice.services.jmx.api.command.EventCatchupCommand;
import uk.gov.justice.services.jmx.api.command.IndexerCatchupCommand;
import uk.gov.justice.services.jmx.api.command.PingCommand;
import uk.gov.justice.services.jmx.api.command.RebuildCommand;
import uk.gov.justice.services.jmx.api.command.RemoveTriggerCommand;
import uk.gov.justice.services.jmx.api.command.ShutterCommand;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.api.command.UnshutterCommand;
import uk.gov.justice.services.jmx.api.command.VerifyCatchupCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import java.util.List;

import org.junit.Test;

public class ListSystemCommandsIT {

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

            final List<SystemCommand> systemCommands = systemCommanderClient.getRemote(CONTEXT_NAME).listCommands();

            assertThat(systemCommands.size(), is(9));
            assertThat(systemCommands, hasItem(new PingCommand()));
            assertThat(systemCommands, hasItem(new ShutterCommand()));
            assertThat(systemCommands, hasItem(new UnshutterCommand()));
            assertThat(systemCommands, hasItem(new RebuildCommand()));
            assertThat(systemCommands, hasItem(new EventCatchupCommand()));
            assertThat(systemCommands, hasItem(new IndexerCatchupCommand()));
            assertThat(systemCommands, hasItem(new AddTriggerCommand()));
            assertThat(systemCommands, hasItem(new RemoveTriggerCommand()));
            assertThat(systemCommands, hasItem(new VerifyCatchupCommand()));
        }
    }
}
