/*
 * Copyright (C) 2016-2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * As an exception to the terms of the GPL, you may copy, modify,
 * propagate, and distribute a work formed by combining 52°North WPS
 * GeoTools Modules with the Eclipse Libraries, or a work derivative of
 * such a combination, even if such copying, modification, propagation, or
 * distribution would otherwise violate the terms of the GPL. Nothing in
 * this exception exempts you from complying with the GPL in all respects
 * for all of the code used other than the Eclipse Libraries. You may
 * include this exception and its grant of permissions when you distribute
 * 52°North WPS GeoTools Modules. Inclusion of this notice with such a
 * distribution constitutes a grant of such permissions. If you do not wish
 * to grant these permissions, remove this paragraph from your
 * distribution. "52°North WPS GeoTools Modules" means the 52°North WPS
 * modules using GeoTools functionality - software licensed under version 2
 * or any later version of the GPL, or a work based on such software and
 * licensed under the GPL. "Eclipse Libraries" means Eclipse Modeling
 * Framework Project and XML Schema Definition software distributed by the
 * Eclipse Foundation and licensed under the Eclipse Public License Version
 * 1.0 ("EPL"), or a work based on such software and licensed under the EPL.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
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

    public static final String SUFFIX_XSD = "xsd";

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
