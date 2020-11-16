package uk.gov.moj.cpp.featurecontrol.example;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.core.featurecontrol.FeatureFetcher;
import uk.gov.justice.services.core.featurecontrol.domain.Feature;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.slf4j.Logger;

@Default
@ApplicationScoped
public class HardCodedFeatureFetcher implements FeatureFetcher {

    private static final Feature HARD_CODED_DISABLED_FEATURE = new Feature(
            "recipes-have-allergens-specified",
            false
    );

    @Inject
    private Logger logger;

    @Override
    public List<Feature> fetchFeatures() {
        final List<Feature> features = singletonList(HARD_CODED_DISABLED_FEATURE);

        final List<String> featureNames = features.stream().map(Feature::getFeatureName).collect(toList());

        logger.warn("Stubbing fetch of hard coded features: " + featureNames);

        return features;
    }
}
