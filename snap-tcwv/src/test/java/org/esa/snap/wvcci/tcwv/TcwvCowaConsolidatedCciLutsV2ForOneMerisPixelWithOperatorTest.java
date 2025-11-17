package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.wvcci.tcwv.dataio.mod35.Mod35L2CloudMaskUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for pixelwise verification of breadboard implementation for MERIS.
 * These tests make full use of the TcwvOp operator to ensure that a reference input product is
 * correctly read and all numbers correctly passed into the TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsV2ForOneMerisPixelWithOperatorTest {

    // test product: subset_of_L2_of_MER_RR__1PRACR20110702_140801_000026343104_00111_48832_0000_era-interim.dim

    @Before
    public void setUp() {
    }

    @Test
    @Ignore
    public void testCowaConsolidatedCciLuts_meris_land_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createMerisLandTestProduct());
        tcwvOp.setParameter("sensor", "MERIS");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("MERIS LAND tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 44.834;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.75);  // Java: 44.19

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        final double tcwvUnc_py = 1.326;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 1.E-2);  // Java: 1.317

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        final double costFunction_py = 0.008817;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.005);  // Java: 0.004
    }

    @Test
    @Ignore
    public void testCowaConsolidatedCciLuts_meris_ocean_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createMerisOceanTestProduct());
        tcwvOp.setParameter("sensor", "MERIS");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("MERIS OCEAN tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 43.164;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.5);  // Java: 42.7

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        final double tcwvUnc_py = 1.542;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 0.5);  // Java: 1.604

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        final double costFunction_py = 0.2345;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.05);  // Java: 0.19
    }

    private Product createMerisLandTestProduct() {
        // we need:
        // - reflectance_13-15
        // - pixel_classif_flags
        // - t2m, tcwv (, u10, v10)
        // - latitude, longitude
        // - dem_alt, atm_press
        // - sun_zenith, sun_azimuth, view_zenith, view_azimuth
        // product: subset_of_L2_of_MER_RR__1PRACR20110702_140801_000026343104_00111_48832_0000_era-interim.dim
        // pixel: 250/1150, land

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        addBand(product, "reflectance_13", 0.3324);  // original reflectances, not normalized
        addBand(product, "reflectance_14", 0.3312);
        addBand(product, "reflectance_15", 0.1881);
//        addBand(product, "t2m", 295.3106630925399);  // ERA value from breadboard
        addBand(product, "t2m", 296.72161968406357);  // ERA value from Java processing
//        addBand(product, "tcwv", 50.368421936278565);  // ERA value from breadboard
        addBand(product, "tcwv", 47.82287330875236);  // ERA value from Java processing
        addBand(product, "latitude", 3.351295);
        addBand(product, "longitude", -70.55016);
        addBand(product, "sun_zenith", 36.832975);
        addBand(product, "sun_azimuth", 54.86207);
        addBand(product, "view_zenith", 25.411228);
        addBand(product, "view_azimuth", 102.27069);
        addBand(product, "dem_alt", 160.78125);
        addBand(product, "atm_press", 1012.3);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {1024}; // only land flag raised
        pixelClassifFlagBand.setRasterData(ProductData.createInstance(v));
        product.addBand(pixelClassifFlagBand);
        FlagCoding pixelClassifFlagCoding = createDefaultIdepixFlagCoding();
        pixelClassifFlagBand.setSampleCoding(pixelClassifFlagCoding);
        product.getFlagCodingGroup().add(pixelClassifFlagCoding);

        return product;
    }

    private Product createMerisOceanTestProduct() {
        // we need:
        // - reflectance_13-15
        // - pixel_classif_flags
        // - t2m, tcwv (, u10, v10)
        // - latitude, longitude
        // - dem_alt, atm_press
        // - sun_zenith, sun_azimuth, view_zenith, view_azimuth
        // product: subset_of_L2_of_MER_RR__1PRACR20110702_140801_000026343104_00111_48832_0000_era-interim.dim
        // pixel: 680/530, ocean

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        addBand(product, "reflectance_13", 0.0584);  // original reflectances, not normalized
        addBand(product, "reflectance_14", 0.0567);
        addBand(product, "reflectance_15", 0.0326);
        addBand(product, "t2m", 301.69305);  // ERA value from Java processing
        addBand(product, "tcwv", 53.07362);  // ERA value from Java processing
        addBand(product, "u10", -4.4887967);  // ERA value from Java processing
        addBand(product, "v10", 0.97683114);  // ERA value from Java processing
        addBand(product, "latitude", 11.078009);
        addBand(product, "longitude", -64.69959);
        addBand(product, "sun_zenith", 28.318886);
        addBand(product, "sun_azimuth", 61.50807);
        addBand(product, "view_zenith", 10.32719);
        addBand(product, "view_azimuth", 282.7206);
        addBand(product, "dem_alt", -219.0);
        addBand(product, "atm_press", 1012.3);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {4096}; // a glint pixel in test product, but we do not care for that
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