package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class ImageInfoFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ImageInfoFactory.class);

    /** Minimum size that will be used in info.json "sizes" keys. */
    private static final int MIN_SIZE = 64;

    /** Minimum size that will be used in info.json "tiles" keys. */
    private static final int MIN_TILE_SIZE = 512;

    /** Delegate script method that returns the JSON object (actually Ruby
     * hash) corresponding to the "service" key. */
    private static final String SERVICE_DELEGATE_METHOD = "get_iiif2_service";

    /** Will be populated in the static initializer. */
    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            new HashSet<>();

    static {
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.SIZE_BY_WHITELISTED);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.BASE_URI_REDIRECT);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.CANONICAL_LINK_HEADER);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.CORS);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.PROFILE_LINK_HEADER);
    }

    public static ImageInfo newImageInfo(final Identifier identifier,
                                         final String imageUri,
                                         final Processor processor)
            throws ProcessorException {
        final Dimension fullSize = processor.getSize();

        // Create an ImageInfo instance, which will eventually be serialized
        // to JSON.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageUri;
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES,
                processor.getSupportedFeatures(),
                processor.getSupportedIiif2_0Qualities(),
                processor.getAvailableOutputFormats()).getUri();
        imageInfo.profile.add(complianceUri);

        // sizes -- this will be a 2^n series that will work for both multi-
        // and monoresolution images.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(fullSize, MIN_SIZE);
        for (double i = 2; i <= Math.pow(2, maxReductionFactor); i *= 2) {
            final int width = (int) Math.round(fullSize.width / i);
            final int height = (int) Math.round(fullSize.height / i);
            if (width < MIN_SIZE || height < MIN_SIZE) {
                break;
            }
            ImageInfo.Size size = new ImageInfo.Size(width, height);
            imageInfo.sizes.add(0, size);
        }

        // tiles -- this is not a canonical listing of tiles that are
        // actually encoded in the image, but rather a hint to the client as
        // to what can be delivered efficiently.
        final List<Dimension> tileSizes = processor.getTileSizes();
        final Set<Dimension> uniqueTileSizes = new HashSet<>();

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE pixels. Otherwise,
        // use the smallest multiple of the tile size above MIN_TILE_SIZE
        // of image resolution 0.
        if (tileSizes.size() == 1 &&
                tileSizes.get(0).width == fullSize.width &&
                tileSizes.get(0).height == fullSize.height) {
            uniqueTileSizes.add(
                    ImageInfoUtil.smallestTileSize(fullSize, MIN_TILE_SIZE));
        } else {
            for (Dimension tileSize : tileSizes) {
                uniqueTileSizes.add(
                        ImageInfoUtil.smallestTileSize(fullSize, tileSize, MIN_TILE_SIZE));
            }
        }
        for (Dimension uniqueTileSize : uniqueTileSizes) {
            final ImageInfo.Tile tile = new ImageInfo.Tile();
            tile.width = uniqueTileSize.width;
            tile.height = uniqueTileSize.height;
            // Add every scale factor up to 2^n.
            for (int i = 0; i <= maxReductionFactor; i++) {
                tile.scaleFactors.add((int) Math.pow(2, i));
            }
            imageInfo.tiles.add(tile);
        }

        // formats
        Map<String, Set<String>> profileMap = new HashMap<>();
        Set<String> formatStrings = new HashSet<>();
        for (OutputFormat format : processor.getAvailableOutputFormats()) {
            formatStrings.add(format.getExtension());
        }
        profileMap.put("formats", formatStrings);
        imageInfo.profile.add(profileMap);

        // qualities
        Set<String> qualityStrings = new HashSet<>();
        for (Quality quality : processor.getSupportedIiif2_0Qualities()) {
            qualityStrings.add(quality.toString().toLowerCase());
        }
        profileMap.put("qualities", qualityStrings);

        // supports
        Set<String> featureStrings = new HashSet<>();
        for (Feature feature : processor.getSupportedFeatures()) {
            featureStrings.add(feature.getName());
        }
        for (Feature feature : SUPPORTED_SERVICE_FEATURES) {
            featureStrings.add(feature.getName());
        }
        profileMap.put("supports", featureStrings);

        // service
        try {
            final String[] args = { identifier.toString() };
            imageInfo.service = (Map) ScriptEngineFactory.getScriptEngine().
                    invoke(SERVICE_DELEGATE_METHOD, args);
        } catch (DelegateScriptDisabledException e) {
            logger.info("Delegate script disabled; skipping service " +
                    "information.");
        } catch (ScriptException | IOException e) {
            logger.error(e.getMessage());
        }

        return imageInfo;
    }

}
