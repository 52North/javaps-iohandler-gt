/*
 * Copyright 2018 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        propertyFileName = "geoservergenerator.properties",
        defaultPropertyFileName = "geoservergenerator.default.properties")
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
                e1.printStackTrace();
                throw new RuntimeException("Error generating shp file for storage in WFS. Reason: " + e1);
            }

            // zip shp file
            String path = file.getAbsolutePath();
            String baseName = path.substring(0, path.length() - ".shp".length());
            File shx = new File(baseName + ".shx");
            File dbf = new File(baseName + ".dbf");
            File prj = new File(baseName + ".prj");
            File zipped = IOUtils.zip(file, shx, dbf, prj);

            file = zipped;

        }
        if (coll instanceof GTRasterDataBinding) {
            GTRasterDataBinding gtData = (GTRasterDataBinding) coll;

            try {
                file = File.createTempFile("primary", ".tif");
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
            file = (File) data.getPayload();
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

            srsString = srsIdentifier.getCodeSpace() + ":" + srsIdentifier.getCode();

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
