package org.esa.snap.wvcci.tcwv.util;

import org.junit.Test;

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
}