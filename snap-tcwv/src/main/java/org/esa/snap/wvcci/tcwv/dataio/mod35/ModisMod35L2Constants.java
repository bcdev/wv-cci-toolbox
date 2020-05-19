package org.esa.snap.wvcci.tcwv.dataio.mod35;

import java.awt.*;

/**
 * Constants for reading MODIS MOD35 or MYD35 L2 products
 *
 * @author olafd
 */
public class ModisMod35L2Constants {

    public static final String CLOUD_MASK_BYTE_TARGET_BAND_NAME = "Cloud_Mask_Byte_Segment";
    public static final String QUALITY_ASSURANCE_QA_DIMENSION_BAND_NAME = "Quality_Assurance_QA_Dimension";

    public static final int CLEAR_CERTAIN_BIT_INDEX = 4;
    public static final int SNOW_ICE_BIT_INDEX = 7;
    public static final int COASTAL_BIT_INDEX = 9;
    public static final int DESERT_BIT_INDEX = 10;
    public static final int LAND_BIT_INDEX = 11;

    static final int CLOUD_DETERMINED_BIT_INDEX = 0;
    static final int CLOUD_CERTAIN_BIT_INDEX = 1;
    static final int CLOUD_UNCERTAIN_BIT_INDEX = 2;
    static final int CLOUD_PROBABLY_CLEAR_BIT_INDEX = 3;
    static final int CLOUD_CONFIDENT_CLEAR_BIT_INDEX = 4;
    static final int DAYTIME_BIT_INDEX = 5;
    static final int GLINT_BIT_INDEX = 6;
    static final int WATER_BIT_INDEX = 8;

    static final String CLOUD_MASK_BAND_NAME = "Cloud_Mask";
    static final String QUALITY_ASSURANCE_BAND_NAME = "Quality_Assurance";

    static final String CELL_ACROSS_SWATH_1KM_DIM_NAME = "Cell_Across_Swath_1km";
    static final String CELL_ACROSS_SWATH_5KM_DIM_NAME = "Cell_Across_Swath_5km";
    static final String CELL_ALONG_SWATH_1KM_DIM_NAME = "Cell_Along_Swath_1km";
    static final String CELL_ALONG_SWATH_5KM_DIM_NAME = "Cell_Along_Swath_5km";
    static final String BYTE_SEGMENT_DIM_NAME = "Byte_Segment";
    static final String QA_DIM_NAME = "QA_Dimension";

    static final String MOD35_l2_PRODUCT_TYPE = "MOD35_L2";
    static final String MOD35_l2_PRODUCT_DESCR = "MODIS MOD35 L2 Product";

    static final String MPH_NAME = "MPH";
    static final String GEOLOCATION_FIELDS_GROUP_NAME = "Geolocation Fields";
    static final String DATA_FIELDS_GROUP_NAME = "Data Fields";

    static final int CHAR_NO_DATA_VALUE = -1;

    static final String PIXEL_CLASSIF_FLAG_BAND_NAME = "pixel_classif_flags";
    static final String QA_FLAG_BAND_NAME = "quality_assurance_flags";

    static final String CLOUD_DETERMINED_FLAG_NAME = "CLOUD_DETERMINED";
    static final String CLOUD_CERTAIN_FLAG_NAME = "CLOUD_CERTAINLY";
    static final String CLOUD_UNCERTAIN_FLAG_NAME = "CLOUD_PROBABLY";
    static final String PROBABLY_CLEAR_FLAG_NAME = "CLEAR_PROBABLY";
    static final String CONFIDENT_CLEAR_FLAG_NAME = "CLEAR_CERTAINLY";
    static final String DAYTIME_FLAG_NAME = "DAYTIME";
    static final String GLINT_FLAG_NAME = "GLINT";
    static final String SNOW_ICE_FLAG_NAME = "SNOW_ICE";
    static final String WATER_FLAG_NAME = "WATER";
    static final String COASTAL_FLAG_NAME = "COAST";
    static final String DESERT_FLAG_NAME = "DESERT";
    static final String LAND_FLAG_NAME = "LAND";

    static final Color[] PIXEL_CLASSIF_COLORS = {
            new Color(120, 255, 180),
            new Color(255, 255, 0),
            new Color(255, 255, 180),
            new Color(180, 255, 255),
            new Color(0, 255, 255),
            new Color(200, 200, 200),
            new Color(255, 100, 0),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(180, 180, 255),
            new Color(255, 150, 100),
            new Color(0, 255, 0)
    };

    static final Color[] QA_COLORS = {
            Color.green,
            new Color(245, 245, 255),
            new Color(210, 210, 255),
            new Color(175, 175, 255),
            new Color(140, 140, 255),
            new Color(105, 105, 255),
            new Color(70, 70, 255),
            new Color(35, 35, 255),
            Color.blue
    };


    static final String CLOUD_DETERMINED_FLAG_DESCR = "Cloud mask was determined for this pixel";
    static final String CLOUD_CERTAIN_FLAG_DESCR = "Certainly cloudy pixel";
    static final String CLOUD_UNCERTAIN_FLAG_DESCR = "Probably cloudy pixel";
    static final String PROBABLY_CLEAR_FLAG_DESCR = "Probably clear pixel";
    static final String CERTAINLY_CLEAR_FLAG_DESCR = "Certainly clear pixel";
    static final String DAYTIME_FLAG_DESCR = "Daytime pixel";
    static final String GLINT_FLAG_DESCR = "Glint pixel";
    static final String SNOW_ICE_FLAG_DESCR = "Snow/ice pixel";
    static final String WATER_FLAG_DESCR = "Water pixel";
    static final String COASTAL_FLAG_DESCR = "Coastal pixel";
    static final String DESERT_FLAG_DESCR = "Desert pixel";
    static final String LAND_FLAG_DESCR = "Clear land pixel (no desert or snow)";

    static final int CLOUD_MASK_USEFUL_BIT_INDEX = 0;
    static final int NUM_CLOUD_MASK_CONFIDENCE_LEVELS = 8;
    static final String CLOUD_MASK_USEFUL_FLAG_NAME = "CLOUD_MASK_USEFUL";

    private static final int CLOUD_MASK_CONFIDENCE_LEVEL1_BIT_INDEX = 1;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL2_BIT_INDEX = 2;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL3_BIT_INDEX = 3;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL4_BIT_INDEX = 4;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL5_BIT_INDEX = 5;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL6_BIT_INDEX = 6;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL7_BIT_INDEX = 7;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL8_BIT_INDEX = 8;
    static final int[] CLOUD_MASK_CONFIDENCE_LEVEL_BIT_INDICES = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL2_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL3_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL4_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL5_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL6_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL7_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL8_BIT_INDEX
    };

    private static final String CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_1";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_2";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_3";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_4";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_5";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_6";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_7";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_8";

    static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME
    };

    static final String CLOUD_MASK_USEFUL_FLAG_DESCR = "Cloud mask determination was useful";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR = "Cloud mask has confidence level 1";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR = "Cloud mask has confidence level 2";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR = "Cloud mask has confidence level 3";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR = "Cloud mask has confidence level 4";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR = "Cloud mask has confidence level 5";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR = "Cloud mask has confidence level 6";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR = "Cloud mask has confidence level 7";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR = "Cloud mask has confidence level 8";

    static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR
    };

}
