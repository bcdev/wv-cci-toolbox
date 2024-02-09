package org.esa.snap.wvcci.tcwv.l3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class L3MergeNirHoapsPhase2Test {


    @Test
    public void testGetNirHoapsNumObsSrcBandNames() {
        String s1 = "MODIS_TERRA";
        String[] numObsSrcBandNames = L3DailyMergeNirHoapsPhase2Op.getNumObsNirSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(1, numObsSrcBandNames.length);
        assertEquals("num_obs", numObsSrcBandNames[0]);

        s1 = "MODIS_TERRA-MODIS_AQUA";
        numObsSrcBandNames = L3DailyMergeNirHoapsPhase2Op.getNumObsNirSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(2, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);

        s1 = "MODIS_TERRA-MODIS_AQUA-OLCI_A";
        numObsSrcBandNames = L3DailyMergeNirHoapsPhase2Op.getNumObsNirSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(3, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs_OLCI_A", numObsSrcBandNames[2]);

        s1 = "MODIS_TERRA-MODIS_AQUA-OLCI_A-OLCI_B";
        numObsSrcBandNames = L3DailyMergeNirHoapsPhase2Op.getNumObsNirSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(4, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs_OLCI_A", numObsSrcBandNames[2]);
        assertEquals("num_obs_OLCI_B", numObsSrcBandNames[3]);
    }


}
