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
//        final double[] rectCorrExt = new double[]{0.0, 1.0};
        final double[] rectCorrExt = null;

        final double ab_corr = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 0, samf, true);
//        assertEquals(0.20387, ab_corr, 1.E-5);
        assertEquals(0.18885, ab_corr, 1.E-5);
    }

    @Test
    public void testRectifyO2Corr_ocean_meris() {
        // extracted from TEST_COWA_OCEAN_1 in functional_tests.py
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.031736, 0.031391};  // bands 13, 14
        final double[] rho_ab = new double[]{0.02135569};    // band 15
//        final double[] rectCorrExt = new double[]{0.0, 1.0};
        final double[] rectCorrExt = null;

        final double ab_corr = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 0, samf, false);
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
//        final double[] rectCorrExt = new double[]{0.0, 1.0};
        final double[] rectCorrExt = null;

        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 0, samf, true);
//        assertEquals(0.20387, ab_corr_1, 1.E-5);
        assertEquals(0.186369, ab_corr_1, 1.E-5);

        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 1, samf, true);
//        assertEquals(0.787277, ab_corr_2, 1.E-5);
        assertEquals(0.697565, ab_corr_2, 1.E-5);
    }

    @Test
    public void testRectifyO2Corr_ocean_olci() {
        // extracted from TEST_COWA_OCEAN_2 in functional_tests.py
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        final double samf = 1.48959;  // sqrt(amf)
        final double[] rho_wb = new double[]{0.031};    // band 18
        final double[] rho_ab = new double[]{0.021, 0.007};  // bands 19, 20
//        final double[] rectCorrExt = new double[]{0.0, 1.0};
        final double[] rectCorrExt = null;

        final double ab_corr_1 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 0, samf, false);
//        assertEquals(0.2614577, ab_corr_1, 1.E-5);
        assertEquals(0.239613, ab_corr_1, 1.E-5);

        final double ab_corr_2 = algorithm.rectifyAndO2Correct(sensor, rho_wb, rho_ab, rectCorrExt, 1, samf, false);
//        assertEquals(0.998984, ab_corr_2, 1.E-5);
        assertEquals(0.920799, ab_corr_2, 1.E-5);
    }

}