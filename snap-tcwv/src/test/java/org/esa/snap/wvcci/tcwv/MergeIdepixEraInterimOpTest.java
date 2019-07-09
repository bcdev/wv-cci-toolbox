package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.wvcci.tcwv.MergeIdepixEraInterimOp;
import org.esa.snap.wvcci.tcwv.Sensor;
import org.junit.Test;

import static org.junit.Assert.*;

public class MergeIdepixEraInterimOpTest {

    @Test
    public void testOperatorSpiIsLoaded() {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = registry.getOperatorSpi("ESACCI.MergeIdepixEraInterim");
        assertNotNull(operatorSpi);
        assertEquals("ESACCI.MergeIdepixEraInterim", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertSame(operatorSpi.getOperatorClass(), operatorSpi.getOperatorDescriptor().getOperatorClass());
    }

    @Test
    public void testMergedProduct() {
        final Product idepixProduct = new Product("dummy1", "mergeOpTest", 10, 10);
        idepixProduct.addBand("reflectance_13", ProductData.TYPE_FLOAT32);
        idepixProduct.addBand("reflectance_14", ProductData.TYPE_FLOAT32);
        idepixProduct.addBand("reflectance_15", ProductData.TYPE_FLOAT32);
        idepixProduct.addBand("pixel_classif_flags", ProductData.TYPE_FLOAT32);

        final Product eraInterimProduct = new Product("dummy2", "mergeOpTest", 10, 10);
        eraInterimProduct.addBand("lon_bnds_nv_41", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("lon_bnds_nv_43", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("lat_bnds_nv_42", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("lat_bnds_nv_44", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("lat", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("lon", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("u10", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("v10", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("t2m", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("msl", ProductData.TYPE_FLOAT32);
        eraInterimProduct.addBand("tcwv", ProductData.TYPE_FLOAT32);

        final MergeIdepixEraInterimOp mergeOp = new MergeIdepixEraInterimOp();
        mergeOp.setSourceProduct("idepixProduct", idepixProduct);
        mergeOp.setSourceProduct("eraInterimProduct", eraInterimProduct);
        mergeOp.setParameter("sensor", Sensor.MERIS);

        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("reflectance_13"));
        assertTrue(mergedProduct.containsBand("reflectance_14"));
        assertTrue(mergedProduct.containsBand("reflectance_15"));
        assertTrue(mergedProduct.containsBand("pixel_classif_flags"));
        assertTrue(mergedProduct.containsBand("t2m"));
        assertTrue(mergedProduct.containsBand("msl"));
        assertTrue(mergedProduct.containsBand("tcwv"));
        assertFalse(mergedProduct.containsBand("lon_bnds_nv_41"));
        assertFalse(mergedProduct.containsBand("lon_bnds_nv_43"));
        assertFalse(mergedProduct.containsBand("lat_bnds_nv_42"));
        assertFalse(mergedProduct.containsBand("lat_bnds_nv_44"));
        assertFalse(mergedProduct.containsBand("lat"));
        assertFalse(mergedProduct.containsBand("lon"));
        assertFalse(mergedProduct.containsBand("u10"));
        assertFalse(mergedProduct.containsBand("v10"));
    }
}
