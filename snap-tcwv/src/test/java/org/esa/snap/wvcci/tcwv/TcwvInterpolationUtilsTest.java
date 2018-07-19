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

    @Test
    public void testChange4DArrayLastToFirstDimension() {
        double[][][][] src4DArr = new double[5][2][4][3];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        src4DArr[i][j][k][l] = (double) index++;
                    }
                }
            }
        }

        final double[][][][] result4DArr = TcwvInterpolationUtils.change4DArrayLastToFirstDimension(src4DArr);
        assertNotNull(result4DArr);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        assertEquals(result4DArr[l][i][j][k], src4DArr[i][j][k][l], 1.E-8);
                    }
                }
            }
        }

    }
}