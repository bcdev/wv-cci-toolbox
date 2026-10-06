package org.esa.snap.wvcci.tcwv.l3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class L3MergeNirNirCCN5Test {


    @Test
    public void testGetNirNirNumObsSrcBandNames() {
        String[] s1 = new String[]{"MODIS_TERRA", "MODIS_AQUA"};
        String[] numObsSrcBandNames = L3DailyMergeNirNirCCN5Op.getNumObsSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(3, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs", numObsSrcBandNames[2]);

        s1 = new String[]{"MODIS_TERRA", "MODIS_AQUA", "OLCI_A"};
        numObsSrcBandNames = L3DailyMergeNirNirCCN5Op.getNumObsSrcBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(4, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs_OLCI_A", numObsSrcBandNames[2]);
        assertEquals("num_obs", numObsSrcBandNames[3]);
    }
}
