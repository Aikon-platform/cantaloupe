package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class PngImageReader extends AbstractImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    PngImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.PNG);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    PngImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.PNG);
    }

    Metadata getMetadata(int imageIndex) throws IOException {
        if (iioReader == null) {
            createReader();
        }
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new PngMetadata(metadata, metadataFormat);
    }

}
