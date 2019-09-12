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

        flagCoding.addFlag("TCWV_OK",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_OK),
                           TcwvConstants.TCWV_OK_DESCR_TEXT);
        flagCoding.addFlag("TCWV_L1_QUALITY_ISSUES",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_L1_QUALITY_ISSUES),
                           TcwvConstants.TCWV_L1_QUALITY_ISSUES_DESCR_TEXT);
        flagCoding.addFlag("TCWV_CRITICAL_RETRIEVAL_CONDITIONS",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_CRITICAL_RETRIEVAL_CONDITIONS),
                           TcwvConstants.TCWV_CRITICAL_RETRIEVAL_CONDITIONS_DESCR_TEXT);
        flagCoding.addFlag("TCWV_HIGH_COST_FUNCTION",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_HIGH_COST_FUNCTION),
                           TcwvConstants.TCWV_HIGH_COST_FUNCTION_DESCR_TEXT);
        flagCoding.addFlag("TCWV_INACCURATE_UNCERTAINTY",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_INACCURATE_UNCERTAINTY),
                           TcwvConstants.TCWV_INACCURATE_UNCERTAINTY_DESCR_TEXT);
        flagCoding.addFlag("TCWV_INVALID",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_INVALID),
                           TcwvConstants.TCWV_INVALID_DESCR_TEXT);

        return flagCoding;
    }

    /**
     * Provides the 'surface type' flag coding. Basically this is an Idepix subset to be propagated into
     * final TCWV L3 product.
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createSurfaceTypeFlagCoding(String flagId) {

        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag("LAND", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_LAND),
                           TcwvConstants.LAND_DESCR_TEXT);
        flagCoding.addFlag("OCEAN", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_OCEAN),
                           TcwvConstants.OCEAN_DESCR_TEXT);
        flagCoding.addFlag("SEA_ICE", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_SEA_ICE),
                           TcwvConstants.SEA_ICE_DESCR_TEXT);
        flagCoding.addFlag("CLOUD", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_CLOUD),
                           TcwvConstants.CLOUD_DESCR_TEXT);
//        flagCoding.addFlag("COASTAL_ZONE", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_COASTAL_ZONE),
//                           TcwvConstants.COASTAL_ZONE_DESCR_TEXT);

        return flagCoding;
    }


    /**
     * Provides the TCWV quality flag bitmask
     *
     * @param tcwvProduct - the TCWV product
     *
     */
    public static void setupTcwvQualityFlagBitmask(Product tcwvProduct) {

        int index = 0;
        int w = tcwvProduct.getSceneRasterWidth();
        int h = tcwvProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random(1234567);

        final String flagBandName = TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME;
        mask = Mask.BandMathsType.create("TCWV_OK",
                                         TcwvConstants.TCWV_OK_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_OK",
                                         Color.GREEN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_L1_QUALITY_ISSUES",
                                         TcwvConstants.TCWV_L1_QUALITY_ISSUES_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_L1_QUALITY_ISSUES",
                                         Color.YELLOW, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_CRITICAL_RETRIEVAL_CONDITIONS",
                                         TcwvConstants.TCWV_CRITICAL_RETRIEVAL_CONDITIONS_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_CRITICAL_RETRIEVAL_CONDITIONS",
                                         Color.BLUE, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_HIGH_COST_FUNCTION",
                                         TcwvConstants.TCWV_HIGH_COST_FUNCTION_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_HIGH_COST_FUNCTION",
                                         Color.ORANGE, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_INACCURATE_UNCERTAINTY",
                                         TcwvConstants.TCWV_INACCURATE_UNCERTAINTY_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_INACCURATE_UNCERTAINTY",
                                         Color.CYAN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_INVALID",
                                         TcwvConstants.TCWV_INVALID_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_INVALID",
                                         Color.RED, 0.5f);
        tcwvProduct.getMaskGroup().add(index, mask);
    }

    /**
     * Provides the surface type flag bitmask
     *
     * @param tcwvProduct - the TCWV product
     *
     */
    public static void setupSurfaceTypeFlagBitmask(Product tcwvProduct) {

        int index = 0;
        int w = tcwvProduct.getSceneRasterWidth();
        int h = tcwvProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random(1234567);

        final String flagBandName = TcwvConstants.SURFACE_TYPE_FLAG_BAND_NAME;
        mask = Mask.BandMathsType.create("LAND",
                                         TcwvConstants.LAND_DESCR_TEXT, w, h,
                                         flagBandName + ".LAND",
                                         Color.GREEN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("OCEAN",
                                         TcwvConstants.OCEAN_DESCR_TEXT, w, h,
                                         flagBandName + ".OCEAN",
                                         Color.BLUE, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("SEA_ICE",
                                         TcwvConstants.SEA_ICE_DESCR_TEXT, w, h,
                                         flagBandName + ".SEA_ICE",
                                         Color.CYAN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("CLOUD",
                                         TcwvConstants.CLOUD_DESCR_TEXT, w, h,
                                         flagBandName + ".CLOUD",
                                         Color.YELLOW, 0.5f);

        tcwvProduct.getMaskGroup().add(index, mask);

//        mask = Mask.BandMathsType.create("COASTAL_ZONE",
//                                         TcwvConstants.COASTAL_ZONE_DESCR_TEXT, w, h,
//                                         flagBandName + ".COASTAL_ZONE",
//                                         Color.ORANGE, 0.5f);
//        tcwvProduct.getMaskGroup().add(index, mask);

    }


}
