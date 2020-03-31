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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.geotools.feature.FeatureCollection;
import org.geotools.gml3.ApplicationSchemaConfiguration;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.gt.io.GTHelper;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.AbstractPropertiesInputOutputHandlerForFiles;
import org.n52.javaps.gt.io.util.FileConstants;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.OutputHandler;
import org.n52.javaps.io.SchemaRepository;
import org.n52.shetland.ogc.wps.Format;
import org.opengis.feature.type.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(
        defaultPropertyFileName = "gml3basichandler.default.json",
        propertyFileName = "gml3basicgenerator.json")
public class GML3BasicGenerator extends AbstractPropertiesInputOutputHandlerForFiles implements OutputHandler {

    private static final String NS_GML = "http://www.opengis.net/gml";

    private static final String SCHEMALOCATION_GML311 = "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd";

    private static Logger LOGGER = LoggerFactory.getLogger(GML3BasicGenerator.class);

    @Inject
    private GTHelper gtHelper;

    public GML3BasicGenerator() {
        super();
        addSupportedBinding(GTVectorDataBinding.class);
    }

    public void writeToStream(Data<?> coll,
            OutputStream os) {
        FeatureCollection<?, ?> fc = ((GTVectorDataBinding) coll).getPayload();

        FeatureCollection<?, ?> correctFeatureCollection = gtHelper.createCorrectFeatureCollection(fc);
        // get the namespace from the features to pass into the encoder
        FeatureType schema = correctFeatureCollection.getSchema();
        String namespace = null;
        String schemaLocation = null;
        if (schema != null) {
            namespace = schema.getName().getNamespaceURI();
            schemaLocation = SchemaRepository.getSchemaLocation(namespace);
        }

        Configuration configuration = null;
        org.geotools.xml.Encoder encoder = null;
        if (schemaLocation == null || namespace == null) {
            namespace = NS_GML;
            schemaLocation = SCHEMALOCATION_GML311;
            configuration = new GMLConfiguration();

            encoder = new org.geotools.xml.Encoder(configuration);
            encoder.setNamespaceAware(true);
            encoder.setSchemaLocation(NS_GML, SCHEMALOCATION_GML311);

        } else {

            configuration = new ApplicationSchemaConfiguration(namespace, schemaLocation);

            encoder = new org.geotools.xml.Encoder(configuration);
            encoder.setNamespaceAware(true);
            encoder.setSchemaLocation(NS_GML + " " + SCHEMALOCATION_GML311, namespace + " " + schemaLocation);

        }

        fc.features().close();
        // use the gml namespace with the FeatureCollection element to start
        // parsing the collection
        QName ns = new QName(NS_GML, "FeatureCollection", "wfs");
        try {
            encoder.encode(correctFeatureCollection, ns, os);
        } catch (IOException e) {
            LOGGER.error("Exception while trying to encode FeatureCollection.", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description,
            Data<?> data,
            Format format) throws IOException, EncodingException {
        File file = FileConstants.createTempFile(FileConstants.dot(FileConstants.SUFFIX_GML3));
        finalizeFiles.add(file);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            this.writeToStream(data, outputStream);
            outputStream.flush();
            outputStream.close();
            if (file.length() <= 0) {
                return null;
            }
        } catch (IOException e) {
            throw e;
        }
        FileInputStream inputStream = new FileInputStream(file);

        return inputStream;
    }

}
