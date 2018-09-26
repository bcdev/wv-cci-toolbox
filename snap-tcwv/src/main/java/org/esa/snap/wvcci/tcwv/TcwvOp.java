package org.esa.snap.wvcci.tcwv;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

import static org.esa.snap.wvcci.tcwv.Sensor.MERIS;

/**
 * TCWV main operator for Water_Vapour_cci.
 * Authors: R.Preusker (Python breadboard), O.Danne (Java conversion), 2018
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv", version = "0.8",
        authors = "R.Preusker, O.Danne",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Spectral Earth, Brockmann Consult",
        description = "TCWV main operator for Water_Vapour_cci.")
public class TcwvOp extends Operator {

    @Parameter(valueSet = {"MERIS", "MODIS_AQUA", "MODIS_TERRA", "OLCI"},
            description = "The sensor (MERIS, MODIS or OLCI).")
    private Sensor sensor;

    @Parameter(valueSet = {"NO_FILTER", "CLOUD_SURE", "CLOUD_SURE_AMBIGUOUS"},
            defaultValue = "CLOUD_SURE_AMBIGUOUS",
            description = "Strength of cloud filter.",
            label = "Strength of cloud filter.")
    private CloudFilterLevel cloudFilterLevel;

    @Parameter(defaultValue = "303.0",
            description = "Temperature constant to be used if no Prior is available.")
    private double temperature;

    @Parameter(defaultValue = "1013.25",
            description = "MSL pressure constant to be used if no Prior is available.")
    private double mslPressure;

    @Parameter(defaultValue = "0.1",
            description = "Prior AOT at 865 nm.")
    private double aot865;


    @SourceProduct(description =
            "Source product (IdePix merged with MERIS, MODIS or OLCI L1b product",
            label = "Source product")
    private Product sourceProduct;

    private Product targetProduct;

    private int width;
    private int height;

    private RasterDataNode szaBand;
    private RasterDataNode vzaBand;
    private RasterDataNode saaBand;
    private RasterDataNode vaaBand;

    private Band pixelClassifBand;

    private Band priorT2mBand;
    private Band priorMslBand;
    private Band priorTcwvBand;
    private Band priorWsBand;

    private Band[] winBands;
    private Band[] absBands;

    private TcwvOceanLut oceanLut;
    private TcwvLandLut landLut;

    private TcwvAlgorithm tcwvAlgorithm;

    private TcwvFunction tcwvFunctionLand;
    private JacobiFunction jacobiFunctionland;
    private TcwvFunction tcwvFunctionOcean;
    private JacobiFunction jacobiFunctionOcean;

    @Override
    public void initialize() throws OperatorException {

        validateSourceProduct(sourceProduct);

        try {
            final Path auxdataPath = TcwvIO.installAuxdata();
            landLut = TcwvIO.readLandLookupTable(auxdataPath.toString(), sensor);
            oceanLut = TcwvIO.readOceanLookupTable(auxdataPath.toString(), sensor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        width = sourceProduct.getSceneRasterWidth();
        height = sourceProduct.getSceneRasterHeight();

        createTargetProduct();

        winBands = new Band[sensor.getWinBandNames().length];
        for (int i = 0; i < winBands.length; i++) {
            final String bandName = sensor.getWinBandNames()[i];
            winBands[i] = sourceProduct.getBand(bandName);
        }

        absBands = new Band[sensor.getAbsBandNames().length];
        for (int i = 0; i < absBands.length; i++) {
            final String bandName = sensor.getAbsBandNames()[i];
            absBands[i] = sourceProduct.getBand(bandName);
        }

        priorT2mBand = null;
        priorMslBand = null;
        priorTcwvBand = null;
        priorWsBand = null;
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_T2M_BAND_NAME)) {
            priorT2mBand = sourceProduct.getBand(TcwvConstants.PRIOR_T2M_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_MSL_BAND_NAME)) {
            priorMslBand = sourceProduct.getBand(TcwvConstants.PRIOR_MSL_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_TCWV_BAND_NAME)) {
            priorTcwvBand = sourceProduct.getBand(TcwvConstants.PRIOR_TCWV_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_WS_BAND_NAME)) {
            priorWsBand = sourceProduct.getBand(TcwvConstants.PRIOR_WS_BAND_NAME);
        }

        szaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[0]);
        vzaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[1]);
        saaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[2]);
        vaaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[3]);

        pixelClassifBand = sourceProduct.getBand(TcwvConstants.IDEPIX_CLASSIF_BAND_NAME);

        tcwvAlgorithm = new TcwvAlgorithm();

        tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);
        tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final String targetBandName = targetBand.getName();
        final Rectangle targetRectangle = targetTile.getRectangle();

        Tile[] winBandTiles = new Tile[winBands.length];
        for (int i = 0; i < winBandTiles.length; i++) {
            winBandTiles[i] = getSourceTile(winBands[i], targetRectangle);
        }

        Tile[] absBandTiles = new Tile[absBands.length];
        for (int i = 0; i < absBandTiles.length; i++) {
            absBandTiles[i] = getSourceTile(absBands[i], targetRectangle);
        }

        Tile szaTile = getSourceTile(szaBand, targetRectangle);
        Tile vzaTile = getSourceTile(vzaBand, targetRectangle);
        Tile saaTile = getSourceTile(saaBand, targetRectangle);
        Tile vaaTile = getSourceTile(vaaBand, targetRectangle);

        Tile pixelClassifTile = getSourceTile(pixelClassifBand, targetRectangle);

        Tile priorT2mTile = null;
        if (priorT2mBand != null) {
            priorT2mTile = getSourceTile(priorT2mBand, targetRectangle);
        }

        Tile priorMslTile = null;
        if (priorMslBand != null) {
            priorMslTile = getSourceTile(priorMslBand, targetRectangle);
        }

        Tile priorTcwvTile = null;
        if (priorTcwvBand != null) {
            priorTcwvTile = getSourceTile(priorTcwvBand, targetRectangle);
        }

        Tile priorWsTile = null;
        if (priorWsBand != null) {
            priorWsTile = getSourceTile(priorWsBand, targetRectangle);
        }

        double[] winBandData = new double[winBandTiles.length];
        double[] absBandData = new double[absBandTiles.length];
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean isValid = !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_INVALID_BIT);
                final boolean isCloud = isCloud(x, y, pixelClassifTile);
                boolean isLand = pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_LAND_BIT);

                // NOTE: we compute land only for MERIS, MODIS_TERRA, or OLCI! 20180925
                isLand = isLand && sensor != Sensor.MODIS_AQUA;
                if (!isValid || isCloud || !isLand) {
                    targetTile.setSample(x, y, Float.NaN);
                } else {
                    // Preparing input data...
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double vza = vzaTile.getSampleDouble(x, y);
                    final double saa = saaTile.getSampleDouble(x, y);
                    final double vaa = vaaTile.getSampleDouble(x, y);
                    final double relAzi = 180. - Math.abs(saa - vaa);
                    final double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
                    double prs =
                            priorMslTile != null ? priorMslTile.getSampleDouble(x, y) : mslPressure;
                    if (sensor == MERIS) {
                        // todo: reset this for new LUTs!
                        prs /= -100.0;
                    }
                    final double t2m =
                            priorT2mTile != null ? priorT2mTile.getSampleDouble(x, y) : temperature;
                    final double priorWs =
                            priorWsTile != null ? priorWsTile.getSampleDouble(x, y) : TcwvConstants.WS_INIT_VALUE;
                    final double priorAot = TcwvConstants.AOT865_INIT_VALUE;
                    final double priorAl0 = TcwvConstants.AL0_INIT_VALUE;
                    final double priorAl1 = TcwvConstants.AL1_INIT_VALUE;
                    final double priorTcwv =
                            priorTcwvTile != null ? priorTcwvTile.getSampleDouble(x, y) : TcwvConstants.TCWV_INIT_VALUE;

                    for (int i = 0; i < winBandData.length; i++) {
                        winBandData[i] = winBandTiles[i].getSampleDouble(x, y) * Math.cos(sza*MathUtils.DTOR);
                    }
                    for (int i = 0; i < absBandData.length; i++) {
                        absBandData[i] = absBandTiles[i].getSampleDouble(x, y)* Math.cos(sza*MathUtils.DTOR);
                    }

                    final TcwvAlgorithmInput input = new TcwvAlgorithmInput(winBandData, absBandData, sza, vza, relAzi,
                                                                            amf, aot865, priorAot, priorAl0, priorAl1,
                                                                            t2m, prs, priorWs, priorTcwv);

                    final TcwvResult result = tcwvAlgorithm.compute(sensor, landLut, oceanLut,
                                                                    tcwvFunctionLand, tcwvFunctionOcean,
                                                                    jacobiFunctionland, jacobiFunctionOcean,
                                                                    input, isLand);

                    if (targetBandName.equals(TcwvConstants.TCWV_BAND_NAME)) {
                        targetTile.setSample(x, y, result.getTcwv());
                    } else {
                        throw new OperatorException("Unexpected target band name: '" +
                                                            targetBandName + "' - exiting.");
                    }
                }
            }
        }


    }

    private boolean isCloud(int x, int y, Tile pixelClassifTile) {

        switch (cloudFilterLevel) {
            case NO_FILTER:
                return false;
            case CLOUD_SURE:
                return  pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
            case CLOUD_SURE_BUFFER:
                return  (pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                         pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT)) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT);
            case CLOUD_SURE_AMBIGUOUS:
                return  (pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                         pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT)) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
            case CLOUD_SURE_AMBIGUOUS_BUFFER:
                return  pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
            default:
                return false;
        }
    }

    private void validateSourceProduct(Product sourceProduct) {
        if (!sourceProduct.containsBand(TcwvConstants.IDEPIX_CLASSIF_BAND_NAME)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                                                "pixel classification flag band '" +
                                                TcwvConstants.IDEPIX_CLASSIF_BAND_NAME + "'.");
        }

        for (String bandName : sensor.getReflBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                                                    "mandatory band '" + bandName + "'.");
            }
        }
    }

    private void createTargetProduct() {
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), width, height);

        final Band tcwvBand = targetProduct.addBand(TcwvConstants.TCWV_BAND_NAME, ProductData.TYPE_FLOAT32);
        tcwvBand.setUnit("mm");
        tcwvBand.setDescription("Total column of water vapour");
        tcwvBand.setNoDataValue(Float.NaN);
        tcwvBand.setNoDataValueUsed(true);

        ProductUtils.copyBand(TcwvConstants.IDEPIX_CLASSIF_BAND_NAME, sourceProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        setTargetProduct(targetProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TcwvOp.class);
        }
    }
}
