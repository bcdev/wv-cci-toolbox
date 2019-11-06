package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

/**
 * Operator for sensor merging of TCWV L3 products of 2 or 3 sensors.
 * We have MERIS/MODIS (land) and HOAPS SSM/I (water).
 * <p>
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Nir.Hoaps", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of TCWV L3 NIR and HOAPS daily products.")
public class L3DailyMergeNirHoapsOp extends PixelOperator {

    @Parameter(interval = "[1, 31]", defaultValue = "15",
            description = "Day of month")
    private int dayOfMonth;
    
    @SourceProduct(description = "NIR product (MERIS, MODIS or OLCI)")
    private Product nirProduct;
    @SourceProduct(description = "HOAPS product")
    private Product hoapsProduct;

    @SourceProduct(description = "Land/Sea mask product")
    private Product landmaskProduct;

    @SourceProduct(description = "Seaice product", optional = true)
    private Product seaiceProduct;

    private Product[] mergeInputProducts;

    private Product landMaskProductToUse;
    private Product seaiceProductToUse;

    private int width;
    private int height;

    private static final int SRC_NIR_NUM_OBS = 1;
    private static final int SRC_NIR_TCWV_MEAN = 2;
    private static final int SRC_NIR_TCWV_SIGMA = 3;
    private static final int SRC_NIR_TCWV_UNCERTAINTY_MEAN = 4;
    private static final int SRC_NIR_TCWV_UNCERTAINTY_COUNTS = 5;
    private static final int SRC_NIR_TCWV_SUMS_SUM = 6;
    private static final int SRC_NIR_TCWV_SUMS_SUM_SQ = 7;
    private static final int SRC_NIR_TCWV_QUALITY_FLAGS_MAJORITY = 8;
    private static final int SRC_NIR_TCWV_QUALITY_FLAGS_MIN = 9;
    private static final int SRC_NIR_TCWV_QUALITY_FLAGS_MAX = 10;
    private static final int SRC_NIR_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = 11;

    private static final int SRC_HOAPS_NUM_OBS = 12;
    private static final int SRC_HOAPS_TCWV = 13;
    private static final int SRC_HOAPS_TCWV_SIGMA = 14;
    private static final int SRC_HOAPS_TCWV_PROPAG_ERR = 15;
    private static final int SRC_HOAPS_TCWV_RANDOM_ERR = 16;

    private static final int SRC_LANDMASK_MASK = 17;
    private static final int SRC_SEAICE_MASK = 18;

    private static final int TRG_NUM_OBS = 0;
    private static final int TRG_TCWV_MEAN = 1;
    private static final int TRG_TCWV_SIGMA = 2;
    private static final int TRG_TCWV_UNCERTAINTY_MEAN = 3;
    private static final int TRG_TCWV_UNCERTAINTY_COUNTS = 4;
    private static final int TRG_TCWV_SUMS_SUM = 5;
    private static final int TRG_TCWV_SUMS_SUM_SQ = 6;
    private static final int TRG_TCWV_QUALITY_FLAGS_MAJORITY = 7;
    private static final int TRG_TCWV_QUALITY_FLAGS_MIN = 8;
    private static final int TRG_TCWV_QUALITY_FLAGS_MAX = 9;
    private static final int TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = 10;
    private static final int TRG_TCWV_PROPAG_ERR = 11;
    private static final int TRG_TCWV_RANDOM_ERR = 12;


    @Override
    protected void prepareInputs() throws OperatorException {
//        super.prepareInputs();

        mergeInputProducts = new Product[]{nirProduct, hoapsProduct};

        width = mergeInputProducts[0].getSceneRasterWidth();
        height = mergeInputProducts[0].getSceneRasterHeight();

        validate();

        if (landmaskProduct.getSceneRasterWidth() != width || landmaskProduct.getSceneRasterHeight() != height) {
            landMaskProductToUse = getResampledProduct(landmaskProduct);
        } else {
            landMaskProductToUse = landmaskProduct;
        }

        if (seaiceProduct.getSceneRasterWidth() != width || seaiceProduct.getSceneRasterHeight() != height) {
            seaiceProductToUse = getResampledProduct(seaiceProduct);
        } else {
            seaiceProductToUse = seaiceProduct;
        }
    }

    private Product getResampledProduct(Product inputProduct) {
        ResamplingOp resamplingOp = new ResamplingOp();
        resamplingOp.setSourceProduct(inputProduct);
        resamplingOp.setParameterDefaultValues();
        resamplingOp.setParameter("targetWidth", width);
        resamplingOp.setParameter("targetHeight", height);
        return resamplingOp.getTargetProduct();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final int srcNirNumObs = sourceSamples[SRC_NIR_NUM_OBS].getInt();
        final double srcNirTcwvMean = sourceSamples[SRC_NIR_TCWV_MEAN].getDouble();
        final double srcNirTcwvNodata = nirProduct.getBand(TcwvConstants.TCWV_MEAN_BAND_NAME).getNoDataValue();
        final double srcNirTcwvSigma = sourceSamples[SRC_NIR_TCWV_SIGMA].getDouble();
        final double srcNirTcwvUncertaintyMean = sourceSamples[SRC_NIR_TCWV_UNCERTAINTY_MEAN].getDouble();
        final double srcNirTcwvUncertaintyCounts = sourceSamples[SRC_NIR_TCWV_UNCERTAINTY_COUNTS].getDouble();
        final double srcNirTcwvCountsNodata = nirProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getNoDataValue();
        final double srcNirTcwvSumsSum = sourceSamples[SRC_NIR_TCWV_SUMS_SUM].getDouble();
        final double srcNirTcwvSumsSumSq = sourceSamples[SRC_NIR_TCWV_SUMS_SUM_SQ].getDouble();
        final int srcNirQualityMajorityFlag = sourceSamples[SRC_NIR_TCWV_QUALITY_FLAGS_MAJORITY].getInt();
        final int srcNirQualityMinFlag = sourceSamples[SRC_NIR_TCWV_QUALITY_FLAGS_MIN].getInt();
        final int srcNirQualityMaxFlag = sourceSamples[SRC_NIR_TCWV_QUALITY_FLAGS_MAX].getInt();
        final int srcNirSurfaceTypeFlag = sourceSamples[SRC_NIR_TCWV_SURFACE_TYPE_FLAGS_MAJORITY].getInt();

        final int srcHoapsNumObs = sourceSamples[SRC_HOAPS_NUM_OBS].getInt();
        final int srcHoapsNumObsNodata = (int) hoapsProduct.getBand(TcwvConstants.NIUM_OBS_HOAPS_BAND_NAME).getNoDataValue();
        final double srcHoapsTcwv = sourceSamples[SRC_HOAPS_TCWV].getDouble();
        final double srcHoapsTcwvNodata = hoapsProduct.getBand(TcwvConstants.TCWV_HOAPS_BAND_NAME).getNoDataValue();
        final double srcHoapsTcwvSigma = sourceSamples[SRC_HOAPS_TCWV_SIGMA].getDouble();
        if (hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME) != null) {
            final double srcHoapsTcwvPropagErr = sourceSamples[SRC_HOAPS_TCWV_PROPAG_ERR].getDouble();
            final double srcHoapsTcwvPropagErrNodata = hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME).getNoDataValue();
            final double srcHoapsTcwvRandomErr = sourceSamples[SRC_HOAPS_TCWV_RANDOM_ERR].getDouble();
            final double srcHoapsTcwvRandomErrNodata = hoapsProduct.getBand(TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME).getNoDataValue();
            final double tcwvPropagErrMerge = useOriginal(srcHoapsTcwvPropagErr, srcHoapsTcwvPropagErrNodata);
            final double tcwvRandomErrMerge = useOriginal(srcHoapsTcwvRandomErr, srcHoapsTcwvRandomErrNodata);
            targetSamples[TRG_TCWV_PROPAG_ERR].set(tcwvPropagErrMerge);
            targetSamples[TRG_TCWV_RANDOM_ERR].set(tcwvRandomErrMerge);
        }

        final int srcLandMask = sourceSamples[SRC_LANDMASK_MASK].getInt();
        final int srcSeaiceMask = seaiceProduct != null ? sourceSamples[SRC_SEAICE_MASK].getInt() : -1;

        final int numObsMerge = mergeNumObs(srcNirNumObs, srcHoapsNumObs, srcHoapsNumObsNodata);
        final double tcwvMerge =
                merge(srcNirTcwvMean, srcNirTcwvNodata, srcHoapsTcwv, srcHoapsTcwvNodata,
                      srcHoapsNumObs, srcHoapsNumObsNodata, srcLandMask, srcSeaiceMask);
        final double tcwvSigmaMerge =
                merge(srcNirTcwvSigma, srcNirTcwvNodata, srcHoapsTcwvSigma, srcHoapsTcwvNodata,
                      srcHoapsNumObs, srcHoapsNumObsNodata, srcLandMask, srcSeaiceMask);
        final double tcwvUncertaintyMeanMerge =
                useOriginal(srcNirTcwvUncertaintyMean, srcNirTcwvNodata);
        final double tcwvUncertaintyCountsMerge =
                useOriginal(srcNirTcwvUncertaintyCounts, srcNirTcwvCountsNodata);
        final double tcwvSumsSumMerge =
                useOriginal(srcNirTcwvSumsSum, srcNirTcwvNodata);
        final double tcwvSumsSumSqMerge =
                useOriginal(srcNirTcwvSumsSumSq, srcNirTcwvNodata);
        final int qualityFlagMajorityMerge = mergeQualityFlag(srcNirQualityMajorityFlag, srcHoapsNumObs, srcHoapsNumObsNodata);
        final int qualityFlagMinMerge = mergeQualityFlag(srcNirQualityMinFlag, srcHoapsNumObs, srcHoapsNumObsNodata);
        final int qualityFlagMaxMerge = mergeQualityFlag(srcNirQualityMaxFlag, srcHoapsNumObs, srcHoapsNumObsNodata);

        targetSamples[TRG_NUM_OBS].set(numObsMerge);
        targetSamples[TRG_TCWV_MEAN].set(tcwvMerge);
        targetSamples[TRG_TCWV_SIGMA].set(tcwvSigmaMerge);
        targetSamples[TRG_TCWV_UNCERTAINTY_MEAN].set(tcwvUncertaintyMeanMerge);
        targetSamples[TRG_TCWV_UNCERTAINTY_COUNTS].set(tcwvUncertaintyCountsMerge);
        targetSamples[TRG_TCWV_SUMS_SUM].set(tcwvSumsSumMerge);
        targetSamples[TRG_TCWV_SUMS_SUM_SQ].set(tcwvSumsSumSqMerge);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MAJORITY].set(qualityFlagMajorityMerge);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MIN].set(qualityFlagMinMerge);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MAX].set(qualityFlagMaxMerge);
        targetSamples[TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY].set(srcNirSurfaceTypeFlag);  // take as is
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_L3_BAND_NAME).
                                      getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME,
                              nirProduct.getBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME).getDataType());
        if (hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME) != null) {
            targetProduct.addBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME,
                                  hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME).getDataType());
            targetProduct.addBand(TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME,
                                  hoapsProduct.getBand(TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME).getDataType());
        }

        for (Band b : targetProduct.getBands()) {
            Band sourceBand = nirProduct.getBand(b.getName());
            if (sourceBand != null) {
                TcwvUtils.copyBandProperties(b, sourceBand);
            } else {
                sourceBand = hoapsProduct.getBand(b.getName());
                if (sourceBand != null) {
                    TcwvUtils.copyBandProperties(b, sourceBand);
                }
            }
        }

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_NIR_NUM_OBS, TcwvConstants.NUM_OBS_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_MEAN, TcwvConstants.TCWV_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_SIGMA, TcwvConstants.TCWV_SIGMA_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_UNCERTAINTY_MEAN, TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_UNCERTAINTY_COUNTS, TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_SUMS_SUM, TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_SUMS_SUM_SQ, TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_QUALITY_FLAGS_MAJORITY, TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_QUALITY_FLAGS_MIN, TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_QUALITY_FLAGS_MAX, TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME, nirProduct);
        configurator.defineSample(SRC_NIR_TCWV_SURFACE_TYPE_FLAGS_MAJORITY, TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME, nirProduct);

        configurator.defineSample(SRC_HOAPS_NUM_OBS, TcwvConstants.NIUM_OBS_HOAPS_BAND_NAME, hoapsProduct);
        configurator.defineSample(SRC_HOAPS_TCWV, TcwvConstants.TCWV_HOAPS_BAND_NAME, hoapsProduct);
        configurator.defineSample(SRC_HOAPS_TCWV_SIGMA, TcwvConstants.TCWV_SIGMA_HOAPS_BAND_NAME, hoapsProduct);
        if (hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME) != null) {
            configurator.defineSample(SRC_HOAPS_TCWV_PROPAG_ERR, TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME, hoapsProduct);
            configurator.defineSample(SRC_HOAPS_TCWV_RANDOM_ERR, TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME, hoapsProduct);
        }
        configurator.defineSample(SRC_LANDMASK_MASK, "mask", landMaskProductToUse);
        configurator.defineSample(SRC_SEAICE_MASK, "mask_time" + dayOfMonth, seaiceProductToUse);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_NUM_OBS, TcwvConstants.NUM_OBS_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_MEAN, TcwvConstants.TCWV_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_SIGMA, TcwvConstants.TCWV_SIGMA_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY_MEAN, TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY_COUNTS, TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_SUMS_SUM, TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_SUMS_SUM_SQ, TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_QUALITY_FLAGS_MAJORITY, TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_QUALITY_FLAGS_MIN, TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_QUALITY_FLAGS_MAX, TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY, TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME);
        if (hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME) != null) {
            configurator.defineSample(TRG_TCWV_PROPAG_ERR, TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME);
            configurator.defineSample(TRG_TCWV_RANDOM_ERR, TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME);
        }
    }

    private static int mergeNumObs(int srcNirNumObs, int srcHoapsNumObs, int srcHoapsNumObsNodata) {
        final boolean hoapsAvailable = srcHoapsNumObs > 0 && srcHoapsNumObs != srcHoapsNumObsNodata;
        if (hoapsAvailable) {
            // HOAPS samples available
            return srcHoapsNumObs;
        } else {
            // only NIR samples, no HOAPS samples (land, coastal, sea ice)
            // todo: ingest coastal zone mask when available. Then set to srcNirNumObsNodata if no HOAPS,
            // no land, no coast no seaice --> this condition only applies for gaps in SSMI swaths
            return srcNirNumObs;
        }
    }

    private static double useOriginal(double srcValue, double srcNodataValue) {
        if (!Double.isNaN(srcValue) && srcValue != srcNodataValue) {
            return srcValue;
        } else {
            return Double.NaN;
        }
    }

    private static double merge(double srcNir, double srcNirNodata,
                                double srcHoaps, double srcHoapsNodata,
                                int srcHoapsNumObs, int srcHoapsNumObsNodata,
                                int srcLandMask, int srcSeaiceMask) {

        // we want (required by DWD):
        // if HOAPS available, set HOAPS value (over ocean excl. coast sea ice)
        // if no HOAPS, set NIR value if land, coastal, sea ice, otherwise set to NaN
        // if no HOAPS because of coverage gaps (no land nor coastal nor sea ice), set to NaN
        final boolean nirAvailable = !Double.isNaN(srcNir) && srcNir != srcNirNodata;
        final boolean hoapsAvailable = srcHoaps != srcHoapsNodata && srcHoapsNumObs > 0 &&
                srcHoapsNumObs != srcHoapsNumObsNodata;
        final boolean hoapsNotAvailableNotOcean = srcLandMask > 0 || srcSeaiceMask > 0;
        if (hoapsAvailable) {
            return srcHoaps;
        } else if (hoapsNotAvailableNotOcean && nirAvailable) {
            return srcNir;
        } else {
            return Double.NaN;
        }
    }

    private static int mergeQualityFlag(int srcNirQualityFlag, int srcHoapsNumObs, int srcHoapsNumObsNodata) {
        // we want:
        // if HOAPS, set quality flag to NaN
        // if no HOAPS, keep it as is for land, coastal (todo: ingest), sea ice, otherwise set to NaN
        final boolean hoapsAvailable = srcHoapsNumObs > 0 && srcHoapsNumObs != srcHoapsNumObsNodata;
        if (hoapsAvailable) {
            // HOAPS samples available
            return -1;
        } else {
            return srcNirQualityFlag;
        }
    }

    private void validate() {
        // product dimensions
        final int width2 = mergeInputProducts[1].getSceneRasterWidth();
        final int height2 = mergeInputProducts[1].getSceneRasterHeight();
        if (width != width2 || height != height2) {
            throw new OperatorException("Dimension of first source product (" + width + "/" + height +
                                                ") differs from second source product (" + width2 + "/" + height2 + ").");
        }

        // band names
        // todo

        // time ranges
        // todo
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3DailyMergeNirHoapsOp.class);
        }
    }
}
