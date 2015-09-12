package edu.illinois.library.cantaloupe.processor;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a javax.imageio.stream.ImageInputStream for consumers that require a
 * java.io.InputStream instead.
 */
class ImageInputStreamWrapper extends InputStream {

    private ImageInputStream imageInputStream;

    public ImageInputStreamWrapper(ImageInputStream is) {
        this.imageInputStream = is;
    }

    public int read() throws IOException {
        return imageInputStream.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return imageInputStream.read(b, off, len);
    }

}
