package org.esa.snap.wvcci.tcwv.dataio.mod35;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;

/**
 * Flags and bit masks utilities
 *
 * @author olafd
 */
public class Mod35BitMaskUtils {

    public static final String PIXEL_CLASSIF_FLAG_BAND_NAME = "pixel_classif_flags";
    public static final String QA_FLAG_BAND_NAME = "quality_assurance_flags";

    public static final int CLOUD_DETERMINED_BIT_INDEX = 0;
    public static final int CLOUD_CERTAIN_BIT_INDEX = 1;
    public static final int CLOUD_UNCERTAIN_BIT_INDEX = 2;
    public static final int CLOUD_PROBABLY_CLEAR_BIT_INDEX = 3;
    public static final int CLOUD_CONFIDENT_CLEAR_BIT_INDEX = 4;
    public static final int DAYTIME_BIT_INDEX = 5;
    public static final int GLINT_BIT_INDEX = 6;
    public static final int SNOW_ICE_BIT_INDEX = 7;
    public static final int WATER_BIT_INDEX = 8;
    public static final int COASTAL_BIT_INDEX = 9;
    public static final int DESERT_BIT_INDEX = 10;
    public static final int LAND_BIT_INDEX = 11;

    private static final String MOD35_CLOUD_DETERMINED_FLAG_NAME = "CLOUD_DETERMINED";
    public static final String MOD35_CLOUD_CERTAIN_FLAG_NAME = "CLOUD_CERTAINLY";
    public static final String MOD35_CLOUD_UNCERTAIN_FLAG_NAME = "CLOUD_PROBABLY";
    public static final String MOD35_PROBABLY_CLEAR_FLAG_NAME = "CLEAR_PROBABLY";
    public static final String MOD35_CONFIDENT_CLEAR_FLAG_NAME = "CLEAR_CERTAINLY";
    public static final String MOD35_DAYTIME_FLAG_NAME = "DAYTIME";
    public static final String MOD35_GLINT_FLAG_NAME = "GLINT";
    public static final String MOD35_SNOW_ICE_FLAG_NAME = "SNOW_ICE";
    public static final String MOD35_WATER_FLAG_NAME = "WATER";
    public static final String MOD35_COASTAL_FLAG_NAME = "COAST";
    public static final String MOD35_DESERT_FLAG_NAME = "DESERT";
    public static final String MOD35_LAND_FLAG_NAME = "LAND";

    public static final Color[] PIXEL_CLASSIF_COLORS = {
        new Color(120,255,180),
        new Color(255,255,0),
        new Color(255,255,180),
        new Color(180,255,255),
        new Color(0,255,255),
        new Color(200,200,200),
        new Color(255,100,0),
        new Color(255,0,255),
        new Color(0,0,255),
        new Color(180,180,255),
        new Color(255,150,100),
        new Color(0,255,0)
    };

    public static final Color[] QA_COLORS = {
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


    public static final String MOD35_CLOUD_DETERMINED_FLAG_DESCR = "Cloud mask was determined for this pixel";
    public static final String MOD35_CLOUD_CERTAIN_FLAG_DESCR = "Certainly cloudy pixel";
    public static final String MOD35_CLOUD_UNCERTAIN_FLAG_DESCR = "Probably cloudy pixel";
    public static final String MOD35_PROBABLY_CLEAR_FLAG_DESCR = "Probably clear pixel";
    public static final String MOD35_CERTAINLY_CLEAR_FLAG_DESCR = "Certainly clear pixel";
    public static final String MOD35_DAYTIME_FLAG_DESCR = "Daytime pixel";
    public static final String MOD35_GLINT_FLAG_DESCR = "Glint pixel";
    public static final String MOD35_SNOW_ICE_FLAG_DESCR = "Snow/ice pixel";
    public static final String MOD35_WATER_FLAG_DESCR = "Water pixel";
    public static final String MOD35_COASTAL_FLAG_DESCR = "Coastal pixel";
    public static final String MOD35_DESERT_FLAG_DESCR = "Desert pixel";
    public static final String MOD35_LAND_FLAG_DESCR = "Land pixel";

    public static final int CLOUD_MASK_USEFUL_BIT_INDEX = 0;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL1_BIT_INDEX = 1;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL2_BIT_INDEX = 2;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL3_BIT_INDEX = 3;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL4_BIT_INDEX = 4;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL5_BIT_INDEX = 5;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL6_BIT_INDEX = 6;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL7_BIT_INDEX = 7;
    public static final int CLOUD_MASK_CONFIDENCE_LEVEL8_BIT_INDEX = 8;

    public static final int NUM_CLOUD_MASK_CONFIDENCE_LEVELS = 8;

    public static final int[] CLOUD_MASK_CONFIDENCE_LEVEL_BIT_INDICES = {
            CLOUD_MASK_CONFIDENCE_LEVEL1_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL2_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL3_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL4_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL5_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL6_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL7_BIT_INDEX,
            CLOUD_MASK_CONFIDENCE_LEVEL8_BIT_INDEX
    };

    private static final String MOD35_CLOUD_MASK_USEFUL_FLAG_NAME = "CLOUD_MASK_USEFUL";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_1";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_2";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_3";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_4";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_5";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_6";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_7";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME = "CLOUD_MASK_CONFIDENCE_LEVEL_8";

    public static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES = {
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_NAME,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_NAME
    };

    private static final String MOD35_CLOUD_MASK_USEFUL_FLAG_DESCR = "Cloud mask determination was useful";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR = "Cloud mask has confidence level 1";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR = "Cloud mask has confidence level 2";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR = "Cloud mask has confidence level 3";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR = "Cloud mask has confidence level 4";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR = "Cloud mask has confidence level 5";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR = "Cloud mask has confidence level 6";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR = "Cloud mask has confidence level 7";
    private static final String MOD35_CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR = "Cloud mask has confidence level 8";

    public static final String[] CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS = {
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL1_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL2_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL3_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL4_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL5_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL6_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL7_FLAG_DESCR,
            MOD35_CLOUD_MASK_CONFIDENCE_LEVEL8_FLAG_DESCR
    };


    public static void attachPixelClassificationFlagBand(Product mod35Product) {
        FlagCoding mod35FC = new FlagCoding(PIXEL_CLASSIF_FLAG_BAND_NAME);

        mod35FC.addFlag(MOD35_CLOUD_DETERMINED_FLAG_NAME, BitSetter.setFlag(0, CLOUD_DETERMINED_BIT_INDEX),
                        MOD35_CLOUD_DETERMINED_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CLOUD_CERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CERTAIN_BIT_INDEX),
                        MOD35_CLOUD_CERTAIN_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CLOUD_UNCERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_UNCERTAIN_BIT_INDEX),
                        MOD35_CLOUD_UNCERTAIN_FLAG_DESCR);
        mod35FC.addFlag(MOD35_PROBABLY_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_PROBABLY_CLEAR_BIT_INDEX),
                        MOD35_PROBABLY_CLEAR_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CONFIDENT_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CONFIDENT_CLEAR_BIT_INDEX),
                        MOD35_CERTAINLY_CLEAR_FLAG_DESCR);
        mod35FC.addFlag(MOD35_DAYTIME_FLAG_NAME, BitSetter.setFlag(0, DAYTIME_BIT_INDEX),
                        MOD35_DAYTIME_FLAG_DESCR);
        mod35FC.addFlag(MOD35_GLINT_FLAG_NAME, BitSetter.setFlag(0, GLINT_BIT_INDEX),
                        MOD35_GLINT_FLAG_DESCR);
        mod35FC.addFlag(MOD35_SNOW_ICE_FLAG_NAME, BitSetter.setFlag(0, SNOW_ICE_BIT_INDEX),
                        MOD35_SNOW_ICE_FLAG_DESCR);
        mod35FC.addFlag(MOD35_WATER_FLAG_NAME, BitSetter.setFlag(0, WATER_BIT_INDEX),
                        MOD35_WATER_FLAG_DESCR);
        mod35FC.addFlag(MOD35_COASTAL_FLAG_NAME, BitSetter.setFlag(0, COASTAL_BIT_INDEX),
                        MOD35_COASTAL_FLAG_DESCR);
        mod35FC.addFlag(MOD35_DESERT_FLAG_NAME, BitSetter.setFlag(0, DESERT_BIT_INDEX),
                        MOD35_DESERT_FLAG_DESCR);
        mod35FC.addFlag(MOD35_LAND_FLAG_NAME, BitSetter.setFlag(0, LAND_BIT_INDEX),
                        MOD35_LAND_FLAG_DESCR);

        ProductNodeGroup<Mask> maskGroup = mod35Product.getMaskGroup();

        int colorIndex = 0;
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_CLOUD_DETERMINED_FLAG_NAME,
                MOD35_CLOUD_DETERMINED_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_CLOUD_CERTAIN_FLAG_NAME,
                MOD35_CLOUD_CERTAIN_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_CLOUD_UNCERTAIN_FLAG_NAME,
                MOD35_CLOUD_UNCERTAIN_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_PROBABLY_CLEAR_FLAG_NAME,
                MOD35_PROBABLY_CLEAR_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_CONFIDENT_CLEAR_FLAG_NAME,
                MOD35_CERTAINLY_CLEAR_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_DAYTIME_FLAG_NAME,
                MOD35_DAYTIME_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_GLINT_FLAG_NAME,
                MOD35_GLINT_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_SNOW_ICE_FLAG_NAME,
                MOD35_SNOW_ICE_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_WATER_FLAG_NAME,
                MOD35_WATER_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_COASTAL_FLAG_NAME,
                MOD35_COASTAL_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_DESERT_FLAG_NAME,
                MOD35_DESERT_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex++], 0.5f);
        addMask(mod35Product, maskGroup, PIXEL_CLASSIF_FLAG_BAND_NAME, MOD35_LAND_FLAG_NAME,
                MOD35_LAND_FLAG_DESCR, PIXEL_CLASSIF_COLORS[colorIndex], 0.5f);

        mod35Product.getFlagCodingGroup().add(mod35FC);
        final Band pixelClassifBand = mod35Product.addBand(PIXEL_CLASSIF_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        pixelClassifBand.setDescription("MOD35 pixel classification");
        pixelClassifBand.setUnit("dl");
        pixelClassifBand.setSampleCoding(mod35FC);

        Mod35BitMaskOp bitMaskOp = new Mod35BitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", mod35Product);
        bitMaskOp.setParameter("srcBandName", "Cloud_Mask_Byte_Segment1");
        bitMaskOp.setParameter("trgBandName", PIXEL_CLASSIF_FLAG_BAND_NAME);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        pixelClassifBand.setSourceImage(bitMaskProduct.getBand(PIXEL_CLASSIF_FLAG_BAND_NAME).getSourceImage());
    }

    public static void attachQualityAssuranceFlagBand(Product mod35Product) {
        FlagCoding mod35FC = new FlagCoding(QA_FLAG_BAND_NAME);

        mod35FC.addFlag(MOD35_CLOUD_MASK_USEFUL_FLAG_NAME, BitSetter.setFlag(0, CLOUD_MASK_USEFUL_BIT_INDEX),
                        MOD35_CLOUD_MASK_USEFUL_FLAG_DESCR);
        for (int i = 0; i < NUM_CLOUD_MASK_CONFIDENCE_LEVELS; i++) {
            mod35FC.addFlag(CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES[i],
                            BitSetter.setFlag(0, CLOUD_MASK_CONFIDENCE_LEVEL_BIT_INDICES[i]),
                            CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS[i]);
        }

        ProductNodeGroup<Mask> maskGroup = mod35Product.getMaskGroup();
        addMask(mod35Product, maskGroup, QA_FLAG_BAND_NAME, MOD35_CLOUD_MASK_USEFUL_FLAG_NAME,
                MOD35_CLOUD_MASK_USEFUL_FLAG_DESCR, QA_COLORS[0], 0.5f);
        for (int i = 0; i < NUM_CLOUD_MASK_CONFIDENCE_LEVELS; i++) {
            addMask(mod35Product, maskGroup, QA_FLAG_BAND_NAME, CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_NAMES[i],
                    CLOUD_MASK_CONFIDENCE_LEVEL_FLAG_DESCRIPTIONS[i], QA_COLORS[i+1], 0.5f);
        }

        mod35Product.getFlagCodingGroup().add(mod35FC);
        final Band qaBand = mod35Product.addBand(QA_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        qaBand.setDescription("MOD35 quality assurance");
        qaBand.setUnit("dl");
        qaBand.setSampleCoding(mod35FC);

        Mod35BitMaskOp bitMaskOp = new Mod35BitMaskOp();
        bitMaskOp.setParameterDefaultValues();
        bitMaskOp.setSourceProduct("sourceProduct", mod35Product);
        bitMaskOp.setParameter("srcBandName", "Quality_Assurance_QA_Dimension1");
        bitMaskOp.setParameter("trgBandName", QA_FLAG_BAND_NAME);
        Product bitMaskProduct = bitMaskOp.getTargetProduct();
        qaBand.setSourceImage(bitMaskProduct.getBand(QA_FLAG_BAND_NAME).getSourceImage());
    }

    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                                              description, width, height,
                                              bandName + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }


}
