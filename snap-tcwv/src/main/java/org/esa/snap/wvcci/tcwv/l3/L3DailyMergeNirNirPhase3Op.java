package org.esa.snap.wvcci.tcwv.l3;

//import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Nir.Nir.Phase3", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of TCWV L3 NIR daily products, for Phase 2.")
public class L3DailyMergeNirNirPhase3Op extends PixelOperator {

    @Parameter(description = "First sensor: combination from previous merge (e.g. MODIS_TERRA-OLCI_A), up to 3 single sensors.")
    private String sensor1Name;

    @Parameter(description = "Second sensor, NO previous merge (MERIS, MODIS_TERRA, MODIS_AQUA, OLCI_A or OLCI_B).")
    private String sensor2Name;

    @SourceProduct(description = "Source product 1")
    private Product sensor1Product;

    @SourceProduct(description = "Source product 2")
    private Product sensor2Product;

    private Product[] mergeInputProducts;

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

    private String[] srcNumObsBandNames;

    private static final int[] TRG_NUM_OBS = {0, 1, 2, 3};
    private static final int TRG_POSSIBLE_NUM_OBS = 4;
    private static final int TRG_TCWV_MEAN = 5;
    private static final int TRG_TCWV_SIGMA = 6;
    private static final int TRG_TCWV_UNCERTAINTY_MEAN = 7;
    private static final int TRG_TCWV_UNCERTAINTY_COUNTS = 8;
    private static final int TRG_TCWV_SUMS_SUM = 9;
    private static final int TRG_TCWV_SUMS_SUM_SQ = 10;
    private static final int TRG_TCWV_QUALITY_FLAGS_MAJORITY = 11;
    private static final int TRG_TCWV_QUALITY_FLAGS_MIN = 12;
    private static final int TRG_TCWV_QUALITY_FLAGS_MAX = 13;
    private static final int TRG_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = 14;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        mergeInputProducts = new Product[]{sensor1Product, sensor2Product};

        width = mergeInputProducts[0].getSceneRasterWidth();
        height = mergeInputProducts[0].getSceneRasterHeight();

        validate();

        srcNumObsBandNames = getNumObsSrcBandNames(sensor1Name);

        SRC_NUM_OBS = new int[srcNumObsBandNames.length];

        SRC_POSSIBLE_NUM_OBS = new int[2];
        SRC_TCWV_MEAN = new int[2];
        SRC_TCWV_SIGMA = new int[2];
        SRC_TCWV_UNCERTAINTY_MEAN = new int[2];
        SRC_TCWV_UNCERTAINTY_COUNTS = new int[2];
        SRC_TCWV_SUMS_SUM = new int[2];
        SRC_TCWV_SUMS_SUM_SQ = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAJORITY = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MIN = new int[2];
        SRC_TCWV_QUALITY_FLAGS_MAX = new int[2];
        SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY = new int[2];

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int[] srcPossibleNumObs = new int[2];
        final int[] srcPossibleNumObsNodata = new int[2];
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

//        if (x == 400 && y == 120) {
//            System.out.println("x,y  = " + x + ", " + y);
//        }

        final int[] srcNumObs = new int[srcNumObsBandNames.length];

        for (int i = 0; i < srcNumObsBandNames.length; i++) {
            if (srcNumObsBandNames.length == 2) {
                // no previous merge: we need to pass as 'number of observations' the number of TCWV retrievals
                // in the L3 grid cell (see PUG). This is implicitly given in the 'tcwv_uncertainty_counts' variable.
                // We do NOT want the 'num_obs' variable, which gives the total number of observations, including
                // the ones without a successful TCWV retrieval.
                srcNumObs[i] = (int) sourceSamples[SRC_TCWV_UNCERTAINTY_COUNTS[i]].getDouble();
            } else {
                // There was a previous merge, so we already have the 'correct' number of observations (see above)
                // in the first source product.
                if (i == srcNumObsBandNames.length - 1) {
                    // second source product
                    srcNumObs[i] = (int) sourceSamples[SRC_TCWV_UNCERTAINTY_COUNTS[1]].getDouble();
                } else {
                    // first source product
                    srcNumObs[i] = sourceSamples[SRC_NUM_OBS[i]].getInt();
                }
            }
        }

        for (int i = 0; i < 2; i++) {
            srcPossibleNumObs[i] = sourceSamples[SRC_POSSIBLE_NUM_OBS[i]].getInt();
            srcPossibleNumObsNodata[i] = (int) mergeInputProducts[i].getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME).getNoDataValue();
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
        final int qualityFlagMajorityMerge = mergeFlag(srcQualityFlagsMajority, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int qualityFlagMinMerge = mergeFlag(srcQualityFlagsMin, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int qualityFlagMaxMerge = mergeFlag(srcQualityFlagsMax, srcTcwvUncertaintyCounts, srcTcwvNodata);
        final int surfaceTypeFlagMerge = mergeFlag(srcSurfaceTypeFlag, srcTcwvUncertaintyCounts, srcTcwvNodata);

        targetSamples[TRG_NUM_OBS[0]].set(srcNumObs[0]);
        if (srcNumObsBandNames.length == 4) {
            targetSamples[TRG_NUM_OBS[1]].set(srcNumObs[1]);
            targetSamples[TRG_NUM_OBS[2]].set(srcNumObs[2]);
            targetSamples[TRG_NUM_OBS[3]].set(srcNumObs[3]);
        } else if (srcNumObsBandNames.length == 3) {
            targetSamples[TRG_NUM_OBS[1]].set(srcNumObs[1]);
            targetSamples[TRG_NUM_OBS[3]].set(srcNumObs[2]);
        } else {
            targetSamples[TRG_NUM_OBS[3]].set(srcNumObs[1]);
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

        if (srcNumObsBandNames.length == 2) {
            targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME + "_" + sensor1Name, ProductData.TYPE_INT32);
            targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME + "_" + sensor2Name, ProductData.TYPE_INT32);
        } else if (srcNumObsBandNames.length == 3 || srcNumObsBandNames.length == 4) {
            for (int i = 0; i < srcNumObsBandNames.length - 1; i++) {
                targetProduct.addBand(srcNumObsBandNames[i], ProductData.TYPE_INT32);
            }
            targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME + "_" + sensor2Name, ProductData.TYPE_INT32);
        } else {
            throw new OperatorException("Invalid number of 'num_obs_*' variables in first source product");
        }

        targetProduct.addBand(TcwvConstants.NUM_OBS_L3_BAND_NAME, ProductData.TYPE_INT32);
        targetProduct.addBand(TcwvConstants.TCWV_L3_BAND_NAME,
                mergeInputProducts[0].getBand(TcwvConstants.TCWV_L3_BAND_NAME).getDataType());
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
            if (b.getName().startsWith(TcwvConstants.NUM_OBS_L3_BAND_NAME)) {
                b.setNoDataValue(-1);
                b.setNoDataValueUsed(true);
            } else {
                final Band sourceBand = mergeInputProducts[0].getBand(b.getName());
                TcwvUtils.copyBandProperties(b, sourceBand);
            }
        }

    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        final int index = srcNumObsBandNames.length;
        for (int i = 0; i < index; i++) {
            SRC_NUM_OBS[i] = i;
        }
        for (int i = 0; i < 2; i++) {
            SRC_POSSIBLE_NUM_OBS[i] = i + index;
            SRC_TCWV_MEAN[i] = i + index + 2;
            SRC_TCWV_SIGMA[i] = i + index + 4;
            SRC_TCWV_UNCERTAINTY_MEAN[i] = i + index + 6;
            SRC_TCWV_UNCERTAINTY_COUNTS[i] = i + index + 8;
            SRC_TCWV_SUMS_SUM[i] = i + index + 10;
            SRC_TCWV_SUMS_SUM_SQ[i] = i + index + 12;
            SRC_TCWV_QUALITY_FLAGS_MAJORITY[i] = i + index + 14;
            SRC_TCWV_QUALITY_FLAGS_MIN[i] = i + index + 16;
            SRC_TCWV_QUALITY_FLAGS_MAX[i] = i + index + 18;
            SRC_TCWV_SURFACE_TYPE_FLAGS_MAJORITY[i] = i + index + 20;
        }

        if (index == 2) {
            configurator.defineSample(SRC_NUM_OBS[0], srcNumObsBandNames[0], mergeInputProducts[0]);
            configurator.defineSample(SRC_NUM_OBS[1], srcNumObsBandNames[1], mergeInputProducts[1]);
        } else if (index == 3 || index == 4) {
            for (int i = 0; i < index - 1; i++) {
                configurator.defineSample(SRC_NUM_OBS[i], srcNumObsBandNames[i], mergeInputProducts[0]);
            }
            configurator.defineSample(SRC_NUM_OBS[index - 1], srcNumObsBandNames[index - 1], mergeInputProducts[1]);
        } else {
            throw new OperatorException("Invalid number of 'num_obs_*' variables in first sozrce product");
        }

        for (int i = 0; i < 2; i++) {
            configurator.defineSample(SRC_POSSIBLE_NUM_OBS[i], TcwvConstants.NUM_OBS_L3_BAND_NAME,
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
        if (srcNumObsBandNames.length == 4) {
            configurator.defineSample(TRG_NUM_OBS[0], srcNumObsBandNames[0]);
            configurator.defineSample(TRG_NUM_OBS[1], srcNumObsBandNames[1]);
            configurator.defineSample(TRG_NUM_OBS[2], srcNumObsBandNames[2]);
        } else if (srcNumObsBandNames.length == 3) {
            configurator.defineSample(TRG_NUM_OBS[0], srcNumObsBandNames[0]);
            configurator.defineSample(TRG_NUM_OBS[1], srcNumObsBandNames[1]);
        } else {
            configurator.defineSample(TRG_NUM_OBS[0], TcwvConstants.NUM_OBS_L3_BAND_NAME + "_" + sensor1Name);
        }
        configurator.defineSample(TRG_NUM_OBS[3], TcwvConstants.NUM_OBS_L3_BAND_NAME + "_" + sensor2Name);
    }

    private static int mergePossibeNumObs(int[] srcNumObs, int[] srcTcwvNumObsNodata) {
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
        for (int i = 0; i <= 1; i++) {
            if (Double.isNaN(srcTcwvCounts[i]) || (srcTcwvCounts[i] == srcTcwvCountsNodata[i])) {
                return srcFlags[1 - i];
            }
        }
        final int majorityIndex = srcTcwvCounts[0] >= srcTcwvCounts[1] ? 0 : 1;
        return srcFlags[majorityIndex];
    }

    private void validate() {
        // sensors
        if (sensor2Name.contains("-")) {
            throw new OperatorException("Sensor 2 must be a single sensor, no previous merge!");
        }

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

    static String[] getNumObsSrcBandNames(String sensor1Name) {
        // e.g. MODIS_TERRA and OLCI_B: --> ["num_obs", "num_obs"]
        // e.g. MODIS_TERRA-MODIS_AQUA and OLCI_A: --> ["num_obs_MODIS_TERRA", "num_obs_MODIS_AQUA", "num_obs"]
        // e.g. MODIS_TERRA-MODIS_AQUA-OLCI_A and OLCI_B:
        //       --> ["num_obs_MODIS_TERRA", "num_obs_MODIS_AQUA", "num_obs_OLCI_A", "num_obs"]

        final int numMergesInSensor1 = StringUtils.countMatches(sensor1Name, "-");
        if (numMergesInSensor1 > 0) {
            final String prefix = TcwvConstants.NUM_OBS_L3_BAND_NAME + "_";
            final String[] sensor1SingleSensorNames = sensor1Name.split("-");
            String[] numObsSrcBandNames = new String[sensor1SingleSensorNames.length + 1];
            for (int i = 0; i < sensor1SingleSensorNames.length; i++) {
                numObsSrcBandNames[i] = prefix + sensor1SingleSensorNames[i];
            }
            numObsSrcBandNames[sensor1SingleSensorNames.length] = TcwvConstants.NUM_OBS_L3_BAND_NAME;
            return numObsSrcBandNames;
        } else {
            return new String[]{TcwvConstants.NUM_OBS_L3_BAND_NAME, TcwvConstants.NUM_OBS_L3_BAND_NAME};
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3DailyMergeNirNirPhase3Op.class);
        }
    }
}
