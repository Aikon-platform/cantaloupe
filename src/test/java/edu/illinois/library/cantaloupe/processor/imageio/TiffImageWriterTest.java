package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TiffImageWriterTest extends BaseTest {

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final File fixture = TestUtil.getImage("tif-xmp.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-exif.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-iptc.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-xmp.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final File fixture = TestUtil.getImage("tif-xmp.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-exif.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-iptc.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("tif-xmp.tif");
        final TiffImageReader reader = new TiffImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    private void checkForIccProfile(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            final TIFFTag tag = dir.getTag(BaselineTIFFTagSet.TAG_ICC_PROFILE);
            assertNotNull(tag);
        } finally {
            reader.dispose();
        }
    }

    private void checkForExifMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            final TIFFTag tag = dir.getTag(EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
            assertNotNull(tag);
        } finally {
            reader.dispose();
        }
    }

    private void checkForIptcMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(33723));
        } finally {
            reader.dispose();
        }
    }

    private void checkForXmpMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(700));
        } finally {
            reader.dispose();
        }
    }

    private TiffImageWriter newWriter(Metadata metadata) throws IOException {
        OperationList opList = new OperationList();
        if (ConfigurationFactory.getInstance().
                getBoolean(Processor.PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }
        return new TiffImageWriter(opList, metadata);
    }

}
