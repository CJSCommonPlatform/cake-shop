package uk.gov.justice.services.example.cakeshop.command.api;


import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.featurecontrol.FeatureControlAnnotationFinder;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class RecipeCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private Logger logger;

    @InjectMocks
    private RecipeCommandApi commandApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleRecipeCommands() throws Exception {
        assertThat(RecipeCommandApi.class, isHandlerClass(COMMAND_API)
                .with(method("addRecipe")
                        .thatHandles("example.add-recipe"))
                .with(method("renameRecipe")
                        .thatHandles("example.rename-recipe"))
                .with(method("removeRecipe")
                        .thatHandles("example.remove-recipe"))
                .with(method("uploadPhotograph")
                        .thatHandles("example.upload-photograph"))
        );
    }

    @Test
    public void shouldHandleAddRecipeRequest() {
        commandApi.addRecipe(buildEnvelopeWith("example.add-recipe"));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("example.command.add-recipe"));
    }

    @Test
    public void shouldHandleRenameRecipeRequest() {
        commandApi.renameRecipe(buildEnvelopeWith("example.rename-recipe"));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("example.command.rename-recipe"));
    }

    @Test
    public void shouldHandleRemoveRecipeRequest() {
        commandApi.removeRecipe(buildEnvelopeWith("example.remove-recipe"));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("example.command.remove-recipe"));
    }

    @Test
    public void shouldHandleUploadPhotographRequest() {
        commandApi.uploadPhotograph(buildEnvelopeWith("example.upload-photograpgh"));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("example.command.upload-photograph"));
    }

    @Test
    public void shouldHaveAnInProgressMethodHiddenBehindAFeature() throws Exception {

        final Method inProgressMethod = commandApi
                .getClass()
                .getMethod("addRecipeWithAllergenSupport", JsonEnvelope.class);

        final List<String> annotatedFeatures = new FeatureControlAnnotationFinder()
                .findAnnotatedFeatures(inProgressMethod);

        assertThat(annotatedFeatures.size(), is(1));
        assertThat(annotatedFeatures, hasItem("recipes-have-allergens-specified"));

        commandApi.addRecipeWithAllergenSupport(mock(JsonEnvelope.class));

        verify(logger).warn("Call to in progress method. Feature 'recipes-have-allergens-specified' is enabled");
    }

    private JsonEnvelope buildEnvelopeWith(final String name) {
        return envelope()
                .with(metadataWithDefaults().withName(name))
                .withPayloadOf("Field", "Value").build();
    }
}
