package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.wvcci.tcwv.Sensor;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;


public class TcwvUtilsTest {


    @Test
    public void testSnrToPseudoAbsorptionMeasurementVariance() {

        double snr = 300.0;
        double interpolError = 0.01;
        double amf = 2.610949;
        final double variance = TcwvUtils.computePseudoAbsorptionMeasurementVariance(snr, interpolError, amf);
        assertEquals(0.003838, variance,1.E-4);
    }

    @Test
    public void testGetAtmosphericPressure() {
        double height = 179.9375;
        double slp = 1012.3078;
        final double atmosphericPressure = TcwvUtils.getSurfacePressure(slp, height);
        assertEquals(990.899, atmosphericPressure, 1.E-3);
    }

    @Test
    @Ignore
    public void testGetSurfaceTemperatureFromVerticalProfiles() throws IOException {
        double seaLevelPress = 1013.0;
        double altitude = 2000.0;
        double surfacePress = TcwvUtils.getSurfacePressure(seaLevelPress, altitude);

//        public final static double[] MERIS_REF_PRESSURE_LEVELS = {
//             1000., 975., 950., 925., 900., 875., 850., 825., 800., 775.,
//              775., 700., 650., 600., 550., 500., 450., 400., 350., 300.
//        };

        final double[] atmosTempProfile = {
                287., 285., 282., 281., 279., 276., 273., 270., 266., 264.,
                260., 258., 256., 255., 252., 248., 246., 246., 242., 238.};

        double surfaceTemp = TcwvUtils.getSurfaceTemperature(Sensor.MERIS, atmosTempProfile, surfacePress);
        assertEquals(265.58, surfaceTemp, 1.E-2);

        altitude = 5000.0;
        surfacePress = TcwvUtils.getSurfacePressure(seaLevelPress, altitude);
        surfaceTemp = TcwvUtils.getSurfaceTemperature(Sensor.MERIS, atmosTempProfile, surfacePress);
        assertEquals(251.21, surfaceTemp, 1.E-2);
    }
}