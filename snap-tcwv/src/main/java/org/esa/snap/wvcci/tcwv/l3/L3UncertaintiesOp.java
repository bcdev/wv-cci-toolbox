package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

/**
 * Operator for adding 'CCI uncertainties' in  TCWV L3 daily or monthly products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Uncertainties", version = "0.8",
        authors = "Olaf Danne",
        internal = true,
        description = "Operator for adding 'CCI uncertainties' in  TCWV L3 daily or monthly products.")
public class L3UncertaintiesOp extends PixelOperator {

    @Parameter(interval = "[0.0f, 1.0f]", defaultValue = "0.5",
            description = "Correlation parameter, see https://www.earth-syst-sci-data.net/9/881/2017/")
    private float c;

    @SourceProduct(description = "Source product")
    private Product sourceProduct;


    private static final int SRC_TCWV_MEAN = 0;
    private static final int SRC_TCWV_SUM = 1;
    private static final int SRC_TCWV_SUM_SQ = 2;
    private static final int SRC_TCWV_UNCERTAINTY_MEAN = 3;
    private static final int SRC_TCWV_UNCERTAINTY_SUM = 4;
    private static final int SRC_TCWV_UNCERTAINTY_SUM_SQ = 5;
    private static final int SRC_TCWV_COUNTS = 6;
    private static final int SRC_NUM_PASSES = 7;

    private static final int TRG_TCWV_MEAN = 0;
    //    private static final int TRG_TCWV_UNCERTAINTY_MEAN = 1;
    private static final int TRG_TCWV_UNCERTAINTY_SIGMA_SD_SQR = 1;     // eq. (1) CCI
    private static final int TRG_TCWV_UNCERTAINTY_SIGMA_MEAN = 2;       // eq. (2) CCI
    private static final int TRG_TCWV_UNCERTAINTY_SIGMA_SQR_MEAN = 3;   // eq. (3) CCI
    private static final int TRG_TCWV_COUNTS = 4;
    private static final int TRG_NUM_PASSES = 5;

    private static final String TCWV_MEAN_BAND_NAME = "tcwv_mean";
    private static final String TCWV_SUM_BAND_NAME = "tcwv_sum";
    private static final String TCWV_SUM_SQ_BAND_NAME = "tcwv_sum_sq";
    private static final String TCWV_UNCERTAINTY_MEAN_BAND_NAME = "tcwv_uncertainty_mean";  // eq. (5) CCI
    private static final String TCWV_UNCERTAINTY_SIGMA_SD_BAND_NAME = "tcwv_standard_deviation";  // eq. (1) CCI
    private static final String TCWV_UNCERTAINTY_SIGMA_MEAN_BAND_NAME = "tcwv_mean_uncertainty";  // eq. (2) CCI
    private static final String TCWV_UNCERTAINTY_SIGMA_SQR_MEAN_BAND_NAME = "tcwv_mean_uncertainty_squares";  // eq. (3) CCI
    private static final String TCWV_UNCERTAINTY_SUM_BAND_NAME = "tcwv_uncertainty_sum";  // eq. (5) CCI
    private static final String TCWV_UNCERTAINTY_SUM_SQ_BAND_NAME = "tcwv_uncertainty_sum_sq";
    private static final String TCWV_COUNTS_BAND_NAME = "tcwv_counts";
    private static final String NUM_PASSES_BAND_NAME = "num_passes";


    private static final String[] TCWV_SRC_BAND_NAMES = new String[]{
            TCWV_MEAN_BAND_NAME,
            TCWV_SUM_SQ_BAND_NAME,
            TCWV_COUNTS_BAND_NAME,
            TCWV_UNCERTAINTY_SUM_SQ_BAND_NAME
    };

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        validateInput();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final float tcwvMean = sourceSamples[SRC_TCWV_MEAN].getFloat();
        final float tcwvSum = sourceSamples[SRC_TCWV_SUM].getFloat();
        final float tcwvSumSq = sourceSamples[SRC_TCWV_SUM_SQ].getFloat();
        final float tcwvCounts = sourceSamples[SRC_TCWV_COUNTS].getFloat();
        final float tcwvUncertaintyMean = sourceSamples[SRC_TCWV_UNCERTAINTY_MEAN].getFloat();
        final float tcwvUncertaintySumSq = sourceSamples[SRC_TCWV_UNCERTAINTY_SUM_SQ].getFloat();
        int numPasses = -1;
        if (sourceProduct.containsBand(NUM_PASSES_BAND_NAME)) {
            numPasses = sourceSamples[SRC_NUM_PASSES].getInt();
        }

        if (tcwvCounts > 0.0 && !Float.isNaN(tcwvMean)) {
            // eq. (1), use sqrt:
            float sigmaSd = 0.0f;
            if (tcwvCounts > 1.0f) {
                sigmaSd = (float) Math.sqrt((tcwvSumSq / tcwvCounts - 2.0 * tcwvMean * tcwvSum /
                        tcwvCounts + tcwvMean * tcwvMean));
            }

            // eq. (2):
            final float sigmaMean = tcwvUncertaintyMean;

            // eq. (3):
            final float sigmaSqrMean = tcwvUncertaintySumSq / tcwvCounts;

            // eq. (4):
            final float sigmaTrueSqr = (float) (sigmaSd * sigmaSd - (1.0 - c) * sigmaSqrMean);

            // eq. (5):
            final float sigmaMeanUncertaintySq =
                    (float) (sigmaTrueSqr / tcwvCounts + c * sigmaMean * sigmaMean +
                            (1.0 - c) * sigmaSqrMean / tcwvCounts);
            final float sigmaMeanUncertainty = (float) Math.sqrt(sigmaMeanUncertaintySq);

            targetSamples[TRG_TCWV_MEAN].set(tcwvMean);
//            targetSamples[TRG_TCWV_UNCERTAINTY_MEAN].set(sigmaMeanUncertainty);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_SD_SQR].set(sigmaSd);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_MEAN].set(sigmaMean);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_SQR_MEAN].set(sigmaSqrMean);
        } else {
            targetSamples[TRG_TCWV_MEAN].set(Float.NaN);
//            targetSamples[TRG_TCWV_UNCERTAINTY_MEAN].set(Float.NaN);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_SD_SQR].set(Float.NaN);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_MEAN].set(Float.NaN);
            targetSamples[TRG_TCWV_UNCERTAINTY_SIGMA_SQR_MEAN].set(Float.NaN);
        }
        targetSamples[TRG_TCWV_COUNTS].set(tcwvCounts);
        if (sourceProduct.containsBand(NUM_PASSES_BAND_NAME)) {
            targetSamples[TRG_NUM_PASSES].set(numPasses);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.addBand(TCWV_MEAN_BAND_NAME, sourceProduct.getBand(TCWV_MEAN_BAND_NAME).getDataType());
//        targetProduct.addBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME,
//                              sourceProduct.getBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME).getDataType());
        targetProduct.addBand(TCWV_UNCERTAINTY_SIGMA_SD_BAND_NAME,
                              sourceProduct.getBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME).getDataType());
        targetProduct.addBand(TCWV_UNCERTAINTY_SIGMA_MEAN_BAND_NAME,
                              sourceProduct.getBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME).getDataType());
        targetProduct.addBand(TCWV_UNCERTAINTY_SIGMA_SQR_MEAN_BAND_NAME,
                              sourceProduct.getBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME).getDataType());


        targetProduct.addBand(TCWV_COUNTS_BAND_NAME, sourceProduct.getBand(TCWV_COUNTS_BAND_NAME).getDataType());
        if (sourceProduct.containsBand(NUM_PASSES_BAND_NAME)) {
            targetProduct.addBand(NUM_PASSES_BAND_NAME, sourceProduct.getBand(NUM_PASSES_BAND_NAME).getDataType());
        }

        for (Band b : targetProduct.getBands()) {
            final Band sourceBand = sourceProduct.getBand(b.getName());
            if (sourceBand != null) {
                TcwvUtils.copyBandProperties(b, sourceBand);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_TCWV_MEAN, TCWV_MEAN_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_SUM, TCWV_SUM_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_SUM_SQ, TCWV_SUM_SQ_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_UNCERTAINTY_MEAN, TCWV_UNCERTAINTY_MEAN_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_UNCERTAINTY_SUM, TCWV_UNCERTAINTY_SUM_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_UNCERTAINTY_SUM_SQ, TCWV_UNCERTAINTY_SUM_SQ_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_COUNTS, TCWV_COUNTS_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_NUM_PASSES, NUM_PASSES_BAND_NAME, sourceProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_TCWV_MEAN, TCWV_MEAN_BAND_NAME);
//        configurator.defineSample(TRG_TCWV_UNCERTAINTY_MEAN, TCWV_UNCERTAINTY_MEAN_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY_SIGMA_SD_SQR, TCWV_UNCERTAINTY_SIGMA_SD_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY_SIGMA_MEAN, TCWV_UNCERTAINTY_SIGMA_MEAN_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY_SIGMA_SQR_MEAN, TCWV_UNCERTAINTY_SIGMA_SQR_MEAN_BAND_NAME);
        configurator.defineSample(TRG_TCWV_COUNTS, TCWV_COUNTS_BAND_NAME);
        configurator.defineSample(TRG_NUM_PASSES, NUM_PASSES_BAND_NAME);
    }

    private void validateInput() {
        // band names
        for (String tcwvSrcBandName : TCWV_SRC_BAND_NAMES) {
            if (!sourceProduct.containsBand(tcwvSrcBandName)) {
                throw new OperatorException("Source product '" + sourceProduct.getName() + "' does not" +
                                                    "contain mandatory band '" + tcwvSrcBandName + "'.");
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3UncertaintiesOp.class);
        }
    }
}
