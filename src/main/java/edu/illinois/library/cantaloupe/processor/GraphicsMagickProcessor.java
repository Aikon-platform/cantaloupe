package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.process.ArrayListOutputConsumer;
import edu.illinois.library.cantaloupe.process.Pipe;
import edu.illinois.library.cantaloupe.process.ProcessStarter;
import edu.illinois.library.cantaloupe.util.CommandLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the GraphicsMagick {@literal gm} command-line tool.
 * Tested with version 1.3.21; other versions may or may not work.</p>
 *
 * <p>Implementation notes:</p>
 *
 * <ul>
 *     <li>{@link FileProcessor} is not implemented because testing indicates
 *     that reading from streams is significantly faster.</li>
 *     <li>This processor is not metadata-aware. (See {@link #readInfo()}.)</li>
 * </ul>
 */
class GraphicsMagickProcessor extends AbstractMagickProcessor
        implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GraphicsMagickProcessor.class);

    private static final String GM_NAME = "gm";

    private static final AtomicBoolean IS_INITIALIZATION_ATTEMPTED =
            new AtomicBoolean(false);

    /**
     * Initialized by {@link #readFormats()}.
     */
    private static final Map<Format, Set<Format>> SUPPORTED_FORMATS =
            new HashMap<>();

    private static String initializationError;

    private Path sourceFile;

    private static String getPath() {
        String searchPath = Configuration.getInstance().
                getString(Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES);
        return CommandLocator.locate(GM_NAME, searchPath);
    }

    /**
     * Performs one-time class-level/shared initialization.
     */
    private static synchronized void initialize() {
        IS_INITIALIZATION_ATTEMPTED.set(true);
        readFormats();
    }

    /**
     * @return Map of available output formats for all known source formats,
     *         based on information reported by {@literal gm version}. The
     *         result is cached.
     */
    private static synchronized Map<Format, Set<Format>> readFormats() {
        if (SUPPORTED_FORMATS.isEmpty()) {
            final Set<Format> sourceFormats = EnumSet.noneOf(Format.class);
            final Set<Format> outputFormats = EnumSet.noneOf(Format.class);

            // Get the output of the `gm version` command, which contains
            // a list of all optional formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath());
            command.add("version");
            pb.command(command);
            final String commandString = String.join(" ", pb.command());

            try {
                LOGGER.debug("readFormats(): invoking {}", commandString);
                final Process process = pb.start();

                final InputStream processInputStream = process.getInputStream();
                try (BufferedReader bReader = new BufferedReader(
                        new InputStreamReader(processInputStream, StandardCharsets.UTF_8))) {
                    String s;
                    boolean read = false;
                    while ((s = bReader.readLine()) != null) {
                        if (s.contains("Feature Support")) {
                            read = true; // start reading
                        } else if (s.contains("Host type:")) {
                            break; // stop reading
                        }
                        if (read) {
                            s = s.trim();
                            if (s.startsWith("JPEG-2000 ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.JP2);
                                outputFormats.add(Format.JP2);
                            } else if (s.startsWith("JPEG ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.JPG);
                                outputFormats.add(Format.JPG);
                            } else if (s.startsWith("PNG ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.PNG);
                                outputFormats.add(Format.PNG);
                            } else if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.PDF);
                            } else if (s.startsWith("TIFF ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.TIF);
                                outputFormats.add(Format.TIF);
                            } else if (s.startsWith("WebP ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.WEBP);
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();

                    // Add formats that are not listed in the output of
                    // "gm version" but are definitely available
                    // (http://www.graphicsmagick.org/formats.html)
                    sourceFormats.add(Format.BMP);
                    sourceFormats.add(Format.DCM);
                    sourceFormats.add(Format.GIF);
                    outputFormats.add(Format.GIF);

                    for (Format format : sourceFormats) {
                        SUPPORTED_FORMATS.put(format, outputFormats);
                    }
                }
            } catch (IOException | InterruptedException e) {
                initializationError = e.getMessage();
                // This is safe to swallow.
            }
        }
        return SUPPORTED_FORMATS;
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        IS_INITIALIZATION_ATTEMPTED.set(false);
        initializationError = null;
        SUPPORTED_FORMATS.clear();
    }

    GraphicsMagickProcessor() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = readFormats().get(getSourceFormat());
        if (formats == null) {
            formats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return formats;
    }

    private List<String> getConvertArguments(final OperationList ops,
                                             final Info imageInfo) {
        final List<String> args = new ArrayList<>(30);
        args.add(getPath());
        args.add("convert");

        args.add("-auto-orient");

        // If we need to rasterize, and the op list contains a scale operation,
        // see if we can use it to compute a scale-appropriate DPI.
        // This needs to be done before the source argument is added.
        if (Format.ImageType.VECTOR.equals(imageInfo.getSourceFormat().getImageType())) {
            Scale scale = (Scale) ops.getFirst(Scale.class);
            if (scale == null) {
                scale = new ScaleByPercent();
            }
            args.add("-density");
            args.add("" + new RasterizationHelper().getDPI(
                    scale, imageInfo.getSize(), ops.getScaleConstraint()));
        }

        int pageIndex = getGMImageIndex(
                (String) ops.getOptions().get("page"),
                imageInfo.getSourceFormat());

        if (sourceFile != null) {
            args.add(getSourceFormat().getPreferredExtension() +
                    ":" + sourceFile + "[" + pageIndex + "]");
        } else {
            // :- = read from stdin
            args.add(getSourceFormat().getPreferredExtension() +
                    ":-[" + pageIndex + "]");
        }

        Encode encode = (Encode) ops.getFirst(Encode.class);

        // If the output format supports transparency, make the background
        // transparent. Otherwise, use a user-configurable background color.
        if (ops.getOutputFormat().supportsTransparency()) {
            args.add("-background");
            args.add("none");
        } else {
            if (encode != null) {
                final Color bgColor = encode.getBackgroundColor();
                if (bgColor != null) {
                    args.add("-background");
                    args.add(bgColor.toRGBHex());
                }
            }
        }

        final Dimension fullSize = imageInfo.getSize();

        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (crop.hasEffect(fullSize, ops)) {
                    final Rectangle cropArea = crop.getRectangle(
                            fullSize, ops.getScaleConstraint());
                    args.add("-crop");
                    args.add(String.format("%dx%d+%d+%d",
                            cropArea.intWidth(), cropArea.intHeight(),
                            cropArea.intX(), cropArea.intY()));
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (scale.hasEffect(fullSize, ops) ||
                        ops.getScaleConstraint().hasEffect()) {
                    final Scale.Filter scaleFilter = scale.getFilter();
                    if (scaleFilter != null) {
                        final String gmFilter = getGMFilter(scaleFilter);
                        if (gmFilter != null) {
                            args.add("-filter");
                            args.add(gmFilter);
                        }
                    }

                    final double scScale =
                            ops.getScaleConstraint().getRational().doubleValue();
                    args.add("-resize");
                    if (scale instanceof ScaleByPercent) {
                        args.add(((ScaleByPercent) scale).getPercent() * scScale * 100 + "%");
                    } else {
                        ScaleByPixels spix = (ScaleByPixels) scale;
                        switch (spix.getMode()) {
                            case FULL:
                                args.add(String.format("%dx%d",
                                        Math.round(fullSize.width() * scScale),
                                        Math.round(fullSize.height() * scScale)));
                                break;
                            case ASPECT_FIT_WIDTH:
                                args.add(spix.getWidth() + "x");
                                break;
                            case ASPECT_FIT_HEIGHT:
                                args.add("x" + spix.getHeight());
                                break;
                            case NON_ASPECT_FILL:
                                args.add(String.format("%dx%d!",
                                        spix.getWidth(), spix.getHeight()));
                                break;
                            case ASPECT_FIT_INSIDE:
                                args.add(String.format("%dx%d",
                                        spix.getWidth(), spix.getHeight()));
                                break;
                        }
                    }
                }
            } else if (op instanceof Transpose) {
                switch ((Transpose) op) {
                    case HORIZONTAL:
                        args.add("-flop");
                        break;
                    case VERTICAL:
                        args.add("-flip");
                        break;
                }
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (rotate.hasEffect(fullSize, ops)) {
                    args.add("-rotate");
                    args.add(Double.toString(rotate.getDegrees()));
                }
            } else if (op instanceof ColorTransform) {
                switch ((ColorTransform) op) {
                    case GRAY:
                        args.add("-colorspace");
                        args.add("Gray");
                        break;
                    case BITONAL:
                        args.add("-monochrome");
                        break;
                }
            } else if (op instanceof Sharpen) {
                if (op.hasEffect(fullSize, ops)) {
                    args.add("-unsharp");
                    args.add(Double.toString(((Sharpen) op).getAmount()));
                }
            } else if (op instanceof Encode) {
                encode = (Encode) op;
                switch (encode.getFormat()) {
                    case JPG:
                        // Quality
                        final int jpgQuality = encode.getQuality();
                        args.add("-quality");
                        args.add(String.format("%d%%", jpgQuality));
                        // Interlace
                        if (encode.isInterlacing()) {
                            args.add("-interlace");
                            args.add("Plane");
                        }
                        break;
                    case TIF:
                        // Compression
                        final Compression compression = encode.getCompression();
                        args.add("-compress");
                        args.add(getGMTIFFCompression(compression));
                        break;
                }
            }
        }

        args.add("-depth");
        args.add("8");

        // Write to stdout.
        args.add(encode.getFormat().getPreferredExtension() + ":-");

        return args;
    }

    /**
     * @return String suitable for passing to {@literal gm convert}'s
     *         {@literal -filter} argument, or {@literal null} if an equivalent
     *         is unknown.
     */
    private String getGMFilter(Scale.Filter filter) {
        // http://www.graphicsmagick.org/GraphicsMagick.html#details-filter
        switch (filter) {
            case BELL:
                return "hamming";
            case BICUBIC:
                return "catrom";
            case BOX:
                return "box";
            case BSPLINE:
                return "gaussian";
            case HERMITE:
                return "hermite";
            case LANCZOS3:
                return "lanczos";
            case MITCHELL:
                return "mitchell";
            case TRIANGLE:
                return "triangle";
        }
        return null;
    }

    /**
     * @param pageStr      Client-provided page number.
     * @param sourceFormat Format of the source image.
     * @return             GraphicsMagick image index argument.
     */
    private int getGMImageIndex(String pageStr, Format sourceFormat) {
        int index = 0;
        if (pageStr != null && Format.PDF.equals(sourceFormat)) {
            try {
                index = Integer.parseInt(pageStr) - 1;
            } catch (NumberFormatException e) {
                LOGGER.info("Page number from URI query string is not " +
                        "an integer; using page 1.");
            }
            index = Math.max(index, 0);
        }
        return index;
    }

    /**
     * @param compression May be {@literal null}.
     * @return            String suitable for passing to {@literal
     *                    gm convert}'s {@literal -compress} argument.
     */
    private String getGMTIFFCompression(Compression compression) {
        if (compression != null) {
            switch (compression) {
                case LZW:
                    return "LZW";
                case DEFLATE:
                    return "Zip";
                case JPEG:
                    return "JPEG";
                case RLE:
                    return "RLE";
            }
        }
        return "None";
    }

    @Override
    public String getInitializationError() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
        return initializationError;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public void process(final OperationList ops,
                        final Info info,
                        final OutputStream outputStream) throws ProcessorException {
        super.process(ops, info, outputStream);
        try {
            if (sourceFile != null) {
                processFromFile(ops, info, outputStream);
            } else {
                processFromStream(ops, info, outputStream);
            }
        } catch (Exception e) {
            throw new ProcessorException(e);
        }
    }

    private void processFromFile(final OperationList ops,
                                 final Info info,
                                 final OutputStream outputStream) throws Exception {
        final List<String> args  = getConvertArguments(ops, info);
        final ProcessStarter cmd = new ProcessStarter();
        cmd.setOutputConsumer(new Pipe(null, outputStream));
        LOGGER.debug("processFromFile(): invoking {}", String.join(" ", args));
        cmd.run(args);
    }

    private void processFromStream(final OperationList ops,
                                   final Info info,
                                   final OutputStream outputStream) throws Exception {
        try (InputStream inputStream = streamFactory.newInputStream()) {
            final List<String> args  = getConvertArguments(ops, info);
            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(new Pipe(null, outputStream));
            LOGGER.debug("processFromStream(): invoking {}", String.join(" ", args));
            cmd.run(args);
        }
    }

    /**
     * Note: it's tough to get all of the info needed to fully populate an
     * {@link Info} from GraphicsMagick. Getting most of it, including raw EXIF
     * and XMP data, would require at least three separate process invocations,
     * and two of them don't work reliably when reading from streams&mdash;and
     * then {@link Info.Image#getTileSize() tile sizes} are still missing. The
     * returned instance is therefore marked {@link Info#isComplete()
     * incomplete}.
     */
    @Override
    public Info readInfo() throws IOException {
        List<String> output;
        try {
            if (sourceFile != null) {
                output = readInfoFromFile();
            } else {
                output = readInfoFromStream();
            }

            if (!output.isEmpty()) {
                final int width  = Integer.parseInt(output.get(0));
                final int height = Integer.parseInt(output.get(1));
                // GM is not tile-aware, so set the tile size to the full
                // dimensions.
                final Info info = Info.builder()
                        .withSize(width, height)
                        .withTileSize(width, height)
                        .withFormat(getSourceFormat())
                        .build();
                info.setComplete(false);

                // Do we have an EXIF orientation to deal with?
                if (output.size() > 2) {
                    final int exifOrientation = Integer.parseInt(
                            output.get(2).replaceAll("[^\\d+]", ""));
                    info.setMetadata(new Metadata() {
                        @Override
                        public Orientation getOrientation() {
                            return Orientation.forEXIFOrientation(exifOrientation);
                        }
                    });
                }
                return info;
            }
            throw new IOException("readInfo(): nothing received on stdout");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * @return Command output.
     */
    private List<String> readInfoFromFile() throws Exception {
        final List<String> args = new ArrayList<>();
        args.add(getPath());
        args.add("identify");
        args.add("-ping");
        args.add("-format");
        // We need to read this even when not respecting orientation,
        // because GM's crop operation is orientation-unaware.
        args.add("%w\n%h");
        args.add(getSourceFormat().getPreferredExtension() +
                ":" + sourceFile + "[0]");

        final ArrayListOutputConsumer consumer =
                new ArrayListOutputConsumer();

        final ProcessStarter cmd = new ProcessStarter();
        cmd.setOutputConsumer(consumer);
        final String cmdString = String.join(" ", args).replace("\n", "");
        LOGGER.info("readInfo(): invoking {}", cmdString);
        cmd.run(args);

        return consumer.getOutput();
    }

    /**
     * @return Command output.
     */
    private List<String> readInfoFromStream() throws Exception {
        try (InputStream inputStream = streamFactory.newInputStream()) {
            final List<String> args = new ArrayList<>();
            args.add(getPath());
            args.add("identify");
            args.add("-ping");
            args.add("-format");
            // We need to read this even when not respecting orientation,
            // because GM's crop operation is orientation-unaware.
            args.add("%w\n%h");
            args.add(getSourceFormat().getPreferredExtension() + ":-[0]");

            final ArrayListOutputConsumer consumer =
                    new ArrayListOutputConsumer();

            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(consumer);
            final String cmdString = String.join(" ", args).replace("\n", "");
            LOGGER.info("readInfo(): invoking {}", cmdString);
            cmd.run(args);

            return consumer.getOutput();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        this.sourceFile = sourceFile;
    }

}
