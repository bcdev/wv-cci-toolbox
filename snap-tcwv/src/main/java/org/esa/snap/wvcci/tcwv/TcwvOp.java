package org.esa.snap.wvcci.tcwv;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.dataio.mod35.ModisMod35L2Constants;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

/**
 * TCWV main operator for Water_Vapour_cci.
 * Authors: R.Preusker (Python breadboard), O.Danne (Java conversion), 2018
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv", version = "0.81",
        authors = "R.Preusker, O.Danne",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Spectral Earth, Brockmann Consult",
        description = "TCWV main operator for Water_Vapour_cci.")
public class TcwvOp extends Operator {

    @Parameter(valueSet = {"MERIS", "MODIS_AQUA", "MODIS_TERRA", "OLCI"},
            description = "The sensor (MERIS, MODIS or OLCI).")
    private Sensor sensor;

    @Parameter(valueSet = {"NO_FILTER", "CLOUD_SURE", "CLOUD_SURE_BUFFER",
            "CLOUD_SURE_AMBIGUOUS", "CLOUD_SURE_AMBIGUOUS_BUFFER"},
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

    @Parameter(description = "If auxdata are already installed, their path can be provided here.")
    private String auxdataPath;

    @Parameter(defaultValue = "true",
            description = "Process also over ocean (would be needed for MODIS-AQUA option later).")
    private boolean processOcean;


    @SourceProduct(description =
            "Source product (IdePix product merged with MERIS, MODIS or OLCI L1b product)",
            label = "Source product")
    private Product sourceProduct;

    @SourceProduct(description =
            "MOD35 L2 cloud product (optional, used for MODIS processing only)",
            optional = true,
            label = "MOD35 L2 product")
    private Product mod35Product;

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

    private boolean mod35Used;

    @Override
    public void initialize() throws OperatorException {

        validateSourceProduct(sensor, sourceProduct);
        if (mod35Product != null && (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA)) {
            validateMod35Product(mod35Product);
            pixelClassifBand = mod35Product.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            mod35Used = true;
        } else {
            pixelClassifBand = sourceProduct.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            mod35Used = false;
        }

        try {
            if (auxdataPath == null || auxdataPath.length() == 0) {
                auxdataPath = TcwvIO.installAuxdataLuts();
            }
            landLut = TcwvIO.readLandLookupTable(auxdataPath, sensor);
            if (processOcean) {
                oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, sensor);
            }
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

        tcwvAlgorithm = new TcwvAlgorithm();

        tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);
        if (processOcean) {
            tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
            jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);
        }

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final Band tcwvBand = targetProduct.getBand(TcwvConstants.TCWV_TARGET_BAND_NAME);
        final Band tcwvUnvertaintyBand = targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME);

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

        double[] absBandData = new double[absBandTiles.length];
        // For old CAWA LUTs we still have to consider that MODIS land uses 5 input bands, MODIS ocean only 4:
        double[] winBandData = new double[winBandTiles.length];
        double[] winBandDataModisOcean = new double[winBandTiles.length - 1];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean isValid = mod35Used ||
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_INVALID_BIT);
                final boolean isCloud = mod35Used ? isMod35Cloud(x, y, pixelClassifTile) :
                        isIdepixCloud(x, y, pixelClassifTile);
                boolean isLand = mod35Used ? isMod35Land(x, y, pixelClassifTile) : isIdepixLand(x, y, pixelClassifTile);

                if (!isValid || isCloud || (!processOcean && !isLand)) {
                    targetTiles.get(tcwvBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvUnvertaintyBand).setSample(x, y, Float.NaN);
                } else {
                    // Preparing input data...
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double vza = vzaTile.getSampleDouble(x, y);
                    final double saa = saaTile.getSampleDouble(x, y);
                    final double vaa = vaaTile.getSampleDouble(x, y);
                    final double relAzi = 180. - Math.abs(saa - vaa);
                    final double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

                    // we have as pressure:
                    // ERA Interim: Pa, e.g. 100500  --> divide by -100 to get negative hPa for current LUTs
                    // no ERAInterim: hPa --> multiply by -1 to get negative hPa
                    double prs;
                    if (priorMslTile != null) {
                        prs = -priorMslTile.getSampleDouble(x, y) / 100.0;
                    } else {
                        prs = -1.0 * mslPressure;
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

                    TcwvAlgorithmInput input;

                    for (int i = 0; i < absBandData.length; i++) {
                        // multiply with cos(sza), see CAWA: cawa_tcwv_<sensor>_op.py!
                        absBandData[i] = absBandTiles[i].getSampleDouble(x, y) * Math.cos(sza * MathUtils.DTOR);
                    }

                    final boolean isModisOceanPixel =
                            (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) && !isLand;
                    if (isModisOceanPixel) {
                        // we have to skip winBandTiles[1]
                        winBandDataModisOcean[0] = winBandTiles[0].getSampleDouble(x, y) * Math.cos(sza * MathUtils.DTOR);
                        for (int i = 2; i < winBandDataModisOcean.length; i++) {
                            winBandDataModisOcean[i - 1] =
                                    winBandTiles[i].getSampleDouble(x, y) * Math.cos(sza * MathUtils.DTOR);
                        }
                        input = new TcwvAlgorithmInput(winBandDataModisOcean, absBandData, sza, vza, relAzi,
                                                       amf, aot865, priorAot, priorAl0, priorAl1,
                                                       t2m, prs, priorWs, priorTcwv);
                    } else {
                        for (int i = 0; i < winBandData.length; i++) {
                            // multiply with cos(sza), see CAWA: cawa_tcwv_<sensor>_op.py!
                            winBandData[i] = winBandTiles[i].getSampleDouble(x, y) * Math.cos(sza * MathUtils.DTOR);
                        }
                        input = new TcwvAlgorithmInput(winBandData, absBandData, sza, vza, relAzi,
                                                       amf, aot865, priorAot, priorAl0, priorAl1,
                                                       t2m, prs, priorWs, priorTcwv);
                    }

                    // 'ocean' parameters are null for land processing!
                    final TcwvResult result = tcwvAlgorithm.compute(sensor, landLut, oceanLut,
                                                                    tcwvFunctionLand, tcwvFunctionOcean,
                                                                    jacobiFunctionland, jacobiFunctionOcean,
                                                                    input, isLand);

                    targetTiles.get(tcwvBand).setSample(x, y, result.getTcwv());
                    // todo: uncertainty tbd. Set 3% for the moment.
                    targetTiles.get(tcwvUnvertaintyBand).setSample(x, y, 0.03 * result.getTcwv());
                }
            }
        }
    }

    private boolean isIdepixCloud(int x, int y, Tile pixelClassifTile) {

        switch (cloudFilterLevel) {
            case NO_FILTER:
                return false;
            case CLOUD_SURE:
                if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
                    // for Idepix MODIS, CLOUD_SURE and CLOUD_AMBIGUOUS are actually the same
                    return pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) &&
                            !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
                } else {
                    return pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) &&
                            !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT) &&
                            !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
                }
            case CLOUD_SURE_BUFFER:
                return (pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT)) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT);
            case CLOUD_SURE_AMBIGUOUS:
                return (pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT)) &&
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
            case CLOUD_SURE_AMBIGUOUS_BUFFER:
                return pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_SURE_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_AMBIGUOUS_BIT) ||
                        pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_CLOUD_BUFFER_BIT);
            default:
                return false;
        }
    }

    private boolean isIdepixLand(int x, int y, Tile pixelClassifTile) {
        return pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_LAND_BIT);
    }

    private boolean isMod35Cloud(int x, int y, Tile pixelClassifTile) {
        return pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_DETERMINED_BIT_INDEX) &&
                (pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_CERTAIN_BIT_INDEX) ||
                        pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_UNCERTAIN_BIT_INDEX));

    }

    private boolean isMod35Land(int x, int y, Tile pixelClassifTile) {
        return pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.SNOW_ICE_BIT_INDEX) ||
                pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.DESERT_BIT_INDEX) ||
                pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.LAND_BIT_INDEX);
    }

    private static void validateSourceProduct(Sensor sensor, Product sourceProduct) {
        if (!sourceProduct.containsBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                                                "pixel classification flag band '" +
                                                TcwvConstants.PIXEL_CLASSIF_BAND_NAME + "'.");
        }

        for (String bandName : sensor.getReflBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                                                    "mandatory band '" + bandName + "'.");
            }
        }
    }

    private static void validateMod35Product(Product mod35Product) {
        for (String bandName : TcwvConstants.MOD35_BAND_NAMES) {
            if (!mod35Product.containsBand(bandName)) {
                throw new OperatorException("MOD35 product is not valid, as it does not contain " +
                                                    "mandatory band '" + bandName + "'.");
            }
        }
    }


    private void createTargetProduct() {
        targetProduct = new Product(getId(), getClass().getName(), width, height);

//        final Band tcwvBand = targetProduct.addBand(TcwvConstants.TCWV_BAND_NAME, ProductData.TYPE_FLOAT32);
        final Band tcwvBand = targetProduct.addBand(TcwvConstants.TCWV_TARGET_BAND_NAME, ProductData.TYPE_UINT16);
        tcwvBand.setScalingFactor(0.01);
        tcwvBand.setUnit("mm");
        tcwvBand.setDescription("Total Column of Water Vapour");
        tcwvBand.setNoDataValue(Float.NaN);
        tcwvBand.setNoDataValueUsed(true);

        final Band tcwvUncertaintyBand =
//                targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME, ProductData.TYPE_FLOAT32);
                targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME, ProductData.TYPE_UINT16);
        tcwvUncertaintyBand.setScalingFactor(0.01);
        tcwvUncertaintyBand.setUnit("mm");
        tcwvUncertaintyBand.setDescription("Uncertainty of Total Column of Water Vapour");
        tcwvUncertaintyBand.setNoDataValue(Float.NaN);
        tcwvUncertaintyBand.setNoDataValueUsed(true);

        ProductUtils.copyBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME, sourceProduct, targetProduct, true);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        final TiePointGrid latTpg = targetProduct.getTiePointGrid("latitude");
        final TiePointGrid lonTpg = targetProduct.getTiePointGrid("longitude");
        final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latTpg, lonTpg);
        targetProduct.setSceneGeoCoding(tiePointGeoCoding);

        if (mod35Used) {
            mod35Product.getFlagCodingGroup().get(TcwvConstants.PIXEL_CLASSIF_BAND_NAME).
                    setName("MOD35_L2_" + TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            ProductUtils.copyFlagCodings(mod35Product, targetProduct);
            ProductUtils.copyMasks(mod35Product, targetProduct);
            targetProduct.getFlagCodingGroup().get(TcwvConstants.PIXEL_CLASSIF_BAND_NAME).
                    setName("IDEPIX_" + TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
        } else {
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
        }

        setTargetProduct(targetProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TcwvOp.class);
        }
    }
}
