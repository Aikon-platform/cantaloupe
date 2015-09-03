package edu.illinois.library.cantaloupe.request;

public enum Format {

    GIF("gif", "image/gif"),
    JP2("jp2", "image/jp2"),
    JPG("jpg", "image/jpeg"),
    PDF("pdf", "application/pdf"),
    PNG("png", "image/png"),
    TIF("tif", "image/tiff"),
    WEBP("webp", "image/webp");

    private String extension;
    private String mediaType;

    Format(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getMediaType() {
        return this.mediaType;
    }

}
