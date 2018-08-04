package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.LookupTable;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolationUtils;
import org.junit.Ignore;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

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
        assertEquals(4.2, value, 1.E-6);

        final long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            lookupTable.getValue(coordinates);
        }
        final long t2 = System.currentTimeMillis();
        System.out.println("time = " + (t2 - t1) * 1. + " ms");

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
        assertEquals(193.7, value, 1.E-6);

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
        assertEquals(3.688015, value, 1.E-6);

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
        final TcwvFunction tcwvFunction = TcwvInterpolation.lut2Function(allLuts, axes);

        final double[] values = tcwvFunction.f(coordinates, null);
        assertEquals(3, values.length);
        assertEquals(4.2, values[0], 1.E-6);
        assertEquals(193.7, values[1], 1.E-6);
        assertEquals(3.688015, values[2], 1.E-6);
    }

    @Test
    @Ignore
    public void testTcwvJacobiLut() throws IOException {
        // uses a dummy LUT generated from test in lut2jacobian_lut.py
        // not a real TCWV LUT!
        final URL resource = TcwvLandLut.class.getResource("jlut3_test.nc");
        if (resource == null) {
            fail();
        }
        final NetcdfFile ncFile = NetcdfFile.open(resource.getPath());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvJacobiLutMetadata(globalAttributes, dimensions, variables);
        testTcwvJacobiLutContent(variables);
    }

    private void testTcwvJacobiLutMetadata(List<Attribute> globalAttributes, List<Dimension> dimensions, List<Variable> variables) {
        assertNotNull(globalAttributes);
        assertEquals(2, globalAttributes.size());
        assertEquals("xidx", globalAttributes.get(1).getName());
        assertEquals("0,2", globalAttributes.get(1).getValue(0));

        assertNotNull(dimensions);
        assertEquals(5, dimensions.size());
        assertEquals("jlut_dimension_0", dimensions.get(0).getName());
        assertEquals(6, dimensions.get(0).getLength());
        assertEquals("jlut_dimension_1", dimensions.get(1).getName());
        assertEquals(400, dimensions.get(1).getLength());
        assertEquals("jlut_dimension_2", dimensions.get(2).getName());
        assertEquals(100, dimensions.get(2).getLength());
        assertEquals("jlut_dimension_3", dimensions.get(3).getName());
        assertEquals(8, dimensions.get(3).getLength());
        assertEquals("jaco_dimension", dimensions.get(4).getName());
        assertEquals(2, dimensions.get(4).getLength());

        assertNotNull(variables);
        assertEquals(5, variables.size());
        assertEquals("axes_0", variables.get(0).getName());
        assertEquals(6, variables.get(0).getSize());
        assertEquals(DataType.FLOAT, variables.get(0).getDataType());
        assertEquals("axes_1", variables.get(1).getName());
        assertEquals(400, variables.get(1).getSize());
        assertEquals(DataType.FLOAT, variables.get(1).getDataType());
        assertEquals("axes_2", variables.get(2).getName());
        assertEquals(100, variables.get(2).getSize());
        assertEquals(DataType.FLOAT, variables.get(2).getDataType());
        assertEquals("jlut", variables.get(3).getName());
        assertEquals(6 * 400 * 100 * 8, variables.get(3).getSize());
        assertEquals(DataType.FLOAT, variables.get(3).getDataType());
        assertEquals("ny_nx", variables.get(4).getName());
        assertEquals(2, variables.get(4).getSize());
        assertEquals(DataType.INT, variables.get(4).getDataType());
    }

    private void testTcwvJacobiLutContent(List<Variable> variables) {
        final Variable axes0Variable = variables.get(0);
        final Variable axes1Variable = variables.get(1);
        final Variable axes2Variable = variables.get(2);
        final Variable jlutVariable = variables.get(3);
        final Variable nynxVariable = variables.get(4);

        try {
            final double[] axes0Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(axes0Variable);
            assertNotNull(axes0Array);
            assertEquals(6, axes0Array.length);
            assertEquals(3.0, axes0Array[0], 1.E-6);
            assertEquals(6.0, axes0Array[2], 1.E-6);
            assertEquals(15.0, axes0Array[5], 1.E-6);

            final double[] axes1Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(axes1Variable);
            assertNotNull(axes1Array);
            assertEquals(400, axes1Array.length);
            assertEquals(-10.0, axes1Array[0], 1.E-6);
            assertEquals(-9.799498, axes1Array[2], 1.E-6);
            assertEquals(30.0, axes1Array[399], 1.E-6);

            final double[] axes2Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(axes2Variable);
            assertNotNull(axes2Array);
            assertEquals(100, axes2Array.length);
            assertEquals(-1.0, axes2Array[0], 1.E-6);
            assertEquals(-0.919192, axes2Array[2], 1.E-6);
            assertEquals(3.0, axes2Array[99], 1.E-6);

            final int[] nynxArray = TcwvInterpolationUtils.getInt1DArrayFromNetcdfVariable(nynxVariable);
            assertNotNull(nynxArray);
            assertEquals(2, nynxArray.length);
            assertEquals(4, nynxArray[0]);
            assertEquals(2, nynxArray[1]);

            final double[][][][] jlutArray = TcwvInterpolationUtils.getDouble4DArrayFromNetcdfVariable(jlutVariable);
            // 6*400*100*8
            assertNotNull(jlutArray);
            assertEquals(6, jlutArray.length);
            assertEquals(400, jlutArray[0].length);
            assertEquals(100, jlutArray[0][0].length);
            assertEquals(8, jlutArray[0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(40000.0, jlutArray[0][0][0][0], 1.E-6);
            assertEquals(30000.0, jlutArray[3][31][88][0], 1.E-6);
            assertEquals(1.8851608576E10, jlutArray[2][47][66][3], 1.E-6);
            assertEquals(4.57763671875E-4, jlutArray[4][202][55][4], 1.E-6);
            assertEquals(91.250427, jlutArray[1][58][44][5], 1.E-6);
            assertEquals(24.749975, jlutArray[2][334][11][1], 1.E-6);
            assertEquals(-1.041643E-6, jlutArray[5][399][99][7], 1.E-6);   // last value

            // 8*6*400*100: we will store 8 (6*400*100) LUTs, each one holding one Jacobi element
            final double[][][][] jlutArraySwapped = TcwvInterpolationUtils.change4DArrayLastToFirstDimension(jlutArray);
            double[][] jlutArray2D = new double[jlutArraySwapped.length][];
            for (int i = 0; i < jlutArraySwapped.length; i++) {
                jlutArray2D[i] = TcwvInterpolationUtils.convert3Dto1DArray(jlutArraySwapped[i]);
            }

            final double[][] axesArray = {axes0Array, axes1Array, axes2Array};
            final JacobiFunction jacobiFunction = TcwvInterpolation.jacobiLut2Function(jlutArray2D,
                                                                                       axesArray,
                                                                                       nynxArray[1],
                                                                                       nynxArray[0]);
            final double[] testVector = new double[]{3., 10., 1.};
            final double[][] jacobiMatrixArr = jacobiFunction.f(testVector, null);
            assertNotNull(jacobiMatrixArr);
            assertEquals(2, jacobiMatrixArr.length);
            assertEquals(2, jacobiMatrixArr[0].length);
            assertEquals(40000.0, jacobiMatrixArr[0][0], 1.E-6);
            assertEquals(0.00143433, jacobiMatrixArr[1][0], 1.E-6);
            assertEquals(24.749975, jacobiMatrixArr[0][1], 1.E-6);
            assertEquals(85.744979, jacobiMatrixArr[1][1], 1.E-6);
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }


}
