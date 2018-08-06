package org.esa.snap.wvcci.tcwv;

/**
 * Constants for TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvConstants {

    public static final String IDEPIX_CLASSIF_BAND_NAME = "pixel_classif_flags";
    public static final String TCWV_BAND_NAME = "tcwv";

    public static final String PRIOR_T2M_BAND_NAME = "t2m";
    public static final String PRIOR_MSL_BAND_NAME = "msl";
    public static final String PRIOR_TCWV_BAND_NAME = "tcwv";
    public static final String PRIOR_WS_BAND_NAME = "ws";

    public static final double AOT865_INIT_VALUE = 0.15;  // AOT 865nm initial value for algorithm
    public static final double AL0_INIT_VALUE = 0.13;     // AL0 865nm initial value for algorithm
    public static final double AL1_INIT_VALUE = 0.13;     // AL1 865nm initial value for algorithm
    public static final double TCWV_INIT_VALUE = 30.0;    // TCWV initial value for algorithm if we have no prior
    public static final double WS_INIT_VALUE = 7.5;       // Windspeed initial value for algorithm if we have no prior

    public static int IDEPIX_INVALID_BIT = 0;
    public static int IDEPIX_CLOUD_BIT = 1;
    public static int IDEPIX_CLOUD_AMBIGUOUS_BIT = 2;
    public static int IDEPIX_CLOUD_SURE_BIT = 3;
    public static int IDEPIX_CLOUD_BUFFER_BIT = 4;
    public static int IDEPIX_CLOUD_SHADOW_BIT = 5;
    public static int IDEPIX_LAND_BIT = 10;

    static final String MERIS_LAND_LUT_NC_FILENAME = "land_core_meris.nc4";
    static final String MERIS_OCEAN_LUT_NC_FILENAME = "ocean_core_meris.nc4";
    static final String MODIS_AQUA_OCEAN_LUT_NC_FILENAME = "ocean_core_modis_aqua.nc4";
    static final String MODIS_TERRA_OCEAN_LUT_NC_FILENAME = "ocean_core_modis_terra.nc4";
//    static final String MODIS_AQUA_LAND_LUT_NC_FILENAME = "land_core_modis_aqua.nc4";
    static final String MODIS_AQUA_LAND_LUT_NC_FILENAME = "land_core_meris.nc4";
    static final String MODIS_TERRA_LAND_LUT_NC_FILENAME = "land_core_modis_terra.nc4";

    final static String[] MERIS_REFL_BAND_NAMES = new String[]{
            "reflectance_13", // 864 nm
            "reflectance_14", // 884 nm
            "reflectance_15"  // 900 nm
    };

    final static String[] MERIS_REFL_WIN_BAND_NAMES = new String[]{
            "reflectance_13", // 864 nm
            "reflectance_14", // 884 nm
    };

    final static String[] MERIS_REFL_ABS_BAND_NAMES = new String[]{
            "reflectance_15"  // 900 nm
    };

    final static String[] MERIS_TPG_NAMES = new String[]{
            "sun_zenith",
            "view_zenith",
            "sun_azimuth",
            "view_azimuth"
    };

    final static double[][] MERIS_SE = {
            {0.0001, 0.0, 0.0},
            {0.0, 0.0001, 0.0},
            {0.0, 0.0, 0.001}
    };


    final static String[] MODIS_REFL_BAND_NAMES = new String[]{
            "EV_250_Aggr1km_RefSB_2",   // 859 nm
            "EV_500_Aggr1km_RefSB_5",   // 1240 nm
            "EV_1KM_RefSB_17",          // 905 nm
            "EV_1KM_RefSB_18",          // 936 nm
            "EV_1KM_RefSB_19"           // 940 nm
    };

    final static String[] MODIS_REFL_WIN_BAND_NAMES = new String[]{
            "EV_250_Aggr1km_RefSB_2",   // 859 nm
    };

    final static String[] MODIS_REFL_ABS_BAND_NAMES = new String[]{
            "EV_1KM_RefSB_17",          // 905 nm
            "EV_1KM_RefSB_18",          // 936 nm
            "EV_1KM_RefSB_19"           // 940 nm
    };

    final static String[] MODIS_TPG_NAMES = new String[]{
            "SolarZenith",
            "SensorZenith",
            "SolarAzimuth",
            "SensorAzimuth"
    };

    final static double[][] MODIS_SE = {
            {0.0001, 0.0, 0.0, 0.0},
            {0.0, 0.001, 0.0, 0.0},
            {0.0, 0.0, 0.001, 0.0},
            {0.0, 0.0, 0.0, 0.001}
    };

    final static String[] OLCI_REFL_BAND_NAMES = new String[]{
            "Oa17_reflectance",  // 864 nm
            "Oa18_reflectance",  // 884 nm
            "Oa19_reflectance"   // 900 nm 
    };

    final static String[] OLCI_REFL_WIN_BAND_NAMES = new String[]{
            "Oa17_reflectance",  // 864 nm
            "Oa18_reflectance",  // 884 nm
    };

    final static String[] OLCI_REFL_ABS_BAND_NAMES = new String[]{
            "Oa19_reflectance"   // 900 nm
    };

    final static String[] OLCI_TPG_NAMES = new String[]{
            "SZA", "OZA", "SAA", "OAA"
    };

    final static double[][] OLCI_SE = {
            {0.0001, 0.0, 0.0},
            {0.0, 0.0001, 0.0},
            {0.0, 0.0, 0.001}
    };

    public final static double[][] SA_LAND = {
            {20.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
    };

    public final static double[][] SA_OCEAN = {
            {8.0, 0.0, 0.0},
            {0.0, 0.1, 0.0},
            {0.0, 0.0, 25.0}
    };


}
