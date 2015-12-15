package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

abstract class NumberUtil {

    public static String formatForUrl(Float f) {
        String s = f.toString();
        return s.indexOf(".") < 0 ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

}
