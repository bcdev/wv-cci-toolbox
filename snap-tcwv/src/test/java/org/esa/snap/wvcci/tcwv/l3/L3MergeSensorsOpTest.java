package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.junit.Test;

import static org.junit.Assert.*;

public class L3MergeSensorsOpTest {

    @Test
    public void testOperatorSpiIsLoaded() {
        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = registry.getOperatorSpi("ESACCI.Tcwv.L3.Merge");
        assertNotNull(operatorSpi);
        assertEquals("ESACCI.Tcwv.L3.Merge", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorDescriptor());
        assertSame(operatorSpi.getOperatorClass(), operatorSpi.getOperatorDescriptor().getOperatorClass());
    }

    @Test
    public void testMergedProduct() {
        final Product merisProduct = new Product("dummy1", "mergeSensorsOpTest", 10, 10);
        merisProduct.addBand("tcwv", ProductData.TYPE_FLOAT32);
        merisProduct.addBand("tcwv_uncertainty", ProductData.TYPE_FLOAT32);
        merisProduct.addBand("tcwv_counts", ProductData.TYPE_FLOAT32);

        final Product modisProduct = new Product("dummy2", "mergeSensorsOpTest", 10, 10);
        modisProduct.addBand("tcwv", ProductData.TYPE_FLOAT32);
        modisProduct.addBand("tcwv_uncertainty", ProductData.TYPE_FLOAT32);
        modisProduct.addBand("tcwv_counts", ProductData.TYPE_FLOAT32);

        final L3MergeSensorsOp mergeOp = new L3MergeSensorsOp();

        mergeOp.setParameterDefaultValues();

        mergeOp.setSourceProducts(merisProduct, modisProduct);

        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("tcwv"));
        assertTrue(mergedProduct.containsBand("tcwv_uncertainty"));
        assertTrue(mergedProduct.containsBand("tcwv_counts"));
    }

    @Test
    public void testMergeTcwv() {
        // 2 products
        double[] srcTcwv = new double[]{20.0, 40.0};
        double[] srcTcwvNodata = new double[]{-999.9, -999.9};
        double[] srcTcwvUncertainty = new double[]{1.0, 2.0};
        double[] srcTcwvUncertaintyNodata = new double[]{-333.3, -333.3};
        double[] srcTcwvCounts = new double[]{5.0, 3.0};
        double[] srcTcwvCountsNodata = new double[]{-666.6, -666.6};

        double[] mergeTcwv = L3MergeSensorsOp.mergeTcwv(2, srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwv.length);
        assertEquals(27.5, mergeTcwv[0], 1.E-6);
        assertEquals(8.0, mergeTcwv[1], 1.E-6);

        double[] mergeTcwvUncertainty =
                L3MergeSensorsOp.mergeTcwv(2, srcTcwvUncertainty, srcTcwvCounts,
                                           srcTcwvUncertaintyNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwvUncertainty.length);
        assertEquals(1.375, mergeTcwvUncertainty[0], 1.E-6);
        assertEquals(8.0, mergeTcwvUncertainty[1], 1.E-6);

        // 3 products
        srcTcwv = new double[]{20.0, 40.0, 50.0};
        srcTcwvNodata = new double[]{-999.9, -999.9, -999.9};
        srcTcwvUncertainty = new double[]{1.0, 2.0, 3.0};
        srcTcwvUncertaintyNodata = new double[]{-333.3, -333.3, -333.3};
        srcTcwvCounts = new double[]{5.0, 3.0, 2.0};
        srcTcwvCountsNodata = new double[]{-666.6, -666.6, -666.6};

        mergeTcwv = L3MergeSensorsOp.mergeTcwv(3, srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwv.length);
        assertEquals(32.0, mergeTcwv[0], 1.E-6);
        assertEquals(10.0, mergeTcwv[1], 1.E-6);

        mergeTcwvUncertainty =
                L3MergeSensorsOp.mergeTcwv(3, srcTcwvUncertainty, srcTcwvCounts,
                                           srcTcwvUncertaintyNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwvUncertainty.length);
        assertEquals(1.7, mergeTcwvUncertainty[0], 1.E-6);
        assertEquals(10.0, mergeTcwvUncertainty[1], 1.E-6);

        // 3 products with NaNs and noData
        srcTcwv = new double[]{20.0, 40.0, Double.NaN};
        srcTcwvNodata = new double[]{-999.9, -999.9, -999.9};
        srcTcwvUncertainty = new double[]{-333.3, 2.0, 3.0};
        srcTcwvUncertaintyNodata = new double[]{-333.3, -333.3, -333.3};
        srcTcwvCounts = new double[]{5.0, 3.0, 2.0};
        srcTcwvCountsNodata = new double[]{-666.6, -666.6, -666.6};

        mergeTcwv = L3MergeSensorsOp.mergeTcwv(3, srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwv.length);
        assertEquals(27.5, mergeTcwv[0], 1.E-6);
        assertEquals(8.0, mergeTcwv[1], 1.E-6);

        mergeTcwvUncertainty =
                L3MergeSensorsOp.mergeTcwv(3, srcTcwvUncertainty, srcTcwvCounts,
                                           srcTcwvUncertaintyNodata, srcTcwvCountsNodata);
        assertEquals(2, mergeTcwvUncertainty.length);
        assertEquals(2.4, mergeTcwvUncertainty[0], 1.E-6);
        assertEquals(5.0, mergeTcwvUncertainty[1], 1.E-6);

    }
}
