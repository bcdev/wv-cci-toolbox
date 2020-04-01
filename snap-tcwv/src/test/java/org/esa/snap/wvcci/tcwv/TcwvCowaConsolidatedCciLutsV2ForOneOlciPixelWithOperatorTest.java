package org.esa.snap.wvcci.tcwv;

import org.apache.commons.math3.exception.MathUnsupportedOperationException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.math.MathUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for pixelwise verification of breadboard implementation for OLCI.
 * These tests make full use of the TcwvOp operator to ensure that a reference input product is
 * correctly read and all numbers correctly passed into the TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsV2ForOneOlciPixelWithOperatorTest {

    // test product: S3A_OL_1_EFR____20180818T095755_20180818T100055_20180819T151521_0179_034_350_2160_LN1_O_NT_002.SEN3

    @Before
    public void setUp() {
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_land_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createOlciLandTestProduct());
        tcwvOp.setParameter("sensor", "OLCI");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("OLCI LAND tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 23.50386;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.005);  // Java: 23.5

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        System.out.println("OLCI LAND tcwvUncBand.getSampleFloat(0, 0) = " + tcwvUncBand.getSampleFloat(0, 0));
        final double tcwvUnc_py = 0.116628;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 1.E-3);  // Java: 0.116

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        System.out.println("OLCI LAND costBand.getSampleFloat(0, 0) = " + costBand.getSampleFloat(0, 0));
        final double costFunction_py = 0.1135;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.001);  // Java: 0.113569
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_ocean_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createOlciOceanTestProduct());
        tcwvOp.setParameter("sensor", "OLCI");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("OLCI OCEAN tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 36.29;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.015);  // Java: 36.279

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        final double tcwvUnc_py = 1.7298;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 0.001);  // Java: 1.729

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        final double costFunction_py = 0.15298;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.00001);  // Java: 0.15297
    }

    private Product createOlciLandTestProduct() {
        // we need:
        // - Oa18_reflectance, Oa19_reflectance, Oa20_reflectance, Oa21_reflectance
        // - pixel_classif_flags
        // - t2m, tcwv (, u10, v10)
        // - altitude, sea_level_pressure
        // - SZA, OZA, SAA, OAA
        // product: S3A_OL_1_EFR____20180818T095755_20180818T100055_20180819T151521_0179_034_350_2160_LN1_O_NT_002.SEN3
        // pixel: 0/46 (i = 99), land, , stride 25x25

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        final double sza = 43.886208378211705;
        final double csza = Math.cos(sza* MathUtils.DTOR);
        addBand(product, "Oa18_reflectance", 0.08114775 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa19_reflectance", 0.051190317 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa20_reflectance", 0.014368968 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa21_reflectance", 0.08399769 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "t2m", 291.06589984011674);  // ERA value from breadboard
        addBand(product, "tcwv", 30.96508819250751);  // ERA value from breadboard
        addBand(product, "SZA", sza);
        addBand(product, "SAA", 142.82275263617674);
        addBand(product, "OZA", 27.926958039054462);
        addBand(product, "OAA", 102.07838529290852);
        addBand(product, "altitude", 0.0);
        addBand(product, "sea_level_pressure", 1006.53175615848);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {1024}; // only land flag raised
        pixelClassifFlagBand.setRasterData(ProductData.createInstance(v));
        product.addBand(pixelClassifFlagBand);
        FlagCoding pixelClassifFlagCoding = createDefaultIdepixFlagCoding();
        pixelClassifFlagBand.setSampleCoding(pixelClassifFlagCoding);
        product.getFlagCodingGroup().add(pixelClassifFlagCoding);

        return product;
    }

    private Product createOlciOceanTestProduct() {
        // we need:
        // - Oa18_reflectance, Oa19_reflectance, Oa20_reflectance, Oa21_reflectance
        // - pixel_classif_flags
        // - t2m, tcwv (, u10, v10) (, wsp)
        // - altitude, sea_level_pressure
        // - SZA, OZA, SAA, OAA
        // product: S3A_OL_1_EFR____20180818T095755_20180818T100055_20180819T151521_0179_034_350_2160_LN1_O_NT_002.SEN3
        // pixel: 0/46 (i = 46), land, , stride 25x25

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        final double sza = 45.883060809866386;
        final double csza = Math.cos(sza* MathUtils.DTOR);
        addBand(product, "Oa18_reflectance", 0.00979678 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa19_reflectance", 0.0066844756 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa20_reflectance", 0.003092268 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "Oa21_reflectance", 0.008414089 * Math.PI / csza);  // original reflectances, not normalized
        addBand(product, "t2m", 289.7122206810444);  // ERA value from breadboard
        addBand(product, "tcwv", 29.414032166687825);  // ERA value from breadboard
        addBand(product, "SZA", sza);
        addBand(product, "SAA", 137.86100919321686);
        addBand(product, "OZA", 44.154376388489204);
        addBand(product, "OAA", 98.7936597872559);
        addBand(product, "wsp", 7.076327272946396);
        addBand(product, "altitude", 0.0);
        addBand(product, "sea_level_pressure", 1007.61);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {4096}; // Idepix Glint
        pixelClassifFlagBand.setRasterData(ProductData.createInstance(v));
        product.addBand(pixelClassifFlagBand);
        FlagCoding pixelClassifFlagCoding = createDefaultIdepixFlagCoding();
        pixelClassifFlagBand.setSampleCoding(pixelClassifFlagCoding);
        product.getFlagCodingGroup().add(pixelClassifFlagCoding);

        return product;
    }

    private static void addBand(Product product, String bandName, double value) {
        Band a = new Band(bandName, ProductData.TYPE_FLOAT64, 1, 1);
        double[] v = {value};
        a.setRasterData(ProductData.createInstance(v));
        a.setNoDataValueUsed(true);
        a.setNoDataValue(Float.NaN);
        product.addBand(a);
    }

    private static FlagCoding createDefaultIdepixFlagCoding() {

        FlagCoding flagCoding = new FlagCoding("pixel_classif_flags");

        flagCoding.addFlag("IDEPIX_INVALID", BitSetter.setFlag(0, 0), null);
        flagCoding.addFlag("IDEPIX_CLOUD", BitSetter.setFlag(0, 1), null);
        flagCoding.addFlag("IDEPIX_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, 2), null);
        flagCoding.addFlag("IDEPIX_CLOUD_SURE", BitSetter.setFlag(0, 3), null);
        flagCoding.addFlag("IDEPIX_CLOUD_BUFFER", BitSetter.setFlag(0, 4), null);
        flagCoding.addFlag("IDEPIX_CLOUD_SHADOW", BitSetter.setFlag(0, 5), null);
        flagCoding.addFlag("IDEPIX_SNOW_ICE", BitSetter.setFlag(0, 6), null);
        flagCoding.addFlag("IDEPIX_BRIGHT", BitSetter.setFlag(0, 7), null);
        flagCoding.addFlag("IDEPIX_WHITE", BitSetter.setFlag(0, 8), null);
        flagCoding.addFlag("IDEPIX_COASTLINE", BitSetter.setFlag(0, 9), null);
        flagCoding.addFlag("IDEPIX_LAND", BitSetter.setFlag(0, 10), null);

        return flagCoding;
    }


}