package org.esa.s3tbx.olci.o2corr;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;

import java.io.FileReader;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class O2CorrOlciIOTest {

    private Path installAuxdataPath;

    @Before
    public void setUp() throws Exception {
        installAuxdataPath = O2CorrOlciIO.installAuxdata();
    }

    @Test
    public void testParseJsonFile_desmileLut() throws Exception {
        final Path pathJSON = installAuxdataPath.resolve("O2_desmile_lut_SMALL_TEST.json");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(pathJSON.toString()));

        // parse JSON file...
        final long L = O2CorrOlciIO.parseJSONInt(jsonObject, "L");
        final long M = O2CorrOlciIO.parseJSONInt(jsonObject, "M");
        final long N = O2CorrOlciIO.parseJSONInt(jsonObject, "N");
        final double[][][] jacobians = O2CorrOlciIO.parseJSON3DimDoubleArray(jsonObject, "JACO");
        final double[][] X = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "X");
        final double[][] Y = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "Y");
        final double[] VARI = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "VARI");
        final double cbwd = O2CorrOlciIO.parseJSONDouble(jsonObject, "cbwd");
        final double cwvl = O2CorrOlciIO.parseJSONDouble(jsonObject, "cwvl");
        final long leafsize = O2CorrOlciIO.parseJSONInt(jsonObject, "leafsize");
        final String[] sequ = O2CorrOlciIO.parseJSON1DimStringArray(jsonObject, "sequ");
        final double[] MEAN = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "MEAN");

        // test all parsed objects...
        assertJSONParsedObjects(L, M, N, jacobians, X, Y, VARI, cbwd, cwvl, leafsize, sequ, MEAN);

        // test DesmileLut holder...
        DesmileLut lut = new DesmileLut(L, M, N, X, Y, jacobians, MEAN, VARI, cwvl, cbwd, leafsize, sequ);
        assertNotNull(lut);
        assertJSONParsedObjects(lut.getL(), lut.getM(), lut.getN(), lut.getJACO(), lut.getX(), lut.getY(), lut.getVARI(),
                                lut.getCbwd(), lut.getCwvl(), lut.getLeafsize(), lut.getSequ(), lut.getMEAN());
    }

    private void assertJSONParsedObjects(long l, long m, long n,
                                         double[][][] jacobians, double[][] x, double[][] y,
                                         double[] VARI, double cbwd, double cwvl, long leafsize,
                                         String[] sequ, double[] MEAN) {
        final long expectedL = 3;
        assertEquals(expectedL, l);

        final long expectedM = 1;
        assertEquals(expectedM, m);

        final long expectedN = 4;
        assertEquals(expectedN, n);

        final double[][][] expectedJacobians = {
                {
                        {
                                -0.197668526954583,
                                0.0,
                                -0.0395649820755298,
                                0.0
                        }
                },
                {
                        {
                                -2.007899810240903138,
                                3.0026280660476688256,
                                4.5873694823399553,
                                5.02119888053260749
                        }
                },
                {
                        {
                                -0.007899810240903138,
                                0.0026280660476688256,
                                1.5873694823399553,
                                0.02119888053260749
                        }
                }
        };
        assertNotNull(jacobians);
        assertEquals(3, jacobians.length);
        assertEquals(1, jacobians[1].length);
        assertEquals(4, jacobians[2][0].length);
        assertArrayEquals(expectedJacobians, jacobians);

        final double[][] expectedX = {
                {
                        -1.6514456476894992,
                        -1.5811388300810618,
                        -0.267609642092725,
                        -1.5491933384829668
                },
                {
                        -5.6514456476894992,
                        -6.5811388300810618,
                        -7.267609642092725,
                        -8.5491933384829668
                },
                {
                        1.6514456476894992,
                        1.5811388300869134,
                        2.088818413372202,
                        1.5491933384829668
                }
        };
        assertNotNull(x);
        assertEquals(3, x.length);
        assertEquals(4, x[1].length);
        assertArrayEquals(expectedX, x);


        final double[][] expectedY = {
                {
                        0.9293298058188657
                },
                {
                        0.9088407606665322
                },
                {
                        1.0582987928525662
                }
        };
        assertNotNull(y);
        assertEquals(3, y.length);
        assertEquals(1, y[1].length);
        assertArrayEquals(expectedY, y);


        final double[] expectedVARI = {
                0.6055300708195136,
                0.09486832980506345,
                0.19662785879946731,
                2.581988897471611
        };
        assertNotNull(VARI);
        assertEquals(4, VARI.length);
        assertArrayEquals(expectedVARI, VARI, 1e-8);


        final double expectedCbwd = 2.65;
        assertEquals(expectedCbwd, cbwd, 1e-8);


        final double expectedCwvl = 761.726;
        assertEquals(expectedCwvl, cwvl, 1e-8);


        final long expectedLeafsize = 4;
        assertEquals(expectedLeafsize, leafsize);


        final String[] expectedSequ = {
                "dwvl,bwd,tra,amf",
                "tra/zero"
        };
        assertNotNull(sequ);
        assertEquals(2, sequ.length);
        assertArrayEquals(expectedSequ, sequ);


        final double[] expectedMEAN = {
                1.5257651681785976e-20,
                2.6499999999997224,
                0.35496667905858464,
                6.0
        };
        assertNotNull(MEAN);
        assertEquals(4, MEAN.length);
        assertArrayEquals(expectedMEAN, MEAN, 1e-8);
    }

    @Test
//    @Ignore
    public void testInstallAuxdata() throws Exception {
        Path auxPath = O2CorrOlciIO.installAuxdata();
        assertNotNull(auxPath);

    }

}