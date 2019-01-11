package org.esa.snap.wvcci.tcwv.dataio.mod35;

/**
 * Constants for reading MODIS MOD35 L2 products
 *
 * @author olafd
 */
class ModisMod35L2Constants {

    static final String CLOUD_MASK_BAND_NAME = "Cloud_Mask";
    static final String CLOUD_MASK_BYTE_TARGET_BAND_NAME = "Cloud_Mask_Byte_Segment";

    static final String QUALITY_ASSURANCE_BAND_NAME = "Quality_Assurance";
    static final String QUALITY_ASSURANCE_QA_DIMENSION_BAND_NAME = "Quality_Assurance_QA_Dimension";

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
}
