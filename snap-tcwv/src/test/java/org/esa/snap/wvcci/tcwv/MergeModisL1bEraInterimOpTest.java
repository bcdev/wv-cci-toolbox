package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.junit.Test;

import static org.junit.Assert.*;

public class MergeModisL1bEraInterimOpTest {

    @Test
    public void testOperatorSpiIsLoaded() {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = registry.getOperatorSpi("ESACCI.MergeModisL1bEraInterim");
        assertNotNull(operatorSpi);
        assertEquals("ESACCI.MergeModisL1bEraInterim", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertSame(operatorSpi.getOperatorClass(), operatorSpi.getOperatorDescriptor().getOperatorClass());
    }

    @Test
    public void testMergedProduct_bandsForTcwvOnly() {
        // exclude unwanted bands fro master product
        final Product l1bProduct = new Product("dummy1", "mergeOpTest", 10, 10);
        l1bProduct.addBand("EV_1KM_RefSB_17", ProductData.TYPE_FLOAT32);  // yes
        l1bProduct.addBand("EV_1KM_RefSB_18", ProductData.TYPE_FLOAT32);  // yes
        l1bProduct.addBand("EV_1KM_RefSB_19", ProductData.TYPE_FLOAT32);  // yes
        l1bProduct.addBand("EV_1KM_RefSB_26", ProductData.TYPE_FLOAT32);  // no
        l1bProduct.addBand("EV_1KM_RefSB_8", ProductData.TYPE_FLOAT32);    // no
        l1bProduct.addBand("EV_1KM_RefSB_13lo", ProductData.TYPE_FLOAT32);    // no
        l1bProduct.addBand("EV_1KM_RefSB_14hi", ProductData.TYPE_FLOAT32);    // no
        l1bProduct.addBand("EV_250_Aggr1km_RefSB_1", ProductData.TYPE_FLOAT32);    // no
        l1bProduct.addBand("EV_250_Aggr1km_RefSB_2", ProductData.TYPE_FLOAT32);    // yes
        l1bProduct.addBand("EV_500_Aggr1km_RefSB_5", ProductData.TYPE_FLOAT32);    // yes
        l1bProduct.addBand("EV_1KM_Emissive_23", ProductData.TYPE_FLOAT32);    // no
        l1bProduct.addBand("EV_1KM_Emissive_35", ProductData.TYPE_FLOAT32);    // no

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

        final MergeModisL1bEraInterimOp mergeOp = new MergeModisL1bEraInterimOp();
        mergeOp.setSourceProduct("l1bProduct", l1bProduct);
        mergeOp.setSourceProduct("eraInterimProduct", eraInterimProduct);
        mergeOp.setParameterDefaultValues();
        mergeOp.setParameter("validateL1b", false);
        mergeOp.setParameter("processDayProductsOnly", false);

        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("EV_1KM_RefSB_17"));
        assertTrue(mergedProduct.containsBand("EV_1KM_RefSB_18"));
        assertTrue(mergedProduct.containsBand("EV_1KM_RefSB_19"));
        assertTrue(mergedProduct.containsBand("EV_250_Aggr1km_RefSB_2"));
        assertTrue(mergedProduct.containsBand("EV_500_Aggr1km_RefSB_5"));

        assertFalse(mergedProduct.containsBand("EV_1KM_RefSB_26"));
        assertFalse(mergedProduct.containsBand("EV_1KM_RefSB_8"));
        assertFalse(mergedProduct.containsBand("EV_1KM_RefSB_13lo"));
        assertFalse(mergedProduct.containsBand("EV_1KM_RefSB_14hi"));
        assertFalse(mergedProduct.containsBand("EV_250_Aggr1km_RefSB_1"));
        assertFalse(mergedProduct.containsBand("EV_1KM_Emissive_23"));
        assertFalse(mergedProduct.containsBand("EV_1KM_Emissive_35"));

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
