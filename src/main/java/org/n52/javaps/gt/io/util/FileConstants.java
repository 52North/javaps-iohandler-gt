package org.n52.javaps.gt.io.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class FileConstants {

    public static final String SUFFIX_SHP = "shp";
    public static final String SUFFIX_SHX = "shx";
    public static final String SUFFIX_DBF = "dbf";
    public static final String SUFFIX_PRF = "prj";
    public static final String SUFFIX_GML2 = "gml2";
    public static final String SUFFIX_GML3 = "gml3";
    public static final String SUFFIX_TIFF = "tiff";
    public static final String SUFFIX_TIF = "tif";
    public static final String SUFFIX_TMP = "tmp";
    public static final String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");
    public static final String SUFFIX_JSON = "json";

    public static String dot(String suffix) {
        return "." + suffix;
    }

    public static File createTempFile() throws IOException {
        return File.createTempFile("tempfile" + UUID.randomUUID(), dot(SUFFIX_TMP));
    }

    public static File createTempFile(String suffix) throws IOException {
        return File.createTempFile("tempfile" + UUID.randomUUID(), dot(suffix));
    }

}
