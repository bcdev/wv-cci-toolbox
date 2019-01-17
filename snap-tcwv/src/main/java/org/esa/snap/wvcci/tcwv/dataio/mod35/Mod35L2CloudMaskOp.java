package org.esa.snap.wvcci.tcwv.dataio.mod35;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.core.util.BitSetter;

/**
 * This Operator extracts and interprets the relevant bit information stored in cloud mask and quality
 * assurance byte bands.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Mod35.Bitmask",
        description = "Extracts and interprets the relevant bit information stored in cloud mask " +
                "and quality assurance byte bands",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2012 by Brockmann Consult",
        internal = true)
public class Mod35L2CloudMaskOp extends PixelOperator {

    private static final int SRC_FLAG = 0;
    private static final int TRG_FLAG = 0;

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "Cloud_Mask_Byte_Segment1",
            valueSet = {"Cloud_Mask_Byte_Segment1", "Quality_Assurance_QA_Dimension1"}, // these should be all we need
            description = "source band name")
    private String srcBandName;

    @Parameter(defaultValue = "", description = "target flag band name")
    private String trgBandName;

    @Parameter(defaultValue = "-1", description = "byte of source flag to extract")
    private int byteIndex;

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.addBand(trgBandName, ProductData.TYPE_INT16);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, srcBandName);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(TRG_FLAG, trgBandName);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        int srcFlagValue = sourceSamples[SRC_FLAG].getInt();
        switch (srcBandName) {
            case "Cloud_Mask_Byte_Segment1":
                computePixelCloudMask(srcFlagValue, targetSamples);
                break;
            case "Quality_Assurance_QA_Dimension1":
                computePixelQualityAssurance(srcFlagValue, targetSamples);
                break;
            default:
                throw new IllegalArgumentException("Invalid mask band name " + srcBandName +
                        " - must be 'Cloud_Mask_Byte_Segment1' " +
                        "or 'Quality_Assurance_QA_Dimension1'.");
        }
    }

    private void computePixelCloudMask(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_DETERMINED_BIT_INDEX, isCloudDetermined(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_CERTAIN_BIT_INDEX, isCertainlyCloud(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_UNCERTAIN_BIT_INDEX, isProbablyCloud(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_PROBABLY_CLEAR_BIT_INDEX, isProbablyClear(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_CONFIDENT_CLEAR_BIT_INDEX, isCertainlyClear(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.DAYTIME_BIT_INDEX, isDaytime(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.GLINT_BIT_INDEX, isGlint(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.SNOW_ICE_BIT_INDEX, isSnowIce(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.WATER_BIT_INDEX, isWater(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.COASTAL_BIT_INDEX, isCoastal(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.DESERT_BIT_INDEX, isDesert(srcValue));
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.LAND_BIT_INDEX, isLand(srcValue));

    }

    private boolean isCloudDetermined(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 0);
    }

    private boolean isCertainlyCloud(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isProbablyCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isProbablyClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isCertainlyClear(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isDaytime(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 3);
    }

    private boolean isGlint(int srcValue) {
        return !BitSetter.isFlagSet(srcValue, 4);
    }

    private boolean isSnowIce(int srcValue) {
        return !BitSetter.isFlagSet(srcValue, 5);
    }

    private boolean isWater(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 6) && !BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isCoastal(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 6) && !BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isDesert(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 6) && BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isLand(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 6) && BitSetter.isFlagSet(srcValue, 7));
    }

    private void computePixelQualityAssurance(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_MASK_USEFUL_BIT_INDEX, isCloudMaskUseful(srcValue));
        for (int i = 0; i < ModisMod35L2Constants.NUM_CLOUD_MASK_CONFIDENCE_LEVELS; i++) {
            targetSamples[TRG_FLAG].set(ModisMod35L2Constants.CLOUD_MASK_CONFIDENCE_LEVEL_BIT_INDICES[i],
                    isQAConfidenceLevel(srcValue, i + 1));
        }
    }

    private boolean isCloudMaskUseful(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 0);
    }

    private boolean isQAConfidenceLevel(int srcValue, int confLevelIndex) {
        switch (confLevelIndex) {
            case 1:
                return (!BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2) &&
                        !BitSetter.isFlagSet(srcValue, 3));
            case 2:
                return (BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2) &&
                        !BitSetter.isFlagSet(srcValue, 3));
            case 3:
                return (!BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2) &&
                        !BitSetter.isFlagSet(srcValue, 3));
            case 4:
                return (BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2) &&
                        !BitSetter.isFlagSet(srcValue, 3));
            case 5:
                return (!BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2) &&
                        BitSetter.isFlagSet(srcValue, 3));
            case 6:
                return (BitSetter.isFlagSet(srcValue, 1) && !BitSetter.isFlagSet(srcValue, 2) &&
                        BitSetter.isFlagSet(srcValue, 3));
            case 7:
                return (!BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2) &&
                        BitSetter.isFlagSet(srcValue, 3));
            case 8:
                return (BitSetter.isFlagSet(srcValue, 1) && BitSetter.isFlagSet(srcValue, 2) &&
                        BitSetter.isFlagSet(srcValue, 3));
            default:
                return false;
        }
    }

}
