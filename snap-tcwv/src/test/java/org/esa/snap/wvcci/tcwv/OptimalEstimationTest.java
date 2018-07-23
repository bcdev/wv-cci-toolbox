package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;


public class OptimalEstimationTest {

    @Test
    public void testNewton_linear_r2r3() {
        // test of Newton method, linear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        double[] y = testFunctionLinR2R3.f(x, null);
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(60.0, y[0], 1.E-6);
        assertEquals(4.5, y[1], 1.E-6);
        assertEquals(-29.0, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {1., 0., 0.},
                {0., 1., 0.},
                {0., 0., 10.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON, se, sa, xa, OEOutputMode.BASIC, maxiter);
        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5, result.getXn()[0], 1.E-4);
        assertEquals(6.5, result.getXn()[1], 1.E-4);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(6.0, result.getKk()[0][0], 1.E-3);
        assertEquals(4.0, result.getKk()[0][1], 1.E-3);
        assertEquals(-3.0, result.getKk()[1][0], 1.E-3);
        assertEquals(2.0, result.getKk()[1][1], 1.E-3);
        assertEquals(1.0, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.0, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(21, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5579, result.getXn()[0], 1.E-4);
        assertEquals(6.3722, result.getXn()[1], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.303, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testNewton_nonlinear_r2r3() {
        // test of Newton method, nonlinear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionNonlinR2R3.f(x, null);
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(422.29375, y[0], 1.E-6);
        assertEquals(8.001821, y[1], 1.E-6);
        assertEquals(-33.769696, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {100., 0., 0.},
                {0., 100., 0.},
                {0., 0., 100.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON, se, sa, xa, OEOutputMode.BASIC, maxiter);

        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(5, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5, result.getXn()[0], 1.E-4);
        assertEquals(6.5, result.getXn()[1], 1.E-4);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(213.025, result.getKk()[0][0], 1.E-3);
        assertEquals(115.475, result.getKk()[0][1], 1.E-3);
        assertEquals(-2.5, result.getKk()[1][0], 1.E-3);
        assertEquals(2.287, result.getKk()[1][1], 1.E-3);
        assertEquals(0.318, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.366, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(21, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.569, result.getXn()[0], 1.E-4);
        assertEquals(6.375, result.getXn()[1], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.3049, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testNewtonAndSE_linear_r2r3() {
        // test of Newton method + standard error, linear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionLinR2R3.f(x, null);

        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(60.0, y[0], 1.E-6);
        assertEquals(4.5, y[1], 1.E-6);
        assertEquals(-29.0, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {1., 0., 0.},
                {0., 1., 0.},
                {0., 0., 10.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON_SE, se, sa, xa, OEOutputMode.BASIC, maxiter);
        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5, result.getXn()[0], 1.E-4);
        assertEquals(6.5, result.getXn()[1], 1.E-4);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(6.0, result.getKk()[0][0], 1.E-3);
        assertEquals(4.0, result.getKk()[0][1], 1.E-3);
        assertEquals(-3.0, result.getKk()[1][0], 1.E-3);
        assertEquals(2.0, result.getKk()[1][1], 1.E-3);
        assertEquals(1.0, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.0, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON_SE, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5155, result.getXn()[0], 1.E-4);
        assertEquals(6.4657, result.getXn()[1], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.0813, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testNewtonAndSE_nonlinear_r2r3() {
        // test of Newton method + standard error, nonlinear R^2-->R^3 test function
        // test of Newton method, nonlinear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionNonlinR2R3.f(x, null);
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(422.29375, y[0], 1.E-6);
        assertEquals(8.001821, y[1], 1.E-6);
        assertEquals(-33.769696, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {100., 0., 0.},
                {0., 100., 0.},
                {0., 0., 100.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON_SE, se, sa, xa, OEOutputMode.BASIC, maxiter);

        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
//        assertEquals(3, result.getIi());
        assertEquals(5, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5, result.getXn()[0], 1.E-6);
        assertEquals(6.5, result.getXn()[1], 1.E-6);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(213.025, result.getKk()[0][0], 1.E-3);
        assertEquals(115.475, result.getKk()[0][1], 1.E-3);
        assertEquals(-2.5, result.getKk()[1][0], 1.E-3);
        assertEquals(2.287, result.getKk()[1][1], 1.E-3);
        assertEquals(0.318, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.366, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON_SE, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
//        assertEquals(3, result.getIi());
        assertEquals(4, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.569013, result.getXn()[0], 1.E-6);
        assertEquals(6.375022, result.getXn()[1], 1.E-6);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.003, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testOptimalEstimation_linear_r2r3() {
        // test of Optimal Estimation method, linear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        double[] y = testFunctionLinR2R3.f(x, null);
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(60.0, y[0], 1.E-6);
        assertEquals(4.5, y[1], 1.E-6);
        assertEquals(-29.0, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {1., 0., 0.},
                {0., 1., 0.},
                {0., 0., 10.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.BASIC, maxiter);
        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5263, result.getXn()[0], 1.E-4);
        assertEquals(6.4421, result.getXn()[1], 1.E-4);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(6.0, result.getKk()[0][0], 1.E-3);
        assertEquals(4.0, result.getKk()[0][1], 1.E-3);
        assertEquals(-3.0, result.getKk()[1][0], 1.E-3);
        assertEquals(2.0, result.getKk()[1][1], 1.E-3);
        assertEquals(1.0, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.0, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.5406, result.getXn()[0], 1.E-4);
        assertEquals(6.4102, result.getXn()[1], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.8121, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testOptimalEstimation_nonlinear_r2r3() {
        // test of Optimal Estimation method, nonlinear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionNonlinR2R3.f(x, null);
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(422.29375, y[0], 1.E-6);
        assertEquals(8.001821, y[1], 1.E-6);
        assertEquals(-33.769696, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, x, null, null);
        double[][] se = new double[][] {
                {100., 0., 0.},
                {0., 100., 0.},
                {0., 0., 100.}
        };
        double[][] sa = new double[][] {
                {1., 0.},
                {0., 1.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.BASIC, maxiter);

        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(4, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.8289, result.getXn()[0], 1.E-4);
        assertEquals(5.9429, result.getXn()[1], 1.E-4);
        assertEquals(3, result.getKk().length);
        assertEquals(2, result.getKk()[0].length);
        assertEquals(195.322, result.getKk()[0][0], 1.E-3);
        assertEquals(125.977, result.getKk()[0][1], 1.E-3);
        assertEquals(-2.544, result.getKk()[1][0], 1.E-3);
        assertEquals(2.329, result.getKk()[1][1], 1.E-3);
        assertEquals(0.377, result.getKk()[2][0], 1.E-3);
        assertEquals(-5.401, result.getKk()[2][1], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(4, result.getIi());
        assertEquals(2, result.getXn().length);
        assertEquals(3.8477, result.getXn()[0], 1.E-4);
        assertEquals(5.9139, result.getXn()[1], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.2221, result.getDiagnoseResult().getCost(), 1.E-4);
    }

    @Test
    public void testOptimalEstimation_linear_r3r2() {
        // test of Optimal Estimation method, linear R^3-->R^2 test function
        double[] x = {3.5, 6.5, 5.8};
        final double[] y = testFunctionLinR3R2.f(x, null);
        assertNotNull(y);
        assertEquals(2, y.length);
        assertEquals(48.4, y[0], 1.E-6);
        assertEquals(64.6, y[1], 1.E-6);

        double[] a = {0.1, 0.1, 0.1};
        double[] b = {10., 10., 10.};
        double[] xa = {3.7, 5.6, 8.5};

        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR3R2, a, b, x, null, null);
        double[][] se = new double[][] {
                {1.0, 0.},
                {0., 1.0}
        };
        double[][] sa = new double[][] {
                {100., 0., 0.},
                {0., 100., 0.},
                {0., 0., 100.}
        };

        int maxiter = 100;
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.BASIC, maxiter);

        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(2, result.getIi());
        assertEquals(3, result.getXn().length);
        assertEquals(4.7941, result.getXn()[0], 1.E-4);
        assertEquals(5.2739, result.getXn()[1], 1.E-4);
        assertEquals(7.2306, result.getXn()[2], 1.E-4);
        assertEquals(2, result.getKk().length);
        assertEquals(3, result.getKk()[0].length);
        assertEquals(6.0, result.getKk()[0][0], 1.E-3);
        assertEquals(4.0, result.getKk()[0][1], 1.E-3);
        assertEquals(-2.0, result.getKk()[0][2], 1.E-3);
        assertEquals(-3.0, result.getKk()[1][0], 1.E-3);
        assertEquals(5.0, result.getKk()[1][1], 1.E-3);
        assertEquals(7.0, result.getKk()[1][2], 1.E-3);

        // 'disturbed' test: as in breadboard code:
        y[y.length-1] += 1.0;
        oe.setYy(y);

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(2, result.getIi());
        assertEquals(3, result.getXn().length);
        assertEquals(4.7728, result.getXn()[0], 1.E-4);
        assertEquals(5.3467, result.getXn()[1], 1.E-4);
        assertEquals(7.3123, result.getXn()[2], 1.E-4);
        assertNotNull(result.getSr());
        assertNotNull(result.getDiagnoseResult());
        assertEquals(0.0262, result.getDiagnoseResult().getCost(), 1.E-4);


    }

    @Test
    public void testOptimalEstimation_cawa_ocean() {

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
            final String wbString = (String) ncFile.getGlobalAttributes().get(2).getValue(0);
            final String abString = (String) ncFile.getGlobalAttributes().get(4).getValue(0);
            final double[][] seArray = OptimalEstimationUtils.getSe(wbString, abString);
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

            OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
            oe.setYy(mes);
            OptimalEstimationResult result = oe.invert(InversionMethod.OE, se, sa, xa, OEOutputMode.BASIC, 3);
            assertNotNull(result);
            final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
            assertEquals(28.007107, resultTcwv, 1.E-6);
            final double resultAot = result.getXn()[1];
            assertEquals(0.961517, resultAot, 1.E-6);
            final double resultWsp = result.getXn()[2];
            assertEquals(6.510984, resultWsp, 1.E-6);


            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

    }


    private TcwvFunction testFunctionLinR2R3 =
            (x, params) -> new double[]{13.0 + 6.0 * x[0] + 4.0 * x[1],
                    2.0 - 3.0 * x[0] + 2.0 * x[1],
                    x[0] - 5.0 * x[1]};

    private TcwvFunction testFunctionNonlinR2R3 =
            (x, params) -> new double[]{13.0 + 6.0 * x[0] + 4.0 * x[1] + 0.7 * Math.pow(x[0] * x[1], 2),
                    2.0 - 3 * x[0] + 2 * x[1] + Math.sqrt(x[0]) * Math.log(x[1]),
                    x[0] - 5 * x[1] - Math.sqrt(x[0] * x[1])};

    private TcwvFunction testFunctionLinR3R2 =
            (x, params) -> new double[]{13.0 + 6.0 * x[0] + 4.0 * x[1] - 2.0 * x[2],
                    2.0 - 3.0 * x[0] + 5.0 * x[1] + 7.0 * x[2]};
}