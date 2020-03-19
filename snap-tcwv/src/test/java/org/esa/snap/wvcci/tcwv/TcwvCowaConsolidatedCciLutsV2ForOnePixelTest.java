package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for pixelwise verification of breadboard implementation for land/water and all sensors.
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsV2ForOnePixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_debug_input_fix() {
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

        double priorAl0 = 0.3435968769616401 ;  //reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
        double priorAl1 = 0.3420229777811804;  //reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorT2m = 295.3106630925399;    // 't2m' from ERA, data['t2m'].flat[99]

        double priorMslPress = -Math.log(992.907881657328);  // 'msl' from ERA, data['prs'].flat[99]

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
        assertEquals(44.834, result.getTcwv(), 2.E-3);
        // TCWV uncertainty:
        // Java: 1.334
        // Cowa Consolidated LUTs v2 Python: 1.326
         assertEquals(1.326, result.getTcwvUncertainty(), 1.E-2);
        // Cost function:
        // Java: 0.008817
        // Cowa Consolidated LUTs v2 Python: 0.008817
         assertEquals(0.008817, result.getCost(), 1.E-6);
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_debug_input_from_data() {
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
//        final double reflectance_13 = 0.08753849611396834;  // data['rad'][13].flat[99]
//        final double reflectance_14 = 0.08713751235500412;  // data['rad'][14].flat[99]
//        final double reflectance_15 = 0.04948350229543842;  // data['rad'][15].flat[99]
        final double reflectance_13 = 0.08468585932956763;
        final double reflectance_14 = 0.08438013420563414;
        final double reflectance_15 = 0.047922413176569395;

//        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
//                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;
        final double relAzi = 132.59137374162674;


        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;

//        double priorAl0 = 0.3435968769616401 ;  //reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
//        double priorAl1 = 0.3420229777811804;  //reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorAl0 = 0.3324;
        double priorAl1 = 0.3312;
        double priorT2m = 295.3106630925399;    // 't2m' from ERA, data['t2m'].flat[99]
//        double priorT2m = 296.72161968406357;

//        double priorMslPress = -Math.log(992.907881657328);  // 'msl' from ERA, data['prs'].flat[99]
        double height = 179.9375;
        double slp = 1012.3078;
        double priorMslPress = -Math.log(TcwvUtils.getAtmosphericPressure(slp, height));

//        double priorMslPress = -6.9193591114654;
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 50.368421936278565;       // 'tcwv' from ERA, data['tcw'].flat[99]
//        double priorTcwv = 47.82287330875236;

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
        // Java: 44.535
        // Cowa Consolidated LUTs v2 Python: 44.834054807581836
        assertEquals(44.834, result.getTcwv(), 0.3);
        // TCWV uncertainty:
        // Java: 1.334
        // Cowa Consolidated LUTs v2 Python: 1.326
        assertEquals(1.326, result.getTcwvUncertainty(), 1.E-2);
        // Cost function:
        // Java: 0.00979
        // Cowa Consolidated LUTs v2 Python: 0.008817
        assertEquals(0.008817, result.getCost(), 1.E-3);
    }
}