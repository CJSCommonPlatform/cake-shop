package uk.gov.justice.services.cakeshop.query.view.response;


import java.util.UUID;

public class PhotoView {
    private UUID fileId;

    public PhotoView(final UUID fileId) {
        this.fileId = fileId;
    }

    public UUID getFileId() {
        return fileId;
    }
}
