package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.junit.Test;

import java.text.ParseException;

import static junit.framework.TestCase.assertEquals;

public class WvcciNc4ComplianceWriterTest {

    @Test
    public void testGetDaysSince1970() {
        String srcDateString = "1970-01-16";
        try {
            assertEquals(15, WvcciNc4ComplianceWriter.getDaysSince1970(srcDateString));
            srcDateString = "1980-01-16";
            // we have two leap years in here:
            assertEquals(3667, WvcciNc4ComplianceWriter.getDaysSince1970(srcDateString));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            srcDateString = "16 JAN 1980";
            WvcciNc4ComplianceWriter.getDaysSince1970(srcDateString);
        } catch (ParseException e) {
            assertEquals("Unparseable date: \"16 JAN 1980\"", e.getMessage());
        }


    }
}
