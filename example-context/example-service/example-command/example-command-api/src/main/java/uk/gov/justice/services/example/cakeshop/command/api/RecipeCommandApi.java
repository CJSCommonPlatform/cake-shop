package uk.gov.justice.services.example.cakeshop.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class RecipeCommandApi {

    @Inject
    Sender sender;

    @Handles("example.add-recipe")
    public void addRecipe(final JsonEnvelope envelope) {
        sender.send(
                envelop(envelope.payloadAsJsonObject())
                        .withName("example.command.add-recipe")
                        .withMetadataFrom(envelope));
    }

    @Handles("example.rename-recipe")
    public void renameRecipe(final JsonEnvelope envelope) {
        sender.send(
                envelop(envelope.payloadAsJsonObject())
                        .withName("example.command.rename-recipe")
                        .withMetadataFrom(envelope));
    }

    @Handles("example.remove-recipe")
    public void removeRecipe(final JsonEnvelope envelope) {
        sender.send(
                envelop(envelope.payloadAsJsonObject())
                        .withName("example.command.remove-recipe")
                        .withMetadataFrom(envelope));
    }

    @Handles("example.upload-photograph")
    public void uploadPhotograph(final JsonEnvelope envelope) {
        sender.send(
                envelop(envelope.payloadAsJsonObject())
                        .withName("example.command.upload-photograph")
                        .withMetadataFrom(envelope));
    }
}
