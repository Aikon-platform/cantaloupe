package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.apache.commons.lang3.StringUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.process.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the ImageMagick `convert` and `identify` command-line tools.
 */
class ImageMagickProcessor extends AbstractProcessor
        implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    public static final String BACKGROUND_COLOR_CONFIG_KEY =
            "ImageMagickProcessor.background_color";
    public static final String PATH_TO_BINARIES_CONFIG_KEY =
            "ImageMagickProcessor.path_to_binaries";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();
    // Lazy-initialized by getFormats()
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

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

    /**
     * @param binaryName Name of an executable
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Application.getConfiguration().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<>();
            final Set<OutputFormat> outputFormats = new HashSet<>();

            // Retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath("identify"));
            command.add("-list");
            command.add("format");
            pb.command(command);
            final String commandString = StringUtils.join(pb.command(), " ");

            try {
                logger.info("Executing {}", commandString);
                final Process process = pb.start();
                final InputStream processInputStream = process.getInputStream();

                try {
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(processInputStream));
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        s = s.trim();
                        if (s.startsWith("JP2")) {
                            sourceFormats.add(SourceFormat.JP2);
                            if (s.contains(" rw")) {
                                outputFormats.add(OutputFormat.JP2);
                            }
                        }
                        if (s.startsWith("JPEG")) {
                            sourceFormats.add(SourceFormat.JPG);
                            if (s.contains(" rw")) {
                                outputFormats.add(OutputFormat.JPG);
                            }
                        }
                        if (s.startsWith("PNG")) {
                            sourceFormats.add(SourceFormat.PNG);
                            if (s.contains(" rw")) {
                                outputFormats.add(OutputFormat.PNG);
                            }
                        }
                        if (s.startsWith("PDF") && s.contains(" rw")) {
                            outputFormats.add(OutputFormat.PDF);
                        }
                        if (s.startsWith("TIFF")) {
                            sourceFormats.add(SourceFormat.TIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(OutputFormat.TIF);
                            }
                        }
                        if (s.startsWith("WEBP")) {
                            sourceFormats.add(SourceFormat.WEBP);
                            if (s.contains(" rw")) {
                                outputFormats.add(OutputFormat.WEBP);
                            }
                        }
                    }
                    process.waitFor();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    processInputStream.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            supportedFormats = new HashMap<>();
            for (SourceFormat sourceFormat : sourceFormats) {
                supportedFormats.put(sourceFormat, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats() {
        Set<OutputFormat> formats = getFormats().get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    @Override
    public Dimension getSize() throws ProcessorException {
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        InputStream inputStream = null;
        try {
            inputStream = streamSource.newInputStream();
            Info sourceInfo = new Info(
                    sourceFormat.getPreferredExtension() + ":-", inputStream,
                    true);
            return new Dimension(sourceInfo.getImageWidth(),
                    sourceInfo.getImageHeight());
        } catch (IM4JavaException | IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public StreamSource getStreamSource() {
        return this.streamSource;
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
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
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
    public void process(final OperationList ops,
                        final Dimension fullSize,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            IMOperation op = new IMOperation();
            op.addImage(sourceFormat.getPreferredExtension() + ":-"); // read from stdin
            assembleOperation(op, ops, fullSize);

            op.addImage(ops.getOutputFormat().getExtension() + ":-"); // write to stdout

            ConvertCmd convert = new ConvertCmd();

            String binaryPath = Application.getConfiguration().
                    getString(PATH_TO_BINARIES_CONFIG_KEY, "");
            if (binaryPath.length() > 0) {
                convert.setSearchPath(binaryPath);
            }

            InputStream inputStream = streamSource.newInputStream();
            try {
                convert.setInputProvider(new Pipe(inputStream, null));
                convert.setOutputConsumer(new Pipe(null, outputStream));
                convert.run(op);
            } finally {
                inputStream.close();
            }
        } catch (IOException | IM4JavaException | InterruptedException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

    private void assembleOperation(IMOperation imOp, OperationList ops,
                                   Dimension fullSize) {
        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (!crop.isNoOp()) {
                    if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                        // im4java doesn't support cropping x/y by percentage
                        // (only width/height), so we have to calculate them.
                        int x = Math.round(crop.getX() * fullSize.width);
                        int y = Math.round(crop.getY() * fullSize.height);
                        int width = Math.round(crop.getWidth() * 100);
                        int height = Math.round(crop.getHeight() * 100);
                        imOp.crop(width, height, x, y, "%");
                    } else {
                        imOp.crop(Math.round(crop.getWidth()),
                                Math.round(crop.getHeight()),
                                Math.round(crop.getX()),
                                Math.round(crop.getY()));
                    }
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (!scale.isNoOp()) {
                    if (scale.getPercent() != null) {
                        imOp.resize(Math.round(scale.getPercent() * 100),
                                Math.round(scale.getPercent() * 100), "%");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        imOp.resize(scale.getWidth());
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        imOp.resize(null, scale.getHeight());
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        imOp.resize(scale.getWidth(), scale.getHeight(), "!");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        imOp.resize(scale.getWidth(), scale.getHeight());
                    }
                }
            } else if (op instanceof Transpose) {
                switch ((Transpose) op) {
                    case HORIZONTAL:
                        imOp.flop();
                        break;
                    case VERTICAL:
                        imOp.flip();
                        break;
                }
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (!rotate.isNoOp()) {
                    // If the output format supports transparency, make the
                    // background transparent. Otherwise, use a
                    // user-configurable background color.
                    if (ops.getOutputFormat().supportsTransparency()) {
                        imOp.background("none");
                    } else {
                        final String bgColor = Application.getConfiguration().
                                getString(BACKGROUND_COLOR_CONFIG_KEY, "black");
                        imOp.background(bgColor);
                    }
                    imOp.rotate((double) rotate.getDegrees());
                }
            } else if (op instanceof Filter) {
                switch ((Filter) op) {
                    case GRAY:
                        imOp.colorspace("Gray");
                        break;
                    case BITONAL:
                        imOp.monochrome();
                        break;
                }
            }
        }
    }

}
