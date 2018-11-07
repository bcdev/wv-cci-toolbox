package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import ucar.ma2.DataType;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnapWvcciNc4WriterTest {

    public static void main(String[] args) throws Exception {
//        final String tcwvFilename = "subset_TCWV.nc";
        final String tcwvFilename = "L2_of_L2_of_MER_RR__1PRACR20080101_085745_000026172064_00408_30522_0000.nc";
        final String tcwvFilePath = SnapWvcciNc4WriterTest.class.getResource(tcwvFilename).getPath();
        final Product tcwvProduct = loadTcwvProduct(tcwvFilePath);
        final String tcwvCawaNc4Filename = "subset_TCWV_wvcci_nc4.nc";
        final String tcwvCawaNc4FilePath = new File(tcwvFilePath).getParent() + File.separator + tcwvCawaNc4Filename;
        Logger.getGlobal().log(Level.INFO, "Writing TCWV WVCCI NetCDF4 file '" + tcwvCawaNc4FilePath + "'...");
        ProductIO.writeProduct(tcwvProduct, tcwvCawaNc4FilePath, "NetCDF4-WVCCI");
    }

    private static Product loadTcwvProduct(String tcwvFilePath) {
        Product tcwvProduct = null;
        try {
            Logger.getGlobal().log(Level.INFO, "Reading TCWV file '" + tcwvFilePath + "'...");
            tcwvProduct = ProductIO.readProduct(new File(tcwvFilePath));
        } catch (IOException e) {
            Logger.getGlobal().log(Level.WARNING, "Warning: cannot open or read TCWV file.");
        }
        return tcwvProduct;
    }
}
