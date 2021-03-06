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

public class TcwvModisOceanLutTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testInstallAuxdata() {
        assertNotNull(auxdataPath);
    }


    @Test
    public void testGetTcwvLutFromNc4_ocean_aqua() throws IOException {
//        final NetcdfFile ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath, "ocean_core_modis_aqua.nc4");
        final NetcdfFile ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath, Sensor.MODIS_AQUA.getOceanLutName());
        assertNotNull(ncFile);

        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        final List<Dimension> dimensions = ncFile.getDimensions();
        final List<Variable> variables = ncFile.getVariables();

        testTcwvLutMetadata_ocean(globalAttributes, dimensions, variables);
        testTcwvLutContent_ocean(variables);
    }

    private void testTcwvLutMetadata_ocean(List<Attribute> globalAttributes, List<Dimension> dimensions, List<Variable> variables) {
        assertNotNull(globalAttributes);
        assertEquals(19, globalAttributes.size());
        assertEquals("instrument", globalAttributes.get(0).getFullName());
        assertEquals("modis_terra", globalAttributes.get(0).getValue(0));
        assertEquals("win_bnd", globalAttributes.get(1).getFullName());
        assertEquals("2", globalAttributes.get(1).getValue(0));
        assertEquals("abs_bnd", globalAttributes.get(3).getFullName());
        assertEquals("17,18,19", globalAttributes.get(3).getValue(0));
        assertEquals("axes", globalAttributes.get(4).getFullName());
        assertEquals("wvc,aot,wsp,azi,vie,suz", globalAttributes.get(4).getValue(0));
        assertEquals("cha_18_nominal", globalAttributes.get(13).getFullName());
        assertEquals(936.0, globalAttributes.get(13).getValue(0));
        assertEquals("cha_18_bwvl", globalAttributes.get(14).getFullName());
        assertEquals(9.503882971929052, globalAttributes.get(14).getValue(0));
        assertEquals("cha_18_cwvl", globalAttributes.get(15).getFullName());
        assertEquals(935.6916504509932, globalAttributes.get(15).getValue(0));

        assertNotNull(dimensions);
        assertEquals(10, dimensions.size());
        assertEquals("bands", dimensions.get(0).getFullName());
        assertEquals(4, dimensions.get(0).getLength());
        assertEquals("ny_nx", dimensions.get(1).getFullName());
        assertEquals(24, dimensions.get(1).getLength());
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
        assertEquals(14, variables.size());
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
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 4, variables.get(7).getSize());
        assertEquals(DataType.DOUBLE, variables.get(7).getDataType());
        assertEquals("jlut", variables.get(8).getFullName());
        assertEquals(6 * 6 * 11 * 11 * 9 * 9 * 24, variables.get(8).getSize());
        assertEquals(DataType.DOUBLE, variables.get(8).getDataType());
        assertEquals("cor/19", variables.get(9).getFullName());
        assertEquals(2, variables.get(9).getSize());
        assertEquals(DataType.DOUBLE, variables.get(9).getDataType());
        assertEquals("cor/18", variables.get(10).getFullName());
        assertEquals(2, variables.get(10).getSize());
        assertEquals(DataType.DOUBLE, variables.get(10).getDataType());
        assertEquals("cor/2", variables.get(11).getFullName());
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
            assertEquals(-2.0, aotArray[0], 1.E-6);
            assertEquals(-1.04, aotArray[2], 1.E-2);
            assertEquals(-0.0099, aotArray[5], 1.E-2);

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
            assertEquals(4, jacoArray[0]);
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
            assertEquals(4, lutArray[0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(0.1203, lutArray[0][0][0][0][0][0][0], 1.E-4);
            assertEquals(0.633589, lutArray[3][3][8][0][8][1][2], 1.E-6);
            assertEquals(0.1803, lutArray[2][4][6][3][2][7][1], 1.E-6);
            assertEquals(0.579594, lutArray[4][2][5][4][1][7][2], 1.E-6);
            assertEquals(0.015008, lutArray[1][5][4][5][3][6][0], 1.E-6);
            assertEquals(0.021198, lutArray[0][5][0][8][6][3][0], 1.E-6);
            assertEquals(0.560888, lutArray[2][4][1][8][8][1][2], 1.E-6);
            assertEquals(0.499643, lutArray[5][1][2][7][0][8][2], 1.E-6);
            assertEquals(0.105830, lutArray[3][0][3][6][7][2][1], 1.E-6);
            assertEquals(0.508843, lutArray[5][5][10][10][8][8][2], 1.E-6);   // last value

            final double[][][][][][][] jlutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(variables.get(8));
            // 6*6*11*11*9*9*18
            assertNotNull(jlutArray);
            assertEquals(6, jlutArray.length);
            assertEquals(6, jlutArray[0].length);
            assertEquals(11, jlutArray[0][0].length);
            assertEquals(11, jlutArray[0][0][0].length);
            assertEquals(9, jlutArray[0][0][0][0].length);
            assertEquals(9, jlutArray[0][0][0][0][0].length);
            assertEquals(24, jlutArray[0][0][0][0][0][0].length);

            // compare some values against LUT being read in Python CAWA code...
            assertEquals(-0.0002007, jlutArray[0][0][0][0][0][0][0], 1.E-6);
            assertEquals(0.000119, jlutArray[3][3][8][0][8][1][2], 1.E-6);
            assertEquals(0.017494, jlutArray[2][4][6][3][2][7][1], 1.E-6);
            assertEquals(1.506136E-5, jlutArray[4][2][5][4][1][7][2], 1.E-6);
            assertEquals(-1.000103E-5, jlutArray[1][5][4][5][3][6][0], 1.E-6);
            assertEquals(-1.15138E-5, jlutArray[0][5][0][8][6][3][0], 1.E-6);
            assertEquals(-0.000108, jlutArray[2][4][1][8][8][1][2], 1.E-6);
            assertEquals(-1.400131E-5, jlutArray[5][1][2][7][0][8][2], 1.E-6);
            assertEquals(0.001394, jlutArray[3][0][3][6][7][2][1], 1.E-6);
            assertEquals(-0.008511, jlutArray[5][5][10][10][8][8][17], 1.E-6);   // last value
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
