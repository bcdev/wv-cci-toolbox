package org.esa.snap.wvcci.tcwv.l3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

import java.awt.*;
import java.util.Map;

/**
 * Operator for sensor merging of TCWV L3 products of 2 or 3 sensors.
 * We have MERIS/MODIS (land) and HOAPS SSM/I (water).
 *
 * TEST VERSION using tile instead of pixel operator-
 *
 * <p>
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.Merge.Nir.Hoaps2", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for merge of TCWV L3 NIR and HOAPS daily products.")
public class L3DailyMergeNirHoaps2Op extends Operator {

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

    private Product targetProduct;

    private int width;
    private int height;

    // NIR bands
    private Band numObsL3Band;
    private Band tcwvL3Band;
    private Band tcwvSigmaL3Band;
    private Band tcwvUncertaintyL3Band;
    private Band tcwvUncertaintyCountsL3Band;
    private Band tcwvSumsSumL3Band;
    private Band tcwvSumsSumSqL3Band;
    private Band tcwvQualityFlagMajorityL3Band;
    private Band tcwvQualityFlagMinL3Band;
    private Band tcwvQualityFlagMaxL3Band;
    private Band surfaceTypeL3Band;

    // HOAPS bands
    private Band numObsHoapsBand;
    private Band tcwvHoapsBand;
    private Band tcwvSigmaHoapsBand;
    private Band tcwvPropagErrHoapsBand;
    private Band tcwvRandomErrHoapsBand;

    // landmask band
    private Band landmaskBand;

    // seaice band
    private Band seaiceBand;

    @Override
    public void initialize() throws OperatorException {
        mergeInputProducts = new Product[]{nirProduct, hoapsProduct};

        width = mergeInputProducts[0].getSceneRasterWidth();
        height = mergeInputProducts[0].getSceneRasterHeight();

        validate();

        if (landmaskProduct.getSceneRasterWidth() != width || landmaskProduct.getSceneRasterHeight() != height) {
            landMaskProductToUse = getResampledProduct(landmaskProduct);
        } else {
            landMaskProductToUse = landmaskProduct;
        }

        if (seaiceProduct != null) {
            if (seaiceProduct.getSceneRasterWidth() != width || seaiceProduct.getSceneRasterHeight() != height) {
                seaiceProductToUse = getResampledProduct(seaiceProduct);
            } else {
                seaiceProductToUse = seaiceProduct;
            }
        }

        initSourceBands();

        createTargetProduct();
    }

    private void initSourceBands() {
        numObsL3Band = nirProduct.getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME);
        tcwvL3Band = nirProduct.getBand(TcwvConstants.TCWV_L3_BAND_NAME);
        tcwvSigmaL3Band = nirProduct.getBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME);
        tcwvUncertaintyL3Band = nirProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME);
        tcwvUncertaintyCountsL3Band = nirProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME);
        tcwvSumsSumL3Band = nirProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME);
        tcwvSumsSumSqL3Band = nirProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME);
        tcwvQualityFlagMajorityL3Band = nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME);
        tcwvQualityFlagMinL3Band = nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME);
        tcwvQualityFlagMaxL3Band = nirProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME);
        surfaceTypeL3Band = nirProduct.getBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME);

        numObsHoapsBand = hoapsProduct.getBand(TcwvConstants.NUM_OBS_HOAPS_BAND_NAME);
        tcwvHoapsBand = hoapsProduct.getBand(TcwvConstants.TCWV_HOAPS_BAND_NAME);
        tcwvSigmaHoapsBand = hoapsProduct.getBand(TcwvConstants.TCWV_SIGMA_HOAPS_BAND_NAME);
        if (hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME) != null) {
            tcwvPropagErrHoapsBand = hoapsProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME);
            tcwvRandomErrHoapsBand = hoapsProduct.getBand(TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME);
        }

        landmaskBand = landmaskProduct.getBand("mask");
        if (seaiceProduct != null) {
            seaiceBand = seaiceProduct.getBand("mask_time" + dayOfMonth);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile numObsL3Tile = getSourceTile(numObsL3Band, targetRectangle);
        Tile tcwvL3Tile = getSourceTile(tcwvL3Band, targetRectangle);
        Tile tcwvSigmaL3Tile = getSourceTile(tcwvSigmaL3Band, targetRectangle);
        Tile tcwvUncertaintyL3Tile = getSourceTile(tcwvUncertaintyL3Band, targetRectangle);
        Tile tcwvUncertaintyCountsL3Tile = getSourceTile(tcwvUncertaintyCountsL3Band, targetRectangle);
        Tile tcwvSumsSumL3Tile = getSourceTile(tcwvSumsSumL3Band, targetRectangle);
        Tile tcwvSumsSumSqL3Tile = getSourceTile(tcwvSumsSumSqL3Band, targetRectangle);
        Tile tcwvQualityFlagMajorityL3Tile = getSourceTile(tcwvQualityFlagMajorityL3Band, targetRectangle);
        Tile tcwvQualityFlagMinL3Tile = getSourceTile(tcwvQualityFlagMinL3Band, targetRectangle);
        Tile tcwvQualityFlagMaxL3Tile = getSourceTile(tcwvQualityFlagMaxL3Band, targetRectangle);
        Tile surfaceTypeTile = getSourceTile(surfaceTypeL3Band, targetRectangle);

        Tile numObsHoapsTile = getSourceTile(numObsHoapsBand, targetRectangle);
        Tile tcwvHoapsTile = getSourceTile(tcwvHoapsBand, targetRectangle);
        Tile tcwvSigmaHoapsTile = getSourceTile(tcwvSigmaHoapsBand, targetRectangle);
        Tile tcwvPropagErrHoapsTile = null;
        Tile tcwvRandomErrHoapsTile = null;
        if (tcwvPropagErrHoapsBand != null) {
            tcwvPropagErrHoapsTile = getSourceTile(tcwvPropagErrHoapsBand, targetRectangle);
            tcwvRandomErrHoapsTile = getSourceTile(tcwvRandomErrHoapsBand, targetRectangle);
        }

        Tile landmaskTile = getSourceTile(landmaskBand, targetRectangle);
        Tile seaiceTile = null;
        if (seaiceProduct != null) {
            seaiceTile = getSourceTile(seaiceBand, targetRectangle);
        }

        final double srcNirTcwvNodata = tcwvL3Band.getNoDataValue();
        final double srcNirTcwvCountsNodata = tcwvUncertaintyCountsL3Band.getNoDataValue();
        final int srcHoapsNumObsNodata = (int) numObsHoapsBand.getNoDataValue();
        final double srcHoapsTcwvNodata = tcwvHoapsBand.getNoDataValue();
        double srcHoapsTcwvPropagErrNodata = 0;
        double srcHoapsTcwvRandomErrNodata = 0;
        if (tcwvPropagErrHoapsBand != null) {
            srcHoapsTcwvPropagErrNodata = tcwvPropagErrHoapsBand.getNoDataValue();
            srcHoapsTcwvRandomErrNodata = tcwvRandomErrHoapsBand.getNoDataValue();
        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                final int srcNirNumObs = numObsL3Tile.getSampleInt(x, y);
                final int srcHoapsNumObs = numObsHoapsTile.getSampleInt(x, y);
                final int numObsMerge = mergeNumObs(srcNirNumObs, srcHoapsNumObs, srcHoapsNumObsNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.NUM_OBS_L3_BAND_NAME)).setSample(x, y, numObsMerge);

                final double srcNirTcwvMean = tcwvL3Tile.getSampleDouble(x, y);
                final double srcHoapsTcwv = tcwvHoapsTile.getSampleDouble(x, y);
                final int srcLandMask = landmaskTile.getSampleInt(x, y);
                final int srcSeaiceMask = seaiceTile != null ? seaiceTile.getSampleInt(x, y) : -1;
                final double tcwvMerge =
                        merge(srcNirTcwvMean, srcNirTcwvNodata, srcHoapsTcwv, srcHoapsTcwvNodata,
                                srcHoapsNumObs, srcHoapsNumObsNodata, srcLandMask, srcSeaiceMask);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_L3_BAND_NAME)).setSample(x, y, tcwvMerge);

                final double srcNirTcwvSigma = tcwvSigmaL3Tile.getSampleDouble(x, y);
                final double srcHoapsTcwvSigma = tcwvSigmaHoapsTile.getSampleDouble(x, y);
                final double tcwvSigmaMerge =
                        merge(srcNirTcwvSigma, srcNirTcwvNodata, srcHoapsTcwvSigma, srcHoapsTcwvNodata,
                                srcHoapsNumObs, srcHoapsNumObsNodata, srcLandMask, srcSeaiceMask);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_SIGMA_L3_BAND_NAME)).setSample(x, y, tcwvSigmaMerge);

                final double srcNirTcwvUncertaintyMean = tcwvUncertaintyL3Tile.getSampleDouble(x, y);
                final double tcwvUncertaintyMeanMerge = useOriginal(srcNirTcwvUncertaintyMean, srcNirTcwvNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME)).setSample(x, y, tcwvUncertaintyMeanMerge);

                final double srcNirTcwvUncertaintyCounts = tcwvUncertaintyCountsL3Tile.getSampleDouble(x, y);
                final double tcwvUncertaintyCountsMerge = useOriginal(srcNirTcwvUncertaintyCounts, srcNirTcwvCountsNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_COUNTS_L3_BAND_NAME)).setSample(x, y, tcwvUncertaintyCountsMerge);

                final double srcNirTcwvSumsSum = tcwvSumsSumL3Tile.getSampleDouble(x, y);
                final double tcwvSumsSumMerge = useOriginal(srcNirTcwvSumsSum, srcNirTcwvNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_L3_BAND_NAME)).setSample(x, y, tcwvSumsSumMerge);

                final double srcNirTcwvSumsSumSq = tcwvSumsSumSqL3Tile.getSampleDouble(x, y);
                final double tcwvSumsSumSqMerge = useOriginal(srcNirTcwvSumsSumSq, srcNirTcwvNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_SUMS_SUM_SQ_L3_BAND_NAME)).setSample(x, y, tcwvSumsSumSqMerge);

                final int srcNirQualityMajorityFlag = tcwvQualityFlagMajorityL3Tile.getSampleInt(x, y);
                final int qualityFlagMajorityMerge = mergeQualityFlag(srcNirQualityMajorityFlag, srcHoapsNumObs, srcHoapsNumObsNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAJORITY_L3_BAND_NAME)).setSample(x, y, qualityFlagMajorityMerge);

                final int srcNirQualityMinFlag = tcwvQualityFlagMinL3Tile.getSampleInt(x, y);
                final int qualityFlagMinMerge = mergeQualityFlag(srcNirQualityMinFlag, srcHoapsNumObs, srcHoapsNumObsNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MIN_L3_BAND_NAME)).setSample(x, y, qualityFlagMinMerge);

                final int srcNirQualityMaxFlag = tcwvQualityFlagMaxL3Tile.getSampleInt(x, y);
                final int qualityFlagMaxMerge = mergeQualityFlag(srcNirQualityMaxFlag, srcHoapsNumObs, srcHoapsNumObsNodata);
                targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_MAX_L3_BAND_NAME)).setSample(x, y, qualityFlagMaxMerge);

                final int surfaceTypeFlag = surfaceTypeTile.getSampleInt(x, y);
                targetTiles.get(targetProduct.getBand(TcwvConstants.SURFACE_TYPE_FLAG_L3_BAND_NAME)).setSample(x, y, surfaceTypeFlag);

                if (tcwvPropagErrHoapsTile != null) {
                    final double srcHoapsTcwvPropagErr = tcwvPropagErrHoapsTile.getSampleDouble(x, y);
                    final double tcwvPropagErrMerge = useOriginal(srcHoapsTcwvPropagErr, srcHoapsTcwvPropagErrNodata);
                    targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_PROPAG_ERR_HOAPS_BAND_NAME)).setSample(x, y, tcwvPropagErrMerge);

                    final double srcHoapsTcwvRandomErr = tcwvRandomErrHoapsTile.getSampleDouble(x, y);
                    final double tcwvRandomErrMerge = useOriginal(srcHoapsTcwvRandomErr, srcHoapsTcwvRandomErrNodata);
                    targetTiles.get(targetProduct.getBand(TcwvConstants.TCWV_RANDOM_ERR_HOAPS_BAND_NAME)).setSample(x, y, tcwvRandomErrMerge);
                }
            }
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product(getId(), getClass().getName(), width, height);

        ProductUtils.copyGeoCoding(hoapsProduct, targetProduct);
        targetProduct.setStartTime(hoapsProduct.getStartTime());
        targetProduct.setEndTime(hoapsProduct.getEndTime());

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

        setTargetProduct(targetProduct);
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


    private Product getResampledProduct(Product inputProduct) {
        ResamplingOp resamplingOp = new ResamplingOp();
        resamplingOp.setSourceProduct(inputProduct);
        resamplingOp.setParameterDefaultValues();
        resamplingOp.setParameter("targetWidth", width);
        resamplingOp.setParameter("targetHeight", height);
        return resamplingOp.getTargetProduct();
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
            super(L3DailyMergeNirHoaps2Op.class);
        }
    }
}
