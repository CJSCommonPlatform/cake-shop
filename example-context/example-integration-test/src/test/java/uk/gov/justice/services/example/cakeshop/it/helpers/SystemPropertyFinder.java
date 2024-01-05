package uk.gov.justice.services.example.cakeshop.it.helpers;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/*
    Random ports are set through pom.xml and this capability is required to run integration tests using embedded wildfly for travis pipeline.
    However it's getting hard to debug any ITs with embedded wildfly.
    So, to provide better debugging capability application can be deployed to local dev environment and tests can be executed from intellij/IDE which points to local environment.
    When test is run through IDE these system properties are not going to be populated, so it will return default values that are related to local dev environment
*/
public class SystemPropertyFinder {

    public static final String STANDALONE_ARTEMIS_PORT = "61616";
    public static final String STANDALONE_ARTEMIS_USER_NAME = "admin";
    public static final String STANDALONE_ARTEMIS_USER_PASSWORD = "password";
    public static final String STANDALONE_HA_PROXY_HTTP_PORT = "8080";
    public static final String STANDALONE_WILDFLY_MANAGEMENT_PORT = "9990";

    public static String findJmsPort() {
        return findValue("random.jms.port", STANDALONE_ARTEMIS_PORT);
    }

    public static String findWildflyHttpPort() {
        return findValue("random.http.port", STANDALONE_HA_PROXY_HTTP_PORT);
    }

    public static Integer findWildflyManagementPort() {
        return Integer.valueOf(findValue("random.management.port", STANDALONE_WILDFLY_MANAGEMENT_PORT));
    }

    public static String findJmsUserName() {
        return findValue("jms.user.name", STANDALONE_ARTEMIS_USER_NAME);
    }

    public static String findJmsUserPassword() {
        return findValue("jms.user.password", STANDALONE_ARTEMIS_USER_PASSWORD);
    }

    public static boolean hasRandomJmsPortConfigured() {
        return !isEmpty(System.getProperty("random.http.port"));
    }

    private static String findValue(final String propertyName, final String defaultValue) {
        String value = System.getProperty(propertyName);
        return isEmpty(value) ? defaultValue : value;
    }
}
