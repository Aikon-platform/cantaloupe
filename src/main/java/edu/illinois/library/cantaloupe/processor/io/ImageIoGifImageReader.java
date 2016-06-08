package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class ImageIoGifImageReader extends AbstractImageIoImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    ImageIoGifImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.GIF);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    ImageIoGifImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.GIF);
    }

    ImageIoMetadata getMetadata(int imageIndex) throws IOException {
        if (reader == null) {
            createReader();
        }
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = reader.getImageMetadata(imageIndex).
                getNativeMetadataFormatName();
        return new ImageIoGifMetadata(metadata, metadataFormat);
    }

}
