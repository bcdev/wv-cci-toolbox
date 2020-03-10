package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

import java.awt.*;

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
        flagCoding.addFlag("TCWV_COST_FUNCTION_1",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_COST_FUNCTION_1),
                           TcwvConstants.TCWV_COST_FUNCTION_1_DESCR_TEXT);
        flagCoding.addFlag("TCWV_COST_FUNCTION_2",
                           BitSetter.setFlag(0, TcwvConstants.TCWV_COST_FUNCTION_2),
                           TcwvConstants.TCWV_COST_FUNCTION_2_DESCR_TEXT);
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
        flagCoding.addFlag("UNDEFINED", BitSetter.setFlag(0, TcwvConstants.SURFACE_TYPE_UNDEFINED),
                           TcwvConstants.UNDEFINED_DESCR_TEXT);

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

        final String flagBandName = TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME;
        mask = Mask.BandMathsType.create("TCWV_OK",
                                         TcwvConstants.TCWV_OK_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_OK",
                                         Color.GREEN, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_COST_FUNCTION_1",
                                         TcwvConstants.TCWV_COST_FUNCTION_1_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_COST_FUNCTION_1",
                                         Color.YELLOW, 0.5f);
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("TCWV_COST_FUNCTION_2",
                                         TcwvConstants.TCWV_COST_FUNCTION_2_DESCR_TEXT, w, h,
                                         flagBandName + ".TCWV_COST_FUNCTION_2",
                                         Color.ORANGE, 0.5f);
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
        tcwvProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("UNDEFINED",
                                         TcwvConstants.UNDEFINED_DESCR_TEXT, w, h,
                                         flagBandName + ".UNDEFINED",
                                         Color.gray, 0.5f);

        tcwvProduct.getMaskGroup().add(index, mask);

    }

    /**
     * Returns the pseudo absorption measurement error for given SNR, interpolation error and air mass factor.
     * See more details in function 'snr_to_pseudo_absoprtion_measurement_variance' in cowa_core.py of software
     * package 'consolidated_cci_luts' provided by RP, Jan 2020
     *
     * @param snr - SNR
     * @param interpolError - interpolation error
     * @param amf - air mass factor
     *
     * @return - the pseudo absorption measurement error
     */
    public static double computePseudoAbsorptionMeasurementVariance(double snr, double interpolError, double amf) {
       return ((1.0/(snr*snr)) + (1.0/(snr*snr) + interpolError)) / amf;
    }

//    public static void checkIfMod021KMDayProduct(Product product) {
//        final MetadataAttribute dayNightAttr = product.getMetadataRoot().getElement("Global_Attributes").
//                getAttribute("DayNightFlag");
//
//        if (dayNightAttr != null && !dayNightAttr.getData().getElemString().equals("Day")) {
//            throw new OperatorException("Product '" + product.getName() +
//                                                "' does not seem to be a MODIS L1b Day product - will exit IdePix.");
//        }
//    }

    public static boolean isMod021KMDayProduct(Product product) {
        final MetadataAttribute dayNightAttr = product.getMetadataRoot().getElement("Global_Attributes").
                getAttribute("DayNightFlag");
        return (dayNightAttr != null && dayNightAttr.getData().getElemString().equals("Day"));
    }

}
