package edu.illinois.library.cantaloupe.image.metadata;

import edu.illinois.library.cantaloupe.image.Operation;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates an operation that copies the EXIF metadata of a source image
 * into a derivative image.
 */
public class ExifCopy extends AbstractCopy implements Operation {

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Single-entry map with key of <var>operation</var> pointing to
     *         <code>exif_copy</code>.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("operation", "exif_copy");
        return map;
    }

    /**
     * @return The string <code>exifcopy</code>.
     */
    @Override
    public String toString() {
        return "exifcopy";
    }

}
