package org.esa.snap.wvcci.tcwv.dataio.nc4;

/**
 * Constants for AVHRR-AC NetCDF4 output
 * todo: cleanup
 *
 * @author olafd
 */
public class Nc4Constants {

    public static final double AC_CORR_VALID_MIN_VALUE = -0.5;
    public static final double AC_CORR_VALID_MAX_VALUE = 1.5;
    public static final double UNCERTAINTY_VALID_MIN_VALUE = 0.0;
    public static final double UNCERTAINTY_VALID_MAX_VALUE = 1.5;
    public static final double LAT_VALID_MIN_VALUE = -90.0;
    public static final double LAT_VALID_MAX_VALUE = 90.0;
    public static final double LON_VALID_MIN_VALUE = -180.0;
    public static final double LON_VALID_MAX_VALUE = 180.0;
    public static final double SZA_VALID_MIN_VALUE = 0.0;
    public static final double SZA_VALID_MAX_VALUE = 90.0;
    public static final double VZA_VALID_MIN_VALUE = -180.0;
    public static final double VZA_VALID_MAX_VALUE = 180.0;
    public static final double SAA_VALID_MIN_VALUE = -180.0;
    public static final double SAA_VALID_MAX_VALUE = 360.0;
    public static final double VAA_VALID_MIN_VALUE = -180.0;
    public static final double VAA_VALID_MAX_VALUE = 360.0;
    public static final double QUALITY_VALID_MIN_VALUE = 0.0;
    public static final double QUALITY_VALID_MAX_VALUE = 1.0;
    public static final double NDVI_VALID_MIN_VALUE = -1.0;
    public static final double NDVI_VALID_MAX_VALUE = 1.0;
    public static final double GROUND_HEIGHT_VALID_MIN_VALUE = -1000.0;
    public static final double GROUND_HEIGHT_VALID_MAX_VALUE = 10000.0;

    public static final String LAT_BAND_NAME = "lat";
    public static final String LON_BAND_NAME = "lon";
    public static final String SZA_BAND_NAME = "sun_zenith";
    public static final String SAA_BAND_NAME = "sun_azimuth";
    public static final String VZA_BAND_NAME = "sat_zenith";
    public static final String VAA_BAND_NAME = "sat_azimuth";
    public static final String QUALITY_FLAG_BAND_NAME = "quality_flags_ac";
    public static final String GROUD_HEIGHT_BAND_NAME = "ground_height";

    public static final String NDVI_BAND_NAME = "NDVI_ac";
    public static final String L_PATH_B_BAND_NAME = "L_path_b";
    public static final String TRANS_DOWN_B_BAND_NAME = "trans_down_b";
    public static final String TRANS_UP_B_BAND_NAME = "trans_up_b";
    public static final String EG0_BAND_NAME = "Eg0_b";
    public static final String AOD_BAND_NAME = "AOD";
    public static final String AEROSOL_TYPE_INDEX_BAND_NAME = "aerosol_type_index";
    public static final String TCWV_BAND_NAME = "TC_WV";
    public static final String TCOZONE_BAND_NAME = "TC_Ozone";
    public static final String DEM_ALTITUDE_BAND_NAME = "DEM_altitude";
    public static final String DEM_SLOPE_BAND_NAME = "DEM_slope";

}
