package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
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
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Ds2", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of TCWV L3 daily products (version for recent Dataset 2 products.")
public class L3MergeNirSensorsDailyDataset2Op extends PixelOperator {

    @Parameter(valueSet = {"0", "1", "2", "3"}, defaultValue = "0",
            description = "Aggregation mode: 0 = aggregate all sensors, " +
                    "1 = use sensor 1 sample only, " +
                    "2 = use sensor 2 sample only, " +
                    "3 = use sensor 3 sample only.")
    private int aggregationMode;

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
    private int[] SRC_TCWV_SUMS_SUM_SQ;
    private int[] SRC_TCWV_QUALITY_FLAGS_MAJORITY;
    private int[] SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY;

    private static final int TRG_NUM_OBS = 0;
    private static final int TRG_TCWV_MEAN = 1;
    private static final int TRG_TCWV_SIGMA = 2;
    private static final int TRG_TCWV_UNCERTAINTY_MEAN = 3;
    private static final int TRG_TCWV_UNCERTAINTY_COUNTS = 4;
    private static final int TRG_TCWV_SUMS_SUM_SQ = 5;
    private static final int TRG_TCWV_QUALITY_FLAGS_MAJORITY = 6;
    private static final int TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = 7;


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
        SRC_TCWV_SUMS_SUM_SQ = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAJORITY = new int[2];
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
        final double[] srcTcwvSumsSumSq = new double[2];
        final int[] srcQualityFlag = new int[2];
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
            srcTcwvSumsSumSq[i] = sourceSamples[SRC_TCWV_SUMS_SUM_SQ[i]].getDouble();
            srcQualityFlag[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MAJORITY[i]].getInt();
            srcSurfaceTypeFlag[i] = sourceSamples[SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i]].getInt();
        }

        final int numObsMerge = mergeNumObs(aggregationMode, srcNumObs, srcNumObsNodata);
        final double[] tcwvMeanMerge =
                mergeTcwv(aggregationMode, srcTcwvMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvSigmaMerge =
                mergeTcwv(aggregationMode, srcTcwvSigma, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvUncertaintyMeanMerge =
                mergeTcwv(aggregationMode, srcTcwvUncertaintyMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvUncertaintyCountsMerge =
                mergeTcwv(aggregationMode, srcTcwvUncertaintyMean, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvSumsSumSqMerge =
                mergeTcwv(aggregationMode, srcTcwvSumsSumSq, srcTcwvUncertaintyCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final int qualityFlagMerge = mergeFlag(aggregationMode, srcQualityFlag, srcTcwvUncertaintyCounts, srcTcwvCountsNodata);
        final int surfaceTypeFlagMerge = mergeFlag(aggregationMode, srcSurfaceTypeFlag, srcTcwvUncertaintyCounts, srcTcwvCountsNodata);

        targetSamples[TRG_NUM_OBS].set(numObsMerge);
        targetSamples[TRG_TCWV_MEAN].set(tcwvMeanMerge[0]);
        targetSamples[TRG_TCWV_SIGMA].set(tcwvSigmaMerge[0]);
        targetSamples[TRG_TCWV_UNCERTAINTY_MEAN].set(tcwvUncertaintyMeanMerge[0]);
        targetSamples[TRG_TCWV_UNCERTAINTY_COUNTS].set(tcwvUncertaintyCountsMerge[1]);
        targetSamples[TRG_TCWV_SUMS_SUM_SQ].set(tcwvSumsSumSqMerge[0]);
        targetSamples[TRG_TCWV_QUALITY_FLAGS_MAJORITY].set(qualityFlagMerge);
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
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_L3_BAND_NAME,
                              mergeInputProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_L3_BAND_NAME).getDataType());
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
            SRC_NUM_OBS[i] = 2 * i;
            SRC_TCWV_MEAN[i] = 2 * i;
            SRC_TCWV_SIGMA[i] = 2 * i;
            SRC_TCWV_UNCERTAINTY_MEAN[i] = 2 * i;
            SRC_TCWV_UNCERTAINTY_COUNTS[i] = 2 * i;
            SRC_TCWV_SUMS_SUM_SQ[i] = 2 * i + 1;
            SRC_TCWV_QUALITY_FLAGS_MAJORITY[i] = 2 * i + 2;
            SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i] = 2 * i + 2;
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
            configurator.defineSample(SRC_TCWV_SUMS_SUM_SQ[i], TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                                      mergeInputProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MAJORITY[i], TcwvConstants.TCWV_QUALITY_FLAG_L3_BAND_NAME,
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
        configurator.defineSample(TRG_TCWV_SUMS_SUM_SQ, TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_QUALITY_FLAGS_MAJORITY, TcwvConstants.TCWV_QUALITY_FLAG_L3_BAND_NAME);
        configurator.defineSample(TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY, TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME);
    }

    private static int mergeNumObs(int aggregationMode, int[] srcNumObs, int[] srcTcwvNumObsNodata) {
        int numObs = 0;

        if (aggregationMode == 0) {
            for (int i = 0; i < 2; i++) {
                if (srcNumObs[i] != srcTcwvNumObsNodata[i]) {
                    numObs += srcNumObs[i];
                }
            }
        } else {
            final int index = aggregationMode - 1;
            if (srcNumObs[index] != srcTcwvNumObsNodata[index]) {
                return srcNumObs[index];
            } else {
                for (int i = 0; i < 2; i++) {
                    if (srcNumObs[i] != srcTcwvNumObsNodata[i]) {
                        numObs += srcNumObs[i];
                    }
                }
            }
        }

        return numObs;
    }


    private static double[] mergeTcwv(int aggregationMode, double[] srcTcwv, double[] srcTcwvCounts,
                              double[] srcTcwvNodata, double[] srcTcwvCountsNodata) {
        double tcwv = 0.0;
        double tcwvCounts = 0.0;

        if (aggregationMode == 0) {
            for (int i = 0; i < 2; i++) {
                if (!Double.isNaN(srcTcwv[i]) && !Double.isNaN(srcTcwvCounts[i]) &&
                        srcTcwv[i] != srcTcwvNodata[i] && srcTcwvCounts[i] != srcTcwvCountsNodata[i]) {
                    tcwv += srcTcwvCounts[i] * srcTcwv[i];
                    tcwvCounts += srcTcwvCounts[i];
                }
            }
        } else {
            final int index = aggregationMode - 1;
            if (!Double.isNaN(srcTcwv[index]) && !Double.isNaN(srcTcwvCounts[index]) &&
                    srcTcwv[index] != srcTcwvNodata[index] && srcTcwvCounts[index] != srcTcwvCountsNodata[index]) {
                return new double[]{srcTcwv[index], srcTcwvCounts[index]};
            } else {
                for (int i = 0; i < 2; i++) {
                    if (!Double.isNaN(srcTcwv[i]) && !Double.isNaN(srcTcwvCounts[i]) &&
                            srcTcwv[i] != srcTcwvNodata[i] && srcTcwvCounts[i] != srcTcwvCountsNodata[i]) {
                        tcwv += srcTcwvCounts[i] * srcTcwv[i];
                        tcwvCounts += srcTcwvCounts[i];
                    }
                }
            }
        }

        tcwv /= tcwvCounts;

        return new double[]{tcwv, tcwvCounts};
    }

    private static int mergeFlag(int aggregationMode, int[] srcFlags, double[] srcTcwvCounts, double[] srcTcwvCountsNodata) {
        int majorityIndex = srcTcwvCounts[0] >= srcTcwvCounts[1] ? 0 : 1;
        int minorityIndex = srcTcwvCounts[0] >= srcTcwvCounts[1] ? 1 : 0;
        if (aggregationMode == 0) {
            if (srcFlags[majorityIndex] >= 0 && srcTcwvCounts[majorityIndex] != srcTcwvCountsNodata[majorityIndex]) {
                return srcFlags[majorityIndex];
            } else {
                return srcFlags[minorityIndex];
            }
        } else {
            final int index = aggregationMode - 1;
            if (srcFlags[index] >= 0) {
                return srcFlags[index];
            } else {
                for (int i = 0; i < 2; i++) {
                    if (srcFlags[i] >= 0) {
                        return srcFlags[i];
                    }
                }
            }
        }

        return srcFlags[0];
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
            super(L3MergeNirSensorsDailyDataset2Op.class);
        }
    }
}
