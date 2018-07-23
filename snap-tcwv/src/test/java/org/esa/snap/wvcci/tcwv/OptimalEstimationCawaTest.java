package org.esa.snap.wvcci.tcwv;

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
            final double[] b = {wvc[wvc.length-1], aot[aot.length-1], wsp[wsp.length-1]};
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

//    @Test
//    public void testOptimalEstimation_meris_land() {
//
//        // 1. read LUT file
//        final NetcdfFile ncFile;
//        try {
//            ncFile = TcwvIO.getTcwvLookupTableNcFile("land_core_meris.nc4");
//            final TcwvLandLut tcwvLandLut = TcwvIO.getTcwvLandLut(ncFile);
//            // Python: self._forward
//            final TcwvFunction tcwvFunction = TcwvInterpolation.getForwardFunctionOcean(tcwvLandLut);
//            // Python: self._jacobi
//            final JacobiFunction jacobiFunction = TcwvInterpolation.getJForwardFunctionOcean(tcwvLandLut);
//
////            #min_state
////            a = np.array([self.axes[i].min() for i in range(3)])
//            final double[] wvc = tcwvLandLut.getWvc();
//            final double[] aot = tcwvLandLut.getAot();
//            final double[] wsp = tcwvLandLut.getWsp();
//            final double[] a = {wvc[0], aot[0], wsp[0]}; // constant for all retrievals!
////            #max_state
////            b = np.array([self.axes[i].max() for i in range(3)])
//            final double[] b = {wvc[wvc.length-1], aot[aot.length-1], wsp[wsp.length-1]};
////            self.inverter = oe.my_inverter(self.forward, a, b, jaco = self.jforward)
//            System.out.println();
//
//            // single ocean pixel:
//            final double[] mes = {0.19034228, 0.18969933, 0.21104884};
//            final double[] par = {135.61277771, 28.43509483, 61.43579102};
//            final double[][] se = {
//                    {0.0001, 0.0, 0.0},
//                    {0.0, 0.0001, 0.0},
//                    {0.0, 0.0, 0.001}
//            };
//            final double[][] sa = {
//                    {8.0, 0.0, 0.0},
//                    {0.0, 0.1, 0.0},
//                    {0.0, 0.0, 25.0}
//            };
//            final double[] xa = {5.47722558, 0.15, 7.5};
//
//            final double[] y = tcwvFunction.f(mes, par);
//
//            OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
//            OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, 3);
//            assertNotNull(result);
//            final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
//            assertEquals(28.007107, resultTcwv, 1.E-6);
//            final double resultAot = result.getXn()[1];
//            assertEquals(0.961517, resultAot, 1.E-6);
//            final double resultWsp = result.getXn()[2];
//            assertEquals(6.510984, resultWsp, 1.E-6);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail();
//        }
//    }

}