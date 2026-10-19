package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.wvcci.tcwv.dataio.nc4.SnapWvcciNc4WriterTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

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

    @Test
    public void testGetExistingSourceProducts() throws IOException {
        Product input_1 = new Product("dummy-MODIS_TERRA.nc", "test");
        input_1.setFileLocation(new File(getClass().getResource("dummy-MODIS_TERRA.nc").getPath()));
        Product input_2 = new Product("dummy-MODIS_AQUA.nc", "test");
        input_2.setFileLocation(new File(getClass().getResource("dummy-MODIS_TERRA.nc").getPath()));
        Product[] inputProducts = new Product[]{input_1, input_2};

        Product[]  existingSourceProducts = L3DailyMergeNirNirCCN5Op.getExistingSourceProducts(inputProducts);
        assertNotNull(existingSourceProducts);
        assertEquals(2, existingSourceProducts.length);
        assertEquals("dummy-MODIS_TERRA.nc", existingSourceProducts[0].getName());
        assertEquals("dummy-MODIS_AQUA.nc", existingSourceProducts[1].getName());

        Product input_3 = new Product("dummy-MERIS.nc", "test");
        inputProducts = new Product[]{input_1, input_2, input_3};
        existingSourceProducts = L3DailyMergeNirNirCCN5Op.getExistingSourceProducts(inputProducts);
        assertNotNull(existingSourceProducts);
        assertEquals(2, existingSourceProducts.length);

        inputProducts = new Product[]{input_3};

        try {
            existingSourceProducts = L3DailyMergeNirNirCCN5Op.getExistingSourceProducts(inputProducts);
        } catch (IOException e) {
            assertEquals("No existing source products found.", e.getMessage());
        }

    }

    @Test
    public void testGetSensorNames() throws IOException {
        Product input_1 = new Product("dummy-MODIS_TERRA.nc", "test");
        input_1.setFileLocation(new File(getClass().getResource("dummy-MODIS_TERRA.nc").getPath()));
        Product input_2 = new Product("dummy-MODIS_AQUA.nc", "test");
        input_2.setFileLocation(new File(getClass().getResource("dummy-MODIS_TERRA.nc").getPath()));
        Product[] inputProducts = new Product[]{input_1, input_2};

        String[]  sensorNames = L3DailyMergeNirNirCCN5Op.getSensorNames(inputProducts);
        assertNotNull(sensorNames);
        assertEquals(2, sensorNames.length);
        assertEquals("MODIS_TERRA", sensorNames[0]);
        assertEquals("MODIS_AQUA", sensorNames[1]);

        Product input_3 = new Product("dummy-LANDSAT.nc", "test");
        inputProducts = new Product[]{input_1, input_2, input_3};
        sensorNames = L3DailyMergeNirNirCCN5Op.getSensorNames(inputProducts);
        assertNotNull(sensorNames);
        assertEquals(2, sensorNames.length);

        inputProducts = new Product[]{input_3};

        try {
            sensorNames = L3DailyMergeNirNirCCN5Op.getSensorNames(inputProducts);
        } catch (IOException e) {
            assertEquals("No valid sensor names found.", e.getMessage());
        }

    }
}
