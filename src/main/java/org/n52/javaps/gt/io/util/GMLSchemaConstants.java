package org.n52.javaps.gt.io.util;

import javax.xml.namespace.QName;

public interface GMLSchemaConstants {

    String VERSION = "1.0";

    String NS_GML = "http://www.opengis.net/gml";

    String NS_GML_PREFIX = "gml";

    String NS_XS = "http://www.w3.org/2001/XMLSchema";

    String NS_XS_PREFIX = "xs";

    String NS_N52_PREFIX = "n52";

    String FEATURE_TYPE = "FeatureType";

    String _FEATURE = "_Feature";

    String ABSTRACT_FEATURE_TYPE = "AbstractFeatureType";

    static QName xs(String element) {
        return new QName(NS_XS, element, NS_XS_PREFIX);
    }

    static String xsString(String element) {
        return NS_XS_PREFIX + ":" + element;
    }

    static String gml(String element) {
        return NS_GML_PREFIX + ":" + element;
    }

    static QName n52(String element, String namespace) {
        return new QName(namespace, element, NS_N52_PREFIX);
    }

    static String n52String(String element) {
        return NS_N52_PREFIX + ":" + element;
    }

    public interface Attr {

        String AN_ELEMENT_FORM_DEFAULT = "elementFormDefault";

        String AN_TARGET_NAMESPACE = "targetNamespace";

        String AN_VERSION = "version";

        String AN_TYPE = "type";

        String AN_NAME = "name";

        String AN_BASE = "base";

        String AN_MINOCCURS = "minOccurs";

        String AN_MAXOCCURS = "maxOccurs";

        String AN_SUBSTITUTION_GROUP = "substitutionGroup";

        String AN_NAMESPACE = "namespace";

        String AN_SCHEMALOCATION = "schemaLocation";

    }

    public interface Elem {

        String EN_SCHEMA = "schema";

        QName QN_SCHEMA = xs(EN_SCHEMA);

        String EN_IMPORT = "import";

        QName QN_IMPORT = xs(EN_IMPORT);

        String EN_ELEMENT = "element";

        QName QN_ELEMENT = xs(EN_ELEMENT);

        String EN_SIMPLE_TYPE = "simpleType";

        QName QN_SIMPLE_TYPE = xs(EN_SIMPLE_TYPE);

        String EN_COMPLEX_TYPE = "complexType";

        QName QN_COMPLEX_TYPE = xs(EN_COMPLEX_TYPE);

        String EN_COMPLEX_CONTENT = "complexContent";

        QName QN_COMPLEX_CONTENT = xs(EN_COMPLEX_CONTENT);

        String EN_EXTENSION = "extension";

        QName QN_EXTENSION = xs(EN_EXTENSION);

        String EN_SEQUENCE = "sequence";

        QName QN_SEQUENCE = xs(EN_SEQUENCE);

        String EN_RESTRICTION = "restriction";

        QName QN_RESTRICTION = xs(EN_RESTRICTION);
    }
}
