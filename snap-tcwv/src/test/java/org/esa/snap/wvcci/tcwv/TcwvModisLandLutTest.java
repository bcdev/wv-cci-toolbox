package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolationUtils;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class TcwvModisLandLutTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testInstallAuxdata() {
        assertNotNull(auxdataPath);
    }

    // continue with tests when we have new MODIS land LUTs.
    // The current one from CAWA is not really usable (i.e., 1D tmp axis with wrong value 10.0)

    @Test
    public void testGetTcwvLutFromNc4_land_aqua() throws IOException {
        final NetcdfFile ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath.toString(), Sensor.MODIS_AQUA.getLandLutName());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvLutMetadata_land(globalAttributes, dimensions, variables);
        testTcwvLutContent_land(variables);
    }

    @Test
    public void testGetTcwvLutFromNc4_land_terra() throws IOException {
        final NetcdfFile ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath.toString(), Sensor.MODIS_TERRA.getLandLutName());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvLutMetadata_land(globalAttributes, dimensions, variables);
//        testTcwvLutContent_land(variables);
    }

    private void testTcwvLutMetadata_land(List<Attribute> globalAttributes, List<Dimension> dimensions, List<Variable> variables) {
        assertNotNull(globalAttributes);
        assertEquals(6, globalAttributes.size());
        assertEquals("win_bnd", globalAttributes.get(2).getName());
        assertEquals("2,5", globalAttributes.get(2).getValue(0));
        assertEquals("fig_bnd", globalAttributes.get(3).getName());
        assertEquals("17", globalAttributes.get(3).getValue(0));
        assertEquals("abs_bnd", globalAttributes.get(4).getName());
        assertEquals("17,18,19", globalAttributes.get(4).getValue(0));
        assertEquals("axes", globalAttributes.get(5).getName());
        assertEquals("wvc,al0,al1,aot,prs,tmp,azi,vie,suz", globalAttributes.get(5).getValue(0));

        assertNotNull(dimensions);
        assertEquals(12, dimensions.size());
        assertEquals("bands", dimensions.get(0).getName());
        assertEquals(5, dimensions.get(0).getLength());
        assertEquals("ny_nx", dimensions.get(1).getName());
        assertEquals(45, dimensions.get(1).getLength());
        assertEquals("wvc", dimensions.get(2).getName());
        assertEquals(6, dimensions.get(2).getLength());
        assertEquals("al0", dimensions.get(3).getName());
        assertEquals(5, dimensions.get(3).getLength());
        assertEquals("al1", dimensions.get(4).getName());
        assertEquals(5, dimensions.get(4).getLength());
        assertEquals("aot", dimensions.get(5).getName());
        assertEquals(6, dimensions.get(5).getLength());
        assertEquals("prs", dimensions.get(6).getName());
        assertEquals(4, dimensions.get(6).getLength());
        assertEquals("tmp", dimensions.get(7).getName());
        assertEquals(3, dimensions.get(7).getLength());
        assertEquals("azi", dimensions.get(8).getName());
        assertEquals(6, dimensions.get(8).getLength());
        assertEquals("vie", dimensions.get(9).getName());
        assertEquals(5, dimensions.get(9).getLength());
        assertEquals("suz", dimensions.get(10).getName());
        assertEquals(5, dimensions.get(10).getLength());
        assertEquals("jaco", dimensions.get(11).getName());
        assertEquals(2, dimensions.get(11).getLength());

        assertNotNull(variables);
        assertEquals(12, variables.size());
        assertEquals("wvc", variables.get(0).getName());
        assertEquals(6, variables.get(0).getSize());
        assertEquals(DataType.DOUBLE, variables.get(0).getDataType());
        assertEquals("al0", variables.get(1).getName());
        assertEquals(5, variables.get(1).getSize());
        assertEquals(DataType.DOUBLE, variables.get(1).getDataType());
        assertEquals("al1", variables.get(2).getName());
        assertEquals(5, variables.get(2).getSize());
        assertEquals(DataType.DOUBLE, variables.get(2).getDataType());
        assertEquals("aot", variables.get(3).getName());
        assertEquals(6, variables.get(3).getSize());
        assertEquals(DataType.DOUBLE, variables.get(3).getDataType());
        assertEquals("prs", variables.get(4).getName());
        assertEquals(4, variables.get(4).getSize());
        assertEquals(DataType.DOUBLE, variables.get(4).getDataType());
        assertEquals("tmp", variables.get(5).getName());
        assertEquals(3, variables.get(5).getSize());
        assertEquals(DataType.DOUBLE, variables.get(5).getDataType());
        assertEquals("azi", variables.get(6).getName());
        assertEquals(6, variables.get(6).getSize());
        assertEquals(DataType.DOUBLE, variables.get(6).getDataType());
        assertEquals("vie", variables.get(7).getName());
        assertEquals(5, variables.get(7).getSize());
        assertEquals(DataType.DOUBLE, variables.get(7).getDataType());
        assertEquals("suz", variables.get(8).getName());
        assertEquals(5, variables.get(8).getSize());
        assertEquals(DataType.DOUBLE, variables.get(8).getDataType());
        assertEquals("jaco", variables.get(9).getName());
        assertEquals(2, variables.get(9).getSize());
        assertEquals(DataType.LONG, variables.get(9).getDataType());
        assertEquals("lut", variables.get(10).getName());
        assertEquals(6 * 5 * 5 * 6 * 4 * 3 * 6 * 5 * 5 * 5, variables.get(10).getSize());   // wvc,al0,al1,aot,prs,tmp,azi,vie,suz
        assertEquals(DataType.DOUBLE, variables.get(10).getDataType());
        assertEquals("jlut", variables.get(11).getName());
        assertEquals(6 * 5 * 5 * 6 * 4 * 3 * 6 * 5 * 5 * 45, variables.get(11).getSize());
        assertEquals(DataType.DOUBLE, variables.get(11).getDataType());
    }

    private void testTcwvLutContent_land(List<Variable> variables) {
        try {
            final double[] wvcArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(0));
            assertNotNull(wvcArray);
            assertEquals(6, wvcArray.length);
            assertEquals(1.0, wvcArray[0], 1.E-6);
            assertEquals(5.3478968, wvcArray[2], 1.E-6);
            assertEquals(8.3666, wvcArray[5], 1.E-6);

            final double[] al0Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(1));
            assertNotNull(al0Array);
            assertEquals(5, al0Array.length);
            assertEquals(0.001, al0Array[0], 1.E-6);
            assertEquals(0.1, al0Array[2], 1.E-6);
            assertEquals(1.0, al0Array[4], 1.E-6);

            final double[] al1Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(2));
            assertNotNull(al1Array);
            assertEquals(5, al1Array.length);
            assertEquals(0.001, al1Array[0], 1.E-6);
            assertEquals(0.1, al1Array[2], 1.E-6);
            assertEquals(1.0, al1Array[4], 1.E-6);

            final double[] aotArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(3));
            assertNotNull(aotArray);
            assertEquals(6, aotArray.length);
            assertEquals(0.0, aotArray[0], 1.E-6);
            assertEquals(0.0512, aotArray[2], 1.E-4);
            assertEquals(0.409, aotArray[4], 1.E-3);

            final double[] prsArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(4));
            assertNotNull(prsArray);
            assertEquals(4, prsArray.length);
            assertEquals(1030.0, prsArray[0], 1.E-6);
            assertEquals(696.666667, prsArray[2], 1.E-6);
            assertEquals(530.0, prsArray[3], 1.E-6);

            final double[] tmpArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(5));
            assertNotNull(tmpArray);
            assertEquals(3, tmpArray.length);
            assertEquals(263.13, tmpArray[0], 1.E-6);
            assertEquals(283.13, tmpArray[1], 1.E-6);
            assertEquals(313.13, tmpArray[2], 1.E-6);

            final double[] aziArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(6));
            assertNotNull(aziArray);
            assertEquals(6, aziArray.length);
            assertEquals(0.0, aziArray[0], 1.E-6);
            assertEquals(72.0, aziArray[2], 1.E-6);
            assertEquals(180.0, aziArray[5], 1.E-6);

            final double[] vieArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(7));
            assertNotNull(vieArray);
            assertEquals(5, vieArray.length);
            assertEquals(0.0, vieArray[0], 1.E-6);
            assertEquals(18.889799, vieArray[1], 1.E-6);
            assertEquals(73.359497, vieArray[4], 1.E-6);

            final double[] suzArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(8));
            assertNotNull(suzArray);
            assertEquals(5, suzArray.length);
            assertEquals(0.0, suzArray[0], 1.E-6);
            assertEquals(18.889799, suzArray[1], 1.E-6);
            assertEquals(73.359497, suzArray[4], 1.E-6);

            final int[] jacoArray = TcwvInterpolationUtils.getInt1DArrayFromNetcdfVariable(variables.get(9));
            assertNotNull(jacoArray);
            assertEquals(2, jacoArray.length);
            assertEquals(5, jacoArray[0]);
            assertEquals(9, jacoArray[1]);

            final double[][][][][][][][][][] lutArray = TcwvInterpolationUtils.getDouble10DArrayFromNetcdfVariable(variables.get(10));
            // 6 * 5 * 5 * 6 * 4 * 3 * 6 * 5 * 5 * 5
            assertNotNull(lutArray);
            assertEquals(6, lutArray.length);
            assertEquals(5, lutArray[0].length);
            assertEquals(5, lutArray[0][0].length);
            assertEquals(6, lutArray[0][0][0].length);
            assertEquals(4, lutArray[0][0][0][0].length);
            assertEquals(3, lutArray[0][0][0][0][0].length);
            assertEquals(6, lutArray[0][0][0][0][0][0].length);
            assertEquals(5, lutArray[0][0][0][0][0][0][0].length);
            assertEquals(5, lutArray[0][0][0][0][0][0][0][0].length);
            assertEquals(5, lutArray[0][0][0][0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(0.002241, lutArray[0][0][0][0][0][0][0][0][0][0], 1.E-6);
            assertEquals(0.220514, lutArray[3][3][2][0][2][0][2][2][1][2], 1.E-6);
            assertEquals(0.087731, lutArray[2][4][3][3][2][0][1][2][1][1], 1.E-6);
            assertEquals(0.249841, lutArray[4][2][1][4][1][0][2][1][1][2], 1.E-6);
            assertEquals(0.005048, lutArray[1][1][4][1][1][0][0][3][2][0], 1.E-6);
            assertEquals(0.007501, lutArray[0][1][0][2][1][0][0][3][3][0], 1.E-6);
            assertEquals(0.203551, lutArray[2][4][1][3][1][0][2][3][1][2], 1.E-6);
            assertEquals(0.102966, lutArray[1][1][2][2][0][0][2][0][3][2], 1.E-6);
            assertEquals(0.073168, lutArray[3][0][3][1][1][0][1][4][2][1], 1.E-6);
            assertEquals(0.393265, lutArray[5][4][4][5][3][0][5][4][4][4], 1.E-6);   // last value

            final double[][][][][][][][][][] jlutArray = TcwvInterpolationUtils.getDouble10DArrayFromNetcdfVariable(variables.get(11));
            // 6 * 5 * 5 * 6 * 4 * 3 * 6 * 5 * 5 * 45
            assertNotNull(jlutArray);
            assertEquals(6, jlutArray.length);
            assertEquals(5, jlutArray[0].length);
            assertEquals(5, jlutArray[0][0].length);
            assertEquals(6, jlutArray[0][0][0].length);
            assertEquals(4, jlutArray[0][0][0][0].length);
            assertEquals(3, jlutArray[0][0][0][0][0].length);
            assertEquals(6, jlutArray[0][0][0][0][0][0].length);
            assertEquals(5, jlutArray[0][0][0][0][0][0][0].length);
            assertEquals(5, jlutArray[0][0][0][0][0][0][0][0].length);
            assertEquals(45, jlutArray[0][0][0][0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(-9.92e-06, jlutArray[0][0][0][0][0][0][0][0][0][0], 1.E-6);
            assertEquals(-0.014364, jlutArray[3][3][2][0][2][0][2][2][1][3], 1.E-6);
            assertEquals(2.3e-06, jlutArray[2][4][3][3][2][0][1][2][1][6], 1.E-6);
            assertEquals(-3.65e-05, jlutArray[4][2][1][4][1][0][2][1][1][9], 1.E-6);
            assertEquals(-0.030787, jlutArray[1][1][4][1][1][0][0][3][2][12], 1.E-6);
            assertEquals(-1.54e-05, jlutArray[0][1][0][2][1][0][0][3][3][15], 1.E-6);
            assertEquals(0.040705, jlutArray[2][4][1][3][1][0][2][3][1][18], 1.E-6);
            assertEquals(-0.024286, jlutArray[1][1][2][2][0][0][2][0][3][21], 1.E-6);
            assertEquals(0.0001235, jlutArray[3][0][3][1][1][0][1][4][2][24], 1.E-6);
            assertEquals(-0.009246, jlutArray[5][4][4][5][3][0][5][4][4][44], 1.E-6);   // last value
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
