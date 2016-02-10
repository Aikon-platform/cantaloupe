package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.resource.iiif.v1.Quality;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the <a href="https://pdfbox.apache.org">Apache PDFBox</a>
 * library to render source PDFs.
 */
class PdfBoxProcessor extends AbstractProcessor
        implements FileProcessor, StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(PdfBoxProcessor.class);

    public static final String DPI_CONFIG_KEY = "PdfBoxProcessor.dpi";
    public static final String JAVA2D_SCALE_MODE_CONFIG_KEY =
            "PdfBoxProcessor.post_processor.java2d.scale_mode";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    private BufferedImage fullImage;
    private File sourceFile;
    private StreamSource streamSource;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats() {
        Set<OutputFormat> outputFormats = new HashSet<>();
        if (sourceFormat == SourceFormat.PDF) {
            outputFormats.addAll(ImageIoImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<Quality> getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Dimension getSize() throws ProcessorException {
        try {
            if (fullImage == null) {
                // This is a very inefficient method of getting the size.
                // Unfortunately, it's apparently the only choice PDFBox offers.
                // At least cache it in an ivar to avoid having to load it
                // multiple times.
                fullImage = readImage();
            }
            return new Dimension(fullImage.getWidth(), fullImage.getHeight());
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public StreamSource getStreamSource() {
        return streamSource;
    }

    /**
     * @return One-element list of the full image dimensions, as this processor
     * doesn't recognize tiles.
     * @throws ProcessorException
     */
    @Override
    public List<Dimension> getTileSizes() throws ProcessorException {
        return new ArrayList<>(Collections.singletonList(getSize()));
    }

    @Override
    public void process(OperationList opList,
                        Dimension sourceSize,
                        OutputStream outputStream) throws ProcessorException {
        try {
            // If the op list contains a scale operation, see if we can use
            // a reduction factor in order to use a scale-appropriate
            // rasterization DPI.
            Scale scale = new Scale();
            scale.setMode(Scale.Mode.FULL);
            for (Operation op : opList) {
                if (op instanceof Scale) {
                    scale = (Scale) op;
                    break;
                }
            }
            float pct = scale.getResultingScale(sourceSize);
            final ReductionFactor rf = ReductionFactor.forScale(pct);

            final BufferedImage image = readImage(rf.factor);
            postProcessUsingJava2d(image, opList, rf, outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private void postProcessUsingJava2d(BufferedImage image,
                                        final OperationList opList,
                                        final ReductionFactor reductionFactor,
                                        final OutputStream outputStream)
            throws IOException, ProcessorException {
        for (Operation op : opList) {
            if (op instanceof Crop) {
                image = Java2dUtil.cropImage(image, (Crop) op);
            } else if (op instanceof Scale) {
                final boolean highQuality = Application.getConfiguration().
                        getString(JAVA2D_SCALE_MODE_CONFIG_KEY, "speed").
                        equals("quality");
                image = Java2dUtil.scaleImage(image,
                        (Scale) op, reductionFactor, highQuality);
            } else if (op instanceof Transpose) {
                image = Java2dUtil.transposeImage(image, (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2dUtil.rotateImage(image, (Rotate) op);
            } else if (op instanceof Filter) {
                image = Java2dUtil.filterImage(image, (Filter) op);
            } else if (op instanceof Watermark) {
                try {
                    image = Java2dUtil.applyWatermark(image, (Watermark) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        new ImageIoImageWriter().write(image, opList.getOutputFormat(),
                outputStream);
        image.flush();
    }

    private BufferedImage readImage() throws IOException {
        return readImage(0);
    }

    /**
     * @param reductionFactor Scale factor by which to reduce the image (or
     *                        enlarge it if negative).
     * @return Rasterized first page of the PDF.
     * @throws IOException
     */
    private BufferedImage readImage(int reductionFactor) throws IOException {
        float dpi = getDpi(reductionFactor);
        logger.debug("readImage(): using a DPI of {} ({}x reduction factor)",
                Math.round(dpi), reductionFactor);

        InputStream inputStream = null;
        PDDocument doc = null;
        try {
            if (sourceFile != null) {
                doc = PDDocument.load(sourceFile);
            } else {
                inputStream = streamSource.newInputStream();
                doc = PDDocument.load(inputStream);
            }
            return new PDFRenderer(doc).renderImageWithDPI(0, dpi);
        } finally {
            if (doc != null) {
                doc.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private float getDpi(int reductionFactor) {
        float dpi = Application.getConfiguration().getFloat(DPI_CONFIG_KEY, 72);
        // Decrease the DPI if the reduction factor is positive.
        for (int i = 0; i < reductionFactor; i++) {
            dpi /= 2f;
        }
        // Increase the DPI if the reduction factor is negative.
        for (int i = 0; i > reductionFactor; i--) {
            dpi *= 2f;
        }
        return dpi;
    }

    @Override
    public void setSourceFile(File sourceFile) {
        this.streamSource = null;
        this.sourceFile = sourceFile;
    }

    @Override
    public void setStreamSource(StreamSource streamSource) {
        this.sourceFile = null;
        this.streamSource = streamSource;
    }

}
