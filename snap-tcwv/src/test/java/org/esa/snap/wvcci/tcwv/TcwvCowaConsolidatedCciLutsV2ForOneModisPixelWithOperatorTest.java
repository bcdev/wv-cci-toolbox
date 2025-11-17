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
 * Test class for pixelwise verification of breadboard implementation for MODIS.
 * These tests make full use of the TcwvOp operator to ensure that a reference input product is
 * correctly read and all numbers correctly passed into the TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsV2ForOneModisPixelWithOperatorTest {

    // test product: MOD021KM.A2010209.1050.061.2017251234617.hdf

    @Before
    public void setUp() {
    }

    @Test
    @Ignore
    public void testCowaConsolidatedCciLuts_modis_land_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createModisLandTestProduct());
        tcwvOp.setParameter("sensor", "MODIS_TERRA");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("MODIS LAND tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 19.807;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.01);  // Java: 19.8

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        System.out.println("MODIS LAND tcwvUncBand.getSampleFloat(0, 0) = " + tcwvUncBand.getSampleFloat(0, 0));
        final double tcwvUnc_py = 0.102;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 0.01);  // Java: 0.109

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        System.out.println("MODIS LAND costBand.getSampleFloat(0, 0) = " + costBand.getSampleFloat(0, 0));
        final double costFunction_py = 0.0496;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.001);  // Java: 0.0493
    }

    @Test
    @Ignore
    public void testCowaConsolidatedCciLuts_modis_ocean_from_operator() {

        TcwvOp tcwvOp = new TcwvOp();
        tcwvOp.setParameterDefaultValues();
        tcwvOp.setSourceProduct("sourceProduct", createModisOceanTestProduct());
        tcwvOp.setParameter("sensor", "MODIS_TERRA");
        tcwvOp.setParameter("writeCostFunctionValue", "true");
        Product tcwvProd = tcwvOp.getTargetProduct();

        final Band tcwvBand = tcwvProd.getBand("tcwv");
        assertNotNull(tcwvBand);
        System.out.println("MODIS OCEAN tcwvBand.getSampleFloat(0, 0) = " + tcwvBand.getSampleFloat(0, 0));
        final double tcwv_py = 25.424;
        assertEquals(tcwv_py, tcwvBand.getSampleFloat(0, 0), 0.05);  // Java: 25.46

        final Band tcwvUncBand = tcwvProd.getBand("tcwv_uncertainty");
        assertNotNull(tcwvUncBand);
        System.out.println("MODIS OCEAN tcwvUncBand.getSampleFloat(0, 0) = " + tcwvUncBand.getSampleFloat(0, 0));
        final double tcwvUnc_py = 2.542;
        assertEquals(tcwvUnc_py, tcwvUncBand.getSampleFloat(0, 0), 0.1);  // Java: 2.536

        final Band costBand = tcwvProd.getBand("cost_function");
        assertNotNull(costBand);
        System.out.println("MODIS OCEAN costBand.getSampleFloat(0, 0) = " + costBand.getSampleFloat(0, 0));
        final double costFunction_py = 0.1438;
        assertEquals(costFunction_py, costBand.getSampleFloat(0, 0), 0.001);  // Java: 0.1435
    }

    private Product createModisLandTestProduct() {
        // we need:
        // - EV_250_Aggr1km_RefSB_2
        // - EV_500_Aggr1km_RefSB_5
        // - EV_1KM_RefSB_17
        // - EV_1KM_RefSB_18
        // - EV_1KM_RefSB_19
        // - pixel_classif_flags (Idepix flag, no MOD35 here)
        // - t2m, tcwv (, u10, v10) (, wsp)
        // - Height
        // - SensorZenith, SensorAzimuth, SolarZenith, SolarAzimuth
        // product: MOD021KM.A2010209.1050.061.2017251234617.hdf, stride 5x5
        // pixel: 0/99 (i = 99), ocean

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        // reflectances: numbers are original reflectances after division by PI (breadboard)
        addBand(product, "EV_250_Aggr1km_RefSB_2", 0.078153186*Math.PI);
        addBand(product, "EV_500_Aggr1km_RefSB_5", 0.075248554*Math.PI);
        addBand(product, "EV_1KM_RefSB_17", 0.060047332*Math.PI);
        addBand(product, "EV_1KM_RefSB_18", 0.018890083*Math.PI);
        addBand(product, "EV_1KM_RefSB_19", 0.03206635*Math.PI);
        addBand(product, "t2m", 294.6319);
        addBand(product, "tcwv", 22.701862);
        addBand(product, "SensorZenith", 31.806626776644094);
        addBand(product, "SensorAzimuth", 288.821547);
        addBand(product, "SolarZenith", 35.82124176111799);
        addBand(product, "SolarAzimuth", 169.765588);
        addBand(product, "Height", 0.135492);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {1024}; // Idepix LAND
        pixelClassifFlagBand.setRasterData(ProductData.createInstance(v));
        product.addBand(pixelClassifFlagBand);
        FlagCoding pixelClassifFlagCoding = createDefaultIdepixFlagCoding();
        pixelClassifFlagBand.setSampleCoding(pixelClassifFlagCoding);
        product.getFlagCodingGroup().add(pixelClassifFlagCoding);

        return product;
    }

    private Product createModisOceanTestProduct() {
        // we need:
        // - EV_250_Aggr1km_RefSB_2
        // - EV_500_Aggr1km_RefSB_5
        // - EV_1KM_RefSB_17
        // - EV_1KM_RefSB_18
        // - EV_1KM_RefSB_19
        // - pixel_classif_flags (Idepix flag, no MOD35 here)
        // - t2m, tcwv (, u10, v10) (, wsp)
        // - Height
        // - SensorZenith, SensorAzimuth, SolarZenith, SolarAzimuth
        // product: MOD021KM.A2010209.1050.061.2017251234617.hdf, stride 5x5
        // pixel: 0/99 (i = 99), ocean

        Product product = new Product("dummy", "dummy", 1, 1);
        product.setProductType("idepix_era");
        // reflectances: numbers are original reflectances after division by PI (breadboard)
        addBand(product, "EV_250_Aggr1km_RefSB_2", 0.0034251227*Math.PI);
        addBand(product, "EV_500_Aggr1km_RefSB_5", 0.0013736046*Math.PI);
        addBand(product, "EV_1KM_RefSB_17", 0.002565998*Math.PI);
        addBand(product, "EV_1KM_RefSB_18", 0.0015034864*Math.PI);
        addBand(product, "EV_1KM_RefSB_19", 0.0017748937*Math.PI);
        addBand(product, "t2m", 289.00278);
        addBand(product, "tcwv", 20.782545);
//        addBand(product, "u10", -4.4887967);
//        addBand(product, "v10", 0.97683114);
        addBand(product, "wsp", 5.0);
        addBand(product, "SensorZenith", 16.440480348558776);
        addBand(product, "SensorAzimuth", 103.73871);
        addBand(product, "SolarZenith", 38.77434689856718);
        addBand(product, "SolarAzimuth", 157.055894);
        addBand(product, "Height", -5.9796);
        final Band pixelClassifFlagBand = new Band("pixel_classif_flags", ProductData.TYPE_INT16, 1, 1);
        final short[] v = {4096}; // Idepix Glint. We do not provide a MOD35
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