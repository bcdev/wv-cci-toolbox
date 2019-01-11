package org.esa.snap.wvcci.tcwv.dataio.mod35;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;

/**
 * MOD35 L2 flags and bit masks utilities
 *
 * @author olafd
 */
class Mod35L2CloudMaskUtils {

    static final int CLOUD_DETERMINED_BIT_INDEX = 0;
    static final int CLOUD_CERTAIN_BIT_INDEX = 1;
    static final int CLOUD_UNCERTAIN_BIT_INDEX = 2;
    static final int CLOUD_PROBABLY_CLEAR_BIT_INDEX = 3;
    static final int CLOUD_CONFIDENT_CLEAR_BIT_INDEX = 4;
    static final int DAYTIME_BIT_INDEX = 5;
    static final int GLINT_BIT_INDEX = 6;
    static final int SNOW_ICE_BIT_INDEX = 7;

    static final int WATER_BIT_INDEX = 8;
    static final int COASTAL_BIT_INDEX = 9;
    static final int DESERT_BIT_INDEX = 10;
    static final int LAND_BIT_INDEX = 11;

    private static final String PIXEL_CLASSIF_FLAG_BAND_NAME = "pixel_classif_flags";
    private static final String QA_FLAG_BAND_NAME = "quality_assurance_flags";

    private static final String CLOUD_DETERMINED_FLAG_NAME = "CLOUD_DETERMINED";
    private static final String CLOUD_CERTAIN_FLAG_NAME = "CLOUD_CERTAINLY";
    private static final String CLOUD_UNCERTAIN_FLAG_NAME = "CLOUD_PROBABLY";
    private static final String PROBABLY_CLEAR_FLAG_NAME = "CLEAR_PROBABLY";
    private static final String CONFIDENT_CLEAR_FLAG_NAME = "CLEAR_CERTAINLY";
    private static final String DAYTIME_FLAG_NAME = "DAYTIME";
    private static final String GLINT_FLAG_NAME = "GLINT";
    private static final String SNOW_ICE_FLAG_NAME = "SNOW_ICE";
    private static final String WATER_FLAG_NAME = "WATER";
    private static final String COASTAL_FLAG_NAME = "COAST";
    private static final String DESERT_FLAG_NAME = "DESERT";
    private static final String LAND_FLAG_NAME = "LAND";

    private static final Color[] PIXEL_CLASSIF_COLORS = {
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

    private static final Color[] QA_COLORS = {
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


    private static final String CLOUD_DETERMINED_FLAG_DESCR = "Cloud mask was determined for this pixel";
    private static final String CLOUD_CERTAIN_FLAG_DESCR = "Certainly cloudy pixel";
    private static final String CLOUD_UNCERTAIN_FLAG_DESCR = "Probably cloudy pixel";
    private static final String PROBABLY_CLEAR_FLAG_DESCR = "Probably clear pixel";
    private static final String CERTAINLY_CLEAR_FLAG_DESCR = "Certainly clear pixel";
    private static final String DAYTIME_FLAG_DESCR = "Daytime pixel";
    private static final String GLINT_FLAG_DESCR = "Glint pixel";
    private static final String SNOW_ICE_FLAG_DESCR = "Snow/ice pixel";
    private static final String WATER_FLAG_DESCR = "Water pixel";
    private static final String COASTAL_FLAG_DESCR = "Coastal pixel";
    private static final String DESERT_FLAG_DESCR = "Desert pixel";
    private static final String LAND_FLAG_DESCR = "Clear land pixel (no desert or snow)";

    static final int CLOUD_MASK_USEFUL_BIT_INDEX = 0;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL1_BIT_INDEX = 1;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL2_BIT_INDEX = 2;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL3_BIT_INDEX = 3;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL4_BIT_INDEX = 4;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL5_BIT_INDEX = 5;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL6_BIT_INDEX = 6;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL7_BIT_INDEX = 7;
    private static final int CLOUD_MASK_CONFIDENCE_LEVEL8_BIT_INDEX = 8;

    static final int NUM_CLOUD_MASK_CONFIDENCE_LEVELS = 8;
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

    private static final String CLOUD_MASK_USEFUL_FLAG_NAME = "CLOUD_MASK_USEFUL";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_1";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_2";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_3";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_4";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_5";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_6";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_7";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_8";

    private static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME,
            CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME
    };

    private static final String CLOUD_MASK_USEFUL_FLAG_DESCR = "Cloud mask determination was useful";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR = "Cloud mask has confidence level 1";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR = "Cloud mask has confidence level 2";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR = "Cloud mask has confidence level 3";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR = "Cloud mask has confidence level 4";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR = "Cloud mask has confidence level 5";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR = "Cloud mask has confidence level 6";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR = "Cloud mask has confidence level 7";
    private static final String CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR = "Cloud mask has confidence level 8";

    private static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR,
            CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR
    };

    /**
     * Attaches pixel classification flag band to original MOD35 L2 product
     *
     * @param mod35L2Product - the MOD35 L2 product
     *
     */
    static void attachPixelClassificationFlagBand(Product mod35L2Product) {
        FlagCoding mod35L2FlagCoding = new FlagCoding(PIXEL_CLASSIF_FLAG_BAND_NAME);

        mod35L2FlagCoding.addFlag(CLOUD_DETERMINED_FLAG_NAME, BitSetter.setFlag(0, CLOUD_DETERMINED_BIT_INDEX),
                CLOUD_DETERMINED_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(CLOUD_CERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CERTAIN_BIT_INDEX),
                CLOUD_CERTAIN_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(CLOUD_UNCERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_UNCERTAIN_BIT_INDEX),
                CLOUD_UNCERTAIN_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(PROBABLY_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_PROBABLY_CLEAR_BIT_INDEX),
                PROBABLY_CLEAR_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(CONFIDENT_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CONFIDENT_CLEAR_BIT_INDEX),
                CERTAINLY_CLEAR_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(DAYTIME_FLAG_NAME, BitSetter.setFlag(0, DAYTIME_BIT_INDEX),
                DAYTIME_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(GLINT_FLAG_NAME, BitSetter.setFlag(0, GLINT_BIT_INDEX),
                GLINT_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(SNOW_ICE_FLAG_NAME, BitSetter.setFlag(0, SNOW_ICE_BIT_INDEX),
                SNOW_ICE_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(WATER_FLAG_NAME, BitSetter.setFlag(0, WATER_BIT_INDEX),
                WATER_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(COASTAL_FLAG_NAME, BitSetter.setFlag(0, COASTAL_BIT_INDEX),
                COASTAL_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(DESERT_FLAG_NAME, BitSetter.setFlag(0, DESERT_BIT_INDEX),
                DESERT_FLAG_DESCR);
        mod35L2FlagCoding.addFlag(LAND_FLAG_NAME, BitSetter.setFlag(0, LAND_BIT_INDEX),
                LAND_FLAG_DESCR);

        ProductNodeGroup<Mask> maskGroup = mod35L2Product.getMaskGroup();

        int colorIndex = 0;
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, CLOUD_DETERMINED_FLAG_NAME,
                CLOUD_DETERMINED_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, CLOUD_CERTAIN_FLAG_NAME,
                CLOUD_CERTAIN_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, CLOUD_UNCERTAIN_FLAG_NAME,
                CLOUD_UNCERTAIN_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, PROBABLY_CLEAR_FLAG_NAME,
                PROBABLY_CLEAR_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, CONFIDENT_CLEAR_FLAG_NAME,
                CERTAINLY_CLEAR_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, DAYTIME_FLAG_NAME,
                DAYTIME_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, GLINT_FLAG_NAME,
                GLINT_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, SNOW_ICE_FLAG_NAME,
                SNOW_ICE_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, WATER_FLAG_NAME,
                WATER_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, COASTAL_FLAG_NAME,
                COASTAL_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, DESERT_FLAG_NAME,
                DESERT_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++]);
        addMask(mod35L2Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, LAND_FLAG_NAME,
                LAND_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex]);

        mod35L2Product.getFlagCodingGroup().add(mod35L2FlagCoding);
        final Band pixelClassifBand = mod35L2Product.addBand(PIXEL_CLASSIF_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        pixelClassifBand.setDescription("MOD35 pixel classification");
        pixelClassifBand.setUnit("dl");
        pixelClassifBand.setSampleCoding(mod35L2FlagCoding);

        Mod35L2CloudMaskOp bitMaskOp = new Mod35L2CloudMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", mod35L2Product);
        bitMaskOp.setParameter("srcBandName", "Cloud_Mask_Byte_Segment1");
        bitMaskOp.setParameter("trgBandName", PIXEL_CLASSIF_FLAG_BAND_NAME);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        pixelClassifBand.setSourceImage(bitMaskProduct.getBand(PIXEL_CLASSIF_FLAG_BAND_NAME).getSourceImage());
    }

    /**
     * Attaches quality assurance flag band to original MOD35 L2 product
     *
     * @param mod35L2Product - the MOD35 L2 product
     *
     */
    static void attachQualityAssuranceFlagBand(Product mod35L2Product) {
        FlagCoding mod35FC = new FlagCoding(QA_FLAG_BAND_NAME);

        mod35FC.addFlag(CLOUD_MASK_USEFUL_FLAG_NAME, BitSetter.setFlag(0, CLOUD_MASK_USEFUL_BIT_INDEX),
                CLOUD_MASK_USEFUL_FLAG_DESCR);
        for (int i = 0; i < NUM_CLOUD_MASK_CONFIDENCE_LEVELS; i++) {
            mod35FC.addFlag(CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES[i],
                    BitSetter.setFlag(0, CLOUD_MASK_CONFIDENCE_LEVEL_BIT_INDICES[i]),
                    CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS[i]);
        }

        ProductNodeGroup<Mask> maskGroup = mod35L2Product.getMaskGroup();
        addMask(mod35L2Product, maskGroup, QA_FLAG_BAND_NAME, CLOUD_MASK_USEFUL_FLAG_NAME,
                CLOUD_MASK_USEFUL_FLAG_DESCR, QA_COLORS[0]);
        for (int i = 0; i < NUM_CLOUD_MASK_CONFIDENCE_LEVELS; i++) {
            addMask(mod35L2Product, maskGroup, QA_FLAG_BAND_NAME, CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES[i],
                    CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS[i], QA_COLORS[i + 1]);
        }

        mod35L2Product.getFlagCodingGroup().add(mod35FC);
        final Band qaBand = mod35L2Product.addBand(QA_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        qaBand.setDescription("MOD35 quality assurance");
        qaBand.setUnit("dl");
        qaBand.setSampleCoding(mod35FC);

        Mod35L2CloudMaskOp bitMaskOp = new Mod35L2CloudMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", mod35L2Product);
        bitMaskOp.setParameter("srcBandName", "Quality_Assurance_QA_Dimension1");
        bitMaskOp.setParameter("trgBandName", QA_FLAG_BAND_NAME);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        qaBand.setSourceImage(bitMaskProduct.getBand(QA_FLAG_BAND_NAME).getSourceImage());
    }

    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                description, width, height,
                bandName + "." + flagName,
                color, 0.5);
        maskGroup.add(mask);
    }


}
