package uk.gov.justice.services.example.cakeshop.it;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.eventstore.management.commands.AddTriggerCommand.ADD_TRIGGER;
import static uk.gov.justice.services.eventstore.management.commands.DisablePublishingCommand.DISABLE_PUBLISHING;
import static uk.gov.justice.services.eventstore.management.commands.EnablePublishingCommand.ENABLE_PUBLISHING;
import static uk.gov.justice.services.eventstore.management.commands.EventCatchupCommand.CATCHUP;
import static uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand.INDEXER_CATCHUP;
import static uk.gov.justice.services.eventstore.management.commands.RebuildCommand.REBUILD;
import static uk.gov.justice.services.eventstore.management.commands.RemoveTriggerCommand.REMOVE_TRIGGER;
import static uk.gov.justice.services.eventstore.management.commands.ValidatePublishedEventsCommand.VALIDATE_EVENTS;
import static uk.gov.justice.services.eventstore.management.commands.VerifyCatchupCommand.VERIFY_CATCHUP;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.management.ping.commands.PingCommand.PING;
import static uk.gov.justice.services.management.shuttering.commands.ShutterCommand.SHUTTER;
import static uk.gov.justice.services.management.shuttering.commands.UnshutterCommand.UNSHUTTER;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.eventstore.management.commands.AddTriggerCommand;
import uk.gov.justice.services.eventstore.management.commands.DisablePublishingCommand;
import uk.gov.justice.services.eventstore.management.commands.EnablePublishingCommand;
import uk.gov.justice.services.eventstore.management.commands.EventCatchupCommand;
import uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand;
import uk.gov.justice.services.eventstore.management.commands.RebuildCommand;
import uk.gov.justice.services.eventstore.management.commands.RemoveTriggerCommand;
import uk.gov.justice.services.eventstore.management.commands.ValidatePublishedEventsCommand;
import uk.gov.justice.services.eventstore.management.commands.VerifyCatchupCommand;
import uk.gov.justice.services.jmx.api.command.SystemCommandDetails;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.management.ping.commands.PingCommand;
import uk.gov.justice.services.management.shuttering.commands.ShutterCommand;
import uk.gov.justice.services.management.shuttering.commands.UnshutterCommand;

import java.util.List;
import java.util.Map;

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

            final List<SystemCommandDetails> systemCommandDetailsList = systemCommanderClient
                    .getRemote(CONTEXT_NAME)
                    .listCommands();

            assertThat(systemCommandDetailsList.size(), is(12));

            final Map<String, SystemCommandDetails> systemCommandDetailsMap = systemCommandDetailsList
                    .stream()
                    .collect(toMap(SystemCommandDetails::getName, systemCommandDetails -> systemCommandDetails));

            assertThat(systemCommandDetailsMap.get(ADD_TRIGGER), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(DISABLE_PUBLISHING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(ENABLE_PUBLISHING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(CATCHUP), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(INDEXER_CATCHUP), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(PING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(REBUILD), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(REMOVE_TRIGGER), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(SHUTTER), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(UNSHUTTER), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(VALIDATE_EVENTS), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(VERIFY_CATCHUP), is(notNullValue()));
        }
    }
}
