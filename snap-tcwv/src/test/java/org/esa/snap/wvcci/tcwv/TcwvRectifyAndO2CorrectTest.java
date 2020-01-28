package org.esa.snap.wvcci.tcwv;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvRectifyAndO2CorrectTest {

    @Test
    public void testRectifyO2Corr_land_meris() {
        // extracted from TEST_COWA_LAND_1 in functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.042, 0.042};  // bands 13, 14
        final double[] rho_ab = new double[]{0.031};    // band 15

        final double ab_corr = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
//        assertEquals(0.20387, ab_corr, 1.E-5);
//        assertEquals(0.18885, ab_corr, 1.E-5);
        assertEquals(0.19449, ab_corr, 1.E-5);
    }

    @Test
    public void testRectifyO2Corr_land_meris_consolidated() {
        // extracted from TEST_COWA_LAND_1 in functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.615843;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.00818567, 0.007840576};  // bands 13, 14
        final double[] rho_ab = new double[]{0.0057849};    // band 15

        final double ab_corr = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
        assertEquals(0.1595915, ab_corr, 1.E-5);
    }


    @Test
    public void testRectifyO2Corr_ocean_meris() {
        // extracted from TEST_COWA_OCEAN_1 in functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.031736, 0.031391};  // bands 13, 14
        final double[] rho_ab = new double[]{0.02135569};    // band 15

        final double ab_corr = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, false);
//        assertEquals(0.25304, ab_corr, 1.E-5);
        assertEquals(0.22462, ab_corr, 1.E-5);
    }

    @Test
    public void testRectifyO2Corr_land_olci() {
        // extracted from TEST_COWA_LAND_3 in functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.042, 0.042};  // bands 18, 21
        final double[] rho_ab = new double[]{0.031, 0.013};    // bands 19, 20

        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
//        assertEquals(0.20387, ab_corr_1, 1.E-5);
//        assertEquals(0.186369, ab_corr_1, 1.E-5);
        assertEquals(0.18712, ab_corr_1, 1.E-5);         // RP 20200113

        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, true);
//        assertEquals(0.787277, ab_corr_2, 1.E-5);
//        assertEquals(0.697565, ab_corr_2, 1.E-5);
        assertEquals(0.705965, ab_corr_2, 1.E-5);        // RP 20200113
    }

    @Test
    public void testRectifyO2Corr_land_olci_consolidated() {
        // extracted from TEST_COWA_LAND_3 in functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.58723313782;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.081147753, 0.083997689};  // bands 18, 21
        final double[] rho_ab = new double[]{0.051190317, 0.014368968};    // bands 19, 20

        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
        assertEquals(0.270955, ab_corr_1, 1.E-5);         // RP 20200113

        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, true);
        assertEquals(0.978353, ab_corr_2, 1.E-5);        // RP 20200113
    }


    @Test
    public void testRectifyO2Corr_ocean_olci() {
        // extracted from TEST_COWA_OCEAN_2 in functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.031};    // band 18
        final double[] rho_ab = new double[]{0.021, 0.007};  // bands 19, 20

        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, false);
//        assertEquals(0.2614577, ab_corr_1, 1.E-5);
//        assertEquals(0.239613, ab_corr_1, 1.E-5);
        assertEquals(0.240257, ab_corr_1, 1.E-5);

        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, false);
//        assertEquals(0.998984, ab_corr_2, 1.E-5);
//        assertEquals(0.920799, ab_corr_2, 1.E-5);
        assertEquals(0.923873, ab_corr_2, 1.E-5);
    }

    @Test
    public void testRectifyO2Corr_land_modis() {
        // extracted from TEST_COWA_LAND_1 in functional_tests.py
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.62919;  // sqrt(amf)
        double[] rho_wb = new double[]{0.042880710235182445, 0.05806035980596497};
        double[] rho_ab = new double[]{0.034583598016895215, 0.012255925435128554, 0.0202874049561323};

        final double ab_corr_0 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
        assertEquals(0.165, ab_corr_0, 1.E-3);
        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, true);
        assertEquals(0.761, ab_corr_1, 1.E-3);
        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 2, samf, true);
//        assertEquals(0.488, ab_corr_2, 1.E-3);
        assertEquals(0.486, ab_corr_2, 1.E-3);
    }

    @Test
    public void testRectifyO2Corr_land_modis_consolidated() {
        // extracted from TEST_COWA_LAND_1 in functional_tests.py
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.55241166;  // sqrt(amf)
        double[] rho_wb = new double[]{0.0781531, 0.07524855};
        double[] rho_ab = new double[]{0.060047, 0.01889, 0.032066};

        final double ab_corr_0 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, true);
        assertEquals(0.173657, ab_corr_0, 1.E-3);
        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, true);
        assertEquals(0.85278, ab_corr_1, 1.E-3);
        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 2, samf, true);
//        assertEquals(0.488, ab_corr_2, 1.E-3);
        assertEquals(0.548938, ab_corr_2, 1.E-3);
    }

    @Test
    public void testRectifyO2Corr_ocean_modis_consolidated() {
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.5248958636;  // sqrt(amf)
        double[] rho_wb = new double[]{0.0034251227};
        double[] rho_ab = new double[]{0.002566, 0.0015034864, 0.0017748937};

        final double ab_corr_0 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 0, samf, false);
        assertEquals(0.185778, ab_corr_0, 1.E-3);
        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 1, samf, false);
        assertEquals(0.510452, ab_corr_1, 1.E-3);
        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, 2, samf, false);
        assertEquals(0.38791, ab_corr_2, 1.E-3);
    }

}