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
package org.n52.javaps.gt.io.datahandler.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.locationtech.jts.geom.Geometry;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.gt.io.GTHelper;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.AbstractPropertiesInputOutputHandlerForFiles;
import org.n52.javaps.gt.io.util.FileConstants;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.javaps.utils.IOUtils;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bastian Schaeffer; Matthias Mueller, TU Dresden
 *
 */
@Properties(
        defaultPropertyFileName = "gtbinzippedwkt64parser.default.json",
        propertyFileName = "gtbinzippedwkt64parser.json")
public class GTBinZippedWKT64Parser extends AbstractPropertiesInputOutputHandlerForFiles implements InputHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(GTBinZippedWKT64Parser.class);

    @Inject
    private GTHelper gtHelper;

    public GTBinZippedWKT64Parser() {
        super();
        addSupportedBinding(GTVectorDataBinding.class);
    }

    private SimpleFeatureCollection createFeatureCollection(List<Geometry> geometries,
            CoordinateReferenceSystem coordinateReferenceSystem) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();

        CoordinateReferenceSystem crsCopy = coordinateReferenceSystem;

        if (crsCopy == null) {
            crsCopy = gtHelper.getDefaultCRS();
            typeBuilder.setCRS(crsCopy);
        }

        typeBuilder.setNamespaceURI(GML2Handler.NS_URI_GML);
        Name nameType = new NameImpl(GML2Handler.NS_URI_GML, "Feature");
        typeBuilder.setName(nameType);
        typeBuilder.add("GEOMETRY", geometries.get(0).getClass());

        List<SimpleFeature> simpleFeatureList = new ArrayList<SimpleFeature>();

        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        for (int i = 0; i < geometries.size(); i++) {
            SimpleFeature feature = gtHelper.createFeature("" + i, geometries.get(i), featureType, new ArrayList<
                    Property>());
            simpleFeatureList.add(feature);
        }

        SimpleFeatureCollection collection = new ListFeatureCollection(featureType, simpleFeatureList);

        return collection;
    }

    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description,
            InputStream stream,
            Format format) throws IOException, DecodingException {
        try {

            File tempFile = FileConstants.writeTempFile(stream);

            List<File> wktFiles = IOUtils.unzip(tempFile, "wkt");
            finalizeFiles.addAll(wktFiles);

            if (wktFiles == null || wktFiles.size() == 0) {
                throw new RuntimeException("Cannot find a shapefile inside the zipped file.");
            }

            // set namespace namespace
            List<Geometry> geometries = new ArrayList<Geometry>();

            // read wkt file
            // please not that only 1 geometry is returned. If multiple
            // geometries are included, perhaps use the read(String wktstring)
            // method
            for (int i = 0; i < wktFiles.size(); i++) {
                File wktFile = wktFiles.get(i);
                Reader fileReader = new InputStreamReader(new FileInputStream(wktFile), StandardCharsets.UTF_8);

                WKTReader2 wktReader = new WKTReader2();
                Geometry geometry = wktReader.read(fileReader);
                geometries.add(geometry);
            }

            SimpleFeatureCollection inputFeatureCollection = createFeatureCollection(geometries,
                    gtHelper.getDefaultCRS());

            return new GTVectorDataBinding(inputFeatureCollection);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException("An error has occurred while accessing provided data", e);
        }
    }

}
