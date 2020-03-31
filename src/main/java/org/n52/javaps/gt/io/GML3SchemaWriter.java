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
package org.n52.javaps.gt.io;

import java.math.BigInteger;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.n52.javaps.gt.io.util.GML3SchemaConstants;
import org.n52.javaps.gt.io.util.GMLSchemaConstants;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.stream.xml.AbstractMultiElementXmlStreamWriter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;

public class GML3SchemaWriter extends AbstractMultiElementXmlStreamWriter {

    private String targetNamespace;

    private String uuid;

    @Override
    public void writeElement(Object object) throws XMLStreamException, EncodingException {
        if (object instanceof SimpleFeatureType) {
            writeSchemaType((SimpleFeatureType) object);
        }
    }

    private void writeNamespacesAndSchemaAttributes() throws XMLStreamException {
        namespace(GMLSchemaConstants.NS_GML_PREFIX, GMLSchemaConstants.NS_GML);
        namespace(GMLSchemaConstants.NS_XS_PREFIX, GMLSchemaConstants.NS_XS);
        namespace(GMLSchemaConstants.NS_N52_PREFIX, targetNamespace);
        attr(GMLSchemaConstants.Attr.AN_TARGET_NAMESPACE, targetNamespace);
        attr(GMLSchemaConstants.Attr.AN_ELEMENT_FORM_DEFAULT, "qualified");
        attr(GMLSchemaConstants.Attr.AN_VERSION, GMLSchemaConstants.VERSION);
    }

    private void writeSchemaType(SimpleFeatureType featureType) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_SCHEMA, featureType, x -> {
            writeNamespacesAndSchemaAttributes();
            writeImport();
            writeXSElement();
            writeComplexType(featureType);
        });
    }

    private void writeComplexType(SimpleFeatureType featureType) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_COMPLEX_TYPE, featureType, x -> {
            attr(GMLSchemaConstants.Attr.AN_NAME, GMLSchemaConstants.FEATURE_TYPE);
            writeComplexContent(featureType);
        });
    }

    private void writeComplexContent(SimpleFeatureType featureType) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_COMPLEX_CONTENT, featureType, x -> {
            writeExtension(featureType);
        });
    }

    private void writeExtension(SimpleFeatureType featureType) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_EXTENSION, featureType, x -> {
            attr(GMLSchemaConstants.Attr.AN_BASE, GMLSchemaConstants.gml(GMLSchemaConstants.ABSTRACT_FEATURE_TYPE));
            writeSequence(featureType);
        });
    }

    private void writeSequence(SimpleFeatureType featureType) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_SEQUENCE, featureType, x -> {
            writeXSElements(featureType);
        });
    }

    private void writeXSElements(SimpleFeatureType featureType) throws XMLStreamException {

        String typeName = featureType.getGeometryDescriptor().getType().getBinding().getName();
        String geometryTypeName = "";

        if (typeName.contains("Point")) {
            geometryTypeName = "PointPropertyType";
        } else if (typeName.contains("MultiPoint")) {
            geometryTypeName = "MultiPointPropertyType";
        } else if (typeName.contains("LineString")) {
            geometryTypeName = "CurvePropertyType";
        } else if (typeName.contains("MultiLineString")) {
            geometryTypeName = "MultiCurvePropertyType";
        } else if (typeName.contains("Polygon")) {
            geometryTypeName = "SurfacePropertyType";
        } else if (typeName.contains("MultiPolygon")) {
            geometryTypeName = "MultiSurfacePropertyType";
        }

        writeGeometryElement(geometryTypeName);

        Collection<PropertyDescriptor> attributes = featureType.getDescriptors();
        for (PropertyDescriptor property : attributes) {
            String attributeName = property.getName().getLocalPart();
            if (!(property instanceof GeometryDescriptor)) {

                if (property.getType().getBinding().equals(String.class)) {
                    writeXSElement(attributeName, "string");
                } else if (property.getType().getBinding().equals(Integer.class) || property.getType().getBinding()
                        .equals(BigInteger.class)) {
                    writeXSElement(attributeName, "integer");
                } else if (property.getType().getBinding().equals(Double.class)) {
                    writeXSElement(attributeName, "double");
                }
            }
        }
    }

    private void writeGeometryElement(String geometryTypeName) throws XMLStreamException {

        element(GMLSchemaConstants.Elem.QN_ELEMENT, geometryTypeName, x -> {
            attr(GMLSchemaConstants.Attr.AN_NAME, "the_geom");
            attr(GMLSchemaConstants.Attr.AN_TYPE, GMLSchemaConstants.gml(geometryTypeName));
        });
    }

    private void writeXSElement() throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_ELEMENT, "", x -> {
            attr(GMLSchemaConstants.Attr.AN_NAME, "Feature-" + uuid);
            attr(GMLSchemaConstants.Attr.AN_TYPE, GMLSchemaConstants.n52String(GMLSchemaConstants.FEATURE_TYPE));
            attr(GMLSchemaConstants.Attr.AN_SUBSTITUTION_GROUP, GMLSchemaConstants.gml(GMLSchemaConstants._FEATURE));
        });
    }

    private void writeXSElement(String name, String restrictionBase) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_ELEMENT, "", x -> {
            attr(GMLSchemaConstants.Attr.AN_NAME, name);
            attr(GMLSchemaConstants.Attr.AN_MINOCCURS, "0");
            attr(GMLSchemaConstants.Attr.AN_MAXOCCURS, "1");
            writeSimpleType(restrictionBase);
        });
    }

    private void writeSimpleType(String restrictionBase) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_SIMPLE_TYPE, restrictionBase, x -> {
            writeRestriction(restrictionBase);
        });

    }

    private void writeRestriction(String restrictionBase) throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_RESTRICTION, restrictionBase, x -> {
            attr(GMLSchemaConstants.Attr.AN_BASE, GMLSchemaConstants.xsString(restrictionBase));
        });
    }

    private void writeImport() throws XMLStreamException {
        element(GMLSchemaConstants.Elem.QN_IMPORT, "", x -> {
            attr(GMLSchemaConstants.Attr.AN_NAMESPACE, GMLSchemaConstants.NS_GML);
            attr(GMLSchemaConstants.Attr.AN_SCHEMALOCATION, GML3SchemaConstants.NS_GML31);
        });
    }

    protected String getTargetNamespace() {
        return targetNamespace;
    }

    protected void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    protected String getUuid() {
        return uuid;
    }

    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
