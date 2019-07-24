package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.dataio.mod35.ModisMod35L2Constants;

/**
 * Constants for TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvConstants {

    static final String PIXEL_CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String TCWV_QUALITY_FLAG_BAND_NAME = "tcwv_quality_flags";

    public static final String TCWV_TARGET_BAND_NAME = "tcwv";
    public static final String TCWV_MEAN_BAND_NAME = "tcwv_mean";
    public static final String TCWV_SSMI_MEAN_BAND_NAME = "wvpa";
    public static final String TCWV_UNCERTAINTY_TARGET_BAND_NAME = "tcwv_uncertainty";
    public static final String TCWV_UNCERTAINTY_MEAN_BAND_NAME = "tcwv_uncertainty_mean";
    public static final String TCWV_SSMI_UNCERTAINTY_MEAN_BAND_NAME = "stdv";
    public static final String TCWV_COUNTS_TARGET_BAND_NAME = "tcwv_counts";
    public static final String TCWV_SSMI_COUNTS_BAND_NAME = "numo";

    public static final int TCWV_OK = 0;
    public static final int TCWV_UNCERTAIN = 1;
    public static final int TCWV_INVALID = 2;

    public static final String TCWV_OK_DESCR_TEXT = "TCWV was successfully retrieved for this pixel";
    public static final String TCWV_UNCERTAIN_DESCR_TEXT = "TCWV was successfully retrieved, but has rather high uncertainty (> 5 kg/m^2)";
    // todo: define 'high uncertainty' with the group.
    public static final String TCWV_INVALID_DESCR_TEXT = "No valid TCWV was retrieved for this pixel";


    static final String TCWV_STATE_VECTOR1_BAND_NAME = "stateVector_1";
    static final String TCWV_STATE_VECTOR2_BAND_NAME = "stateVector_2";

    static final String PRIOR_T2M_BAND_NAME = "t2m";
    static final String PRIOR_MSL_BAND_NAME = "msl";
    static final String PRIOR_TCWV_BAND_NAME = "tcwv";
    static final String PRIOR_WS_BAND_NAME = "ws";

    static final double AOT865_INIT_VALUE = 0.15;  // AOT 865nm initial value for algorithm
    static final double AL0_INIT_VALUE = 0.13;     // AL0 865nm initial value for algorithm
    static final double AL1_INIT_VALUE = 0.13;     // AL1 865nm initial value for algorithm
    static final double TCWV_INIT_VALUE = 30.0;    // TCWV initial value for algorithm if we have no prior
    static final double WS_INIT_VALUE = 7.5;       // Windspeed initial value for algorithm if we have no prior

    static final int IDEPIX_INVALID_BIT = 0;
    static final int IDEPIX_CLOUD_AMBIGUOUS_BIT = 2;
    static final int IDEPIX_CLOUD_SURE_BIT = 3;
    static final int IDEPIX_CLOUD_BUFFER_BIT = 4;
    static final int IDEPIX_LAND_BIT = 10;

    static final String[] MOD35_BAND_NAMES = {
            ModisMod35L2Constants.CLOUD_MASK_BYTE_TARGET_BAND_NAME +  "1",
            ModisMod35L2Constants.QUALITY_ASSURANCE_QA_DIMENSION_BAND_NAME +  "1"
    };

//    static final String MERIS_LAND_LUT_NC_FILENAME = "land_core_meris.nc4";
    static final String MERIS_LAND_LUT_NC_FILENAME = "land_core_meris_calib_arm.nc4";
//    static final String MERIS_OCEAN_LUT_NC_FILENAME = "ocean_core_meris.nc4";
    static final String MERIS_OCEAN_LUT_NC_FILENAME = "ocean_core_meris_calib.nc4";
    static final String MODIS_AQUA_OCEAN_LUT_NC_FILENAME = "ocean_core_modis_aqua.nc4";
    static final String MODIS_TERRA_OCEAN_LUT_NC_FILENAME = "ocean_core_modis_terra.nc4";
//    static final String MODIS_AQUA_LAND_LUT_NC_FILENAME = "land_core_modis_aqua.nc4";
    static final String MODIS_AQUA_LAND_LUT_NC_FILENAME = "land_core_modis_aqua_calib_arm.nc4";
//    static final String MODIS_TERRA_LAND_LUT_NC_FILENAME = "land_core_modis_terra.nc4";
    static final String MODIS_TERRA_LAND_LUT_NC_FILENAME = "land_core_modis_terra_calib_arm.nc4";

    static final String OLCI_LAND_LUT_NC_FILENAME = "land_core_olci_calib_arm.nc4";
    static final String OLCI_OCEAN_LUT_NC_FILENAME = "ocean_core_olci_calib.nc4";

    final static String[] MERIS_REFL_BAND_NAMES = new String[]{
            "reflectance_13", // 864 nm
            "reflectance_14", // 884 nm
            "reflectance_15"  // 900 nm
    };

    final static String[] MERIS_LAND_REFL_WIN_BAND_NAMES = new String[]{
            "reflectance_13", // 864 nm
            "reflectance_14", // 884 nm
    };

    final static String[] MERIS_LAND_REFL_ABS_BAND_NAMES = new String[]{
            "reflectance_15"  // 900 nm
    };

    final static String[] MERIS_OCEAN_REFL_WIN_BAND_NAMES = new String[]{
            "reflectance_13", // 864 nm
            "reflectance_14", // 884 nm
    };

    final static String[] MERIS_OCEAN_REFL_ABS_BAND_NAMES = new String[]{
            "reflectance_15"  // 900 nm
    };

    final static String[] MERIS_TPG_NAMES = new String[]{
            "sun_zenith",
            "view_zenith",
            "sun_azimuth",
            "view_azimuth",
            "latitude", "longitude"
    };

    // Introduce reasonable input reflectance uncertainty:
    // MERIS: radiometric accuracy < 4% 
    // https://earth.esa.int/handbooks/meris/CNTR2-6-2.html
    // 4% of 0.25 for bands 13, 14; 4% of 0.125
    // Based on rough estimates from histograms in a land subset!
//    final static double[][] MERIS_LAND_SE = {
//            {0.0001, 0.0, 0.0},
//            {0.0, 0.0001, 0.0},
//            {0.0, 0.0, 0.001}
//    };
    final static double[][] MERIS_LAND_SE = {
            {0.001, 0.0, 0.0},
            {0.0, 0.001, 0.0},
            {0.0, 0.0, 0.0005}
    };

//    final static double[][] MERIS_OCEAN_SE = {
//            {0.0001, 0.0, 0.0},
//            {0.0, 0.0001, 0.0},
//            {0.0, 0.0, 0.001}
//    };
    // 4% of 0.05 for bands 13, 14; 4% of 0.05 for band 15
    // Based on rough estimates from histograms in an ocean subset!
    final static double[][] MERIS_OCEAN_SE = {
            {0.002, 0.0, 0.0},
            {0.0, 0.002, 0.0},
            {0.0, 0.0, 0.002}
    };


    // 'cor' from land_core_meris_calib_arm.nc4, bands 13, 14, 15
    final static double[][] MERIS_LAND_RECT_CORR = {
            {0.0, 1.0},
            {0.0, 1.0},
            {-0.0015867, 0.9185331}
    };

    // 'cor' from ocean_core_meris_calib.nc4, bands 13, 14, 15
    final static double[][] MERIS_OCEAN_RECT_CORR = {
            {0.0, 1.0},
            {0.0, 1.0},
            {-0.0342265, 0.7524297}
    };

    // cwvl from land_core_meris_calib_arm.nc4, bands 13, 14, 15
    final static double[] MERIS_CWVL_RECT_CORR = {
            864.6143,
            884.6766,
            899.721
    };


    final static String[] MODIS_REFL_BAND_NAMES = new String[]{
            "EV_250_Aggr1km_RefSB_2",   // 859 nm
            "EV_500_Aggr1km_RefSB_5",   // 1240 nm    currently needed over land only!
            "EV_1KM_RefSB_17",          // 905 nm
            "EV_1KM_RefSB_18",          // 936 nm
            "EV_1KM_RefSB_19"           // 940 nm
    };

    final static String[] MODIS_LAND_REFL_WIN_BAND_NAMES = new String[]{
            "EV_250_Aggr1km_RefSB_2",   // 859 nm
            "EV_500_Aggr1km_RefSB_5",   // 1240 nm    currently needed over land only!
    };

    final static String[] MODIS_LAND_REFL_ABS_BAND_NAMES = new String[]{
            "EV_1KM_RefSB_17",          // 905 nm
            "EV_1KM_RefSB_18",          // 936 nm
            "EV_1KM_RefSB_19"           // 940 nm
    };

    final static String[] MODIS_OCEAN_REFL_WIN_BAND_NAMES = new String[]{
            "EV_250_Aggr1km_RefSB_2",   // 859 nm
    };

    final static String[] MODIS_OCEAN_REFL_ABS_BAND_NAMES = new String[]{
            "EV_1KM_RefSB_17",          // 905 nm
            "EV_1KM_RefSB_18",          // 936 nm
            "EV_1KM_RefSB_19"           // 940 nm
    };

    final static String[] MODIS_TPG_NAMES = new String[]{
            "SolarZenith",
            "SensorZenith",
            "SolarAzimuth",
            "SensorAzimuth",
             "latitude", "longitude"
    };

    // RSS total < 3% (https://modis.gsfc.nasa.gov/data/atbd/atbd_mod01.pdf)
    // 3% of 0.5, 0.5, 0.5, 0.5, 0.6
    // --> gives ~1% TCWV L2 uncertainty
    final static double[][] MODIS_LAND_SE = {
            {0.015, 0.0, 0.0, 0.0, 0.0},
            {0.0, 0.015, 0.0, 0.0, 0.0},
            {0.0, 0.0, 0.015, 0.0, 0.0},
            {0.0, 0.0, 0.0, 0.015, 0.0},
            {0.0, 0.0, 0.0, 0.0, 0.018}
    };

    // RSS total < 3% (https://modis.gsfc.nasa.gov/data/atbd/atbd_mod01.pdf)
    // 3% of 0.05, 0.05, 0.05, 0.05
    // --> gives ~1% TCWV L2 uncertainty
    final static double[][] MODIS_OCEAN_SE = {
            {0.0015, 0.0, 0.0, 0.0},
            {0.0, 0.0015, 0.0, 0.0},
            {0.0, 0.0, 0.0015, 0.0},
            {0.0, 0.0, 0.0, 0.0015}
    };

    // 'cor' from land_core_modis_terra_calib_arm.nc4, bands 2, 5, 17, 18, 19 in this sequence!
    final static double[][] MODIS_LAND_RECT_CORR = {
            {0.0, 1.0},
            {0.0, 1.0},
            {-0.0034669, 1.0203956},
            {0.00041882, 0.9378},
            {-0.0115824, 0.94457}
    };

    // 'cor' from ocean_core_modis_terra.nc4, bands 2, 5, 17, 18, 19 in this sequence!
    final static double[][] MODIS_OCEAN_RECT_CORR = {
            {0.0, 1.0},
            {0.0, 1.0},
            {0.0, 1.0},
            {0.0, 1.0},
            {0.0, 1.0}
    };

    // cwvl for MODIS bands 2, 5, 17, 18, 19 in this sequence, taken from MOD021KM product
    final static double[] MODIS_CWVL_RECT_CORR = {
            858.0,
            1240.0,
            905.0,
            936.0,
            940.0
    };

    final static String[] OLCI_REFL_BAND_NAMES = new String[]{
            "Oa18_reflectance",  // 884 nm
            "Oa19_reflectance",  // 899 nm
            "Oa20_reflectance",   // 939 nm
            "Oa21_reflectance"   // 1015 nm
    };

    final static String[] OLCI_LAND_REFL_WIN_BAND_NAMES = new String[]{
            "Oa18_reflectance",  // 884.3246 nm
            "Oa21_reflectance",  // 1015.966 nm
    };

    final static String[] OLCI_LAND_REFL_ABS_BAND_NAMES = new String[]{
            "Oa19_reflectance",   // 899.316 nm
            "Oa20_reflectance"    // 939.02 nm
    };

    final static String[] OLCI_OCEAN_REFL_WIN_BAND_NAMES = new String[]{
            "Oa18_reflectance",  // 884.3246 nm
    };

    final static String[] OLCI_OCEAN_REFL_ABS_BAND_NAMES = new String[]{
            "Oa19_reflectance",   // 899.316 nm
            "Oa20_reflectance"    // 939.02 nm
    };


    final static String[] OLCI_TPG_NAMES = new String[]{
            "SZA", "OZA", "SAA", "OAA", "TP_latitude", "TP_longitude"
    };

//    final static double[][] OLCI_LAND_SE = {
//            {0.0001, 0.0, 0.0, 0.0},
//            {0.0, 0.0001, 0.0, 0.0},
//            {0.0, 0.0, 0.001, 0.0},
//            {0.0, 0.0, 0.0, 0.001}
//    };
//
//    final static double[][] OLCI_OCEAN_SE = {
//            {0.0001, 0.0, 0.0},
//            {0.0, 0.0001, 0.0},
//            {0.0, 0.0, 0.001}
//    };

    // Introduce reasonable input reflectance uncertainty:
    // see https://sentinel.esa.int/web/sentinel/technical-guides/sentinel-3-olci/olci-instrument/specifications
    // 2% of 0.5 for band 18, 2% of 0.4 for band 19, 5% of 0.1 for band 20, 5% of 0.5 for band 21 --> gives ~5% TCWV L2 uncertainty ( < 2 kg/m^2)
    // Based on rough estimates from histograms in an ocean subset!
    final static double[][] OLCI_LAND_SE = {
            {0.01, 0.0, 0.0, 0.0},
            {0.0, 0.008, 0.0, 0.0},
            {0.0, 0.0, 0.005, 0.0},
            {0.0, 0.0, 0.0, 0.025}
    };

    // Introduce reasonable input reflectance uncertainty:
    // see https://sentinel.esa.int/web/sentinel/technical-guides/sentinel-3-olci/olci-instrument/specifications
    // 2% of 0.02 for band 18, 2% of 0.01 for band 19, 5% of 0.01 for band 20 --> gives ~5% TCWV L2 uncertainty
    // Based on rough estimates from histograms in an ocean subset!
    final static double[][] OLCI_OCEAN_SE = {
            {0.0004, 0.0, 0.0},
            {0.0, 0.0001, 0.0},
            {0.0, 0.0, 0.0005}
    };

    // 'cor' from land_core_olci_calib_arm.nc4, bands 18, 21, 19, 20 in this sequence!
    final static double[][] OLCI_LAND_RECT_CORR = {
            {0.0, 1.0},
            {0.0, 1.0},
            {0.00727, 0.94982},
            {0.002236, 0.8888878}
    };

    // 'cor' from ocean_core_olci_calib.nc4, bands 18, 21, 19, 20 in this sequence!
    final static double[][] OLCI_OCEAN_RECT_CORR = {
            {0.0, 1.0},
            {-2.22494E-4, 0.9156},
            {-5.7741E-4, 0.9211578}
    };

    // 'cwvl' from ocean_core_olci_calib.nc4, bands 18, 21, 19, 20 in this sequence!
    final static double[] OLCI_CWVL_RECT_CORR = {
            884.3246,
            1015.966,
            899.316,
            939.02
    };

    final static double[][] SA_LAND = {
            {20.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
    };

    final static double[][] SA_OCEAN = {
            {8.0, 0.0, 0.0},
            {0.0, 0.1, 0.0},
            {0.0, 0.0, 25.0}
    };


}
