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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.io.AbstractPropertiesInputOutputHandler;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.javaps.io.data.binding.complex.JTSGeometryBinding;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * This class parses json into JTS geometries.
 *
 * @author BenjaminPross(bpross-52n)
 *
 */
@Properties(
        defaultPropertyFileName = "geojsonhandler.default.json",
        propertyFileName = "geojson.json")
public class GeoJSONParser extends AbstractPropertiesInputOutputHandler implements InputHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(GeoJSONParser.class);

    public GeoJSONParser() {
        super();
        addSupportedBinding(JTSGeometryBinding.class);
        addSupportedBinding(GTVectorDataBinding.class);
    }

    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description,
            InputStream input,
            Format format) throws IOException, DecodingException {

        String geojsonstring = "";

        String line = "";

        BufferedReader breader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        try {
            while ((line = breader.readLine()) != null) {
                geojsonstring = geojsonstring.concat(line);
            }
        } catch (IOException e) {
            LOGGER.error("Exception while reading inputstream.", e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        if (geojsonstring.contains("FeatureCollection")) {

            try {
                SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) new FeatureJSON()
                        .readFeatureCollection(geojsonstring);

                return new GTVectorDataBinding(featureCollection);

            } catch (IOException e) {
                LOGGER.info("Could not read FeatureCollection from inputstream");
            }

        } else if (geojsonstring.contains("Feature")) {

            try {
                SimpleFeature feature = new FeatureJSON().readFeature(geojsonstring);

                List<SimpleFeature> featureList = new ArrayList<SimpleFeature>();

                featureList.add(feature);

                ListFeatureCollection featureCollection = new ListFeatureCollection(feature.getFeatureType(),
                        featureList);

                return new GTVectorDataBinding(featureCollection);

            } catch (IOException e) {
                LOGGER.info("Could not read Feature from inputstream");
            }

        } else if (geojsonstring.contains("GeometryCollection")) {

            try {
                GeometryCollection g = new GeometryJSON().readGeometryCollection(geojsonstring);

                return new JTSGeometryBinding(g);

            } catch (IOException e) {
                LOGGER.info("Could not read GeometryCollection from inputstream.");
            }

        } else if (geojsonstring.contains("Point") || geojsonstring.contains("LineString") || geojsonstring.contains(
                "Polygon") || geojsonstring.contains("MultiPoint") || geojsonstring.contains("MultiLineString")
                || geojsonstring.contains("MultiPolygon")) {

            try {
                Geometry g = new GeometryJSON().read(geojsonstring);

                return new JTSGeometryBinding(g);

            } catch (IOException e) {
                LOGGER.info("Could not read single Geometry from inputstream.");
            }

        }
        LOGGER.error("Could not parse inputstream, returning null.");
        return null;
    }

}
