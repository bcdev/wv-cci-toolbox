package org.esa.snap.wvcci.tcwv.l3;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class L3MergeNirNirCCN5Test {


    @Test
    public void testGetNirNirNumObsSrcBandNames() {
        String[] s1 = new String[]{"MODIS_TERRA", "MODIS_AQUA"};
        String[] numObsSrcBandNames = L3DailyMergeNirNirCCN5Op.getNumObsTargetBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(3, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs", numObsSrcBandNames[2]);

        s1 = new String[]{"MODIS_TERRA", "MODIS_AQUA", "OLCI_A"};
        numObsSrcBandNames = L3DailyMergeNirNirCCN5Op.getNumObsTargetBandNames(s1);
        assertNotNull(numObsSrcBandNames);
        assertEquals(4, numObsSrcBandNames.length);
        assertEquals("num_obs_MODIS_TERRA", numObsSrcBandNames[0]);
        assertEquals("num_obs_MODIS_AQUA", numObsSrcBandNames[1]);
        assertEquals("num_obs_OLCI_A", numObsSrcBandNames[2]);
        assertEquals("num_obs", numObsSrcBandNames[3]);
    }

    @Test
    public void testMergeTcwv() {
        double[] srcTcwv = new double[]{30.0, 60.0};
        double[] srcTcwvCounts = new double[]{10.0, 5.0};
        double[] srcTcwvNodata = new double[]{Double.NaN, Double.NaN};
        double[] srcTcwvCountsNodata = new double[]{Double.NaN, Double.NaN};
        double[] mergedTcwv =
                L3DailyMergeNirNirCCN5Op.mergeTcwv(srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        assertNotNull(mergedTcwv);
        assertEquals(2, mergedTcwv.length);
        assertEquals(40.0, mergedTcwv[0], 1.E-10);
        assertEquals(15.0, mergedTcwv[1], 1.E-10);

        srcTcwv = new double[]{30.0, 60.0, Double.NaN, 10.0};
        srcTcwvCounts = new double[]{10.0, 5.0, 37.0, 15.0};
        srcTcwvNodata = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        srcTcwvCountsNodata = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        mergedTcwv =
                L3DailyMergeNirNirCCN5Op.mergeTcwv(srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        assertNotNull(mergedTcwv);
        assertEquals(2, mergedTcwv.length);
        assertEquals(25.0, mergedTcwv[0], 1.E-10);
        assertEquals(30.0, mergedTcwv[1], 1.E-10);
    }

    @Test
    public void testMergePossibeNumObs() {
        int[] srcNumObs = new int[]{30, 60};
        int[] srcTcwvNumObsNoData = new int[]{-1, -1};
        int mergedPossibeNumObs =
                L3DailyMergeNirNirCCN5Op.mergePossibeNumObs(srcNumObs, srcTcwvNumObsNoData);
        assertEquals(90, mergedPossibeNumObs);

        srcNumObs = new int[]{66, -1, 1234, 0};
        srcTcwvNumObsNoData = new int[]{-1, -1, -1, -1};
        mergedPossibeNumObs =
                L3DailyMergeNirNirCCN5Op.mergePossibeNumObs(srcNumObs, srcTcwvNumObsNoData);
        assertEquals(1300, mergedPossibeNumObs);
    }

    @Test
    public void testMergeFlags() {
        double[] srcFlags = new double[]{3.0, 7.0};
        double[] srcTcwvCounts = new double[]{10.0, 5.0};
        double[] srcTcwvCountsNodata = new double[]{Double.NaN, Double.NaN};
        double mergedFlag = L3DailyMergeNirNirCCN5Op.mergeFlag(srcFlags, srcTcwvCounts, srcTcwvCountsNodata);
        assertEquals(3.0, mergedFlag, 1.E-10);

        srcTcwvCounts = new double[]{10.0, 25.0};
        mergedFlag = L3DailyMergeNirNirCCN5Op.mergeFlag(srcFlags, srcTcwvCounts, srcTcwvCountsNodata);
        assertEquals(7.0, mergedFlag, 1.E-10);

        srcFlags = new double[]{3.0, 7.0, 1.0, 5.0};
        srcTcwvCounts = new double[]{10.0, 5.0, 100.0, Double.NaN};
        srcTcwvCountsNodata = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        mergedFlag = L3DailyMergeNirNirCCN5Op.mergeFlag(srcFlags, srcTcwvCounts, srcTcwvCountsNodata);
        assertEquals(1.0, mergedFlag, 1.E-10);

        srcFlags = new double[]{Double.NaN, 10.0};
        srcTcwvCounts = new double[]{Double.NaN, 0.0};
        srcTcwvCountsNodata = new double[]{Double.NaN, Double.NaN};
        mergedFlag = L3DailyMergeNirNirCCN5Op.mergeFlag(srcFlags, srcTcwvCounts, srcTcwvCountsNodata);
        assertEquals(10.0, mergedFlag, 1.E-10);
    }
}
