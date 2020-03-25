package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for pixelwise verification of breadboard implementation for land/water and all sensors.
 *
 * @author olafd
 */
public class TcwvCowaConsolidatedCciLutsForOnePixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_land() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 45.286419;     // data['geo']['SZA'].flat[99]     , 'sun_zenith'
        final double vza = 32.795418;     // data['geo']['OZA'].flat[99]     , 'view_zenith'
        final double saa = 185.191369;    // data['geo']['SAA'].flat[99]     , 'sun_azimuth'
        final double vaa = 306.755;       // data['geo']['OAA'].flat[99]     , 'view_azimuth'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_13 = 0.00818;  // data['rad'][13].flat[99]
        final double reflectance_14 = 0.00784;  // data['rad'][14].flat[99]
        final double reflectance_15 = 0.00578;  // data['rad'][15].flat[99]

        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;

        double priorAl0 = reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
        double priorAl1 = reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorT2m = 279.78384;    // 't2m' from ERA, data['t2m'].flat[99]

        double priorMslPress = 998.3356;  // 'msl' from ERA, data['prs'].flat[99]
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 15.4964;       // 'tcwv' from ERA, data['tcw'].flat[99]

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
        // Java: 16.47131
        // Cowa Consolidated LUTs Python: 16.37232
        assertEquals(16.37232, result.getTcwv(), 0.5);
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_ocean() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        final double sza = 45.3854179;     // data['geo']['SZA'].flat[97]     , 'sun_zenith'
        final double vza = 31.512769;     // data['geo']['OZA'].flat[97]     , 'view_zenith'
        final double saa = 184.654147;    // data['geo']['SAA'].flat[97]     , 'sun_azimuth'
        final double vaa = 306.37474;       // data['geo']['OAA'].flat[97]     , 'view_azimuth'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_13 = 0.019905;  // data['rad'][13].flat[97]
        final double reflectance_14 = 0.019788;  // data['rad'][14].flat[97]
        final double reflectance_15 = 0.016378;  // data['rad'][15].flat[97]

        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // aot is in log10(0.1+aot)
//        double priorAot = -0.6;
        // inp['aot'] = ternary(np.isfinite(data['aot'].flat[i]), data['aot'].flat[i],
        //                      DEMO_CONFIG['PROCESSING']['land_aot_fallback'])
        double priorAot = TcwvConstants.AOT_FALLBACK_OCEAN;

        double priorAl0 = reflectance_13 * Math.PI;   // data['rad'][13].flat[97]*np.pi
        double priorAl1 = reflectance_14 * Math.PI;   // data['rad'][14].flat[97]*np.pi
        double priorT2m = 279.34308;                  // 't2m' from ERA, data['t2m'].flat[97]

        double priorMslPress = 998.3356;  // 'msl' from ERA, data['prs'].flat[97]
        double priorWsp = 9.45422;                   // 'wsp' from ERA, data['wsp'].flat[97]
        double priorTcwv = 15.4964;                  // 'tcwv' from ERA, data['tcw'].flat[97]

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
        // Java: 9.9043
        // Cowa Consolidated LUTs Python: 9.9277
        assertEquals(9.9277, result.getTcwv(), 0.025);
        // TCWV uncertainty:
        // Java: 1.29847
        // Cowa Consolidated LUTs Python: 1.29911
        assertEquals(1.29911, result.getTcwvUncertainty(), 0.001);
        // Cost function:
        // Java: 0.514326
        // Cowa Consolidated LUTs Python: 0.519088
        assertEquals(0.519088, result.getCost(), 0.005);
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_land() {
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 43.8862;     // data['geo']['SZA'].flat[89]     , 'SZA'
        final double vza = 27.92696;     // data['geo']['OZA'].flat[89]     , 'OZA'
        final double saa = 142.82275;    // data['geo']['SAA'].flat[89]     , 'SAA'
        final double vaa = 102.0784;       // data['geo']['OAA'].flat[89]     , 'OAA'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_18 = 0.0811478;  // data['rad'][18].flat[89]
        final double reflectance_19 = 0.05119;  // data['rad'][19].flat[89]
        final double reflectance_20 = 0.014369;  // data['rad'][20].flat[89]
        final double reflectance_21 = 0.084;  // data['rad'][21].flat[89]

        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
//        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;
        double priorAot = TcwvConstants.AOT_FALLBACK_OCEAN;  // we have this in the breadboard land test pixel

        double priorAl0 = reflectance_18 * Math.PI;   // data['rad'][18].flat[89]*np.pi
        double priorAl1 = reflectance_21 * Math.PI;   // data['rad'][21].flat[89]*np.pi
        double priorT2m = 291.066;    // 't2m' from ERA, data['t2m'].flat[89]

        double priorMslPress = 1006.531756;  // 'msl' from ERA, data['prs'].flat[89]
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.965088;       // 'tcwv' from ERA, data['tcw'].flat[89]

        double[] rhoToaWin = new double[]{reflectance_18, reflectance_21};  // bands 18, 21
        double[] rhoToaAbs = new double[]{reflectance_19, reflectance_20};  // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("OLCI LAND result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 23.4505
        // Cowa Consolidated LUTs Python: 23.3301
        assertEquals(23.3301, result.getTcwv(), 0.15);
        // TCWV uncertainty:
        // Java: 0.1091
        // Cowa Consolidated LUTs Python: 0.1099
        assertEquals(0.1099, result.getTcwvUncertainty(), 2.E-3);
        // Cost function:
        // Java: 0.25055
        // Cowa Consolidated LUTs Python: 0.17089
        assertEquals(0.17089, result.getCost(), 0.1);
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_ocean() {
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        final double sza = 45.8830608099;     // data['geo']['SZA'].flat[46]     , 'sun_zenith'
        final double vza = 44.1543763885;     // data['geo']['OZA'].flat[46]     , 'view_zenith'
        final double saa = 137.861009193;    // data['geo']['SAA'].flat[46]     , 'sun_azimuth'
        final double vaa = 98.7936597873;       // data['geo']['OAA'].flat[46]     , 'view_azimuth'

        // NOTE: for MERIS and OLCI, these reflectances are radiance / flux = rhoToa(product) * cos(sza)
        final double reflectance_18 = 0.00979678;  // data['rad'][18].flat[46]
        final double reflectance_19 = 0.00668448;  // data['rad'][19].flat[46]
        final double reflectance_20 = 0.00309227;  // data['rad'][20].flat[46]
        final double reflectance_21 = 0.00841409;  // data['rad'][21].flat[46]

        final double relAzi = 180. - Math.acos(Math.cos(saa * MathUtils.DTOR) * Math.cos(vaa * MathUtils.DTOR) +
                                                       Math.sin(saa * MathUtils.DTOR) * Math.sin(vaa * MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // aot is in log10(0.1+aot)
//        double priorAot = -0.6;
        // inp['aot'] = ternary(np.isfinite(data['aot'].flat[i]), data['aot'].flat[i],
        //                      DEMO_CONFIG['PROCESSING']['land_aot_fallback'])
//        double priorAot = TcwvConstants.AOT_FALLBACK_OCEAN;
        double priorAot = -0.453496;  // we have real data for the test pixel

        double priorAl0 = reflectance_18 * Math.PI;   // data['rad'][18].flat[46]*np.pi
        double priorAl1 = reflectance_21 * Math.PI;   // data['rad'][21].flat[46]*np.pi
        double priorT2m = 289.71222;                  // 't2m' from ERA, data['t2m'].flat[46]

        double priorMslPress = 1007.6075;  // 'msl' from ERA, data['prs'].flat[46]
        double priorWsp = 7.07632727295;                   // 'wsp' from ERA, data['wsp'].flat[46]
        double priorTcwv = 29.4140321667;                  // 'tcwv' from ERA, data['tcw'].flat[46]

        double[] rhoToaWin = new double[]{reflectance_18};  // bands 18 only
        double[] rhoToaAbs = new double[]{reflectance_19, reflectance_20};  // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);

        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("OLCI OCEAN result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 35.4657
        // Cowa Consolidated LUTs Python: 34.8929
        assertEquals(34.8929, result.getTcwv(), 0.75);
        // TCWV uncertainty:
        // Java: 1.63641
        // Cowa Consolidated LUTs Python: 2.26387
        assertEquals(2.26387, result.getTcwvUncertainty(), 0.75);
        // Cost function:
        // Java: 0.09092
        // Cowa Consolidated LUTs Python: 0.08944
        assertEquals(0.08944, result.getCost(), 0.01);
    }

    @Test
    public void testCowaConsolidatedCciLuts_modis_land() {
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionLand = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 35.82124176;     // data['suz'].flat[1555]     , 'suz'
        final double vza = 31.8066267;     // data['vie'].flat[1555]     , 'vie'
        final double saa = 169.765588;    // data['saa'].flat[1555]     , 'saa'
        final double vaa = 288.8215473;       // data['vaa'].flat[1555]     , 'vaa'

        // NOTE: for MODIS, these reflectances are RefSB_* / Math.PI !!!
        final double reflectance_2 = 0.0781532;  // data['rad'][2].flat[1555]
        final double reflectance_5 = 0.0752486;  // data['rad'][5].flat[1555]
        final double reflectance_17 = 0.060047332;  // data['rad'][17].flat1555]
        final double reflectance_18 = 0.0188901;  // data['rad'][18].flat[1555]
        final double reflectance_19 = 0.0320663;  // data['rad'][19].flat[1555]

        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;     // data['aot'][1555].flat[1555]

        double priorAl0 = reflectance_2 * Math.PI;   // data['rad'][18].flat[1555]*np.pi
        double priorAl1 = reflectance_5 * Math.PI;   // data['rad'][21].flat[1555]*np.pi
        double priorT2m = 294.632;    // 't2m' from ERA, data['t2m'].flat[1555]

        double priorMslPress = 1012.983728;  // 'msl' from ERA, data['prs'].flat[1555]
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 22.7019;       // 'tcwv' from ERA, data['tcwv'].flat[1555]

        double[] rhoToaWin = new double[]{reflectance_2, reflectance_5};  // bands 2, 5
        double[] rhoToaAbs = new double[]{reflectance_17, reflectance_18, reflectance_19};  // bands 17, 18, 19

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionLand, null,
                                                    input, true);

        System.out.println("MODIS TERRA LAND result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 19.8539
        // Cowa Consolidated LUTs Python: 19.8067
        assertEquals(19.8067, result.getTcwv(), 0.05);
        // TCWV uncertainty:
        // Java: 0.10812
        // Cowa Consolidated LUTs Python: 0.10234
        assertEquals(0.10234, result.getTcwvUncertainty(), 0.01);
        // Cost function:
        // Java: 0.063818
        // Cowa Consolidated LUTs Python: 0.004964
        assertEquals(0.004964, result.getCost(), 0.1);
    }

    @Test
    public void testCowaConsolidatedCciLuts_modis_ocean() {
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        final double sza = 38.774347;     // data['suz'].flat[99]     , 'suz'
        final double vza = 16.44048;     // data['vie'].flat[99]     , 'vie'
        final double saa = 157.055894;    // data['saa'].flat[99]     , 'saa'
        final double vaa = 103.73871;       // data['vaa'].flat[99]     , 'vaa'

        // NOTE: for MODIS, these reflectances are RefSB_* / Math.PI !!!
        final double reflectance_2 = 0.00342512;  // data['rad'][2].flat[99]
        final double reflectance_5 = 0.0013736;  // data['rad'][5].flat[99]
        final double reflectance_17 = 0.002566;  // data['rad'][17].flat[99]
        final double reflectance_18 = 0.00150349;  // data['rad'][18].flat[99]
        final double reflectance_19 = 0.00177489;  // data['rad'][19].flat[99]

        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
//        double priorAot = TcwvConstants.AOT_FALLBACK_LAND;     // data['aot'].flat[99]
        double priorAot = TcwvConstants.AOT_FALLBACK_OCEAN;     // we have this in the breadboard land test pixel

        double priorAl0 = reflectance_2 * Math.PI;   // data['rad'][2].flat[99]*np.pi
        double priorAl1 = reflectance_5 * Math.PI;   // data['rad'][5].flat[99]*np.pi
        double priorT2m = 289.003;    // 't2m' from ERA, data['t2m'].flat[99]

        double priorMslPress = 1013.0;  // 'msl' from ERA, data['prs'].flat[99]
        double priorWsp = 5.0;     // 'wsp' from ERA, data['wsp'].flat[99]
        double priorTcwv = 20.7825;       // 'tcwv' from ERA, data['tcwv'].flat[99]

        double[] rhoToaWin = new double[]{reflectance_2};  // bands 2
        double[] rhoToaAbs = new double[]{reflectance_17, reflectance_18, reflectance_19};  // bands 17, 18, 19

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MODIS TERRA OCEAN result.getTcwv() = " + result.getTcwv());
        // TCWV:
        // Java: 25.474
        // Cowa Consolidated LUTs Python: 25.4244
        assertEquals(25.4244, result.getTcwv(), 0.05);
        // TCWV uncertainty:
        // Java: 2.53112
        // Cowa Consolidated LUTs Python: 2.54177
        assertEquals(2.54177, result.getTcwvUncertainty(), 0.02);
        // Cost function:
        // Java: 0.14349
        // Cowa Consolidated LUTs Python: 0.14383
        assertEquals(0.14383, result.getCost(), 0.001);
    }

}