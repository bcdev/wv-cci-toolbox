package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvFullAlgoModisTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    // Java version of tests from Python breadboard 'test_cawa.py', which in return is a standalone version
    // of GPF operator code 'cawa_tcwv_modis_op.py' for a single test pixel,
    // using corresponding LUTs from CAWA.
    // Towards a Water_Vapour_cci Java operator, we have to test:
    //     - MODIS ocean
    //     - MODIS land
    // needs lot of heap space, ignore for the moment!

    @Test
    @Ignore
    public void testOptimalEstimation_modis_ocean() {
        final Sensor sensor = Sensor.MODIS_AQUA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_AQUA);
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_AQUA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double[] rhoToaWin = new double[]{0.00320285};
        double[] rhoToaAbs = new double[]{0.00241964629966, 0.0017246503591, 0.00196269592823};
        double sza = 61.64859772;
        double vza = 11.34500027;
        double relAzi = 118.03159332;
        double amf = 1./Math.cos(sza* MathUtils.DTOR) + 1./Math.cos(vza* MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = Double.NaN;            // not needed for ocean
        double priorMslPress = Double.NaN;       // not needed for ocean
        double priorWsp = 7.5;
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        assertEquals(47.566, result.getTcwv(), 1.E-3);
    }

    @Test
    @Ignore
    public void testOptimalEstimation_modis_land() {
        // todo: ignored for the moment. We need an applicable land LUT first. The 'land_core_modis_aqua

        // also remember that MODIS land uses 5 input bands, MODIS ocean only 4 !!

        final Sensor sensor = Sensor.MODIS_AQUA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_AQUA);
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_AQUA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double[] rhoToaWin = new double[]{0.00320285};
        double[] rhoToaAbs = new double[]{0.00241964629966, 0.0017246503591, 0.00196269592823};
        double sza = 61.64859772;
        double vza = 11.34500027;
        double relAzi = 118.03159332;
        double amf = 1./Math.cos(sza* MathUtils.DTOR) + 1./Math.cos(vza* MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;
        double priorMslPress = -1013.25/100.0;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        assertEquals(47.566, result.getTcwv(), 1.E-3);
    }

}