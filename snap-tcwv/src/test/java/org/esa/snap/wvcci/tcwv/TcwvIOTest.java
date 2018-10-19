package org.esa.snap.wvcci.tcwv;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TcwvIOTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testInstallAuxdata() {
        assertNotNull(auxdataPath);
    }


}
