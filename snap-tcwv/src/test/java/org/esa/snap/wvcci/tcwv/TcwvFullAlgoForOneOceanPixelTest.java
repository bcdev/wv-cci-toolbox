package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvFullAlgoForOneOceanPixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    // Java version of tests from Python breadboard 'test_cawa.py', which in return is a standalone version
    // of GPF operator code 'cawa_tcwv_modis_op.py' for a single test pixel,
    // using corresponding LUTs from CAWA.

    @Test
//    @Ignore
    public void testOptimalEstimation_ocean_meris() {
        // make sure you have the right LUT, otherwise ignore this test!
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 61.43579102;
        double vza = 28.43509483;
        double relAzi = 135.61277771;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // the mes array is taken from test_cawa.py --> test_cawa_meris_ocean() in wv-cci-parent_se_only:
        // mes = [0.19034228, 0.18969933, 0.21104884]
        // here we invert it to the 'true' rhoToa input and re-invert later:
        double[] mes = new double[]{0.19034228, 0.18969933, 0.21104884};
        double[] rhoToaWin = new double[2];
        double[] rhoToaAbs = new double[1];
        System.arraycopy(mes, 0, rhoToaWin, 0, rhoToaWin.length);
        for (int i = 0; i < rhoToaAbs.length; i++) {
            // inversion of line 56-58, TcwvAlgorithm
            rhoToaAbs[i] = rhoToaWin[1] * Math.exp(-mes[2+i] * Math.sqrt(amf));
        }
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 7.5;
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MERIS OCEAN result.getTcwv() = " + result.getTcwv());
        assertEquals(28.007, result.getTcwv(), 1.E-3);     // Python result: 28.007xxx --> almost exactly matching
    }

    @Test
    public void testOptimalEstimation_ocean_modis_terra() {
        // make sure you have the right LUT, otherwise ignore this test!
        // remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 61.64859772;
        double vza = 11.34500027;
        double relAzi = 118.03159332;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // the mes array is taken from test_cawa.py --> test_cawa_land_modis_terra() in wv-cci-parent_se_only:
        // remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!
        // mes = [0.00320285,  0.15861148,  0.35012841,  0.27699689]
        // here we invert it to the 'true' rhoToa input and re-invert later:
        double[] mes = new double[]{0.00320285,  0.15861148,  0.35012841,  0.27699689};
        double[] rhoToaWin = new double[1];
        double[] rhoToaAbs = new double[3];
        System.arraycopy(mes, 0, rhoToaWin, 0, rhoToaWin.length);
        for (int i = 0; i < rhoToaAbs.length; i++) {
            rhoToaAbs[i] = rhoToaWin[0] * Math.exp(-mes[1+i] * Math.sqrt(amf));
        }
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 7.5;
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut, 
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MODIS TERRA OCEAN result.getTcwv() = " + result.getTcwv());
        assertEquals(46.252, result.getTcwv(), 1.E-3);
        // Python result: 46.252 --> exactly matching
        // todo: re-check later with updated LUTs
    }

    @Test
    public void testOptimalEstimation_ocean_modis_terra_2() {
        // make sure you have the right LUT, otherwise ignore this test!
        // remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 24.951;
        double saa = 118.234;
        double vza = 38.047;
        double vaa = 97.217;
        double relAzi = 180. - Math.abs(saa - vaa);;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // the mes array is taken from test_cawa.py --> test_cawa_land_modis_terra() in wv-cci-parent_se_only:
        // remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!
        // mes = [0.00320285,  0.15861148,  0.35012841,  0.27699689]
        double[] mes = new double[]{0.019208,  0.013863,  0.007082,  0.008598};
        double[] rhoToaWin = new double[1];
        rhoToaWin[0] = mes[0]*Math.cos(sza*MathUtils.DTOR);
        double[] rhoToaAbs = new double[3];
        for (int i = 0; i < rhoToaAbs.length; i++) {
            rhoToaAbs[i] = mes[i+1]*Math.cos(sza*MathUtils.DTOR);
        }
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 7.5;
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MODIS TERRA OCEAN result.getTcwv() = " + result.getTcwv());
        assertEquals(65.636, result.getTcwv(), 1.E-3);
        // Python result: 65.636 --> exactly matching
        // todo: re-check later with updated LUTs
    }


}