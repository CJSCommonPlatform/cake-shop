package uk.gov.justice.services.cakeshop.it.helpers;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.write;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.domain.FeatureControl;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WildflyFeatureUpdater {

    private static final String FEATURE_CONTROL_YAML = "feature-control.yaml";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().yamlObjectMapper();
    private final WildflyDeploymentDir wildflyDeploymentDir = new WildflyDeploymentDir();

    public void writeFeaturesToWildflyDeploymentDir(final FeatureControl featureControl) throws Exception{


        final String yaml = objectMapper.writeValueAsString(featureControl);

        final File featureControlFile = getFeatureControlFile(FEATURE_CONTROL_YAML);
        write(featureControlFile, yaml);

        System.out.println("Wrote feature file '" + featureControlFile.getAbsolutePath() + "'");
    }

    public Optional<FeatureControl> readFeaturesInWildflyDeploymentDir() throws Exception {

        final File featureControlFile = getFeatureControlFile(FEATURE_CONTROL_YAML);

        if (featureControlFile.exists())  {
            return of(objectMapper.readValue(featureControlFile, FeatureControl.class));
        }

        return empty();
    }

    public void deleteFeatureFile() throws Exception {

        final File featureControlFile = getFeatureControlFile(FEATURE_CONTROL_YAML);

        deleteQuietly(featureControlFile);
    }

    @SuppressWarnings("SameParameterValue")
    private File getFeatureControlFile(final String featureControlFile) throws URISyntaxException {
        return new File(wildflyDeploymentDir.getPath().toFile(), featureControlFile);
    }
}
