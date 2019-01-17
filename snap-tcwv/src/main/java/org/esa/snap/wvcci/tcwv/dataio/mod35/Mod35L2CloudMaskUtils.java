package org.esa.snap.wvcci.tcwv.dataio.mod35;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;

import static org.esa.snap.wvcci.tcwv.dataio.mod35.ModisMod35L2Constants.*;

import java.awt.*;


/**
 * MOD35 L2 flags and bit masks utilities
 *
 * @author olafd
 */
class Mod35L2CloudMaskUtils {



    /**
     * Attaches pixel classification flag band to original MOD35 L2 product
     *
     * @param mod35L2Product - the MOD35 L2 product
     *
     */
    static void attachPixelClassificationFlagBand(Product mod35L2Product) {
        FlagCoding mod35L2FlagCoding = new FlagCoding(PIXEL_CLASSIF_FLAG_BAND_NAME);

        mod35L2FlagCoding.addFlag(CLOUD_DETERMINED_FLAG_NAME, BitSetter.setFlag(0, ModisMod35L2Constants.CLOUD_DETERMINED_BIT_INDEX),
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
