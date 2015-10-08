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

    @Override
    public void close() throws IOException {
        imageInputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        imageInputStream.mark();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        return imageInputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return imageInputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return imageInputStream.read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        imageInputStream.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return imageInputStream.skipBytes(n);
    }

}
