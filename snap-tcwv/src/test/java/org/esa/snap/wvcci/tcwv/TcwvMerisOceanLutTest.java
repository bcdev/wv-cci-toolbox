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

public class TcwvMerisOceanLutTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdata();
    }

    @Test
    public void testInstallAuxdata() {
        assertNotNull(auxdataPath);
    }


    @Test
    public void testGetTcwvLutFromNc4_ocean() throws IOException {
        final NetcdfFile ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath, "ocean_core_meris.nc4");
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvLutMetadata_ocean(globalAttributes, dimensions, variables);
        testTcwvLutContent_ocean(variables);
    }

    private void testTcwvLutMetadata_ocean(List<Attribute> globalAttributes, List<Dimension> dimensions, List<Variable> variables) {
        assertNotNull(globalAttributes);
        assertEquals(15, globalAttributes.size());
        assertEquals("instrument", globalAttributes.get(1).getFullName());
        assertEquals("meris", globalAttributes.get(1).getValue(0));
        assertEquals("win_bnd", globalAttributes.get(2).getFullName());
        assertEquals("13,14", globalAttributes.get(2).getValue(0));
        assertEquals("abs_bnd", globalAttributes.get(4).getFullName());
        assertEquals("15", globalAttributes.get(4).getValue(0));
        assertEquals("axes", globalAttributes.get(5).getFullName());
        assertEquals("wvc,aot,wsp,azi,vie,suz", globalAttributes.get(5).getValue(0));
        assertEquals("cha_15_nominal", globalAttributes.get(12).getFullName());
        assertEquals(900.0, globalAttributes.get(12).getValue(0));
        assertEquals("cha_15_bwvl", globalAttributes.get(13).getFullName());
        assertEquals(5.901237351371007, globalAttributes.get(13).getValue(0));
        assertEquals("cha_15_cwvl", globalAttributes.get(14).getFullName());
        assertEquals(899.721037286666, globalAttributes.get(14).getValue(0));

        assertNotNull(dimensions);
        assertEquals(10, dimensions.size());
        assertEquals("bands", dimensions.get(0).getFullName());
        assertEquals(3, dimensions.get(0).getLength());
        assertEquals("ny_nx", dimensions.get(1).getFullName());
        assertEquals(18, dimensions.get(1).getLength());
        assertEquals("wvc", dimensions.get(2).getFullName());
        assertEquals(6, dimensions.get(2).getLength());
        assertEquals("aot", dimensions.get(3).getFullName());
        assertEquals(6, dimensions.get(3).getLength());
        assertEquals("wsp", dimensions.get(4).getFullName());
        assertEquals(11, dimensions.get(4).getLength());
        assertEquals("azi", dimensions.get(5).getFullName());
        assertEquals(11, dimensions.get(5).getLength());
        assertEquals("vie", dimensions.get(6).getFullName());
        assertEquals(9, dimensions.get(6).getLength());
        assertEquals("suz", dimensions.get(7).getFullName());
        assertEquals(9, dimensions.get(7).getLength());
        assertEquals("jaco", dimensions.get(8).getFullName());
        assertEquals(2, dimensions.get(8).getLength());
        assertEquals("cor_two", dimensions.get(9).getFullName());
        assertEquals(2, dimensions.get(9).getLength());

        assertNotNull(variables);
        assertEquals(12, variables.size());
        assertEquals("wvc", variables.get(0).getFullName());
        assertEquals(6, variables.get(0).getSize());
        assertEquals(DataType.DOUBLE, variables.get(0).getDataType());
        assertEquals("aot", variables.get(1).getFullName());
        assertEquals(6, variables.get(1).getSize());
        assertEquals(DataType.DOUBLE, variables.get(1).getDataType());
        assertEquals("wsp", variables.get(2).getFullName());
        assertEquals(11, variables.get(2).getSize());
        assertEquals(DataType.DOUBLE, variables.get(2).getDataType());
        assertEquals("azi", variables.get(3).getFullName());
        assertEquals(11, variables.get(3).getSize());
        assertEquals(DataType.DOUBLE, variables.get(3).getDataType());
        assertEquals("vie", variables.get(4).getFullName());
        assertEquals(9, variables.get(4).getSize());
        assertEquals(DataType.DOUBLE, variables.get(4).getDataType());
        assertEquals("suz", variables.get(5).getFullName());
        assertEquals(9, variables.get(5).getSize());
        assertEquals(DataType.DOUBLE, variables.get(5).getDataType());
        assertEquals("jaco", variables.get(6).getFullName());
        assertEquals(2, variables.get(6).getSize());
        assertEquals(DataType.LONG, variables.get(6).getDataType());
        assertEquals("lut", variables.get(7).getFullName());
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 3, variables.get(7).getSize());
        assertEquals(DataType.DOUBLE, variables.get(7).getDataType());
        assertEquals("jlut", variables.get(8).getFullName());
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 18, variables.get(8).getSize());
        assertEquals(DataType.DOUBLE, variables.get(8).getDataType());
        assertEquals("cor/13", variables.get(9).getFullName());
        assertEquals(2, variables.get(9).getSize());
        assertEquals(DataType.DOUBLE, variables.get(9).getDataType());
        assertEquals("cor/15", variables.get(10).getFullName());
        assertEquals(2, variables.get(10).getSize());
        assertEquals(DataType.DOUBLE, variables.get(10).getDataType());
        assertEquals("cor/14", variables.get(11).getFullName());
        assertEquals(2, variables.get(11).getSize());
        assertEquals(DataType.DOUBLE, variables.get(11).getDataType());
    }

    private void testTcwvLutContent_ocean(List<Variable> variables) {
        try {
            final double[] wvcArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(0));
            assertNotNull(wvcArray);
            assertEquals(6, wvcArray.length);
            assertEquals(1.0, wvcArray[0], 1.E-6);
            assertEquals(5.347897, wvcArray[2], 1.E-6);
            assertEquals(8.3666, wvcArray[5], 1.E-6);

            final double[] aotArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(1));
            assertNotNull(aotArray);
            assertEquals(6, aotArray.length);
            assertEquals(0.0, aotArray[0], 1.E-6);
            assertEquals(0.080126, aotArray[2], 1.E-6);
            assertEquals(0.961517, aotArray[5], 1.E-6);

            final double[] wspArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(2));
            assertNotNull(wspArray);
            assertEquals(11, wspArray.length);
            assertEquals(2.0, wspArray[0], 1.E-6);
            assertEquals(4.6, wspArray[2], 1.E-6);
            assertEquals(8.5, wspArray[5], 1.E-6);

            final double[] aziArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(3));
            assertNotNull(aziArray);
            assertEquals(11, aziArray.length);
            assertEquals(0.0, aziArray[0], 1.E-6);
            assertEquals(36.0, aziArray[2], 1.E-6);
            assertEquals(180.0, aziArray[10], 1.E-6);

            final double[] vieArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(4));
            assertNotNull(vieArray);
            assertEquals(9, vieArray.length);
            assertEquals(0.0, vieArray[0], 1.E-6);
            assertEquals(18.889799, vieArray[2], 1.E-6);
            assertEquals(73.359497, vieArray[8], 1.E-6);

            final double[] suzArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(5));
            assertNotNull(suzArray);
            assertEquals(9, suzArray.length);
            assertEquals(0.0, suzArray[0], 1.E-6);
            assertEquals(18.889799, suzArray[2], 1.E-6);
            assertEquals(73.359497, suzArray[8], 1.E-6);

            final int[] jacoArray = TcwvInterpolationUtils.getInt1DArrayFromNetcdfVariable(variables.get(6));
            assertNotNull(jacoArray);
            assertEquals(2, jacoArray.length);
            assertEquals(3, jacoArray[0]);
            assertEquals(6, jacoArray[1]);

            final double[][][][][][][] lutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(variables.get(7));
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

            final double[][][][][][][] jlutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(variables.get(8));
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
