package org.n52.javaps.gt.io.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final String SUFFIX_KML = "kml";

    public static final String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    public static final String SUFFIX_JSON = "json";

    public static final String SUFFIX_ZIP = "zip";

    private static Logger LOGGER = LoggerFactory.getLogger(FileConstants.class);

    public static String dot(String suffix) {
        return "." + suffix;
    }

    public static File createTempFile() throws IOException {
        return createTempFile(dot(SUFFIX_TMP));
    }

    public static File createTempFile(String suffix) throws IOException {
        return File.createTempFile("tempfile" + UUID.randomUUID(), dot(suffix));
    }

    public static File writeTempFile(InputStream input) throws Exception {
        return writeTempFile(input, SUFFIX_TMP);
    }

    public static File writeTempFile(InputStream input,
            String suffix) throws Exception {

        File tempFile = createTempFile();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {

            tempFile.deleteOnExit();

            int i = input.read();
            while (i != -1) {
                fos.write(i);
                i = input.read();
            }
            fos.flush();
            fos.close();
            input.close();
        } catch (Exception e) {
            LOGGER.error("Could not write inputstream to file: " + tempFile.getAbsolutePath(), e);
            throw e;
        }

        return tempFile;
    }

}
