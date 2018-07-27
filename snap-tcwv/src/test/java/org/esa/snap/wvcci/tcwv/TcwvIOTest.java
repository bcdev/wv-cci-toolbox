package org.esa.snap.wvcci.tcwv;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class TcwvIOTest {

    private Path auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdata();
    }

    @Test
    public void testInstallAuxdata() {
        assertNotNull(auxdataPath);
    }


}
