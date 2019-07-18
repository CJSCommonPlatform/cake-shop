package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.api.command.CatchupCommand;
import uk.gov.justice.services.jmx.api.command.IndexerCatchupCommand;
import uk.gov.justice.services.jmx.api.command.PingSystemCommand;
import uk.gov.justice.services.jmx.api.command.RebuildCommand;
import uk.gov.justice.services.jmx.api.command.ShutterSystemCommand;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.api.command.UnshutterSystemCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import java.util.List;

import org.junit.Test;

public class ListSystemCommandsIT {

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));


    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        final String contextName = "example-single";
        final JmxParameters jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {

            final List<SystemCommand> systemCommands = systemCommanderClient.getRemote(contextName).listCommands();

            assertThat(systemCommands.size(), is(6));
            assertThat(systemCommands, hasItem(new PingSystemCommand()));
            assertThat(systemCommands, hasItem(new ShutterSystemCommand()));
            assertThat(systemCommands, hasItem(new UnshutterSystemCommand()));
            assertThat(systemCommands, hasItem(new RebuildCommand()));
            assertThat(systemCommands, hasItem(new CatchupCommand()));
            assertThat(systemCommands, hasItem(new IndexerCatchupCommand()));
        }
    }
}
