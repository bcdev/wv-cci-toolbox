package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class OptimalEstimationUtilsTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testClip1D() {
        final double[] x = new double[]{0, 1, 2, 3, 4, 5};
        final double[] a = new double[]{2, 3, 0, -1, 4, 1};
        final double[] b = new double[]{3, 5, 3, 2, 4, 4};
        final double[] clipped = OptimalEstimationUtils.clip1D(a, b, x);
        assertNotNull(clipped);
        assertEquals(a.length, clipped.length);
        assertEquals(2, clipped[0], 0.);
        assertEquals(3, clipped[1], 0.);
        assertEquals(2, clipped[2], 0.);
        assertEquals(2, clipped[3], 0.);
        assertEquals(4, clipped[4], 0.);
        assertEquals(4, clipped[5], 0.);
    }

    @Test
    public void testNorm() {
        double[] a = {0, 1, 2, 3};
        assertEquals(3.5, OptimalEstimationUtils.norm(a), 1.E-6);
    }

    @Test
    public void testNormErrorWeighted() {
        double[] a = {0, 1, 2, 3};
        double[][] b = new double[][]{
                {2.5, 2.5, 2.5, 2.5},
                {2.5, 2.5, 2.5, 2.5},
                {2.5, 2.5, 2.5, 2.5},
                {2.5, 2.5, 2.5, 2.5}
        };
        assertEquals(90.0, OptimalEstimationUtils.normErrorWeighted(a, b), 1.E-6);
    }

    @Test
    public void testLeftInverse() {
        final double[][] src = new double[][]{
                {6.0, 4.0}, {-3.0, 2.0}, {1.0, -5.0}
        };
        final Matrix leftInverse = OptimalEstimationUtils.leftInverse(src);
        
        assertNotNull(leftInverse);
        final double[][] leftInverseArray = leftInverse.getArray();
        assertNotNull(leftInverseArray);
        assertEquals(2, leftInverseArray.length);
        assertEquals(3, leftInverseArray[0].length);
        assertEquals(0.114675, leftInverseArray[0][0], 1.E-5);
        assertEquals(-0.084692, leftInverseArray[0][1], 1.E-5);
        assertEquals(0.057864, leftInverseArray[0][2], 1.E-5);
        assertEquals(0.05576, leftInverseArray[1][0], 1.E-5);
        assertEquals(0.068911, leftInverseArray[1][1], 1.E-5);
        assertEquals(-0.127827, leftInverseArray[1][2], 1.E-5);
    }

    @Test
    public void testGetNumericalJacobi() {

        double[] a = {0.1, 0.1};
        double[] b = {10., 10.};
        double[] x = {5.05, 5.05};
        final TcwvFunction testFunctionLinR2R3 =
                (xx, params) -> new double[]{13.0 + 6.0 * xx[0] + 4.0 * xx[1],
                        2.0 - 3.0 * xx[0] + 2.0 * xx[1],
                        xx[0] - 5.0 * xx[1]};

        double[] y = {60.0, 4.5, -29.0};
        double delta = 0.001;

        final ClippedDifferenceFunction fnc = new ClippedDifferenceFunction(a, b, testFunctionLinR2R3, y);
        double[][] jac = OptimalEstimationUtils.getNumericalJacobi(a, b, x, fnc, null, x.length, y.length, delta);

        assertNotNull(jac);
        assertEquals(3, jac.length);
        assertEquals(2, jac[0].length);
        assertEquals(6.0, jac[0][0], 1.E-6);
        assertEquals(4.0, jac[0][1], 1.E-6);
        assertEquals(-3.0, jac[1][0], 1.E-6);
        assertEquals(2.0, jac[1][1], 1.E-6);
        assertEquals(1.0, jac[2][0], 1.E-6);
        assertEquals(-5.0, jac[2][1], 1.E-6);
    }
}