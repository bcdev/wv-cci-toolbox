package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TcwvInterpolationUtilsTest {


    @Test
    public void testIsMonotonicallyIncreasing() {
        double[] a = new double[]{0, 1, 2, 3, 4, 5};
        assertTrue(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 0, 1, 2, 3, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 1, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 0, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{4, 4, 3, 2, 1, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{4, 3, 2, 1, 0, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{5, 4, 3, 2, 1, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
        assertTrue(TcwvInterpolationUtils.isMontonicallyDecreasing(a));
    }
}