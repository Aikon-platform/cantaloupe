package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

/**
 * PNG image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * PNGs.
 *
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 * @see <a href="http://libpng.org/pub/png/spec/1.2/PNG-Contents.html">
 *     PNG Specification, Version 1.2</a>
 */
class ImageIoPngImageWriter extends AbstractImageIoImageWriter {

    ImageIoPngImageWriter(OperationList opList) {
        super(opList);
    }

    ImageIoPngImageWriter(OperationList opList,
                          ImageIoMetadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * @param baseTree Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @throws IOException
     */
    @Override
    protected void addIccProfile(final IIOMetadataNode baseTree,
                                 final IccProfile profile)
            throws IOException {
        final ICC_ColorSpace colorSpace =
                new ICC_ColorSpace(profile.getProfile());
        final byte[] compressedProfile =
                deflate(colorSpace.getProfile().getData());
        final IIOMetadataNode iccNode =
                new IIOMetadataNode("iCCP");
        iccNode.setAttribute("compressionMethod", "deflate");
        iccNode.setAttribute("profileName", profile.getName());
        iccNode.setUserObject(compressedProfile);

        baseTree.appendChild(iccNode);
    }

    /**
     * PNG doesn't (formally) support EXIF or IPTC, though it does support
     * EXIF tags in XMP.
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree)
            throws IOException {
        final Object xmp = sourceMetadata.getXmp();
        if (xmp instanceof String) {
            final NodeList itxtNodes = baseTree.getElementsByTagName("iTXt");
            IIOMetadataNode itxtNode;
            if (itxtNodes.getLength() > 0) {
                itxtNode = (IIOMetadataNode) itxtNodes.item(0);
            } else {
                itxtNode = new IIOMetadataNode("iTXt");
                baseTree.appendChild(itxtNode);
            }

            final IIOMetadataNode xmpNode = new IIOMetadataNode("iTXtEntry");
            xmpNode.setAttribute("keyword", "XML:com.adobe.xmp");
            xmpNode.setAttribute("compressionFlag", "FALSE");
            xmpNode.setAttribute("compressionMethod", "0");
            xmpNode.setAttribute("languageTag", "");
            xmpNode.setAttribute("translatedKeyword", "");
            xmpNode.setAttribute("text", (String) xmp);
            itxtNode.appendChild(xmpNode);
        }
    }

    /**
     * Applies Deflate compression to the given bytes.
     *
     * @param data Data to compress.
     * @return Deflate-compressed data.
     * @throws IOException
     */
    private byte[] deflate(final byte[] data) throws IOException {
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(deflated);
        deflater.write(data);
        deflater.flush();
        deflater.close();
        return deflated.toByteArray();
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image,
               final OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.PNG.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();
        try {
            final IIOMetadata metadata = getMetadata(
                    writer, writer.getDefaultWriteParam(), image);
            final IIOImage iioImage = new IIOImage(image, null, metadata);
            final ImageOutputStream os =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(os);
            writer.write(iioImage);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(PlanarImage image,
               OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.PNG.getPreferredMediaType().toString());
        final ImageWriter writer = writers.next();
        try {
            final IIOMetadata metadata = getMetadata(
                    writer, writer.getDefaultWriteParam(), image);
            final IIOImage iioImage = new IIOImage(image, null, metadata);
            final ImageOutputStream os =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(os);
            writer.write(iioImage);
        } finally {
            writer.dispose();
        }
    }

}
