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
    private static final int SRC_TCWV_SIGMA = 1;
    private static final int SRC_TCWV_SUM = 2;
    private static final int SRC_TCWV_SUM_SQ = 3;
    private static final int SRC_TCWV_COUNTS = 4;
    private static final int SRC_TCWV_UNCERTAINTY_SUM_SQ = 8;

    private static final int TRG_TCWV_MEAN = 0;
    private static final int TRG_TCWV_MEAN_UNCERTAINTY = 1;
    private static final int TRG_TCWV_COUNTS = 2;

    private static final String TCWV_MEAN_BAND_NAME = "tcwv_mean";
    private static final String TCWV_UNCERTAINTY_MEAN_BAND_NAME = "tcwv_uncertainty_mean";  // eq. (5) CCI
    private static final String TCWV_COUNTS_BAND_NAME = "tcwv_counts";

    private static final String TCWV_SUM_SRC_BAND_NAME = "tcwv_sum";
    private static final String TCWV_SUM_SQ_SRC_BAND_NAME = "tcwv_sum_sq";
    private static final String TCWV_UNCERTAINTY_SUM_SQ_SRC_BAND_NAME = "tcwv_unvertainty_sum_sq";


    private static final String[] TCWV_SRC_BAND_NAMES = new String[]{
            TCWV_MEAN_BAND_NAME,
            TCWV_SUM_SQ_SRC_BAND_NAME,
            TCWV_COUNTS_BAND_NAME,
            TCWV_UNCERTAINTY_SUM_SQ_SRC_BAND_NAME
    };

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        validate();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final float tcwvMean = sourceSamples[SRC_TCWV_MEAN].getFloat();
        final float tcwvSum = sourceSamples[SRC_TCWV_SUM].getFloat();
        final float tcwvSumSq = sourceSamples[SRC_TCWV_SUM_SQ].getFloat();
        final float tcwvCounts = sourceSamples[SRC_TCWV_COUNTS].getFloat();
        final float tcwvUncertaintySumSq = sourceSamples[SRC_TCWV_UNCERTAINTY_SUM_SQ].getFloat();

        if (Float.isNaN(tcwvMean)) {
            targetSamples[TRG_TCWV_MEAN].set(Float.NaN);
            targetSamples[TRG_TCWV_MEAN_UNCERTAINTY].set(Float.NaN);
            targetSamples[TRG_TCWV_COUNTS].set(Float.NaN);
        } else {
            // eq. (1):
            final float sigmaSdSqr =
                    (float) (tcwvSumSq / tcwvCounts - 2.0 * tcwvMean * tcwvSum / tcwvCounts + tcwvMean * tcwvMean);

            // eq. (3):
            final float sigmaSqrMean = tcwvUncertaintySumSq / tcwvCounts;

            // eq. (4):
            // float sigma_TRUE_sqr = sigma_SD_sqr - (1.0f - c) * sumSqrUnc / counts;
            final float sigmaTrueSqr = (float) (sigmaSdSqr - (1.0 - c) * sigmaSqrMean);

            // eq. (5):
            // float sigma_FINAL_sqr = sigma_TRUE_sqr/counts + c*meanUnc*meanUnc + (1.0f - c) * sumSqrUnc / (counts*counts);
            final float sigmaMeanUncertaintySq =
                    (float) (sigmaTrueSqr/tcwvCounts + c * sigmaSqrMean * sigmaSqrMean +
                            (1.0 - c) * sigmaSqrMean / tcwvCounts);

            targetSamples[TRG_TCWV_MEAN].set(tcwvMean);
            targetSamples[TRG_TCWV_MEAN_UNCERTAINTY].set(sigmaMeanUncertaintySq);
            targetSamples[TRG_TCWV_COUNTS].set(tcwvCounts);

        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.addBand(TCWV_MEAN_BAND_NAME, sourceProduct.getBand(TCWV_MEAN_BAND_NAME).getDataType());
        targetProduct.addBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME,
                              sourceProduct.getBand(TCWV_UNCERTAINTY_MEAN_BAND_NAME).getDataType());
        targetProduct.addBand(TCWV_COUNTS_BAND_NAME, sourceProduct.getBand(TCWV_COUNTS_BAND_NAME).getDataType());

        for (Band b : targetProduct.getBands()) {
            final Band sourceBand = sourceProduct.getBand(b.getName());
            b.setNoDataValue(sourceBand.getNoDataValue());
            b.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            b.setScalingFactor(sourceBand.getScalingFactor());
            b.setScalingOffset(sourceBand.getScalingOffset());
            b.setUnit(sourceBand.getUnit());
            b.setDescription(sourceBand.getDescription());
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_TCWV_MEAN, TCWV_MEAN_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_SUM, TCWV_SUM_SRC_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_SUM_SQ, TCWV_SUM_SQ_SRC_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_COUNTS, TCWV_COUNTS_BAND_NAME, sourceProduct);
        configurator.defineSample(SRC_TCWV_UNCERTAINTY_SUM_SQ, TCWV_UNCERTAINTY_SUM_SQ_SRC_BAND_NAME, sourceProduct);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_TCWV_MEAN, TcwvConstants.TCWV_TARGET_BAND_NAME);
        configurator.defineSample(TRG_TCWV_MEAN_UNCERTAINTY, TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME);
        configurator.defineSample(TRG_TCWV_COUNTS, TcwvConstants.TCWV_COUNTS_TARGET_BAND_NAME);
    }

    private void validate() {
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
