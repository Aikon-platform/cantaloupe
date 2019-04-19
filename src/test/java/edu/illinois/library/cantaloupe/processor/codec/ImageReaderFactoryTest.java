package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class ImageReaderFactoryTest extends BaseTest {

    private ImageReaderFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageReaderFactory();
    }

    @Test
    void testSupportedFormats() {
        final HashSet<Format> formats = new HashSet<>();
        for (String mediaTypeStr : ImageIO.getReaderMIMETypes()) {
            if (mediaTypeStr.length() < 1 || mediaTypeStr.equals("image/jp2")) {
                continue;
            }
            final Format format = new MediaType(mediaTypeStr).toFormat();
            if (format != null && !format.equals(Format.UNKNOWN)) {
                formats.add(format);
            }
        }
        assertEquals(formats, ImageReaderFactory.supportedFormats());
    }

    @Test
    void testNewImageReaderWithUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.newImageReader(Paths.get("/dev/null"), Format.UNKNOWN));
    }

    @Test
    void testNewImageReaderWithPath() throws Exception {
        instance.newImageReader(Paths.get("/dev/null"), Format.JPG);
    }

    @Test
    void testNewImageReaderWithInputStream() throws Exception {
        try (InputStream is = Files.newInputStream(TestUtil.getImage("jpg"))) {
            instance.newImageReader(is, Format.JPG);
        }
    }

    @Test
    void testNewImageReaderWithImageInputStream() throws Exception {
        try (ImageInputStream iis = ImageIO.createImageInputStream(TestUtil.getImage("jpg").toFile())) {
            instance.newImageReader(iis, Format.JPG);
        }
    }

    @Test
    void testNewImageReaderWithStreamSource() throws Exception {
        StreamFactory source = new PathStreamFactory(Paths.get("/dev/null"));
        instance.newImageReader(source, Format.JPG);
    }

}
