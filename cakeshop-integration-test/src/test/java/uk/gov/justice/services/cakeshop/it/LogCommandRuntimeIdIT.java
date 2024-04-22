package uk.gov.justice.services.cakeshop.it;

import org.junit.Test;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.cakeshop.it.helpers.JmxParametersFactory.buildJmxParameters;
import static uk.gov.justice.services.cakeshop.it.helpers.TestConstants.CONTEXT_NAME;
import static uk.gov.justice.services.jmx.api.mbean.CommandRunMode.FORCED;
import static uk.gov.justice.services.management.ping.commands.LogRuntimeIdCommand.LOG_RUNTIME_ID;

public class LogCommandRuntimeIdIT {

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    @Test
    public void shouldListAllSystemCommands() throws Exception {

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(buildJmxParameters())) {

            final UUID commandRuntimeId = randomUUID();
            systemCommanderClient
                    .getRemote(CONTEXT_NAME)
                    .callWithRuntimeId(LOG_RUNTIME_ID, commandRuntimeId, FORCED);
        }
    }
}
