package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for pixelwise verification of breadboard implementation for land/water and all sensors.
 * These tests basically test the TCWV retrieval, i.e. same retrieval input should result in (almost)
 * identical TCWVs in Java and Python
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsV2ForOnePixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    // test product: subset_of_L2_of_MER_RR__1PRACR20110702_140801_000026343104_00111_48832_0000_era-interim.dim

    @Test
    public void testCowaConsolidatedCciLuts_meris_land_debug_input_fix() {
        // pixel 254/1101, breadboard index 1234475, land
        // test uses input retrieval values from breadboard --> very small Java/Python differences
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 36.83297526836395;     // data['geo']['SZA'].flat[99]     , 'sun_zenith'
        final double vza = 25.411228895187378;     // data['geo']['OZA'].flat[99]     , 'view_zenith'
//        final double saa = 185.191369;    // data['geo']['SAA'].flat[99]     , 'sun_azimuth'
//        final double vaa = 306.755;       // data['geo']['OAA'].flat[99]     , 'view_azimuth'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_13 = 0.08753849611396834;  // data['rad'][13].flat[99]
        final double reflectance_14 = 0.08713751235500412;  // data['rad'][14].flat[99]
        final double reflectance_15 = 0.04948350229543842;  // data['rad'][15].flat[99]

//        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
//                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;
        final double relAzi = 132.59137374162674;


        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;

        double priorAl0 = 0.3435968769616401;  //reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
        double priorAl1 = 0.3420229777811804;  //reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorT2m = 295.3106630925399;    // 't2m' from ERA, data['t2m'].flat[99]

        double priorMslPress = 992.907881657328;  // 'msl' from ERA, data['prs'].flat[99]

        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 50.368421936278565;       // 'tcwv' from ERA, data['tcw'].flat[99]

        double[] rhoToaWin = new double[]{reflectance_13, reflectance_14};  // bands 13, 14
        double[] rhoToaAbs = new double[]{reflectance_15};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                landLut, null,
                tcwvFunctionLand, null,
                jacobiFunctionland, null,
                input, true);

        System.out.println("MERIS LAND result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 44.8353
        // Cowa Consolidated LUTs v2 Python: 44.834054807581836
//        assertEquals(44.834, result.getTcwv(), 2.E-3);
        // TCWV uncertainty:
        // Java: 1.334
        // Cowa Consolidated LUTs v2 Python: 1.326
//        assertEquals(1.326, result.getTcwvUncertainty(), 1.E-2);
        // Cost function:
        // Java: 0.008817
        // Cowa Consolidated LUTs v2 Python: 0.008817
//        assertEquals(0.008817, result.getCost(), 1.E-6);
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_ocean_debug_input_fix() {
        // pixel 568/306, breadboard index 343594, water
        // test uses input retrieval values from breadboard --> small Java/Python differences
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);
        final double sza = 29.593973;     // data['geo']['SZA'].flat[99]     , 'sun_zenith'
        final double vza = 0.6950351;     // data['geo']['OZA'].flat[99]     , 'view_zenith'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_13 = 0.00525476;  // data['rad'][13].flat[99]
        final double reflectance_14 = 0.00490123;  // data['rad'][14].flat[99]
        final double reflectance_15 = 0.0030563;   // data['rad'][15].flat[99]

        final double relAzi = 6.9178667;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_OCEAN;

        double priorAl0 = 0.3435968769616401;  //reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
        double priorAl1 = 0.3420229777811804;  //reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorT2m = 298.416715;    // 't2m' from ERA, data['t2m'].flat[99]

        double priorMslPress = 1012.4751356;  // 'msl' from ERA, data['prs'].flat[99]

        double priorWsp = 1.91074;
        double priorTcwv = 50.78113;

        double[] rhoToaWin = new double[]{reflectance_13, reflectance_14};  // bands 13, 14
        double[] rhoToaAbs = new double[]{reflectance_15};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                null, oceanLut,
                null, tcwvFunctionOcean,
                null, jacobiFunctionOcean,
                input, false);

        System.out.println("MERIS OCEAN result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 46.31
        // Cowa Consolidated LUTs Python: 46.494
        final double tcwv_py = 46.494;
        assertEquals(tcwv_py, result.getTcwv(), 0.2);
        // TCWV uncertainty:
        // Java: 2.707
        // Cowa Consolidated LUTs Python: 3.00735
        assertEquals(3.00735, result.getTcwvUncertainty(), 0.5);
        // Cost function:
        // Java: 0.15649
        // Cowa Consolidated LUTs Python: 0.15651
        assertEquals(0.15651, result.getCost(), 0.05);
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_land_river_input_from_processing() {
        // pixel 254/1101, breadboard index 1234475, river/valley (made to land)
        // this test uses 'real' input taken from a Java processing product to illustrate differences
        // introduced by different solar fluxes and ERA interpolation compared to the breadboard
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 38.01553678512573;
        final double vza = 16.27353799343109;

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza) / PI
//        final double reflectance_13 = 0.0543860100113034;   // BB
//        final double reflectance_14 = 0.05356103042764023;    // BB
//        final double reflectance_15 = 0.028922264726026405;   // BB
        final double reflectance_13 = 0.05258824334044112;
        final double reflectance_14 = 0.05186098580259048;
        final double reflectance_15 = 0.028011954130316034;

        final double relAzi = 57.044994115829454;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;

//        double priorAl0 = 0.21686874291165054 ;   // BB
//        double priorAl1 = 0.21357906813683944;    // BB
        double priorAl0 = 0.2097;
        double priorAl1 = 0.2068;
//        double priorT2m = 297.41864830547735;     // BB
        double priorT2m = 299.24500248777895;

//        double priorMslPress = 1005.7426752325695;    // BB
        double priorMslPress = 1005.9908841849962;

        double priorWsp = Double.NaN;     // not needed for land
//        double priorTcwv = 47.29425655150536;     // BB
        double priorTcwv = 47.41606339550014;

        double[] rhoToaWin = new double[]{reflectance_13, reflectance_14};  // bands 13, 14
        double[] rhoToaAbs = new double[]{reflectance_15};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                landLut, null,
                tcwvFunctionLand, null,
                jacobiFunctionland, null,
                input, true);

        System.out.println("MERIS LAND RIVER result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 58.786
        // Cowa Consolidated LUTs v2 Python: 58.44048
        // Java/Python differences due to different solar fluxes and different ERA Interim interpolation
//        assertEquals(58.44048, result.getTcwv(), 0.4);
    }

}