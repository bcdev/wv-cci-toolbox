package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.pointop.*;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

/**
 * Operator for sensor merging of TCWV L3 products of 2 or 3 sensors.
 * We have MERIS/MODIS (land) and HOAPS SSM/I (water).
 * <p>
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for post-processing of TCWV L3 daily products.")
public class L3MergeSensorsOp extends PixelOperator {

    @Parameter(valueSet = {"005deg", "05deg"},
            description = "L3 resolution (0.5 or 0.05deg).")
    private String l3Resolution;

    @Parameter(description = "If auxdata are already installed, their path can be provided here.")
    private String auxdataPath;


//    @SourceProduct(description = "Source product 1")
//    private Product sourceProduct1;
//    @SourceProduct(description = "Source product 2")
//    private Product sourceProduct2;
//    @SourceProduct(description = "Source product 3", optional = true)
//    private Product sourceProduct3;

    @SourceProducts(description = "Source products")
    private Product[] sourceProducts;

    private Product targetProduct;

    private int numSrcProducts;

    private int width;
    private int height;

    private int[] SRC_TCWV;
    private int[] SRC_TCWV_UNCERTAINTY;
    private int[] SRC_TCWV_COUNTS;

    private static final int TRG_TCWV = 0;
    private static final int TRG_TCWV_UNCERTAINTY = 1;
    private static final int TRG_TCWV_COUNTS = 2;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        width = sourceProducts[0].getSceneRasterWidth();
        height = sourceProducts[0].getSceneRasterHeight();

        numSrcProducts = sourceProducts.length;
        validate();

        SRC_TCWV = new int[numSrcProducts];
        SRC_TCWV_UNCERTAINTY = new int[numSrcProducts];
        SRC_TCWV_COUNTS = new int[numSrcProducts];

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double[] srcTcwv = new double[numSrcProducts];
        final double[] srcTcwvNodata = new double[numSrcProducts];
        final double[] srcTcwvUncertainty = new double[numSrcProducts];
        final double[] srcTcwvCounts = new double[numSrcProducts];
        final double[] srcTcwvCountsNodata = new double[numSrcProducts];
        for (int i = 0; i < numSrcProducts; i++) {
            srcTcwv[i] = sourceSamples[SRC_TCWV[i]].getDouble();
            srcTcwvNodata[i] = sourceProducts[i].getBand(TcwvConstants.TCWV_BAND_NAME).getNoDataValue();
            srcTcwvUncertainty[i] = sourceSamples[SRC_TCWV_UNCERTAINTY[i]].getDouble();
            srcTcwvCounts[i] = sourceSamples[SRC_TCWV_COUNTS[i]].getDouble();
            srcTcwvCountsNodata[i] = sourceProducts[i].getBand(TcwvConstants.TCWV_COUNTS_BAND_NAME).getNoDataValue();
        }

        final double[] tcwvMerge = mergeTcwv(numSrcProducts, srcTcwv, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);
        final double[] tcwvUncertaintyMerge =
                mergeTcwv(numSrcProducts, srcTcwvUncertainty, srcTcwvCounts, srcTcwvNodata, srcTcwvCountsNodata);

        targetSamples[TRG_TCWV].set(tcwvMerge[0]);
        targetSamples[TRG_TCWV_UNCERTAINTY].set(tcwvUncertaintyMerge[0]);
        targetSamples[TRG_TCWV_COUNTS].set(tcwvMerge[1]);
    }
    
    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        for (int i = 0; i < numSrcProducts; i++) {
            SRC_TCWV[i] = 3 * i;
            SRC_TCWV_UNCERTAINTY[i] = 3 * i + 1;
            SRC_TCWV_COUNTS[i] = 3 * i + 2;
        }

        targetProduct.addBand(TcwvConstants.TCWV_BAND_NAME,
                                              sourceProducts[0].getBand(TcwvConstants.TCWV_BAND_NAME).getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME,
                                                         sourceProducts[0].getBand(TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME).
                                                                 getDataType());
        targetProduct.addBand(TcwvConstants.TCWV_COUNTS_BAND_NAME,
                                                    sourceProducts[0].getBand(TcwvConstants.TCWV_COUNTS_BAND_NAME).getDataType());

        for (Band b : targetProduct.getBands()) {
            final Band sourceBand = sourceProducts[0].getBand(b.getName());
            b.setNoDataValue(sourceBand.getNoDataValue());
            b.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            b.setScalingFactor(sourceBand.getScalingFactor());
            b.setScalingOffset(sourceBand.getScalingOffset());
            b.setUnit(sourceBand.getUnit());
            b.setDescription(sourceBand.getDescription());
        }

    }

    @Override
    protected Product createTargetProduct() throws OperatorException {
        return super.createTargetProduct();
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < numSrcProducts; i++) {
            configurator.defineSample(SRC_TCWV[i], TcwvConstants.TCWV_BAND_NAME, sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_UNCERTAINTY[i], TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME,
                                      sourceProducts[i]);
            configurator.defineSample(SRC_TCWV_COUNTS[i], TcwvConstants.TCWV_COUNTS_BAND_NAME, sourceProducts[i]);
        }
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_TCWV, TcwvConstants.TCWV_BAND_NAME);
        configurator.defineSample(TRG_TCWV_UNCERTAINTY, TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME);
        configurator.defineSample(TRG_TCWV_COUNTS, TcwvConstants.TCWV_COUNTS_BAND_NAME);
    }

    static double[] mergeTcwv(int numProducts, double[] srcTcwv, double[] srcTcwvCounts,
                              double[] srcTcwvNodata, double[] srcTcwvCountsNodata) {
        double tcwv = 0.0;
        double tcwvCounts = 0.0;
        for (int i = 0; i < numProducts; i++) {
            if (!Double.isNaN(srcTcwv[i]) && !Double.isNaN(srcTcwvCounts[i]) &&
                    srcTcwv[i] != srcTcwvNodata[i] && srcTcwvCounts[i] != srcTcwvCountsNodata[i]) {
                tcwv += srcTcwvCounts[i] * srcTcwv[i];
                tcwvCounts += srcTcwvCounts[i];
            }
        }
        tcwv /= tcwvCounts;

        return new double[]{tcwv, tcwvCounts};
    }

    private void validate() {
        // number of products
        if (numSrcProducts != 2 && numSrcProducts != 3) {
            throw new OperatorException("Number of source products must be 2 or 3");
        }

        // product dimensions
        final int width2 = sourceProducts[1].getSceneRasterWidth();
        final int height2 = sourceProducts[1].getSceneRasterHeight();
        if (width != width2 || height != height2) {
            throw new OperatorException("Dimension of first source product (" + width + "/" + height +
                                                ") differs from second source product (" + width2 + "/" + height2 + ").");
        } else {
            if (numSrcProducts == 3) {
                final int width3 = sourceProducts[2].getSceneRasterWidth();
                final int height3 = sourceProducts[2].getSceneRasterHeight();
                if (width != width3 || height != height3) {
                    throw new OperatorException("Dimension of first source product (" + width + "/" + height +
                                                        ") differs from third source product (" + width3 + "/" + height3 + ").");
                }
            }
        }

        // band names
        // todo

        // time ranges
        // todo
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3MergeSensorsOp.class);
        }
    }
}
