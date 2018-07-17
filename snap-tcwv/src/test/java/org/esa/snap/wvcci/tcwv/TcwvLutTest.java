package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.LookupTable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TcwvLutTest {

    @Test
    public void testTcwvLut() {
        // test values from breadboard:
        // lut2func.py --> test

        double[] axis1 = new double[]{3.f, 4.f, 6.f, 7.f, 9.f, 15.f};
        double[] axis2 = new double[]{1.f, 5.f, 10.f, 15.f};

        // this is what we shall get from
        double[][] testLutArr = new double[][]{
                {0.f, 1.f, 2.f, 3.f},
                {4.f, 5.f, 6.f, 7.f},
                {8.f, 9.f, 10.f, 11.f},
                {12.f, 13.f, 14.f, 15.f},
                {16.f, 17.f, 18.f, 19.f},
                {20.f, 21.f, 22.f, 23.f}
        };

        double[] testLutArrAs1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr);

        LookupTable lookupTable = new LookupTable(testLutArrAs1D, axis1, axis2);
        double[] coordinates = {3.5, 11.0};
        double value = lookupTable.getValue(coordinates);
        assertEquals(4.2,value, 1.E-6);

        final long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
           lookupTable.getValue(coordinates);
        }
        final long t2 = System.currentTimeMillis();
        System.out.println("time = " + (t2-t1)*1. + " ms");

        double[][] testLutArr2 = new double[][]{
                {0.f, 36.f, 144.f, 324.f},
                {1.f, 49.f, 169.f, 361.f},
                {4.f, 64.f, 196.f, 400.f},
                {9.f, 81.f, 225.f, 441.f},
                {16.f, 100.f, 256.f, 484.f},
                {25.f, 121.f, 289.f, 529.f}
        };
        double[] testLutArr2As1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr2);
        lookupTable = new LookupTable(testLutArr2As1D, axis1, axis2);
        value = lookupTable.getValue(coordinates);
        assertEquals(193.7,value, 1.E-6);

        double[][] testLutArr3 = new double[][]{
                {0.f, 2.44949f, 3.464102f, 4.24264f},
                {1.f, 2.645751f, 3.605551f, 4.358898f},
                {1.4142135f, 2.828427f, 3.741657f, 4.472136f},
                {1.7320508f, 3.f, 3.872983f, 4.582577f},
                {2.f, 3.162776f, 4.f, 4.690416f},
                {2.236068f, 3.316624f, 4.123106f, 4.795831f}
        };

        double[] testLutArr3As1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr3);
        lookupTable = new LookupTable(testLutArr3As1D, axis1, axis2);
        value = lookupTable.getValue(coordinates);
        assertEquals(3.688015,value, 1.E-6);

    }

    @Test
    public void testTcwvLut_all3() {
        // test values from breadboard:
        // lut2func.py --> test

        double[] axis1 = new double[]{3.f, 4.f, 6.f, 7.f, 9.f, 15.f};
        double[] axis2 = new double[]{1.f, 5.f, 10.f, 15.f};

        // this is what we shall get from
        double[][] testLutArr = new double[][]{
                {0.f, 1.f, 2.f, 3.f},
                {4.f, 5.f, 6.f, 7.f},
                {8.f, 9.f, 10.f, 11.f},
                {12.f, 13.f, 14.f, 15.f},
                {16.f, 17.f, 18.f, 19.f},
                {20.f, 21.f, 22.f, 23.f}
        };

        double[] testLutArrAs1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr);

        double[][] testLutArr2 = new double[][]{
                {0.f, 36.f, 144.f, 324.f},
                {1.f, 49.f, 169.f, 361.f},
                {4.f, 64.f, 196.f, 400.f},
                {9.f, 81.f, 225.f, 441.f},
                {16.f, 100.f, 256.f, 484.f},
                {25.f, 121.f, 289.f, 529.f}
        };
        double[] testLutArr2As1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr2);

        double[][] testLutArr3 = new double[][]{
                {0.f, 2.44949f, 3.464102f, 4.24264f},
                {1.f, 2.645751f, 3.605551f, 4.358898f},
                {1.4142135f, 2.828427f, 3.741657f, 4.472136f},
                {1.7320508f, 3.f, 3.872983f, 4.582577f},
                {2.f, 3.162776f, 4.f, 4.690416f},
                {2.236068f, 3.316624f, 4.123106f, 4.795831f}
        };

        double[] testLutArr3As1D = TcwvInterpolationUtils.convert2Dto1DArray(testLutArr3);

        double[][] allLuts = new double[3][];
        allLuts[0] = testLutArrAs1D;
        allLuts[1] = testLutArr2As1D;
        allLuts[2] = testLutArr3As1D;

        final double[][] axes = new double[2][];
        axes[0] = axis1;
        axes[1] = axis2;

        double[] coordinates = {3.5, 11.0};
        TcwvInterpolation tcwvInterpolation = new TcwvInterpolation();
        final TcwvFunction tcwvFunction = tcwvInterpolation.lut2Function(allLuts, axes);

        final double[] values = tcwvFunction.f(coordinates, null);
        assertEquals(3,values.length);
        assertEquals(4.2,values[0], 1.E-6);
        assertEquals(193.7,values[1], 1.E-6);
        assertEquals(3.688015,values[2], 1.E-6);
    }
}
