package uk.gov.justice.services.cakeshop.it;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.cakeshop.it.helpers.JmxParametersFactory;
import uk.gov.justice.services.jmx.api.command.SystemCommandDetails;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.eventstore.management.commands.DisablePublishingCommand.DISABLE_PUBLISHING;
import static uk.gov.justice.services.eventstore.management.commands.EnablePublishingCommand.ENABLE_PUBLISHING;
import static uk.gov.justice.services.eventstore.management.commands.EventCatchupCommand.CATCHUP;
import static uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand.INDEXER_CATCHUP;
import static uk.gov.justice.services.eventstore.management.commands.RebuildCommand.REBUILD;
import static uk.gov.justice.services.eventstore.management.commands.ValidatePublishedEventsCommand.VALIDATE_EVENTS;
import static uk.gov.justice.services.eventstore.management.commands.VerifyCatchupCommand.VERIFY_CATCHUP;
import static uk.gov.justice.services.eventstore.management.commands.VerifyRebuildCommand.VERIFY_REBUILD;
import static uk.gov.justice.services.cakeshop.it.helpers.TestConstants.CONTEXT_NAME;
import static uk.gov.justice.services.management.ping.commands.PingCommand.PING;
import static uk.gov.justice.services.management.suspension.commands.RefreshFeatureControlCacheCommand.REFRESH_FEATURE_CACHE;
import static uk.gov.justice.services.management.suspension.commands.SuspendCommand.SUSPEND;
import static uk.gov.justice.services.management.suspension.commands.UnsuspendCommand.UNSUSPEND;

public class ListSystemCommandsIT {
    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(JmxParametersFactory.buildJmxParameters())) {

            final List<SystemCommandDetails> systemCommandDetailsList = systemCommanderClient
                    .getRemote(CONTEXT_NAME)
                    .listCommands();

            assertThat(systemCommandDetailsList.size(), is(12));

            final Map<String, SystemCommandDetails> systemCommandDetailsMap = systemCommandDetailsList
                    .stream()
                    .collect(toMap(SystemCommandDetails::getName, systemCommandDetails -> systemCommandDetails));

            assertThat(systemCommandDetailsMap.get(DISABLE_PUBLISHING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(ENABLE_PUBLISHING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(CATCHUP), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(INDEXER_CATCHUP), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(PING), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(REBUILD), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(REFRESH_FEATURE_CACHE), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(SUSPEND), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(UNSUSPEND), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(VALIDATE_EVENTS), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(VERIFY_REBUILD), is(notNullValue()));
            assertThat(systemCommandDetailsMap.get(VERIFY_CATCHUP), is(notNullValue()));
        }
    }
}
