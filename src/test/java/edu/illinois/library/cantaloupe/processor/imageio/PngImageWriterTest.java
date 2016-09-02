package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.MetadataCopy;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class PngImageWriterTest {

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final File fixture = TestUtil.getImage("png-xmp.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndNativeMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("png-nativemetadata.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForNativeMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXmpMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("png-xmp.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final File fixture = TestUtil.getImage("png-xmp.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithPlanarImageAndNativeMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("png-nativemetadata.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForNativeMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        final File fixture = TestUtil.getImage("png-xmp.png");
        final PngImageReader reader = new PngImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    private void checkForIccProfile(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            final Node iccNode = tree.getChildNodes().item(1);
            assertEquals("iCCP", iccNode.getNodeName());
        } finally {
            reader.dispose();
        }
    }

    private void checkForNativeMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList textNodes = tree.getElementsByTagName("tEXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("Title".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private void checkForXmpMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList textNodes = tree.getElementsByTagName("iTXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("XML:com.adobe.xmp".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private PngImageWriter getWriter(Metadata metadata) throws IOException {
        OperationList opList = new OperationList();
        if (Configuration.getInstance().
                getBoolean(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }
        return new PngImageWriter(opList, metadata);
    }

}
