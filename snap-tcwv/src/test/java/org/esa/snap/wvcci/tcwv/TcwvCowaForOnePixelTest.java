package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvCowaForOnePixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testCowaLand_1() {
        // this is 'test_cowa_land_1' from functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double sza = 30.;
        double vza = 20.0;
        double relAzi = 170.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 280.0;

        double priorMslPress = -Math.log(1005.0);
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 15.0;

        double[] rhoToaWin = new double[]{0.042, 0.042};  // bands 13, 14
        double[] rhoToaAbs = new double[]{0.031};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("MERIS LAND result.getTcwv() = " + result.getTcwv());
        // Java: 16.4272978
        // Cowa Python: 16.46472
        assertEquals(16.46472, result.getTcwv(), 0.05);
    }

    @Test
    public void testCowaOcean_1() {
        // this is 'test_cowa_ocean_1' from functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 30.0;
        double vza = 20.0;
        double relAzi = 10.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 8.0;
        double priorTcwv = 25.0;

        double[] rhoToaWin = new double[]{0.0317360866889, 0.0313907463};  // bands 13, 14
        double[] rhoToaAbs = new double[]{0.021355692};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MERIS OCEAN result.getTcwv() = " + result.getTcwv());
        // Java: 19.54744
        // Cowa Python: 19.35311
        assertEquals(19.35311, result.getTcwv(), 0.2);
    }

    @Test
    public void testCowaLand_3() {
        // this is 'test_cowa_land_1' from functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double sza = 30.;
        double vza = 20.0;
        double relAzi = 170.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 280.0;

        double priorMslPress = -Math.log(1005.0);
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 15.0;

        double[] rhoToaWin = new double[]{0.042, 0.042};  // bands 18, 21
        double[] rhoToaAbs = new double[]{0.031, 0.013};   // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("OLCI LAND result.getTcwv() = " + result.getTcwv());
        // Java: 13.7652
        // Cowa Python: 13.7686
        assertEquals(13.7686, result.getTcwv(), 0.01);
    }

    @Test
//    @Ignore
    public void testCowaOcean_2() {
        // this is 'test_cowa_ocean_2' from functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 30.0;
        double vza = 20.0;
        double relAzi = 10.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 8.0;
        double priorTcwv = 25.0;

        double[] rhoToaWin = new double[]{0.031};  // band 18 only!!
        double[] rhoToaAbs = new double[]{0.021, 0.007};   // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);

        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("OLCI OCEAN result.getTcwv() = " + result.getTcwv());
        // Java: 22.13765
        // Cowa Python: 22.18858
        assertEquals(22.18858, result.getTcwv(), 0.1);
    }

    @Test
    public void testOptimalEstimation_ocean_modis_terra() {
        // taken from 1x1 pixel product subset_1x1_ocean_of_MOD021KM.A2011196.0930.061,
        // comparison with TcwvOp called from SNAP desktop
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 50.97;
        double saa = 34.2362;
        double vza = 6.7142;
        double vaa = 99.53059;
        double relAzi = 180. - Math.abs(saa - vaa);
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double[] rhoToaWin = new double[]{0.00949};  // RefSB_2
        double[] rhoToaAbs = new double[]{0.00694, 0.0048, 0.00526};   //  RefSB_17, RefSB_18, RefSB_19
        // todo: check with RP if this needs to be done:
        rhoToaWin[0] *= Math.cos(sza*MathUtils.DTOR);
        for (int i = 0; i < rhoToaAbs.length; i++) {
            rhoToaAbs[i] *= Math.cos(sza*MathUtils.DTOR);
        }

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 293.9698;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 7.5;
        double priorTcwv = 18.0153;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MODIS TERRA OCEAN result.getTcwv() = " + result.getTcwv());
        assertEquals(41.79, result.getTcwv(), 0.2);
        // This test result: 41.986
        // SNAP desktop result: 41.79 --> ok due to some truncations
        // Python result: tbd
        // todo: re-check later with updated LUTs etc
    }


}