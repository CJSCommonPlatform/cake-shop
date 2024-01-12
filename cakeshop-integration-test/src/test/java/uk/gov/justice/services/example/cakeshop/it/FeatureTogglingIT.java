package uk.gov.justice.services.example.cakeshop.it;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopMediaTypes.ADD_RECIPE_VERSION_2_MEDIA_TYPE;
import static uk.gov.justice.services.example.cakeshop.it.params.CakeShopUris.RECIPES_RESOURCE_URI;
import static uk.gov.justice.services.test.utils.core.matchers.HttpStatusCodeMatcher.isStatus;

import uk.gov.justice.services.core.featurecontrol.domain.Feature;
import uk.gov.justice.services.core.featurecontrol.domain.FeatureControl;
import uk.gov.justice.services.example.cakeshop.it.helpers.CommandFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.RestEasyClientFactory;
import uk.gov.justice.services.example.cakeshop.it.helpers.WildflyFeatureUpdater;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Fix and re-enable when the java 11 move has calmed down")
public class FeatureTogglingIT {

    private final CommandFactory commandFactory = new CommandFactory();
    private final WildflyFeatureUpdater wildflyFeatureUpdater = new WildflyFeatureUpdater();

    private Client client;

    @BeforeEach
    public void before() throws Exception {
        client = new RestEasyClientFactory().createResteasyClient();
        wildflyFeatureUpdater.deleteFeatureFile();
    }

    @AfterEach
    public void cleanup() throws Exception {
        client.close();
        wildflyFeatureUpdater.deleteFeatureFile();
    }

    @Test
    public void shouldRejectAddRecipeVersion2CommandIfTheFeatureIsMissing() throws Exception {

        assertThat(wildflyFeatureUpdater.readFeaturesInWildflyDeploymentDir().isPresent(), is(false));

        final String recipeId = randomUUID().toString();

        final Response response = client
                .target(RECIPES_RESOURCE_URI + recipeId)
                .request()
                .post(entity(commandFactory.addRecipeCommandWithAllergensListed(), ADD_RECIPE_VERSION_2_MEDIA_TYPE));

        assertThat(response.getStatus(), isStatus(FORBIDDEN));
    }

    @Test
    public void shouldAcceptAddRecipeVersion2CommandIfTheFeatureIsEnabled() throws Exception {

        final boolean enabled = true;
        final Feature feature = new Feature(
                "recipes-have-allergens-specified",
                enabled
        );

        wildflyFeatureUpdater.writeFeaturesToWildflyDeploymentDir(new FeatureControl(singletonList(feature)));
        assertThat(wildflyFeatureUpdater.readFeaturesInWildflyDeploymentDir().isPresent(), is(true));

        final String recipeId = randomUUID().toString();

        final Response response = client
                .target(RECIPES_RESOURCE_URI + recipeId)
                .request()
                .post(entity(commandFactory.addRecipeCommandWithAllergensListed(), ADD_RECIPE_VERSION_2_MEDIA_TYPE));

        assertThat(response.getStatus(), isStatus(ACCEPTED));
    }

    
    @Test
    public void shouldRejectAddRecipeVersion2CommandIfTheFeatureIsDisabled() throws Exception {

        final boolean enabled = false;
        final Feature feature = new Feature(
                "recipes-have-allergens-specified",
                enabled
        );

        wildflyFeatureUpdater.writeFeaturesToWildflyDeploymentDir(new FeatureControl(singletonList(feature)));
        assertThat(wildflyFeatureUpdater.readFeaturesInWildflyDeploymentDir().isPresent(), is(true));

        final String recipeId = randomUUID().toString();

        final Response response = client
                .target(RECIPES_RESOURCE_URI + recipeId)
                .request()
                .post(entity(commandFactory.addRecipeCommandWithAllergensListed(), ADD_RECIPE_VERSION_2_MEDIA_TYPE));

        assertThat(response.getStatus(), isStatus(FORBIDDEN));
    }
}
