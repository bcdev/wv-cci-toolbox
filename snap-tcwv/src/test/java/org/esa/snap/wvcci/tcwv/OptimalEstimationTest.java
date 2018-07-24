package org.esa.snap.wvcci.tcwv;

import org.junit.Test;

import static org.junit.Assert.*;


public class OptimalEstimationTest {

    // Java version of tests from Python breadboard 'optimal_estimation_py3_bc.py'
    // (BC sandbox of RP code 'optimal_estimation.py', 'optimal_estimation_py3.py').

    @Test
    public void testNewton_linear_r2r3() {
        // test of Newton method, linear R^2-->R^3 FORWARD test function: 'state' --> 'measurement'
        double[] x = {3.5, 6.5};                          // 'x' is the 'state'
        double[] y = testFunctionLinR2R3.f(x, null);      // 'y' is the 'measurement', which is 'mes' in Cawa BB!
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(60.0, y[0], 1.E-6);
        assertEquals(4.5, y[1], 1.E-6);
        assertEquals(-29.0, y[2], 1.E-6);

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] xa = {3.7, 5.6};

        // test of optimal estimation
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON, y, se, sa, xa, OEOutputMode.BASIC, maxiter);
        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
        assertEquals(2, result.getIi());
        assertEquals(2, result.getXn().length);
        // result from inversion 'measurement --> 'state': same as state vector x = {3.5, 6.5} above
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

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
        assertEquals(21, result.getIi());
        assertEquals(2, result.getXn().length);
        // from disturbance we get an inversion result which deviates from initial 'state' vector
        // todo: ask RP how to interpret this
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
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON, y, se, sa, xa, OEOutputMode.BASIC, maxiter);

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

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
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
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON_SE, y, se, sa, xa, OEOutputMode.BASIC, maxiter);
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

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON_SE, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
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
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON_SE, y, se, sa, xa, OEOutputMode.BASIC, maxiter);

        assertNotNull(result);
        assertNotNull(result.getXn());
        assertNotNull(result.getKk());
        assertNull(result.getSr());
        assertNull(result.getDiagnoseResult());
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

        maxiter = 20;
        result = oe.invert(InversionMethod.NEWTON_SE, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
        assertNotNull(result);
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
        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, maxiter);
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

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
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
        OptimalEstimation oe = new OptimalEstimation(testFunctionNonlinR2R3, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, maxiter);

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

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
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

        OptimalEstimation oe = new OptimalEstimation(testFunctionLinR3R2, a, b, y, null, null);
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
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.BASIC, maxiter);

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

        maxiter = 20;
        result = oe.invert(InversionMethod.OE, y, se, sa, xa, OEOutputMode.EXTENDED, maxiter);
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

    //// test forward functions: ////

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