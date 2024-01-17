package uk.gov.justice.services.example.cakeshop.it.helpers;

import static java.nio.file.Paths.get;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class WildflyDeploymentDir {

    private static final String WILDFLY_VERSION = "wildfly-10.0.0.Final";
    private static final String PATH_IN_TARGET_DIR = WILDFLY_VERSION + "/standalone/deployments";

    @SuppressWarnings("ConstantConditions")
    public Path getPath() throws URISyntaxException {

        final URL classesDir = getClass().getClassLoader().getResource("");
        final Path targetDir = get(classesDir.toURI()).getParent();

        return targetDir.toAbsolutePath()
                .resolve(get(PATH_IN_TARGET_DIR));
    }
}
