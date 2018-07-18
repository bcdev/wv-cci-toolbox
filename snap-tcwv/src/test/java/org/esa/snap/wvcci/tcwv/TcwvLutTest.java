package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.LookupTable;
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
        TcwvInterpolation tcwvInterpolation = new TcwvInterpolation();
        final TcwvFunction tcwvFunction = tcwvInterpolation.lut2Function(allLuts, axes);

        final double[] values = tcwvFunction.f(coordinates, null);
        assertEquals(3, values.length);
        assertEquals(4.2, values[0], 1.E-6);
        assertEquals(193.7, values[1], 1.E-6);
        assertEquals(3.688015, values[2], 1.E-6);
    }

    @Test
    public void testTcwvJacobiLut() throws IOException {
        // uses a dummy LUT generated from test in lut2jacobian_lut.py
        // not a real TCWV LUT!
        final URL resource = TcwvLut.class.getResource("jlut3_test.nc");
        if (resource == null) {
            System.out.println("WARNING: NetCDF file 'jlut3_test.nc' does not exist in test resources." +
                                       " Test will be ignored.");
            System.out.println("This large file shall not be committed to GitHub repository!");
            System.out.println("Get it from CAWA and copy manually to " +
                                       "../wv-cci-toolbox/snap-tcwv/src/test/resources/org/esa/snap/wvcci/tcwv," +
                                       " but make sure not to add it to GitHub!");
            return;
        }
        final NetcdfFile ncFile = NetcdfFile.open(resource.getPath());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvJacobiLutMetadata(globalAttributes, dimensions, variables);
        testTcwvJacobiLutContentContent(variables);
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

    private void testTcwvJacobiLutContentContent(List<Variable> variables) {
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

            final double[][][][] jlutArray = TcwvInterpolationUtils.getDouble4DArrayFromNetcdfVariable(jlutVariable);
            // 6**400*100*100*8
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

            double[][] jlutArray2D = new double[jlutArray.length][];
            for (int i = 0; i < jlutArray.length; i++) {
                jlutArray2D[i] = TcwvInterpolationUtils.convert3Dto1DArray(jlutArray[i]);
            }

            final double[][] axesArray = {axes0Array, axes1Array, axes2Array};
            final TcwvInterpolation tcwvInterpolation = new TcwvInterpolation();
            final TcwvFunction jacobiFunction = tcwvInterpolation.jacobiLut2Function(jlutArray2D, axesArray, 1, 1);
            double[] testVector = new double[]{3., 10., 1.};
            final double[] testValue = jacobiFunction.f(testVector, null);
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetTcwvLutFromNc4() throws IOException {

        final URL resource = TcwvLut.class.getResource("ocean_core_meris.nc4");
        if (resource == null) {
            System.out.println("WARNING: NetCDF file 'ocean_core_meris.nc4' does not exist in test resources." +
                                       " Test will be ignored.");
            System.out.println("This large file shall not be committed to GitHub repository!");
            System.out.println("Get it from CAWA and copy manually to " +
                                       "../wv-cci-toolbox/snap-tcwv/src/test/resources/org/esa/snap/wvcci/tcwv," +
                                       " but make sure not to add it to GitHub!");
            return;
        }
        final NetcdfFile ncFile = NetcdfFile.open(resource.getPath());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvLutMetadata(globalAttributes, dimensions, variables);
        testTcwvLutContentContent(variables);
    }

    private void testTcwvLutMetadata(List<Attribute> globalAttributes, List<Dimension> dimensions, List<Variable> variables) {
        assertNotNull(globalAttributes);
        assertEquals(15, globalAttributes.size());
        assertEquals("instrument", globalAttributes.get(1).getName());
        assertEquals("meris", globalAttributes.get(1).getValue(0));
        assertEquals("win_bnd", globalAttributes.get(2).getName());
        assertEquals("13,14", globalAttributes.get(2).getValue(0));
        assertEquals("abs_bnd", globalAttributes.get(4).getName());
        assertEquals("15", globalAttributes.get(4).getValue(0));
        assertEquals("axes", globalAttributes.get(5).getName());
        assertEquals("wvc,aot,wsp,azi,vie,suz", globalAttributes.get(5).getValue(0));
        assertEquals("cha/15/nominal", globalAttributes.get(12).getName());
        assertEquals(900.0, globalAttributes.get(12).getValue(0));
        assertEquals("cha/15/bwvl", globalAttributes.get(13).getName());
        assertEquals(5.901237351371007, globalAttributes.get(13).getValue(0));
        assertEquals("cha/15/cwvl", globalAttributes.get(14).getName());
        assertEquals(899.721037286666, globalAttributes.get(14).getValue(0));

        assertNotNull(dimensions);
        assertEquals(10, dimensions.size());
        assertEquals("bands", dimensions.get(0).getName());
        assertEquals(3, dimensions.get(0).getLength());
        assertEquals("ny_nx", dimensions.get(1).getName());
        assertEquals(18, dimensions.get(1).getLength());
        assertEquals("wvc", dimensions.get(2).getName());
        assertEquals(6, dimensions.get(2).getLength());
        assertEquals("aot", dimensions.get(3).getName());
        assertEquals(6, dimensions.get(3).getLength());
        assertEquals("wsp", dimensions.get(4).getName());
        assertEquals(11, dimensions.get(4).getLength());
        assertEquals("azi", dimensions.get(5).getName());
        assertEquals(11, dimensions.get(5).getLength());
        assertEquals("vie", dimensions.get(6).getName());
        assertEquals(9, dimensions.get(6).getLength());
        assertEquals("suz", dimensions.get(7).getName());
        assertEquals(9, dimensions.get(7).getLength());
        assertEquals("jaco", dimensions.get(8).getName());
        assertEquals(2, dimensions.get(8).getLength());
        assertEquals("cor/two", dimensions.get(9).getName());
        assertEquals(2, dimensions.get(9).getLength());

        assertNotNull(variables);
        assertEquals(12, variables.size());
        assertEquals("wvc", variables.get(0).getName());
        assertEquals(6, variables.get(0).getSize());
        assertEquals(DataType.DOUBLE, variables.get(0).getDataType());
        assertEquals("aot", variables.get(1).getName());
        assertEquals(6, variables.get(1).getSize());
        assertEquals(DataType.DOUBLE, variables.get(1).getDataType());
        assertEquals("wsp", variables.get(2).getName());
        assertEquals(11, variables.get(2).getSize());
        assertEquals(DataType.DOUBLE, variables.get(2).getDataType());
        assertEquals("azi", variables.get(3).getName());
        assertEquals(11, variables.get(3).getSize());
        assertEquals(DataType.DOUBLE, variables.get(3).getDataType());
        assertEquals("vie", variables.get(4).getName());
        assertEquals(9, variables.get(4).getSize());
        assertEquals(DataType.DOUBLE, variables.get(4).getDataType());
        assertEquals("suz", variables.get(5).getName());
        assertEquals(9, variables.get(5).getSize());
        assertEquals(DataType.DOUBLE, variables.get(5).getDataType());
        assertEquals("jaco", variables.get(6).getName());
        assertEquals(2, variables.get(6).getSize());
        assertEquals(DataType.LONG, variables.get(6).getDataType());
        assertEquals("lut", variables.get(7).getName());
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 3, variables.get(7).getSize());
        assertEquals(DataType.DOUBLE, variables.get(7).getDataType());
        assertEquals("jlut", variables.get(8).getName());
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 18, variables.get(8).getSize());
        assertEquals(DataType.DOUBLE, variables.get(8).getDataType());
        assertEquals("cor/13", variables.get(9).getName());
        assertEquals(2, variables.get(9).getSize());
        assertEquals(DataType.DOUBLE, variables.get(9).getDataType());
        assertEquals("cor/15", variables.get(10).getName());
        assertEquals(2, variables.get(10).getSize());
        assertEquals(DataType.DOUBLE, variables.get(10).getDataType());
        assertEquals("cor/14", variables.get(11).getName());
        assertEquals(2, variables.get(11).getSize());
        assertEquals(DataType.DOUBLE, variables.get(11).getDataType());
    }

    private void testTcwvLutContentContent(List<Variable> variables) {
        final Variable wvcVariable = variables.get(0);
        final Variable aziVariable = variables.get(3);
        final Variable suzVariable = variables.get(5);
        final Variable jacoVariable = variables.get(6);
        final Variable lutVariable = variables.get(7);
        final Variable jlutVariable = variables.get(8);
        try {
            final double[] wvcArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(wvcVariable);
            assertNotNull(wvcArray);
            assertEquals(6, wvcArray.length);
            assertEquals(1.0, wvcArray[0], 1.E-6);
            assertEquals(5.347897, wvcArray[2], 1.E-6);
            assertEquals(8.3666, wvcArray[5], 1.E-6);

            final double[] aziArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(aziVariable);
            assertNotNull(aziArray);
            assertEquals(11, aziArray.length);
            assertEquals(0.0, aziArray[0], 1.E-6);
            assertEquals(36.0, aziArray[2], 1.E-6);
            assertEquals(180.0, aziArray[10], 1.E-6);

            final double[] suzArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(suzVariable);
            assertNotNull(suzArray);
            assertEquals(9, suzArray.length);
            assertEquals(0.0, suzArray[0], 1.E-6);
            assertEquals(18.889799, suzArray[2], 1.E-6);
            assertEquals(73.359497, suzArray[8], 1.E-6);

            final int[] jacoArray = TcwvInterpolationUtils.getShort1DArrayFromNetcdfVariable(jacoVariable);
            assertNotNull(jacoArray);
            assertEquals(2, jacoArray.length);
            assertEquals(3, jacoArray[0]);
            assertEquals(6, jacoArray[1]);

            final double[][][][][][][] lutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(lutVariable);
            // 6*6*11*11*9*9*3
            assertNotNull(lutArray);
            assertEquals(6, lutArray.length);
            assertEquals(6, lutArray[0].length);
            assertEquals(11, lutArray[0][0].length);
            assertEquals(11, lutArray[0][0][0].length);
            assertEquals(9, lutArray[0][0][0][0].length);
            assertEquals(9, lutArray[0][0][0][0][0].length);
            assertEquals(3, lutArray[0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(0.120378, lutArray[0][0][0][0][0][0][0], 1.E-6);
            assertEquals(0.239316, lutArray[3][3][8][0][8][1][2], 1.E-6);
            assertEquals(0.010825, lutArray[2][4][6][3][2][7][1], 1.E-6);
            assertEquals(0.237486, lutArray[4][2][5][4][1][7][2], 1.E-6);
            assertEquals(0.015133, lutArray[1][5][4][5][3][6][0], 1.E-6);
            assertEquals(0.02134, lutArray[0][5][0][8][6][3][0], 1.E-6);
            assertEquals(0.187777, lutArray[2][4][1][8][8][1][2], 1.E-6);
            assertEquals(0.234266, lutArray[5][1][2][7][0][8][2], 1.E-6);
            assertEquals(0.002756, lutArray[3][0][3][6][7][2][1], 1.E-6);
            assertEquals(0.193763, lutArray[5][5][10][10][8][8][2], 1.E-6);   // last value

            final double[][][][][][][] jlutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(jlutVariable);
            // 6*6*11*11*9*9*18
            assertNotNull(jlutArray);
            assertEquals(6, jlutArray.length);
            assertEquals(6, jlutArray[0].length);
            assertEquals(11, jlutArray[0][0].length);
            assertEquals(11, jlutArray[0][0][0].length);
            assertEquals(9, jlutArray[0][0][0][0].length);
            assertEquals(9, jlutArray[0][0][0][0][0].length);
            assertEquals(18, jlutArray[0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(-1.115856E-5, jlutArray[0][0][0][0][0][0][0], 1.E-6);
            assertEquals(0.00012, jlutArray[3][3][8][0][8][1][2], 1.E-6);
            assertEquals(0.01366, jlutArray[2][4][6][3][2][7][1], 1.E-6);
            assertEquals(1.518494E-5, jlutArray[4][2][5][4][1][7][2], 1.E-6);
            assertEquals(-1.03756E-6, jlutArray[1][5][4][5][3][6][0], 1.E-6);
            assertEquals(-8.60967E-7, jlutArray[0][5][0][8][6][3][0], 1.E-6);
            assertEquals(-0.00011, jlutArray[2][4][1][8][8][1][2], 1.E-6);
            assertEquals(-1.461916E-5, jlutArray[5][1][2][7][0][8][2], 1.E-6);
            assertEquals(0.024714, jlutArray[3][0][3][6][7][2][1], 1.E-6);
            assertEquals(-2.977509E-5, jlutArray[5][5][10][10][8][8][2], 1.E-6);   // last value

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
