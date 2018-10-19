package org.esa.snap.wvcci.tcwv.l3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.esa.snap.wvcci.tcwv.TcwvIO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Operator for post-processing of TCWV L3 daily products.
 *
 * Current actions are:
 * - rename tcwv_mean --> tcwv
 * - remove tcwv_sigma
 * - rename tcwv_uncertainty_mean --> tcwv_uncertainty
 * - remove tcwv_uncertainty_sigma
 * - filter with L3 LC land/water mask
 *
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.DailyPostProcessing", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for post-processing of TCWV L3 daily products.")
public class L3DailyPostProcessOp extends Operator {

    @Parameter(valueSet = {"005deg", "05deg"},
            description = "L3 resolution (0.5 or 0.05deg).")
    private String l3Resolution;

    @Parameter(description = "If auxdata are already installed, their path can be provided here.")
    private String auxdataPath;

    
    @SourceProduct(description =
            "Source product (TCWV L3 Daily global)",
            label = "Source product")
    private Product sourceProduct;

    private Product targetProduct;

    private int width;
    private int height;

    private Band tcwvSourceBand;
    private Band tcwvUncertaintySourceBand;
    private Band tcwvCountsSourceBand;

    private Band waterMaskBand;

    private static final String L3_WATERMASK_FILENAME_PREFIX = "ESACCI-LC-L4-WB-Ocean-Map_";

    private Product watermaskProduct;

    @Override
    public void initialize() throws OperatorException {

        validateSourceProduct(sourceProduct);

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

        createTargetProduct();

        tcwvSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_MEAN_BAND_NAME);
        tcwvUncertaintySourceBand = sourceProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_MEAN_BAND_NAME);
        tcwvCountsSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_COUNTS_BAND_NAME);

        waterMaskBand = watermaskProduct.getBand("band_1");
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile tcwvSourceTile = getSourceTile(tcwvSourceBand, targetRectangle);
        Tile tcwvUncertaintySourceTile = getSourceTile(tcwvUncertaintySourceBand, targetRectangle);
        Tile tcwvCountsSourceTile = getSourceTile(tcwvCountsSourceBand, targetRectangle);

        Tile waterMaskTile = getSourceTile(waterMaskBand, targetRectangle);

        final Band tcwvTargetBand = targetProduct.getBand(TcwvConstants.TCWV_BAND_NAME);
        final Band tcwvUnvertaintyTargetBand = targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME);
        final Band tcwvCountsTargetBand = targetProduct.getBand(TcwvConstants.TCWV_COUNTS_BAND_NAME);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean isLand = waterMaskTile.getSampleInt(x, y) == 1;
                final float tcwvSourceValue = tcwvSourceTile.getSampleFloat(x, y);
                // NOTE: we compute only land for MERIS, MODIS_TERRA, or OLCI! 20180925
                if (Float.isNaN(tcwvSourceValue) || !isLand) {
                    targetTiles.get(tcwvTargetBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvUnvertaintyTargetBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvCountsTargetBand).setSample(x, y, Float.NaN);
                } else {
                    targetTiles.get(tcwvTargetBand).setSample(x, y, tcwvSourceValue);
                    targetTiles.get(tcwvUnvertaintyTargetBand).setSample(x, y, tcwvUncertaintySourceTile.getSampleFloat(x, y));
                    targetTiles.get(tcwvCountsTargetBand).setSample(x, y, tcwvCountsSourceTile.getSampleFloat(x, y));
                }
            }
        }
    }

    private void validateSourceProduct(Product sourceProduct) {
        if (!sourceProduct.containsBand(TcwvConstants.TCWV_MEAN_BAND_NAME)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                    "TCWV band '" + TcwvConstants.TCWV_MEAN_BAND_NAME + "'.");
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), width, height);

        ProductUtils.copyBand(TcwvConstants.TCWV_MEAN_BAND_NAME, sourceProduct,
                              TcwvConstants.TCWV_BAND_NAME, targetProduct, false);
        ProductUtils.copyBand(TcwvConstants.TCWV_UNCERTAINTY_MEAN_BAND_NAME, sourceProduct,
                              TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME, targetProduct, false);
        ProductUtils.copyBand(TcwvConstants.TCWV_COUNTS_BAND_NAME, sourceProduct, targetProduct, false);

        targetProduct.setSceneGeoCoding(sourceProduct.getSceneGeoCoding());
        ProductUtils.copyMetadata(sourceProduct, targetProduct);  // todo: make final metadata CF and CCI compliant

        setTargetProduct(targetProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3DailyPostProcessOp.class);
        }
    }
}
