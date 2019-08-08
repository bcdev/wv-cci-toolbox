package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvFullAlgoForOneLandPixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    // Java version of tests from Python breadboard 'test_cawa.py', which in return is a standalone version
    // of GPF operator code 'cawa_tcwv_modis_op.py' for a single test pixel,
    // using corresponding LUTs from CAWA.
    // Towards a Water_Vapour_cci Java operator, we actually have to test:
    //     - MERIS land
    //     - MODIS TERRA land

    @Test
    @Ignore
    public void testOptimalEstimation_land_meris() {
        // ignored - TODO: adapt to Cowa and new LUTs
        // make sure you have the right LUT, otherwise ignore this test!
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double sza = 5.29114494e+01;
        double vza = 2.70720062e+01;
        double relAzi = 4.48835754e+01;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // the mes array is taken from test_cawa.py --> test_cawa_land_modis_terra() in wv-cci-parent_se_only:
        // mes = [0.19290966,  0.19140355,  0.14358414]
        // here we invert it to the 'true' rhoToa input and re-invert later:
        double[] mes = new double[]{0.19290966,  0.19140355,  0.14358414};
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

        double priorMslPress = -1013./100.;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("MERIS result.getTcwv() = " + result.getTcwv());
        assertEquals(7.1699, result.getTcwv(), 1.E-4);     // Python result: 7.1699 --> exactly matching
    }

    @Test
//    @Ignore
    public void testOptimalEstimation_land_modis_terra() {
        // make sure you have the right LUT, otherwise ignore this test!
        // remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double sza = 6.23843994e+01;
        double vza = 2.59459991e+01;
        double relAzi = 1.18127800e+02;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        // the mes array is taken from test_cawa.py --> test_cawa_land_modis_terra() in wv-cci-parent_se_only:
        // mes = [0.05588217,  0.06197434,  0.10987211,  0.33038937,  0.22174702]
        // here we invert it to the 'true' rhoToa input and re-invert later:
        double[] mes = new double[]{0.05588217,  0.06197434, 0.10987211,  0.33038937,  0.22174702};
        double[] rhoToaWin = new double[2];
        double[] rhoToaAbs = new double[3];
        System.arraycopy(mes, 0, rhoToaWin, 0, rhoToaWin.length);
        for (int i = 0; i < rhoToaAbs.length; i++) {
            rhoToaAbs[i] = rhoToaWin[1] * Math.exp(-mes[2+i] * Math.sqrt(amf));
        }
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1003./100.;  // todo: to be fixed by RP
        priorMslPress = -Math.log(priorMslPress);
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("MODIS TERRA result.getTcwv() = " + result.getTcwv());
        assertEquals(7.009, result.getTcwv(), 1.E-3);      // with new LUT, 20190607
        // Python result: 6.38 --> might be ok
    }

}