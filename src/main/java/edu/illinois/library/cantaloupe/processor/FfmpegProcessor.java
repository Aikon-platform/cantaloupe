package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the ffmpeg and ffprobe tools to extract video frames.
 */
class FfmpegProcessor implements FileProcessor, StreamProcessor {

    private class StreamCopier implements Runnable {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        public StreamCopier(InputStream is, OutputStream os) {
            inputStream = is;
            outputStream = os;
        }

        public void run() {
            try {
                IOUtils.copy(inputStream, outputStream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(FfmpegProcessor.class);

    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

    static {
        //SUPPORTED_QUALITIES.add(Quality.BITONAL);
        SUPPORTED_QUALITIES.add(Quality.COLOR);
        SUPPORTED_QUALITIES.add(Quality.DEFAULT);
        //SUPPORTED_QUALITIES.add(Quality.GRAY);

        //SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        //SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
        //SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_ABOVE_FULL);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
    }

    /**
     * @param binaryName Name of one of the kdu_* binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Application.getConfiguration().
                getString("FfmpegProcessor.path_to_binaries");
        if (path != null) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    /**
     * Quotes command-line parameters with spaces.
     *
     * @param path
     * @return
     */
    private static String quote(String path) {
        if (path.contains(" ")) {
            path = "\"" + path + "\"";
        }
        return path;
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> outputFormats = new HashSet<>();
        outputFormats.add(OutputFormat.JPG);
        return outputFormats;
    }

    /**
     * Gets the size of the given video by parsing the output of ffprobe.
     *
     * @param inputFile Source image
     * @param sourceFormat Format of the source image
     * @return
     * @throws ProcessorException
     */
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        final List<String> command = new ArrayList<>();
        // ffprobe -v quiet -print_format xml -show_streams <file>
        command.add(getPath("ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("xml");
        command.add("-show_streams");
        command.add(inputFile.getAbsolutePath());
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(process.getInputStream());

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//stream[@index=\"0\"]/@width");
            int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            expr = xpath.compile("//stream[@index=\"0\"]/@height");
            int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            return new Dimension(width, height);
        } catch (SAXException e) {
            throw new ProcessorException("Failed to parse XML. Command: " +
                    StringUtils.join(command, " "), e);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public Dimension getSize(ImageInputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException {
        final List<String> command = new ArrayList<>();
        // ffprobe -v quiet -print_format xml -show_streams pipe: < <file>
        command.add(getPath("ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("xml");
        command.add("-show_streams");
        command.add("pipe:");

        final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = new ProcessBuilder(command);

            logger.debug("Executing " + StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                IOUtils.copy(new ImageInputStreamWrapper(inputStream), process.getOutputStream());
            } catch (IOException e) {
                if (e.getMessage().startsWith("Broken pipe")) {
                    // this is expected and not a problem
                }
            }

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffmpeg returned with code " + code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(bais);
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("//stream[@index=\"0\"]/@width");
                int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
                expr = xpath.compile("//stream[@index=\"0\"]/@height");
                int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
                return new Dimension(width, height);
            } finally {
                process.getInputStream().close();
                //process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
        } catch (SAXException e) {
            throw new ProcessorException("Failed to parse XML. Command: " +
                    StringUtils.join(command, " "), e);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<Quality> getSupportedQualities(SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        File inputFile, OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final Dimension fullSize = getSize(inputFile, sourceFormat);
            final ProcessBuilder pb = getProcessBuilder(params, fullSize,
                    inputFile);
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffmpeg returned with code " + code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                IOUtils.copy(bais, outputStream);
            } finally {
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final Dimension fullSize = getSize(inputStream, sourceFormat);
            final ProcessBuilder pb = getProcessBuilder(params, fullSize);

            logger.debug("Executing " + StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                IOUtils.copy(new ImageInputStreamWrapper(inputStream), process.getOutputStream());
            } catch (IOException e) {
                if (e.getMessage().startsWith("Broken pipe")) {
                    // this is expected and not a problem
                }
            }

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffmpeg returned with code " + code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                IOUtils.copy(bais, outputStream);
            } finally {
                process.getInputStream().close();
                //process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param params
     * @param fullSize The full size of the source image
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(Parameters params,
                                             Dimension fullSize) {
        return getProcessBuilder(params, fullSize, "pipe:");
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param params
     * @param fullSize The full size of the source image
     * @param inputFile
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(Parameters params,
                                             Dimension fullSize,
                                             File inputFile) {
        return getProcessBuilder(params, fullSize,
                quote(inputFile.getAbsolutePath()));
    }

    /**
     * @param params
     * @param fullSize
     * @param inputArg Either an absolute pathname or <code>pipe:</code>
     * @return
     */
    private ProcessBuilder getProcessBuilder(Parameters params,
                                             Dimension fullSize,
                                             String inputArg) {
        /*
        file i/o
        ffmpeg -i "My Great Movie.avi" -nostdin -v quiet -vframes 1 -an -vf crop="200:200:0:0",scale="min(320\\, iw):-1" out.jpg
        stdin
        ffmpeg -i /dev/stdin -nostdin -v quiet -vframes 1 -an -vf crop="200:200:0:0",scale="min(320\\, iw):-1" out.jpg < My\ Great\ Movie.avi
        stdout
        ffmpeg -i "My Great Movie.avi" -nostdin -v quiet -vframes 1 -an -vf crop="200:200:0:0",scale="min(320\\, iw):-1" -f jpg /dev/stdout > out.jpg
        stdin/stdout
        ffmpeg -i pipe: -nostdin -v quiet -vframes 1 -an -vf crop="200:200:0:0",scale="min(320\\, iw):-1" -f image2pipe pipe:1 < My\ Great\ Movie.avi > out.jpg
        */
        final List<String> command = new ArrayList<>();
        command.add(getPath("ffmpeg"));
        command.add("-i");
        command.add(inputArg);
        command.add("-nostdin");
        //command.add("-v");
        //command.add("quiet");
        command.add("-vframes");
        command.add("1");
        command.add("-an"); // disable audio

        final Region region = params.getRegion();
        final Size size = params.getSize();
        final Rotation rotation = params.getRotation();

        if (!region.isFull() || size.getScaleMode() != Size.ScaleMode.FULL ||
                rotation.shouldMirror() || rotation.getDegrees() > 0) {
            command.add("-vf");

            StringBuilder filterStr = new StringBuilder();

            if (!region.isFull()) {
                Rectangle cropArea = region.getRectangle(fullSize);
                filterStr.append(String.format("crop=%d:%d:%d:%d", cropArea.width,
                        cropArea.height, cropArea.x, cropArea.y));
            }
            if (size.getScaleMode() != Size.ScaleMode.FULL) {
                filterStr.append(",");
                if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                    filterStr.append(String.format("scale=min(%d\\, iw):-1",
                            size.getWidth()));
                } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                    filterStr.append(String.format("scale=min(%d\\, iw):-1",
                            size.getHeight()));
                } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                    int min = Math.min(size.getWidth(), size.getHeight());
                    filterStr.append(String.format("scale=min(%d\\, iw):-1", min));
                } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                    // TODO: write this
                }
            }
            if (rotation.shouldMirror()) {
                // TODO: write this
            }
            if (rotation.getDegrees() > 0) {
                // TODO: write this
            }
            command.add(filterStr.toString());
        }

        command.add("-f"); // source or output format depending on position
        command.add("image2pipe");
        command.add("pipe:1");

        return new ProcessBuilder(command);
    }

}
