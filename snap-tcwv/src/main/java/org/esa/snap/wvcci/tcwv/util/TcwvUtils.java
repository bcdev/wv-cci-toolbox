package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

import java.awt.*;
import java.util.Random;

/**
 * Utility class for TCWV retrievals
 *
 * @author olafd
 */
public class TcwvUtils {

    /**
     * Copies properties from a source band to a new band.
     *
     * @param b - the new band
     * @param sourceBand - the source band
     */
    public static void copyBandProperties(Band b, Band sourceBand) {
        b.setNoDataValue(sourceBand.getNoDataValue());
        b.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        b.setScalingFactor(sourceBand.getScalingFactor());
        b.setScalingOffset(sourceBand.getScalingOffset());
        b.setUnit(sourceBand.getUnit());
        b.setDescription(sourceBand.getDescription());
    }

    /**
     * Provides the TCWV quality flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createTcwvQualityFlagCoding(String flagId) {

        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag("TCWV_INVALID", BitSetter.setFlag(0, TcwvConstants.TCWV_INVALID),
                           TcwvConstants.TCWV_INVALID_DESCR_TEXT);
        flagCoding.addFlag("TCWV_UNCERTAIN", BitSetter.setFlag(0, TcwvConstants.TCWV_UNCERTAIN),
                           TcwvConstants.TCWV_UNCERTAIN_DESCR_TEXT);
        flagCoding.addFlag("TCWV_OK", BitSetter.setFlag(0, TcwvConstants.TCWV_OK),
                           TcwvConstants.TCWV_OK_DESCR_TEXT);

        // todo: discuss these and maybe more flags with group
        return flagCoding;
    }

    /**
     * Provides the TCWV quality flag bitmask
     *
     * @param tcwvProduct - the pixel classification product
     *
     * @return the number of bitmasks set
     */
    public static int setupTcwvQualityFlagBitmask(Product tcwvProduct) {

        int index = 0;
        int w = tcwvProduct.getSceneRasterWidth();
        int h = tcwvProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random(1234567);

        final String flagBandName = TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME;
        mask = Mask.BandMathsType.create("TCWV_INVALID", TcwvConstants.TCWV_INVALID_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_INVALID",
                                         Color.RED, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_UNCERTAIN", TcwvConstants.TCWV_UNCERTAIN_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_UNCERTAIN",
                                         Color.YELLOW, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_OK", TcwvConstants.TCWV_OK_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_OK",
                                         Color.GREEN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        return index;
    }


}
