package org.esa.snap.wvcci.tcwv;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class OptimalEstimationTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testNewton_linear_r2r3() {
        // test of Newton method, linear R^2-->R^3 test function
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
        final OptimalEstimationResult result = oe.invert(InversionMethod.NEWTON, se, sa, xa, OEOutputMode.BASIC);
        assertNotNull(result);

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

        // todo: test inversion
    }

    @Test
    public void testNewtonAndSE_linear_r2r3() {
        // test of Newton method + standard error, linear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionLinR2R3.f(x, null);

        // todo: test inversion
    }

    @Test
    public void testNewtonAndSE_nonlinear_r2r3() {
        // test of Newton method + standard error, nonlinear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionNonlinR2R3.f(x, null);

        // todo: test inversion
    }

    @Test
    public void testOptimalEstimation_linear_r2r3() {
        // test of Optimal Estimation method, linear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionLinR2R3.f(x, null);

        // todo: test inversion
    }

    @Test
    public void testOptimalEstimation_nonlinear_r2r3() {
        // test of Optimal Estimation method, nonlinear R^2-->R^3 test function
        double[] x = {3.5, 6.5};
        final double[] y = testFunctionNonlinR2R3.f(x, null);

        // todo: test inversion
    }

    @Test
    public void testOptimalEstimation_linear_r3r2() {
        // test of Optimal Estimation method, linear R^3-->R^2 test function
        double[] x = {3.7, 5.6, 8.5};
        final double[] y = testFunctionLinR3R2.f(x, null);
        assertNotNull(y);
        assertEquals(2, y.length);
        assertEquals(40.6, y[0], 1.E-6);
        assertEquals(78.4, y[1], 1.E-6);

        // todo: test inversion
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