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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.gml3.ApplicationSchemaConfiguration;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.gt.io.GTHelper;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.AbstractPropertiesInputOutputHandlerForFiles;
import org.n52.javaps.gt.io.util.FileConstants;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.javaps.io.SchemaRepository;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This parser handles xml files for GML 3.2.1
 *
 * @author matthes rieke
 */
@Properties(
        defaultPropertyFileName = "gml32basicparser.default.json",
        propertyFileName = "gml32basicparser.json")
public class GML32BasicParser extends AbstractPropertiesInputOutputHandlerForFiles implements InputHandler {

    private static final String IO_EXCEPTION_WHILE_TRYING_TO_CLOSE_INPUTSTREAM =
            "IOException while trying to close inputstream.";

    private static Logger LOGGER = LoggerFactory.getLogger(GML32BasicParser.class);

    @Inject
    private GTHelper gtHelper;

    private Configuration configuration;

    public GML32BasicParser() {
        super();
        addSupportedBinding(GTVectorDataBinding.class);
    }

    public void setConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description,
            InputStream input,
            Format format) throws IOException, DecodingException {

        File tempFile;
        try {
            tempFile = FileConstants.writeTempFile(input);
        } catch (Exception e1) {
            throw new IOException(e1.getMessage());
        }

        InputStream in = null;
        try {
        in = new FileInputStream(tempFile);

            QName schematypeTuple = determineFeatureTypeSchema(tempFile);
            return parse(in, schematypeTuple);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while creating tempFile", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                LOGGER.trace(IO_EXCEPTION_WHILE_TRYING_TO_CLOSE_INPUTSTREAM);
            }
        }
    }

    public GTVectorDataBinding parse(InputStream input,
            QName schematypeTuple) {
        if (configuration == null) {
            configuration = resolveConfiguration(schematypeTuple);
        }

        Parser parser = new Parser(configuration);
        parser.setStrict(true);

        // parse
        SimpleFeatureCollection fc = resolveFeatureCollection(parser, input);

        GTVectorDataBinding data = new GTVectorDataBinding(fc);

        return data;
    }

    @SuppressWarnings("unchecked")
    private SimpleFeatureCollection resolveFeatureCollection(Parser parser,
            InputStream input) {
        SimpleFeatureCollection fc = null;
        try {
            Object parsedData = parser.parse(input);
            if (parsedData instanceof SimpleFeatureCollection) {
                fc = (SimpleFeatureCollection) parsedData;
            } else {

                List<SimpleFeature> featureList = null;

                if (parsedData instanceof Map<?, ?>) {

                    Map<?, ?> parsedMap = (Map<?, ?>) parsedData;

                    Object featureMemberObject = parsedMap.get("featureMember");

                    if (featureMemberObject != null && (featureMemberObject instanceof ArrayList<?>)) {

                        List<?> featureMemberList = (List<?>) featureMemberObject;

                        if (featureMemberList.get(0) instanceof SimpleFeature) {
                            featureList = (ArrayList<SimpleFeature>) featureMemberList;
                        }
                    }
                }
                if (featureList != null) {
                    if (featureList.size() > 0) {
                        fc = new ListFeatureCollection(featureList.get(0).getFeatureType(), featureList);
                    } else {
                        fc = new DefaultFeatureCollection();
                    }
                } else {
                    fc = (SimpleFeatureCollection) ((Map<?, ?>) parsedData).get("FeatureCollection");
                }
            }

            gtHelper.checkGeometries(fc);

        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return fc;
    }

    private Configuration resolveConfiguration(QName schematypeTuple) {
        /*
         * TODO all if-statements are nonsense.. clean up
         */
        Configuration resolvedConfiguration = null;
        if (schematypeTuple != null) {
            String schemaLocation = schematypeTuple.getLocalPart();
            if (schemaLocation != null && schemaLocation.startsWith("http://schemas.opengis.net/gml/3.2")) {
                resolvedConfiguration = new GMLConfiguration();
            } else {
                if (schemaLocation != null && schematypeTuple.getNamespaceURI() != null) {
                    SchemaRepository.registerSchemaLocation(schematypeTuple.getNamespaceURI(), schemaLocation);
                    resolvedConfiguration = new ApplicationSchemaConfiguration(schematypeTuple.getNamespaceURI(),
                            schemaLocation);
                } else {
                    resolvedConfiguration = new GMLConfiguration();
                }
            }
        } else {
            resolvedConfiguration = new GMLConfiguration();
        }

        return resolvedConfiguration;
    }

    private QName determineFeatureTypeSchema(File file) {
        InputStream in = null;
        try {
        in = new FileInputStream(file);
            GML2Handler handler = new GML2Handler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);

            factory.newSAXParser().parse(in, handler);

            String schemaUrl = handler.getSchemaUrl();

            if (schemaUrl == null) {
                return null;
            }

            String namespaceURI = handler.getNameSpaceURI();

            /*
             * TODO dude, wtf? Massive abuse of QName.
             */
            return new QName(namespaceURI, schemaUrl);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                LOGGER.trace(IO_EXCEPTION_WHILE_TRYING_TO_CLOSE_INPUTSTREAM);
            }
        }
    }

    public static GML32BasicParser getInstanceForConfiguration(Configuration config) {
        GML32BasicParser parser = new GML32BasicParser();
        parser.setConfiguration(config);
        return parser;
    }

}
