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
