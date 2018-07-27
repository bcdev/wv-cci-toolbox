package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.esa.snap.wvcci.tcwv.oe.InversionMethod;
import org.esa.snap.wvcci.tcwv.oe.OEOutputMode;
import org.esa.snap.wvcci.tcwv.oe.OptimalEstimation;
import org.esa.snap.wvcci.tcwv.oe.OptimalEstimationResult;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

import static org.junit.Assert.*;


public class OptimalEstimationCawaTest {

    // Java version of tests from Python breadboard 'test_cawa.py', which in return is a standalone version
    // of GPF operator code 'cawa_tcwv_meris_op.py', 'cawa_tcwv_modis_op.py' for a single test pixel,
    // using corresponding LUTs from CAWA.
    // Towards a Water_Vapour_cci Java operator, we have four cases to test:
    //     - MERIS ocean
    //     - MERIS land
    //     - MODIS ocean
    //     - MODIS land
    //

    @Test
    public void testOptimalEstimation_meris_ocean() {

        // 1. read LUT file
        final NetcdfFile ncFile;
        try {
            ncFile = TcwvIO.getTcwvLookupTableNcFile("ocean_core_meris.nc4");
            final TcwvOceanLut tcwvOceanLut = TcwvIO.getTcwvOceanLut(ncFile);
            // Python: self._forward
            final TcwvFunction tcwvFunction = TcwvInterpolation.getForwardFunctionOcean(tcwvOceanLut);
            // Python: self._jacobi
            final JacobiFunction jacobiFunction = TcwvInterpolation.getJForwardFunctionOcean(tcwvOceanLut);

//            #min_state
//            a = np.array([self.axes[i].min() for i in range(3)])
            final double[] wvc = tcwvOceanLut.getWvc();
            final double[] aot = tcwvOceanLut.getAot();
            final double[] wsp = tcwvOceanLut.getWsp();
            final double[] a = {wvc[0], aot[0], wsp[0]}; // constant for all retrievals!
//            #max_state
//            b = np.array([self.axes[i].max() for i in range(3)])
            final double[] b = {wvc[wvc.length - 1], aot[aot.length - 1], wsp[wsp.length - 1]};
//            self.inverter = oe.my_inverter(self.forward, a, b, jaco = self.jforward)
            System.out.println();

            // single ocean pixel:
            final double[] mes = {0.19034228, 0.18969933, 0.21104884};
            final double[] par = {135.61277771, 28.43509483, 61.43579102};
            final double[][] se = {
                    {0.0001, 0.0, 0.0},
                    {0.0, 0.0001, 0.0},
                    {0.0, 0.0, 0.001}
            };
            final double[][] sa = {
                    {8.0, 0.0, 0.0},
                    {0.0, 0.1, 0.0},
                    {0.0, 0.0, 25.0}
            };
            final double[] xa = {5.47722558, 0.15, 7.5};

            final double[] y = tcwvFunction.f(mes, par);

            OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
            OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, 3);
            assertNotNull(result);
            final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
            assertEquals(28.007107, resultTcwv, 1.E-6);
            final double resultAot = result.getXn()[1];
            assertEquals(0.961517, resultAot, 1.E-6);
            final double resultWsp = result.getXn()[2];
            assertEquals(6.510984, resultWsp, 1.E-6);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testOptimalEstimation_meris_land() {

        // 1. read LUT file
        final NetcdfFile ncFile;
        try {
            ncFile = TcwvIO.getTcwvLookupTableNcFile("land_core_meris.nc4");
            final TcwvLandLut tcwvLandLut = TcwvIO.getTcwvLandLut(ncFile);
            final TcwvFunction tcwvFunction = TcwvInterpolation.getForwardFunctionLand(tcwvLandLut);
            final JacobiFunction jacobiFunction = TcwvInterpolation.getJForwardFunctionLand(tcwvLandLut);

            final double[] wvc = tcwvLandLut.getWvc();
            final double[] al0 = tcwvLandLut.getAl0();
            final double[] al1 = tcwvLandLut.getAl1();
            final double[] a = {wvc[0], al0[0], al1[0]}; // constant for all retrievals!
            final double[] b = {wvc[wvc.length - 1], al0[al0.length - 1], al1[al1.length - 1]};

            // single land pixel:
            final double[] mes = {0.19290966, 0.19140355, 0.14358414};
            final double[] par = {1.00000000e-01, -1.01325000e+01, 3.03000000e+02, 4.48835754e+01,
                    2.70720062e+01, 5.29114494e+01};
            final double[][] se = {
                    {0.0001, 0.0, 0.0},
                    {0.0, 0.0001, 0.0},
                    {0.0, 0.0, 0.001}
            };
            final double[][] sa = {
                    {20.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0},
                    {0.0, 0.0, 1.0}
            };
            final double[] xa = {5.47722558, 0.13, 0.13};

            final double[] y = tcwvFunction.f(mes, par);

            OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
            OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, 3);
            assertNotNull(result);
            final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
            assertEquals(7.16992, resultTcwv, 1.E-6);
            final double resultAl0 = result.getXn()[1];
            assertEquals(1.0, resultAl0, 1.E-6);
            final double resultAl1 = result.getXn()[2];
            assertEquals(1.0, resultAl1, 1.E-6);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testOptimalEstimation_modis_ocean() {
        // todo
    }

    @Test
    public void testOptimalEstimation_modis_land() {
        // todo
    }

    @Test
    public void testComputeTcwv_meris_land() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        final TcwvLandLut landLut = TcwvIO.readLandLookupTable(sensor);
        final TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(sensor);
        double[] rhoToaWin = new double[]{0.192909659591, 0.191403549212};
        double[] rhoToaAbs = new double[]{0.15064498747946906};
        double sza = 52.9114494;
        double vza = 27.0720062;
        double relAzi = 44.8835754;
        double amf = 2.78128741934;
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
        final TcwvResult result = algorithm.compute(sensor, landLut, oceanLut, input, true);

        assertEquals(7.16992, result.getTcwv(), 1.E-6);
    }

    @Test
    public void testComputeTcwv_meris_ocean() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        final TcwvLandLut landLut = TcwvIO.readLandLookupTable(sensor);
        final TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(sensor);
        double[] rhoToaWin = new double[]{0.190342278759, 0.189699328631};
        double[] rhoToaAbs = new double[]{0.129829271947};
        double sza = 61.435791;
        double vza = 28.435095;
        double relAzi = 135.61277770996094;
        double amf = 3.22861762089;
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
        final TcwvResult result = algorithm.compute(sensor, landLut, oceanLut, input, false);

        assertEquals(28.007107, result.getTcwv(), 1.E-6);
    }
}