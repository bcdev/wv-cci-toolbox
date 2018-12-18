package org.esa.snap.wvcci.tcwv.l3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.esa.snap.wvcci.tcwv.TcwvIO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Master operator for post-processing of TCWV L3 products.
 * <p>
 * Current standard actions are:
 * - rename tcwv mean source band --> tcwv
 * - rename tcwv uncertainty source band --> tcwv_uncertainty
 * - rename tcwv counts source band --> tcwv_counts
 * - filter with L3 LC land/water mask
 * <p>
 * <p/>
 *
 * @author Olaf Danne
 */
abstract class L3PostProcessOp extends Operator {

    @Parameter(valueSet = {"005deg", "05deg"},
            description = "L3 resolution (0.5 or 0.05deg).")
    private String l3Resolution;

    @Parameter(description = "If auxdata are already installed, their path can be provided here.")
    private String auxdataPath;

    @Parameter(valueSet = {"0", "1", "2"}, defaultValue = "0",
            description = "Land/water processing mode: 0 = land only, 1 = water only, 2 = everywhere.")
    private int landWaterMode;


    @SourceProduct(description =
            "Source product (TCWV L3 Daily global)",
            label = "Source product")
    Product sourceProduct;

    private Product targetProduct;

    Band tcwvSourceBand;
    Band tcwvUncertaintySourceBand;
    Band tcwvCountsSourceBand;

    private int width;
    private int height;

    private Product watermaskProduct;
    private Band waterMaskBand;

    private static final String L3_WATERMASK_FILENAME_PREFIX = "ESACCI-LC-L4-WB-Ocean-Map_";

    @Override
    public void initialize() throws OperatorException {
        try {
            if (auxdataPath == null || auxdataPath.length() == 0) {
                auxdataPath = TcwvIO.installAuxdataL3();
            }
            final String waterMaskFileName = L3_WATERMASK_FILENAME_PREFIX + l3Resolution + ".nc";
            final String watermaskFilePath = auxdataPath + File.separator + waterMaskFileName;
            watermaskProduct = ProductIO.readProduct(new File(watermaskFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        width = sourceProduct.getSceneRasterWidth();
        height = sourceProduct.getSceneRasterHeight();

        waterMaskBand = watermaskProduct.getBand("band_1");
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile tcwvSourceTile = getSourceTile(tcwvSourceBand, targetRectangle);
        Tile tcwvUncertaintySourceTile = getSourceTile(tcwvUncertaintySourceBand, targetRectangle);
        Tile tcwvCountsSourceTile = getSourceTile(tcwvCountsSourceBand, targetRectangle);

        Tile waterMaskTile = getSourceTile(waterMaskBand, targetRectangle);

        final Band tcwvTargetBand = targetProduct.getBand(TcwvConstants.TCWV_TARGET_BAND_NAME);
        final Band tcwvUnvertaintyTargetBand = targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME);
        final Band tcwvCountsTargetBand = targetProduct.getBand(TcwvConstants.TCWV_COUNTS_TARGET_BAND_NAME);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean isLand = waterMaskTile.getSampleInt(x, y) == 1;
                final float tcwvSourceValue = tcwvSourceTile.getSampleFloat(x, y);
                final float tcwvUncertaintySourceValue = tcwvUncertaintySourceTile.getSampleFloat(x, y);
                final boolean processPixel =
                        (isLand && landWaterMode == 0) || (!isLand && landWaterMode == 1) || landWaterMode == 2;
                // We compute only land for MERIS, MODIS_TERRA, or OLCI.
                // We compute only water for SSM/I.
                if (processPixel && tcwvSourceValue > 0.0f && tcwvUncertaintySourceValue > 0.0f) {
                    targetTiles.get(tcwvTargetBand).setSample(x, y, tcwvSourceValue);
                    targetTiles.get(tcwvUnvertaintyTargetBand).setSample(x, y, tcwvUncertaintySourceTile.getSampleFloat(x, y));
                    targetTiles.get(tcwvCountsTargetBand).setSample(x, y, tcwvCountsSourceTile.getSampleFloat(x, y));
                } else {
                    targetTiles.get(tcwvTargetBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvUnvertaintyTargetBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvCountsTargetBand).setSample(x, y, Float.NaN);
                }

            }
        }
    }

    void validateSourceProduct(Product sourceProduct, String tcwvMeanBandName) {
        if (!sourceProduct.containsBand(tcwvMeanBandName)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                                                "TCWV band '" + tcwvMeanBandName + "'.");
        }
    }

    void createTargetProduct(String srcMeanBandName, String srcUncertaintyBandName, String srcCountsBandName) {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), width, height);

        final Band tcwvTargetBand = ProductUtils.copyBand(srcMeanBandName, sourceProduct,
                                                          TcwvConstants.TCWV_TARGET_BAND_NAME, targetProduct, false);
        final Band tcwvUncertaintyTargetBand = ProductUtils.copyBand(srcUncertaintyBandName, sourceProduct,
                                                                     TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME, targetProduct, false);
        final Band tcwvCountsTargetBand = ProductUtils.copyBand(srcCountsBandName, sourceProduct,
                                                                TcwvConstants.TCWV_COUNTS_TARGET_BAND_NAME, targetProduct, false);

        tcwvTargetBand.setUnit("kg/m^2");
        tcwvUncertaintyTargetBand.setUnit("kg/m^2");
        tcwvCountsTargetBand.setUnit("dl");

        for (Band band : targetProduct.getBands()) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(Float.NaN);
        }

        targetProduct.setSceneGeoCoding(sourceProduct.getSceneGeoCoding());
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        setTargetProduct(targetProduct);
    }

}
