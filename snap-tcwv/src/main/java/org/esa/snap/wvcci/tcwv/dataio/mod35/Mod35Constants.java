package org.esa.snap.wvcci.tcwv.dataio.mod35;

import java.awt.*;

/**
 * Constants for reading Proba-V products
 *
 * @author olafd
 */
public class Mod35Constants {

    public static final String CLOUD_MASK_BAND_NAME = "Cloud_Mask";
    public static final String CLOUD_MASK_BYTE1_TARGET_BAND_NAME = "Cloud_Mask_Byte_Segment1";
    public static final String CLOUD_MASK_BYTE2_TARGET_BAND_NAME = "Cloud_Mask_Byte_Segment2";
    public static final String CLOUD_MASK_BYTE_TARGET_BAND_NAME = "Cloud_Mask_Byte_Segment";
    public static final String CLOUD_FLAG_BAND_NAME = "CLOUD_FLAGS";

    public static final String CELL_ACROSS_SWATH_1KM_DIM_NAME = "Cell_Across_Swath_1km";
    public static final String CELL_ACROSS_SWATH_5KM_DIM_NAME = "Cell_Across_Swath_5km";
    public static final String CELL_ALONG_SWATH_1KM_DIM_NAME = "Cell_Along_Swath_1km";
    public static final String CELL_ALONG_SWATH_5KM_DIM_NAME = "Cell_Along_Swath_5km";
    public static final String BYTE_SEGMENT_DIM_NAME = "Byte_Segment";
    public static final String QA_DIM_NAME = "QA_Dimension";

    public static final int CLOUD_CLEAR_BIT_INDEX = 0;
    public static final int CLOUD_UNDEFINED_BIT_INDEX = 1;
    public static final int CLOUD_CLOUD_BIT_INDEX = 2;
    public static final int CLOUD_SNOWICE_INDEX = 3;
    public static final int CLOUD_CLOUD_SHADOW_BIT_INDEX = 4;
    public static final int CLOUD_LAND_BIT_INDEX = 5;
    public static final int CLOUD_GOOD_SWIR_INDEX = 6;
    public static final int CLOUD_GOOD_NIR_BIT_INDEX = 7;
    public static final int CLOUD_GOOD_RED_BIT_INDEX = 8;
    public static final int CLOUD_GOOD_BLUE_BIT_INDEX = 9;
    public static final int CLOUD_SWIR_COVERAGE_INDEX = 10;
    public static final int CLOUD_NIR_COVERAGE_BIT_INDEX = 11;
    public static final int CLOUD_RED_COVERAGE_BIT_INDEX = 12;
    public static final int CLOUD_BLUE_COVERAGE_BIT_INDEX = 13;

    public static final String CLOUD_CLEAR_FLAG_NAME = "CLEAR";
    public static final String CLOUD_UNDEFINED_FLAG_NAME = "UNDEFINED";
    public static final String CLOUD_CLOUD_FLAG_NAME = "CLOUD";
    public static final String CLOUD_SNOWICE_FLAG_NAME = "SNOWICE";
    public static final String CLOUD_CLOUD_SHADOW_FLAG_NAME = "CLOUD_SHADOW";
    public static final String CLOUD_LAND_FLAG_NAME = "LAND";
    public static final String CLOUD_GOOD_SWIR_FLAG_NAME = "GOOD_SWIR";
    public static final String CLOUD_GOOD_NIR_FLAG_NAME = "GOOD_NIR";
    public static final String CLOUD_GOOD_RED_FLAG_NAME = "GOOD_RED";
    public static final String CLOUD_GOOD_BLUE_FLAG_NAME = "GOOD_BLUE";
    public static final String CLOUD_SWIR_COVERAGE_FLAG_NAME = "SWIR_COVERAGE";
    public static final String CLOUD_NIR_COVERAGE_FLAG_NAME = "NIR_COVERAGE";
    public static final String CLOUD_RED_COVERAGE_FLAG_NAME = "RED_COVERAGE";
    public static final String CLOUD_BLUE_COVERAGE_FLAG_NAME = "BLUE_COVERAGE";

    public static final String CLOUD_CLEAR_FLAG_DESCR = "Clear pixel";
    public static final String CLOUD_UNDEFINED_FLAG_DESCR = "Pixel classified as undefined";
    public static final String CLOUD_CLOUD_FLAG_DESCR = "Cloudy pixel";
    public static final String CLOUD_SNOWICE_FLAG_DESCR = "Snow or ice pixel";
    public static final String CLOUD_CLOUD_SHADOW_FLAG_DESCR = "Cloud shadow pixel";
    public static final String CLOUD_LAND_FLAG_DESCR = "Land pixel";
    public static final String CLOUD_GOOD_SWIR_FLAG_DESCR = "Pixel with good SWIR data";
    public static final String CLOUD_GOOD_NIR_FLAG_DESCR = "Pixel with good NIR data";
    public static final String CLOUD_GOOD_RED_FLAG_DESCR = "Pixel with good RED data";
    public static final String CLOUD_GOOD_BLUE_FLAG_DESCR = "Pixel with good BLUE data";
    public static final String CLOUD_SWIR_COVERAGE_FLAG_DESCR = "Pixel with SWIR coverage";
    public static final String CLOUD_NIR_COVERAGE_FLAG_DESCR = "Pixel with NIR coverage";
    public static final String CLOUD_RED_COVERAGE_FLAG_DESCR = "Pixel with RED coverage";
    public static final String CLOUD_BLUE_COVERAGE_FLAG_DESCR = "Pixel with BLUE coverage";

    public static final Color[] FLAG_COLORS = {
            new Color(120, 255, 180),
            new Color(255, 255, 0),
            new Color(0, 255, 255),
            new Color(255, 100, 0),
            new Color(255, 255, 180),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(180, 180, 255),
            new Color(255, 150, 100),
            new Color(0, 255, 0),
            new Color(0, 120, 180),
            new Color(180, 0, 255),
            new Color(130, 200, 250),
            new Color(50, 50, 100)
    };

    public static final String MOD35_l2_PRODUCT_TYPE = "MOD35_L2";
    public static final String MOD35_l2_PRODUCT_DESCR = "MODIS MOD35 L2 Product";

    public static final String PROBAV_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String MPH_NAME = "MPH";
    public static final String GEOLOCATION_FIELDS_GROUP_NAME = "Geolocation Fields";
    public static final String DATA_FIELDS_GROUP_NAME = "Data Fields";

    public static final float GEOMETRY_NO_DATA_VALUE = Float.NaN;
    public static final float NDVI_NO_DATA_VALUE = Float.NaN;
    public static final int CLOUD_MASK_NO_DATA_VALUE = -1;
    public static final int TIME_NO_DATA_VALUE_UINT16 = 0;
    public static final int TIME_NO_DATA_VALUE_UINT8 = 255;

    public static final int[] RADIOMETRY_CHILD_INDEX = {0, 2, 1, 3};
}
