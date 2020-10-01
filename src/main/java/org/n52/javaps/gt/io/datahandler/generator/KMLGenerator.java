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

import org.geotools.feature.FeatureCollection;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.n52.javaps.annotation.Properties;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.AbstractPropertiesInputOutputHandlerForFiles;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bastian Schaeffer, IfGI; Matthias Mueller, TU Dresden
 *
 */
@Properties(
        defaultPropertyFileName = "kmlhandler.default.json",
        propertyFileName = "kmlgenerator.json")
public class KMLGenerator extends AbstractPropertiesInputOutputHandlerForFiles implements OutputHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(KMLGenerator.class);

    public KMLGenerator() {
        super();
        addSupportedBinding(GTVectorDataBinding.class);
    }

    private void writeToStream(Data<?> coll,
            OutputStream os) {
        FeatureCollection<?, ?> fc = ((GTVectorDataBinding) coll).getPayload();

        Configuration configuration = new KMLConfiguration();
        Encoder encoder = new Encoder(configuration);

        try {
            encoder.encode(fc, KML.kml, os);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description,
            Data<?> data,
            Format format) throws IOException, EncodingException {
        File tempFile = null;
        InputStream stream = null;
        tempFile = File.createTempFile("kml", "xml");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            this.finalizeFiles.add(tempFile);
            this.writeToStream(data, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new IOException("Unable to generate KML");
        }
        stream = new FileInputStream(tempFile);

        return stream;
    }

}
