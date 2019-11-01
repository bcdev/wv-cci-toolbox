package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
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
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Nir.Nir", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of TCWV L3 NIR daily products .")
public class L3DailyMergeNirNirOp extends PixelOperator {

    @SourceProduct(description = "Source product 1")
    private Product sensor1Product;
    @SourceProduct(description = "Source product 2")
    private Product sensor2Product;

    private Product[] mergeInputProducts;

    private int width;
    private int height;

    private int[] SRC_NUM_OBS;
    private int[] SRC_TCWV_MEAN;
    private int[] SRC_TCWV_SIGMA;
    private int[] SRC_TCWV_UNCERTAINTY_MEAN;
    private int[] SRC_TCWV_UNCERTAINTY_COUNTS;
    private int[] SRC_TCWV_SUMS_SUM;
    private int[] SRC_TCWV_SUMS_SUM_SQ;
    private int[] SRC_TCWV_QUALITY_FLAGS_MAJORITY;
    private int[] SRC_TCWV_QUALITY_FLAGS_MIN;
    private int[] SRC_TCWV_QUALITY_FLAGS_MAX;
    private int[] SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY;

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


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        mergeInputProducts = new Product[]{sensor1Product, sensor2Product};

        width = mergeInputProducts[0].getSceneRasterWidth();
        height = mergeInputProducts[0].getSceneRasterHeight();

        validate();

        SRC_NUM_OBS = new int[2];
        SRC_TCWV_MEAN = new int[2];
        SRC_TCWV_SIGMA = new int[2];
        SRC_TCWV_UNCERTAINTY_MEAN = new int[2];
        SRC_TCWV_UNCERTAINTY_COUNTS = new int[2];
        SRC_TCWV_SUMS_SUM = new int[2];
        SRC_TCWV_SUMS_SUM_SQ = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAJORITY = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MIN = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAX = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MIN = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAX = new int[2];
        SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = new int[2];

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int[] srcNumObs = new int[2];
        final int[] srcNumObsNodata = new int[2];
        final double[] srcTcwvMean = new double[2];
        final double[] srcTcwvNodata = new double[2];
        final double[] srcTcwvSigma = new double[2];
        final double[] srcTcwvUncertaintyMean = new double[2];
        final double[] srcTcwvUncertaintyCounts = new double[2];
        final double[] srcTcwvCountsNodata = new double[2];
        final double[] srcTcwvSumsSum = new double[2];
        final double[] srcTcwvSumsSumSq = new double[2];
        final int[] srcQualityFlagsMajority = new int[2];
        final int[] srcQualityFlagsMin = new int[2];
        final int[] srcQualityFlagsMax = new int[2];
        final int[] srcSurfaceTypeFlag = new int[2];

        for (int i = 0; i < 2; i++) {
            srcNumObs[i] = sourceSamples[SRC_NUM_OBS[i]].getInt();
            srcNumObsNodata[i] = (int) mergeInputProducts[i].getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME).getNoDataValue();
            srcTcwvMean[i] = sourceSamples[SRC_TCWV_MEAN[i]].getDouble();
            srcTcwvNodata[i] = mergeInputProducts[i].getBand(TcwvConstants.TCWV_MEAN_BAND_NAME).getNoDataValue();
            srcTcwvSigma[i] = sourceSamples[SRC_TCWV_SIGMA[i]].getDouble();
            srcTcwvUncertaintyMean[i] = sourceSamples[SRC_TCWV_UNCERTAINTY_MEAN[i]].getDouble();
            srcTcwvUncertaintyCounts[i] = sourceSamples[SRC_TCWV_UNCERTAINTY_COUNTS[i]].getDouble();
            srcTcwvCountsNodata[i] = mergeInputProducts[i].getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getNoDataValue();
            srcTcwvSumsSum[i] = sourceSamples[SRC_TCWV_SUMS_SUM[i]].getDouble();
            srcTcwvSumsSumSq[i] = sourceSamples[SRC_TCWV_SUMS_SUM_SQ[i]].getDouble();
            srcQualityFlagsMajority[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MAJORITY[i]].getInt();
            srcQualityFlagsMin[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MIN[i]].getInt();
            srcQualityFlagsMax[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MAX[i]].getInt();
            srcSurfaceTypeFlag[i] = sourceSamples[SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i]].getInt();
        }

        final int numObsMerge = mergeNumObs(srcNumObs, srcNumObsNodata);
        final double[] tcwvMeanMerge =
                mergeTcwv(srcTcwvMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvSigmaMerge =
                mergeTcwv(srcTcwvSigma, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvUncertaintyMeanMerge =
                mergeTcwv(srcTcwvUncertaintyMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvUncertaintyCountsMerge =
                mergeTcwv(srcTcwvUncertaintyMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvSumsSumMerge =
                mergeTcwv(srcTcwvSumsSum, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvSumsSumSqMerge =
                mergeTcwv(srcTcwvSumsSumSq, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final int qualityFlagMajorityMerge = mergeFlag(srcQualityFlagsMajority, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int qualityFlagMinMerge = mergeFlag(srcQualityFlagsMin, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int qualityFlagMaxMerge = mergeFlag(srcQualityFlagsMax, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int surfaceTypeFlagMerge = mergeFlag(srcSurfaceTypeFlag, srcTcwvUncertaintyCounts, srcTcwvNodata);

        targetSamples[TRG_NUM_OBS].set(numObsMerge);
        targetSamples[TRG_TCWV_MEAN].set(tcwvMeanMerge[0]);
        targetSamples[TRG_TCWV_SIGMA].set(tcwvSigmaMerge[0]);
        targetSamples[TRG_TCWV_UNCERTAINTY_MEAN].set(tcwvUncertaintyMeanMerge[0]);
        targetSamples[TRG_TCWV_UNCERTAINTY_COUNTS].set(tcwvUncertaintyCountsMerge[1]);
        targetSamples[TRG_TCWV_SUMS_SUM].set(tcwvSumsSumMerge[0]);
        targetSamples[TRG_TCWV_SUMS_SUM_SQ].set(tcwvSumsSumSqMerge[0]);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MAJORITY].set(qualityFlagMajorityMerge);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MIN].set(qualityFlagMinMerge);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MAX].set(qualityFlagMaxMerge);
        targetSamples[TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY].set(surfaceTypeFlagMerge);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_L3_BAND_NAME).
                                      getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME).getDataType());

        for (Band b : targetProduct.getBands()) {
            final Band sourceBand = mergeInputProducts[0].getBand(b.getName());
            TcwvUtils.copyBandProperties(b, sourceBand);
        }

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < 2; i++) {
            SRC_NUM_OBS[i] = i;
            SRC_TCWV_MEAN[i] = i + 2;
            SRC_TCWV_SIGMA[i] = i + 4;
            SRC_TCWV_UNCERTAINTY_MEAN[i] = i + 6;
            SRC_TCWV_UNCERTAINTY_COUNTS[i] = i + 8;
            SRC_TCWV_SUMS_SUM[i] = i + 10;
            SRC_TCWV_SUMS_SUM_SQ[i] = i + 12;
            SRC_TCWV_QUALITY_FLAGS_MAJORITY[i] = i + 14;
            SRC_TCWV_QUALITY_FLAGS_MIN[i] = i + 16;
            SRC_TCWV_QUALITY_FLAGS_MAX[i] = i + 18;
            SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i] = i + 20;
        }

        for (int i = 0; i < 2; i++) {
            configurator.defineSample(SRC_NUM_OBS[i], TcwvConstants.NUM_OBS_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_MEAN[i], TcwvConstants.TCWV_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_SIGMA[i], TcwvConstants.TCWV_SIGMA_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_UNCERTAINTY_MEAN[i], TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_UNCERTAINTY_COUNTS[i], TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_SUMS_SUM[i], TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_SUMS_SUM_SQ[i], TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MAJORITY[i], TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MIN[i], TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MAX[i], TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i], TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME,
                                      mergeInputProducts[i]);
        }
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
    }

    private static int mergeNumObs(int[] srcNumObs, int[] srcTcwvNumObsNodata) {
        int numObs = 0;
        for (int i = 0; i < 2; i++) {
            if (srcNumObs[i] != srcTcwvNumObsNodata[i]) {
                numObs += srcNumObs[i];
            }
        }
        return numObs;
    }


    private static double[] mergeTcwv(double[] srcTcwv, double[] srcTcwvCounts,
                                      double[] srcTcwvNodata, double[] srcTcwvCountsNodata) {
        double tcwv = 0.0;
        double tcwvCounts = 0.0;

        for (int i = 0; i < 2; i++) {
            if (!Double.isNaN(srcTcwv[i]) && !Double.isNaN(srcTcwvCounts[i]) &&
                    srcTcwv[i] != srcTcwvNodata[i] && srcTcwvCounts[i] != srcTcwvCountsNodata[i]) {
                tcwv += srcTcwvCounts[i] * srcTcwv[i];
                tcwvCounts += srcTcwvCounts[i];
            }
        }
        tcwv /= tcwvCounts;

        return new double[]{tcwv, tcwvCounts};
    }

    private static int mergeFlag(int[] srcFlags, double[] srcTcwvCounts, double[] srcTcwvCountsNodata) {
        int majorityIndex = srcTcwvCounts[0] >= srcTcwvCounts[1] ? 0 : 1;
        int minorityIndex = srcTcwvCounts[0] >= srcTcwvCounts[1] ? 1 : 0;
        if (srcFlags[majorityIndex] >= 0 && srcTcwvCounts[majorityIndex] != srcTcwvCountsNodata[majorityIndex]) {
            return srcFlags[majorityIndex];
        } else {
            return srcFlags[minorityIndex];
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
            super(L3DailyMergeNirNirOp.class);
        }
    }
}
