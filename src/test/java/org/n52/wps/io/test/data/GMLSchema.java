package org.n52.wps.io.test.data;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.n52.javaps.gt.io.GTHelper;
import org.n52.javaps.gt.io.data.binding.complex.GTVectorDataBinding;
import org.n52.javaps.gt.io.datahandler.parser.GeoJSONParser;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.test.AbstractTestCase;

public class GMLSchema extends AbstractTestCase {

    @Inject
    private GTHelper gtHelper;
    
    @Inject
    private GeoJSONParser geoJSONParser;
    
    @Test
    public void testGenerateGML3Schema() {
        
        //TODO test different geometries
        GTVectorDataBinding point = null;
        try {
            point = (GTVectorDataBinding) geoJSONParser.parse(null, getClass().getClassLoader().getResourceAsStream("point.json"), null);
        } catch (IOException | DecodingException e) {
            fail(e.getMessage());
        }
        
        gtHelper.createGML3SchemaForFeatureType(point.getPayload().getSchema());
                
    }
    
}
