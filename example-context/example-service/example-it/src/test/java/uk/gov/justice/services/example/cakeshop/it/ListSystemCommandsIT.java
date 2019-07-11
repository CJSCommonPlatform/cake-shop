package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.eventstore.management.catchup.commands.CatchupCommand;
import uk.gov.justice.services.eventstore.management.indexer.commands.IndexerCatchupCommand;
import uk.gov.justice.services.eventstore.management.rebuild.commands.RebuildCommand;
import uk.gov.justice.services.jmx.command.SystemCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder;
import uk.gov.justice.services.management.ping.command.PingSystemCommand;
import uk.gov.justice.services.management.shuttering.command.ShutterSystemCommand;
import uk.gov.justice.services.management.shuttering.command.UnshutterSystemCommand;

import java.util.List;

import org.junit.Test;

public class ListSystemCommandsIT {

    private static final String HOST = getHost();
    private static final int PORT = valueOf(getProperty("random.management.port"));


    private final SystemCommanderClientFactory systemCommanderClientFactory = new SystemCommanderClientFactory();

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        final JmxParametersBuilder jmxParameters = jmxParameters()
                .withHost(HOST)
                .withPort(PORT);

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters.build())) {

            final List<SystemCommand> systemCommands = systemCommanderClient.getRemote().listCommands();

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
