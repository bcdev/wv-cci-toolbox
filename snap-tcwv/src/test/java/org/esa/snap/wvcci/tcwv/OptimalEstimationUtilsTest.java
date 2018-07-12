package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class OptimalEstimationUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

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
}