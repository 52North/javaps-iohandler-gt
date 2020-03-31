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
package org.n52.wps.io.test.datahandler;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.n52.javaps.description.TypedComplexInputDescription;
import org.n52.javaps.description.impl.TypedProcessDescriptionFactory;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.parser.GeoJSONParser;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.shetland.ogc.wps.Format;

public class ComplexInputHandlerTest {

    @Rule
    public ErrorCollector errors = new ErrorCollector();

    private GeoJSONParser handler;

    private TypedProcessDescriptionFactory descriptionFactory;

    @Before
    public void setup() {
        this.handler = new GeoJSONParser();
        this.descriptionFactory = new TypedProcessDescriptionFactory();
    }

    @Test
    public void testXmlDecodingEmptyFormat() throws IOException, DecodingException {

        String value = "{\"type\":\"FeatureCollection\",\"features\":"
                + "[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":"
                + "[[[-70.826111,-33.445193],"
                + "[-70.826111,-33.293804],"
                + "[-70.716248,-33.293804],"
                + "[-70.716248,-33.445193],"
                + "[-70.826111,-33.445193]]]}}]}";

        Charset charset = StandardCharsets.UTF_8;
        ByteArrayInputStream input = new ByteArrayInputStream(value.getBytes(charset));

        Data<?> parsedData = this.handler.parse(input(), input, new Format());
        errors.checkThat(parsedData, is(notNullValue()));
        errors.checkThat(parsedData, is(notNullValue()));
        errors.checkThat(parsedData, is(instanceOf(GTVectorDataBinding.class)));
        errors.checkThat(parsedData.getPayload(), is(instanceOf(SimpleFeatureCollection.class)));
//        errors.checkThat(parsedData.getPayload().toString(), is(value));
    }

    private TypedComplexInputDescription input() {

        return descriptionFactory.complexInput()
                        .withIdentifier("input")
                        .withDefaultFormat(new Format("application/vnd.geo+json"))
//                        .withSupportedFormat(new Format(""))
                        .withType(GTVectorDataBinding.class)
                        .build();
    }

}
