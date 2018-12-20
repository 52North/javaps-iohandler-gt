package org.n52.javaps.gt.io.util;

import javax.xml.namespace.QName;

public interface GML3SchemaConstants {

    String NS_XS = "http://www.w3.org/2001/XMLSchema";

    String NS_XS_PREFIX = "xs";

    String NS_GML31 = "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd";

    static QName xs(String element) {
        return new QName(NS_XS, element, NS_XS_PREFIX);
    }

    public interface Attr {
        String AN_DATA_TYPE = "dataType";
    }

    public interface Elem {

    }
}
