package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
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
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Nir.Nir.CCN5", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of up to 4 TCWV L3 NIR daily products, for CCN5.")
public class L3DailyMergeNirNirCCN5Op extends PixelOperator {

    @Parameter(description = "First sensor: combination from previous merge (e.g. MODIS_TERRA-OLCI_A), up to 3 single sensors.")
    private String[] sensorNames;

    @SourceProducts(description = "Source products")
    private Product[] sourceProducts;

    private int numSensors;

    private int width;
    private int height;

    private int[] SRC_POSSIBLE_NUM_OBS;
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

    private String[] numObsTargetBandNames;

    private int[] TRG_NUM_OBS;

    private int TRG_POSSIBLE_NUM_OBS;
    private int TRG_TCWV_MEAN;
    private int TRG_TCWV_SIGMA;
    private int TRG_TCWV_UNCERTAINTY_MEAN;
    private int TRG_TCWV_UNCERTAINTY_COUNTS;
    private int TRG_TCWV_SUMS_SUM;
    private int TRG_TCWV_SUMS_SUM_SQ;
    private int TRG_TCWV_QUALITY_FLAGS_MAJORITY;
    private int TRG_TCWV_QUALITY_FLAGS_MIN;
    private int TRG_TCWV_QUALITY_FLAGS_MAX;
    private int TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        width = sourceProducts[0].getSceneRasterWidth();
        height = sourceProducts[0].getSceneRasterHeight();

        validate();

        numSensors = sensorNames.length;

        numObsTargetBandNames = getNumObsTargetBandNames(sensorNames);

        TRG_NUM_OBS = new int[numObsTargetBandNames.length];
        for (int i = 0; i < numObsTargetBandNames.length; i++) {
            TRG_NUM_OBS[i] = i;
        }
        TRG_POSSIBLE_NUM_OBS = numSensors;
        TRG_TCWV_MEAN = numSensors + 1;
        TRG_TCWV_SIGMA = numSensors + 2;
        TRG_TCWV_UNCERTAINTY_MEAN = numSensors + 3;
        TRG_TCWV_UNCERTAINTY_COUNTS = numSensors + 4;
        TRG_TCWV_SUMS_SUM = numSensors + 5;
        TRG_TCWV_SUMS_SUM_SQ = numSensors + 6;
        TRG_TCWV_QUALITY_FLAGS_MAJORITY = numSensors + 7;
        TRG_TCWV_QUALITY_FLAGS_MIN = numSensors + 8;
        TRG_TCWV_QUALITY_FLAGS_MAX = numSensors + 9;
        TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = numSensors + 10;

        SRC_POSSIBLE_NUM_OBS = new int[numSensors];
        SRC_TCWV_MEAN = new int[numSensors];
        SRC_TCWV_SIGMA = new int[numSensors];
        SRC_TCWV_UNCERTAINTY_MEAN = new int[numSensors];
        SRC_TCWV_UNCERTAINTY_COUNTS = new int[numSensors];
        SRC_TCWV_SUMS_SUM = new int[numSensors];
        SRC_TCWV_SUMS_SUM_SQ = new int[numSensors];
        SRC_TCWV_QUALITY_FLAGS_MAJORITY = new int[numSensors];
        SRC_TCWV_QUALITY_FLAGS_MIN = new int[numSensors];
        SRC_TCWV_QUALITY_FLAGS_MAX = new int[numSensors];
        SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = new int[numSensors];

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int[] srcPossibleNumObs = new int[numSensors];
        final int[] srcPossibleNumObsNodata = new int[numSensors];
        final double[] srcTcwvMean = new double[numSensors];
        final double[] srcTcwvNodata = new double[numSensors];
        final double[] srcTcwvSigma = new double[numSensors];
        final double[] srcTcwvUncertaintyMean = new double[numSensors];
        final double[] srcTcwvUncertaintyCounts = new double[numSensors];
        final double[] srcTcwvCountsNodata = new double[numSensors];
        final double[] srcTcwvSumsSum = new double[numSensors];
        final double[] srcTcwvSumsSumSq = new double[numSensors];
        final double[] srcQualityFlagsMajority = new double[numSensors];
        final double[] srcQualityFlagsMin = new double[numSensors];
        final double[] srcQualityFlagsMax = new double[numSensors];
        final double[] srcSurfaceTypeFlag = new double[numSensors];

        final int[] srcNumObs = new int[numSensors];

        for (int i = 0; i < numSensors; i++) {
            // We need to pass as 'number of observations' the number of TCWV retrievals
            // in the L3 grid cell (see PUG). This is implicitly given in the 'tcwv_uncertainty_counts' variable.
            // We do NOT want the 'num_obs' variable, which gives the total number of observations, including
            // the ones without a successful TCWV retrieval.
            srcNumObs[i] = (int) sourceSamples[SRC_TCWV_UNCERTAINTY_COUNTS[i]].getDouble();
        }

        for (int i = 0; i < numSensors; i++) {
            srcPossibleNumObs[i] = sourceSamples[SRC_POSSIBLE_NUM_OBS[i]].getInt();
            srcPossibleNumObsNodata[i] = (int) sourceProducts[i].getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME).getNoDataValue();
            srcTcwvMean[i] = sourceSamples[SRC_TCWV_MEAN[i]].getDouble();
            srcTcwvNodata[i] = sourceProducts[i].getBand(TcwvConstants.TCWV_MEAN_BAND_NAME).getNoDataValue();
            srcTcwvSigma[i] = sourceSamples[SRC_TCWV_SIGMA[i]].getDouble();
            srcTcwvUncertaintyMean[i] = sourceSamples[SRC_TCWV_UNCERTAINTY_MEAN[i]].getDouble();
            srcTcwvUncertaintyCounts[i] = sourceSamples[SRC_TCWV_UNCERTAINTY_COUNTS[i]].getDouble();
            srcTcwvCountsNodata[i] = sourceProducts[i].getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getNoDataValue();
            srcTcwvSumsSum[i] = sourceSamples[SRC_TCWV_SUMS_SUM[i]].getDouble();
            srcTcwvSumsSumSq[i] = sourceSamples[SRC_TCWV_SUMS_SUM_SQ[i]].getDouble();
            srcQualityFlagsMajority[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MAJORITY[i]].getDouble();
            srcQualityFlagsMin[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MIN[i]].getDouble();
            srcQualityFlagsMax[i] = sourceSamples[SRC_TCWV_QUALITY_FLAGS_MAX[i]].getDouble();
            srcSurfaceTypeFlag[i] = sourceSamples[SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i]].getDouble();
        }

        if (x == 450 && y == 209) {
            System.out.println("x,y  = " + x + ", " + y);
        }

        final int possibleNumObsMerge = mergePossibeNumObs(srcPossibleNumObs, srcPossibleNumObsNodata);
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
        final double qualityFlagMajorityMerge = mergeFlag(srcQualityFlagsMajority, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final double qualityFlagMinMerge = mergeFlag(srcQualityFlagsMin, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final double qualityFlagMaxMerge = mergeFlag(srcQualityFlagsMax, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final double surfaceTypeFlagMerge = mergeFlag(srcSurfaceTypeFlag, srcTcwvUncertaintyCounts, srcTcwvNodata);

        for (int i = 0; i < numSensors; i++) {
            targetSamples[TRG_NUM_OBS[i]].set(srcNumObs[i]);
        }

        targetSamples[TRG_POSSIBLE_NUM_OBS].set(possibleNumObsMerge);
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

        for (int i = 0; i < numObsTargetBandNames.length; i++) {
            targetProduct.addBand(numObsTargetBandNames[i], ProductData.TYPE_INT32);
        }

        targetProduct.addBand(TcwvConstants.TCWV_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME,
                sourceProducts[0].getBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME).getDataType());

        for (Band b : targetProduct.getBands()) {
            if (b.getName().startsWith(TcwvConstants.NUM_OBS_L3_BAND_NAME)) {
                b.setNoDataValue(-1);
                b.setNoDataValueUsed(true);
            } else {
                final Band sourceBand = sourceProducts[0].getBand(b.getName());
                TcwvUtils.copyBandProperties(b, sourceBand);
            }
        }

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < numSensors; i++) {
            SRC_POSSIBLE_NUM_OBS[i] = i;
            SRC_TCWV_MEAN[i] = i + numSensors;
            SRC_TCWV_SIGMA[i] = i + 2*numSensors;
            SRC_TCWV_UNCERTAINTY_MEAN[i] = i + 3*numSensors;
            SRC_TCWV_UNCERTAINTY_COUNTS[i] = i + 4*numSensors;
            SRC_TCWV_SUMS_SUM[i] = i + 5*numSensors;
            SRC_TCWV_SUMS_SUM_SQ[i] = i + 6*numSensors;
            SRC_TCWV_QUALITY_FLAGS_MAJORITY[i] = i + 7*numSensors;
            SRC_TCWV_QUALITY_FLAGS_MIN[i] = i + 8*numSensors;
            SRC_TCWV_QUALITY_FLAGS_MAX[i] = i + 9*numSensors;
            SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i] = i + 10*numSensors;
        }

        for (int i = 0; i < numSensors; i++) {
            configurator.defineSample(SRC_POSSIBLE_NUM_OBS[i], TcwvConstants.NUM_OBS_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_MEAN[i], TcwvConstants.TCWV_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_SIGMA[i], TcwvConstants.TCWV_SIGMA_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_UNCERTAINTY_MEAN[i], TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_UNCERTAINTY_COUNTS[i], TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_SUMS_SUM[i], TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_SUMS_SUM_SQ[i], TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MAJORITY[i], TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MIN[i], TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_QUALITY_FLAGS_MAX[i], TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME,
                    sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i], TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME,
                    sourceProducts[i]);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) throws OperatorException {
        configureTargetNumObsSamples(configurator);

        configurator.defineSample(TRG_POSSIBLE_NUM_OBS, TcwvConstants.NUM_OBS_L3_BAND_NAME);
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

    private void configureTargetNumObsSamples(TargetSampleConfigurer configurator) {
        for (int i = 0; i < numSensors; i++) {
            configurator.defineSample(TRG_NUM_OBS[i], numObsTargetBandNames[i]);
        }
    }

    static int mergePossibeNumObs(int[] srcNumObs, int[] srcTcwvNumObsNodata) {
        int numObs = 0;
        for (int i = 0; i < srcNumObs.length; i++) {
            if (srcNumObs[i] != srcTcwvNumObsNodata[i]) {
                numObs += srcNumObs[i];
            }
        }
        return numObs;
    }

    static double[] mergeTcwv(double[] srcTcwv, double[] srcTcwvCounts,
                                      double[] srcTcwvNodata, double[] srcTcwvCountsNodata) {
        double tcwv = 0.0;
        double tcwvCounts = 0.0;

        for (int i = 0; i < srcTcwv.length; i++) {
            if (!Double.isNaN(srcTcwv[i]) && !Double.isNaN(srcTcwvCounts[i]) &&
                    srcTcwv[i] != srcTcwvNodata[i] && srcTcwvCounts[i] != srcTcwvCountsNodata[i]) {
                tcwv += srcTcwvCounts[i] * srcTcwv[i];
                tcwvCounts += srcTcwvCounts[i];
            }
        }
        tcwv /= tcwvCounts;

        return new double[]{tcwv, tcwvCounts};
    }

    static double mergeFlag(double[] srcFlags, double[] srcTcwvCounts, double[] srcTcwvCountsNodata) {
        int majorityIndex = -1;
        double srcTcwvCountsMax = Double.MIN_VALUE;

        for (int i = 0; i < srcFlags.length; i++) {
            if (!Double.isNaN(srcFlags[i])) {
                if (majorityIndex == -1 || srcTcwvCounts[i] > srcTcwvCountsMax) {
                    majorityIndex = i;
                    srcTcwvCountsMax = srcTcwvCounts[i];
                }

            }
        }

//        int majorityIndex = 0;
//        double srcTcwvCountsMax = 0.0;
//        for (int i = 0; i < srcFlags.length; i++) {
//            if (!Double.isNaN(srcTcwvCounts[i]) && srcTcwvCounts[i] > srcTcwvCountsMax) {
//                srcTcwvCountsMax = srcTcwvCounts[i];
//                majorityIndex = i;
//            }
//        }

        return majorityIndex >= 0 ? srcFlags[majorityIndex] * 1.0 : Double.NaN;
    }

    static String[] getNumObsTargetBandNames(String[] sensorNames) {
        //       e.g.["num_obs_MODIS_TERRA", "num_obs_MODIS_AQUA", "num_obs_OLCI_A", "num_obs"]
        final String prefix = TcwvConstants.NUM_OBS_L3_BAND_NAME;
        String[] numObsSrcBandNames = new String[sensorNames.length + 1];
        for (int i = 0; i < sensorNames.length; i++) {
            numObsSrcBandNames[i] = prefix + "_" + sensorNames[i];
        }
        numObsSrcBandNames[sensorNames.length] = prefix;

        return numObsSrcBandNames;
    }

    private void validate() {
        // sensors
        if (sensorNames.length != sourceProducts.length) {
            throw new OperatorException("Number of specified sensor names (" + numSensors + ") not " +
                    "equal to number of input products (" + sourceProducts.length + ")");
        }

        for (int i = 0; i < numSensors; i++) {
            boolean sensorOk = false;
            for (int j = 0; j < TcwvConstants.SUPPORTED_NIR_SENSORS.length; j++) {
                if (sensorNames[i].equals(TcwvConstants.SUPPORTED_NIR_SENSORS[j])) {
                    sensorOk = true;
                    break;
                }
            }
            if (!sensorOk) {
                throw new OperatorException("Sensor '" + sensorNames[i] + "' not supported.");
            }
        }

        // product dimensions
        for (int i = 1; i < numSensors; i++) {
            final int width2 = sourceProducts[i].getSceneRasterWidth();
            final int height2 = sourceProducts[i].getSceneRasterHeight();
            if (width != width2 || height != height2) {
                throw new OperatorException("Dimension of first source product (" + width + "/" + height +
                        ") differs from second source product (" + width2 + "/" + height2 + ").");
            }
        }

        // band names
        // todo

        // time ranges
        // todo
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3DailyMergeNirNirCCN5Op.class);
        }
    }
}
