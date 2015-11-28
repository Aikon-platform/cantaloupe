package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Filter;

public class QualityTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
        assertEquals(4, Quality.values().length);
    }

    public void testToFilter() {
        assertEquals(Filter.BITONAL, Quality.BITONAL.toFilter());
        assertEquals(Filter.DEFAULT, Quality.COLOR.toFilter());
        assertEquals(Filter.DEFAULT, Quality.DEFAULT.toFilter());
        assertEquals(Filter.GRAY, Quality.GRAY.toFilter());
    }

}
