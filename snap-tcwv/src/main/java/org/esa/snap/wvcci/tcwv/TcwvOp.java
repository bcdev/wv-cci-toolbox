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
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

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
@OperatorMetadata(alias = "ESACCI.Tcwv", version = "1.4",
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
            defaultValue = "CLOUD_SURE_AMBIGUOUS_BUFFER",
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

    @Parameter(defaultValue = "false",
            description = "Write full state vector, not just TCWV (for debugging purpose).")
    private boolean writeFullStateVector;


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
    private Band idepixClassifBand;

    private Band priorT2mBand;
    private Band priorMslBand;
    private Band priorTcwvBand;
    private Band priorU10Band;
    private Band priorV10Band;

    private Band[] landWinBands;
    private Band[] landAbsBands;
    private Band[] oceanWinBands;
    private Band[] oceanAbsBands;

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

        if (sensor == null) {
            throw new OperatorException("No sensor selected - TCWV computation aborted.");
        }
        validateSourceProduct(sensor, sourceProduct);
        if (mod35Product != null && (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA)) {
            validateMod35Product(mod35Product);
            pixelClassifBand = mod35Product.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            idepixClassifBand = sourceProduct.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            mod35Used = true;
        } else {
            pixelClassifBand = sourceProduct.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            idepixClassifBand = pixelClassifBand;
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

        landWinBands = new Band[sensor.getLandWinBandNames().length];
        for (int i = 0; i < landWinBands.length; i++) {
            final String bandName = sensor.getLandWinBandNames()[i];
            landWinBands[i] = sourceProduct.getBand(bandName);
        }

        landAbsBands = new Band[sensor.getLandAbsBandNames().length];
        for (int i = 0; i < landAbsBands.length; i++) {
            final String bandName = sensor.getLandAbsBandNames()[i];
            landAbsBands[i] = sourceProduct.getBand(bandName);
        }

        oceanWinBands = new Band[sensor.getOceanWinBandNames().length];
        for (int i = 0; i < oceanWinBands.length; i++) {
            final String bandName = sensor.getOceanWinBandNames()[i];
            oceanWinBands[i] = sourceProduct.getBand(bandName);
        }

        oceanAbsBands = new Band[sensor.getOceanAbsBandNames().length];
        for (int i = 0; i < oceanAbsBands.length; i++) {
            final String bandName = sensor.getOceanAbsBandNames()[i];
            oceanAbsBands[i] = sourceProduct.getBand(bandName);
        }

        priorT2mBand = null;
        priorMslBand = null;
        priorTcwvBand = null;
        priorU10Band = null;
        priorV10Band = null;
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_T2M_BAND_NAME)) {
            priorT2mBand = sourceProduct.getBand(TcwvConstants.PRIOR_T2M_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_MSL_BAND_NAME)) {
            priorMslBand = sourceProduct.getBand(TcwvConstants.PRIOR_MSL_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_TCWV_BAND_NAME)) {
            priorTcwvBand = sourceProduct.getBand(TcwvConstants.PRIOR_TCWV_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_U10_BAND_NAME)) {
            priorU10Band = sourceProduct.getBand(TcwvConstants.PRIOR_U10_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_V10_BAND_NAME)) {
            priorU10Band = sourceProduct.getBand(TcwvConstants.PRIOR_V10_BAND_NAME);
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
        final Band tcwvUncertaintyBand = targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME);
        final Band tcwvQualityFlagBand = targetProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME);
        final Band tcwvSurfaceTypeFlagBand = targetProduct.getBand(TcwvConstants.SURFACE_TYPE_FLAG_BAND_NAME);
        final Band stateVector1Band = targetProduct.getBand(TcwvConstants.TCWV_STATE_VECTOR1_BAND_NAME);
        final Band stateVector2Band = targetProduct.getBand(TcwvConstants.TCWV_STATE_VECTOR2_BAND_NAME);

        Tile[] landWinBandTiles = new Tile[landWinBands.length];
        for (int i = 0; i < landWinBandTiles.length; i++) {
            landWinBandTiles[i] = getSourceTile(landWinBands[i], targetRectangle);
        }

        Tile[] landAbsBandTiles = new Tile[landAbsBands.length];
        for (int i = 0; i < landAbsBandTiles.length; i++) {
            landAbsBandTiles[i] = getSourceTile(landAbsBands[i], targetRectangle);
        }

        Tile[] oceanWinBandTiles = new Tile[oceanWinBands.length];
        for (int i = 0; i < oceanWinBandTiles.length; i++) {
            oceanWinBandTiles[i] = getSourceTile(oceanWinBands[i], targetRectangle);
        }

        Tile[] oceanAbsBandTiles = new Tile[oceanAbsBands.length];
        for (int i = 0; i < oceanAbsBandTiles.length; i++) {
            oceanAbsBandTiles[i] = getSourceTile(oceanAbsBands[i], targetRectangle);
        }

        Tile szaTile = getSourceTile(szaBand, targetRectangle);
        Tile vzaTile = getSourceTile(vzaBand, targetRectangle);
        Tile saaTile = getSourceTile(saaBand, targetRectangle);
        Tile vaaTile = getSourceTile(vaaBand, targetRectangle);

        Tile pixelClassifTile = getSourceTile(pixelClassifBand, targetRectangle);
        Tile idepixClassifTile = getSourceTile(idepixClassifBand, targetRectangle);

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

        Tile priorU10Tile = null;
        if (priorU10Band != null) {
            priorU10Tile = getSourceTile(priorU10Band, targetRectangle);
        }

        Tile priorV10Tile = null;
        if (priorV10Band != null) {
            priorV10Tile = getSourceTile(priorV10Band, targetRectangle);
        }

        double[] landWinBandData = new double[landWinBandTiles.length];
        double[] landAbsBandData = new double[landAbsBandTiles.length];
        double[] oceanWinBandData = new double[oceanWinBandTiles.length];
        double[] oceanAbsBandData = new double[oceanAbsBandTiles.length];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean isValid = mod35Used ||
                        !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_INVALID_BIT);
                final boolean isLand = isValid && (mod35Used ? isMod35Land(x, y, pixelClassifTile) :
                        isIdepixLand(x, y, pixelClassifTile));
                final boolean isSeaIce = isValid && isIdepixSeaIce(x, y, idepixClassifTile);
                final boolean isOcean = isValid && !isLand && !isSeaIce;
                final boolean isCloud = isValid && (mod35Used ? isMod35Cloud(x, y, pixelClassifTile) :
                        isIdepixCloud(x, y, pixelClassifTile));

                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_LAND, isLand);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_OCEAN, isOcean);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_CLOUD, isCloud);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_SEA_ICE, isSeaIce);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_UNDEFINED, !isValid);
                // todo: determine coastal zone elsewhere, tbd

                if (!isValid || isCloud || (!processOcean && !isLand)) {
                    targetTiles.get(tcwvBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvUncertaintyBand).setSample(x, y, Float.NaN);
                    targetTiles.get(tcwvQualityFlagBand).setSample(x, y, TcwvConstants.TCWV_INVALID, true);
                    if (writeFullStateVector) {
                        targetTiles.get(stateVector1Band).setSample(x, y, Float.NaN);
                        targetTiles.get(stateVector2Band).setSample(x, y, Float.NaN);
                    }
                } else {
                    // Preparing input data...
                    final double sza = szaTile.getSampleDouble(x, y);
                    final double szaR = sza * MathUtils.DTOR;
                    final double vza = vzaTile.getSampleDouble(x, y);
                    final double vzaR = vza * MathUtils.DTOR;
                    final double saa = saaTile.getSampleDouble(x, y);
                    final double saaR = saa * MathUtils.DTOR;
                    final double vaa = vaaTile.getSampleDouble(x, y);
                    final double vaaR = vaa * MathUtils.DTOR;
//                    final double relAzi = 180. - Math.abs(saa - vaa);
                    final double relAzi = 180. - Math.acos(Math.cos(saaR) * Math.cos(vaaR) + Math.sin(saaR) * Math.sin(vaaR)) * MathUtils.RTOD;
                    final double csza = Math.cos(szaR);
                    final double amf = 1. / csza + 1. / Math.cos(vzaR);

                    // we have as pressure:
                    // ERA Interim: Pa, e.g. 100500  --> divide by -100 to get negative hPa for current LUTs
                    // no ERAInterim: hPa --> multiply by -1 to get negative hPa
                    double prs;
                    // the new LUTs (20190607) all have log(prs) in descending order, so we need to convert like this:
                    if (priorMslTile != null) {
                        prs = priorMslTile.getSampleDouble(x, y) / 100.0;
                    } else {
                        prs = mslPressure;
                    }
                    prs = -Math.log(prs);


                    final double t2m =
                            priorT2mTile != null ? priorT2mTile.getSampleDouble(x, y) : temperature;
                    double priorWs = TcwvConstants.WS_INIT_VALUE;
                    if (priorU10Tile != null && priorV10Tile != null) {
                        final double u10 = priorU10Tile.getSampleDouble(x, y);
                        final double v10 = priorV10Tile.getSampleDouble(x, y);
                        priorWs = Math.sqrt(u10 * u10 + v10 * v10);
                    }

                    final double priorAot = TcwvConstants.AOT865_INIT_VALUE;
                    final double priorAl0 = TcwvConstants.AL0_INIT_VALUE;
                    final double priorAl1 = TcwvConstants.AL1_INIT_VALUE;
                    final double priorTcwv =
                            priorTcwvTile != null ? priorTcwvTile.getSampleDouble(x, y) : TcwvConstants.TCWV_INIT_VALUE;

                    TcwvAlgorithmInput input;

                    // clarification of correct normalisation of input reflectances (email RP, 20190903):
                    // - MODIS: refl_for_tcwv = refl_input / PI
                    // - MERIS/OLCI: refl_for_tcwv = radiance / flux = refl_input * cos(sza) because
                    //          refl_input = radiance / (flux * cos(sza)), see RsMathUtils.radianceToReflectance(...)

                    final Tile[] winBandTiles = isLand ? landWinBandTiles : oceanWinBandTiles;
                    final Tile[] absBandTiles = isLand ? landAbsBandTiles : oceanAbsBandTiles;
                    final double[] winBandData = isLand ? landWinBandData : oceanWinBandData;
                    final double[] absBandData = isLand ? landAbsBandData : oceanAbsBandData;
                    for (int i = 0; i < winBandData.length; i++) {
                        if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
                            // this was wrong before!
                            winBandData[i] = winBandTiles[i].getSampleDouble(x, y) / Math.PI;
                        } else {
                            // this was already correct before
                            winBandData[i] = winBandTiles[i].getSampleDouble(x, y) * csza;
                        }
                        if (!isLand) {
                        }
                    }
                    for (int i = 0; i < absBandData.length; i++) {
                        if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
                            // this was wrong before!
                            absBandData[i] = absBandTiles[i].getSampleDouble(x, y) / Math.PI;
                        } else {
                            // this was already correct before
                            absBandData[i] = absBandTiles[i].getSampleDouble(x, y) * csza;
                        }
                        if (!isLand) {
                        }
                    }
                    input = new TcwvAlgorithmInput(winBandData, absBandData, sza, vza, relAzi,
                                                   amf, aot865, priorAot, priorAl0, priorAl1,
                                                   t2m, prs, priorWs, priorTcwv);

                    // 'ocean' parameters are null for land processing!
                    final TcwvResult result = tcwvAlgorithm.compute(sensor, landLut, oceanLut,
                                                                    tcwvFunctionLand, tcwvFunctionOcean,
                                                                    jacobiFunctionland, jacobiFunctionOcean,
                                                                    input, isLand);

                    targetTiles.get(tcwvBand).setSample(x, y, result.getTcwv());
//                    targetTiles.get(tcwvBand).setSample(x, y, result.getCost());        // test!!
                    if (writeFullStateVector) {
                        targetTiles.get(stateVector1Band).setSample(x, y, result.getStateVector1());
                        targetTiles.get(stateVector2Band).setSample(x, y, result.getStateVector2());
                    }
                    targetTiles.get(tcwvUncertaintyBand).setSample(x, y, result.getTcwvUncertainty());

                    if (result.getCost() > TcwvConstants.TCWV_RETRIEVAL_COST_2) {
                        targetTiles.get(tcwvQualityFlagBand).setSample(x, y, TcwvConstants.TCWV_COST_FUNCTION_2, true);
                    } else if (result.getCost() > TcwvConstants.TCWV_RETRIEVAL_COST_1) {
                        targetTiles.get(tcwvQualityFlagBand).setSample(x, y, TcwvConstants.TCWV_COST_FUNCTION_1, true);
                    } else {
                        targetTiles.get(tcwvQualityFlagBand).setSample(x, y, TcwvConstants.TCWV_OK, true);
                    }
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

    private boolean isIdepixSeaIce(int x, int y, Tile pixelClassifTile) {
        return !(isIdepixLand(x, y, pixelClassifTile)) &&
                pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_SNOW_ICE_BIT);
    }

    private boolean isMod35Cloud(int x, int y, Tile pixelClassifTile) {
//        return pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_DETERMINED_BIT_INDEX) &&
//                (pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_CERTAIN_BIT_INDEX) ||
//                        pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLOUD_UNCERTAIN_BIT_INDEX));

        // make this even more strict: regard as cloud and exclude everything which is not certainly clear and not snow
        // (RP, 20190902)
        return !pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.CLEAR_CERTAIN_BIT_INDEX) &&
                !pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.SNOW_ICE_BIT_INDEX);

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
        tcwvBand.setUnit("kg/m^2");
        tcwvBand.setDescription("Total Column of Water Vapour");
        tcwvBand.setNoDataValue(Float.NaN);
        tcwvBand.setNoDataValueUsed(true);

        final Band tcwvUncertaintyBand =
//                targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_BAND_NAME, ProductData.TYPE_FLOAT32);
                targetProduct.addBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME, ProductData.TYPE_UINT16);
        tcwvUncertaintyBand.setScalingFactor(0.001);
        tcwvUncertaintyBand.setUnit("kg/m^2");
        tcwvUncertaintyBand.setDescription("Uncertainty of Total Column of Water Vapour");
        tcwvUncertaintyBand.setNoDataValue(Float.NaN);
        tcwvUncertaintyBand.setNoDataValueUsed(true);

        final Band tcwvQualityFlagBand =
                targetProduct.addBand(TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME, ProductData.TYPE_INT8);
        FlagCoding tcwvQualityFlagCoding = TcwvUtils.createTcwvQualityFlagCoding(TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME);
        tcwvQualityFlagBand.setSampleCoding(tcwvQualityFlagCoding);
        targetProduct.getFlagCodingGroup().add(tcwvQualityFlagCoding);
        TcwvUtils.setupTcwvQualityFlagBitmask(targetProduct);

        final Band tcwvSurfaceTypeFlagBand =
                targetProduct.addBand(TcwvConstants.SURFACE_TYPE_FLAG_BAND_NAME, ProductData.TYPE_INT8);
        FlagCoding surfaceTypeFlagCoding = TcwvUtils.createSurfaceTypeFlagCoding(TcwvConstants.SURFACE_TYPE_FLAG_BAND_NAME);
        tcwvSurfaceTypeFlagBand.setSampleCoding(surfaceTypeFlagCoding);
        targetProduct.getFlagCodingGroup().add(surfaceTypeFlagCoding);
        TcwvUtils.setupSurfaceTypeFlagBitmask(targetProduct);

        if (writeFullStateVector) {
            // for debugging and result quality checks
            final Band stateVector1Band = targetProduct.addBand("stateVector_1", ProductData.TYPE_FLOAT32);
            stateVector1Band.setDescription("stateVector_1 (aot1 over land, aot over ocean)");
            stateVector1Band.setNoDataValue(Float.NaN);
            stateVector1Band.setNoDataValueUsed(true);
            final Band stateVector2Band = targetProduct.addBand("stateVector_2", ProductData.TYPE_FLOAT32);
            stateVector2Band.setDescription("stateVector_1 (aot2 over land, wsp over ocean)");
            stateVector2Band.setNoDataValue(Float.NaN);
            stateVector2Band.setNoDataValueUsed(true);
        }

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        final TiePointGrid latTpg = targetProduct.getTiePointGrid(sensor.getTpgNames()[4]);
        final TiePointGrid lonTpg = targetProduct.getTiePointGrid(sensor.getTpgNames()[5]);
        if (latTpg != null && lonTpg != null) {
            final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latTpg, lonTpg);
            targetProduct.setSceneGeoCoding(tiePointGeoCoding);
        } else {
            // MODIS
            final Band latBand = sourceProduct.getBand(sensor.getTpgNames()[4]);
            final Band lonBand = sourceProduct.getBand(sensor.getTpgNames()[5]);
            targetProduct.setSceneGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));
        }

        setTargetProduct(targetProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TcwvOp.class);
        }
    }
}
