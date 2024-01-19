package uk.gov.justice.services.cakeshop.it.helpers;

import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

import static uk.gov.justice.services.cakeshop.it.helpers.SystemPropertyFinder.findWildflyManagementPort;
import static uk.gov.justice.services.cakeshop.it.helpers.SystemPropertyFinder.hasRandomJmsPortConfigured;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

public class JmxParametersFactory {
    private static final String HOST = getHost();
    private static final int PORT = findWildflyManagementPort();
    private static final String STANDALONE_WILDFLY_USER = "admin";
    private static final String STANDALONE_WILDFLY_PASSWORD = "admin";

    public static JmxParameters buildJmxParameters() {
        final boolean runningWithEmbeddedWildfly = hasRandomJmsPortConfigured();
        return runningWithEmbeddedWildfly ? buildJmxParametersWithoutAuthForEmbeddedWildfly()
                : buildJmxParametersWitAuthForStandaloneWildfly();
    }

    private static JmxParameters buildJmxParametersWithoutAuthForEmbeddedWildfly() {
        return jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .build();
    }

    private static JmxParameters buildJmxParametersWitAuthForStandaloneWildfly() {
        return jmxParameters()
                .withHost(HOST)
                .withPort(PORT)
                .withUsername(STANDALONE_WILDFLY_USER)
                .withPassword(STANDALONE_WILDFLY_PASSWORD)
                .build();
    }
}
