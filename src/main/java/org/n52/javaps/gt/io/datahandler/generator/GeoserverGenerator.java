/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.javaps.gt.io.datahandler.generator;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.gt.io.data.GenericFileDataWithGT;
import org.n52.javaps.gt.io.data.binding.complex.GTRasterDataBinding;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.data.binding.complex.GenericFileDataWithGTBinding;
import org.n52.javaps.gt.io.data.binding.complex.ShapefileBinding;
import org.n52.javaps.gt.io.util.FileConstants;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.OutputHandler;
import org.n52.javaps.io.data.binding.complex.GeotiffBinding;
import org.n52.javaps.utils.IOUtils;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.referencing.ReferenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(
        propertyFileName = "geoservergenerator.json",
        defaultPropertyFileName = "geoservergenerator.default.json")
public class GeoserverGenerator extends AbstractPropertiesInputOutputHandler implements OutputHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(GeoserverGenerator.class);

    private GeotiffGenerator geotiffGenerator;

    private String geoServerURL;

    private GeoServerUploader geoServerUploader;

    public GeoserverGenerator() {
        super();
        addSupportedBinding(GTRasterDataBinding.class);
        addSupportedBinding(ShapefileBinding.class);
        addSupportedBinding(GeotiffBinding.class);
        addSupportedBinding(GTVectorDataBinding.class);
        addSupportedBinding(GenericFileDataWithGTBinding.class);
        addSupportedBinding(GTVectorDataBinding.class);

        try {
            geoServerUploader = new GeoServerUploader();
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        }

        geoServerURL = geoServerUploader.getGeoServerURL();
    }

    @Inject
    public void setGeotiffGenerator(GeotiffGenerator geotiffGenerator) {
        this.geotiffGenerator = geotiffGenerator;
    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description,
            Data<?> data,
            Format format) throws IOException {

        InputStream stream = null;
        try {
            String getMapURL = storeLayer(data);
            stream = new ByteArrayInputStream(getMapURL.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | HttpException | ParserConfigurationException e) {
            String message = "Error generating WMS output. Reason: ";
            LOGGER.error(message, e);
            throw new RuntimeException(message + e);
        }
        return stream;
    }

    private String storeLayer(Data<?> coll) throws HttpException, IOException, ParserConfigurationException {
        File file = null;
        String storeName = "";
        if (coll instanceof GTVectorDataBinding) {
            GTVectorDataBinding gtData = (GTVectorDataBinding) coll;

            try {
                GenericFileDataWithGT fileData = new GenericFileDataWithGT(gtData.getPayload());
                file = fileData.getBaseFile(true);
            } catch (IOException e1) {
                LOGGER.error(e1.getMessage());
                throw new RuntimeException("Error generating shp file for storage in WFS. Reason: " + e1);
            }

            // zip shp file
            String path = file.getAbsolutePath();
            String baseName = path.substring(0, path.length() - FileConstants.dot(FileConstants.SUFFIX_SHP).length());
            File shx = new File(baseName + ".shx");
            File dbf = new File(baseName + ".dbf");
            File prj = new File(baseName + ".prj");
            File zipped = IOUtils.zip(file, shx, dbf, prj);

            file = zipped;

        }
        if (coll instanceof GTRasterDataBinding) {
            GTRasterDataBinding gtData = (GTRasterDataBinding) coll;

            try {
                file = File.createTempFile("primary", FileConstants.dot(FileConstants.SUFFIX_TIF));
                FileOutputStream outputStream = new FileOutputStream(file);

                InputStream is = geotiffGenerator.generate(null, gtData, null);
                org.apache.commons.io.IOUtils.copy(is, outputStream);
                is.close();

            } catch (IOException | EncodingException e) {
                LOGGER.error("Could not generate GeoTiff.");
            }

        }
        if (coll instanceof ShapefileBinding) {
            ShapefileBinding data = (ShapefileBinding) coll;
            file = data.getZippedPayload();

        }
        if (coll instanceof GeotiffBinding) {
            GeotiffBinding data = (GeotiffBinding) coll;
            file = data.getPayload();
        }
        if (coll instanceof GenericFileDataWithGTBinding) {
            file = ((GenericFileDataWithGTBinding) coll).getPayload().getBaseFile(true);
        }
        storeName = file.getName();

        storeName = storeName + "_" + UUID.randomUUID();
        GeoServerUploader geoserverUploader = new GeoServerUploader();

        int width = 0;
        int height = 0;

        String bboxString = "";
        String srsString = "";

        String result = geoserverUploader.createWorkspace();
        LOGGER.debug(result);
        if (coll instanceof GTVectorDataBinding) {
            result = geoserverUploader.uploadShp(file, storeName);
        }
        if (coll instanceof GTRasterDataBinding) {
            result = geoserverUploader.uploadGeotiff(file, storeName);

            GridCoverage2D gridCoverage2D = ((GTRasterDataBinding) coll).getPayload();

            Rectangle2D bounds = gridCoverage2D.getEnvelope2D().getBounds();

            double minX = bounds.getMinX();
            double maxX = bounds.getMaxX();
            double minY = bounds.getMinY();
            double maxY = bounds.getMaxY();

            bboxString = minX + "," + minY + "," + maxX + "," + maxY;

            ReferenceIdentifier srsIdentifier = null;

            try {
                srsIdentifier = gridCoverage2D.getEnvelope2D().getCoordinateReferenceSystem().getCoordinateSystem()
                        .getIdentifiers().iterator().next();
            } catch (Exception e) {
                LOGGER.info("Could not get CRS from grid.");
            }

            if (srsIdentifier != null) {
                srsString = srsIdentifier.getCodeSpace() + ":" + srsIdentifier.getCode();
            }

            width = gridCoverage2D.getRenderedImage().getWidth();
            height = gridCoverage2D.getRenderedImage().getHeight();
        }
        if (coll instanceof GenericFileDataWithGTBinding) {
            // TODO, could also be a shapefile
            result = geoserverUploader.uploadGeotiff(file, storeName);
        }

        LOGGER.debug(result);

        String getMapLink = geoServerURL + "/wms?Service=WMS&Request=GetMap&Version=1.1.1&layers=" + "N52:" + storeName
                + "&width=" + width + "&height=" + height + "&format=image/png" + "&bbox=" + bboxString + "&srs="
                + srsString;

        return getMapLink;
    }

}
