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

    @Parameter(description = "If auxdata are already installed, their path can be provided here.")
    private String auxdataPath;

    @Parameter(defaultValue = "true",
            description = "Process also over ocean (would be needed for MODIS-AQUA option later).")
    private boolean processOcean;

    @Parameter(defaultValue = "false",
            description = "Write full state vector, not just TCWV (for debugging purpose).")
    private boolean writeFullStateVector;

    @Parameter(defaultValue = "false",
            description = "Write cost function value (for debugging purpose).")
    private boolean writeCostFunctionValue;

    @SourceProduct(description =
            "Source product (IdePix product merged with MERIS, MODIS or OLCI L1b product)",
            label = "Source product")
    private Product sourceProduct;

    @SourceProduct(description =
            "MOD35 or MYD35 L2 cloud product (optional, used for MODIS processing only)",
            optional = true,
            label = "MOD35 or MYD35 L2 product")
    private Product mod35Product;

    private Product targetProduct;

    private int width;
    private int height;

    private RasterDataNode szaBand;
    private RasterDataNode vzaBand;
    private RasterDataNode saaBand;
    private RasterDataNode vaaBand;
    private RasterDataNode altitudeBand;
    private RasterDataNode seaLevelPressBand;

    private Band pixelClassifBand;
    private Band idepixClassifBand;

    private Band priorT2mBand;
    private RasterDataNode priorMslBand;
    private RasterDataNode priorTcwvBand;
    private RasterDataNode priorU10Band;
    private RasterDataNode priorV10Band;
    private Band priorWspBand;

    private RasterDataNode[] atmTempBands;  // atmospheric_temperature_profile_<i> , OLCI/MERIS
    private double[] refPressureLevels;

    private Band[] landWinBands;
    private Band[] landAbsBands;
    private Band[] oceanWinBands;
    private Band[] oceanAbsBands;

    private Band[] landFluxWinBands;
    private Band[] landFluxAbsBands;
    private Band[] oceanFluxWinBands;
    private Band[] oceanFluxAbsBands;

    private TcwvOceanLut oceanLut;
    private TcwvLandLut landLut;

    private TcwvAlgorithm tcwvAlgorithm;

    private TcwvFunction tcwvFunctionLand;
    private JacobiFunction jacobiFunctionland;
    private TcwvFunction tcwvFunctionOcean;
    private JacobiFunction jacobiFunctionOcean;

    private boolean mod35Used;
    private Band tcwvBand;
    private Band tcwvUncertaintyBand;
    private Band tcwvQualityFlagBand;
    private Band costFunctionBand;
    private Band stateVector1Band;
    private Band stateVector2Band;

    private Band seaiceMaskTestBand;

    @Override
    public void initialize() throws OperatorException {

        if (sensor == null) {
            throw new OperatorException("No sensor selected - TCWV computation aborted.");
        }
        validateSourceProduct(sensor, sourceProduct);
        if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
            validateMod35Product();
            pixelClassifBand = mod35Product.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
//            idepixClassifBand = sourceProduct.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            mod35Used = true;
        } else {
            pixelClassifBand = sourceProduct.getBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME);
            idepixClassifBand = pixelClassifBand;
            mod35Used = false;
        }

        // test 20201115: take sea ice from L3 HOAPS product collocated with source product
        // (usually, this band is null)
        seaiceMaskTestBand = sourceProduct.getBand("mask_time25");

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

        if (sensor == Sensor.MERIS) {
            landFluxWinBands = new Band[sensor.getLandFluxWinBandNames().length];
            for (int i = 0; i < landFluxWinBands.length; i++) {
                final String bandName = sensor.getLandFluxWinBandNames()[i];
                landFluxWinBands[i] = sourceProduct.getBand(bandName);
            }

            landFluxAbsBands = new Band[sensor.getLandFluxAbsBandNames().length];
            for (int i = 0; i < landFluxAbsBands.length; i++) {
                final String bandName = sensor.getLandFluxAbsBandNames()[i];
                landFluxAbsBands[i] = sourceProduct.getBand(bandName);
            }

            oceanFluxWinBands = new Band[sensor.getOceanFluxWinBandNames().length];
            for (int i = 0; i < oceanFluxWinBands.length; i++) {
                final String bandName = sensor.getOceanFluxWinBandNames()[i];
                oceanFluxWinBands[i] = sourceProduct.getBand(bandName);
            }

            oceanFluxAbsBands = new Band[sensor.getOceanFluxAbsBandNames().length];
            for (int i = 0; i < oceanFluxAbsBands.length; i++) {
                final String bandName = sensor.getOceanFluxAbsBandNames()[i];
                oceanFluxAbsBands[i] = sourceProduct.getBand(bandName);
            }
        }

        priorT2mBand = null;
        priorMslBand = null;
        priorTcwvBand = null;
        priorU10Band = null;
        priorV10Band = null;
        priorWspBand = null;
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_T2M_BAND_NAME)) {
            // from ERA for MODIS
            priorT2mBand = sourceProduct.getBand(TcwvConstants.PRIOR_T2M_BAND_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_MSL_BAND_NAME)) {
            // from ERA for MODIS
            priorMslBand = sourceProduct.getBand(TcwvConstants.PRIOR_MSL_BAND_NAME);
        } else if (sourceProduct.containsRasterDataNode(TcwvConstants.PRIOR_MSL_TPG_NAME)) {
            // for MERIS 4RP and OLCI: from TPG
            priorMslBand = sourceProduct.getRasterDataNode(TcwvConstants.PRIOR_MSL_TPG_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_TCWV_BAND_NAME)) {
            // from ERA for MODIS
            priorTcwvBand = sourceProduct.getBand(TcwvConstants.PRIOR_TCWV_BAND_NAME);
        } else if (sourceProduct.containsRasterDataNode(TcwvConstants.PRIOR_TCWV_TPG_NAME)) {
            // for MERIS 4RP and OLCI: from TPG
            priorTcwvBand = sourceProduct.getRasterDataNode(TcwvConstants.PRIOR_TCWV_TPG_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_U10_BAND_NAME)) {
            // from ERA for MODIS
            priorU10Band = sourceProduct.getBand(TcwvConstants.PRIOR_U10_BAND_NAME);
        } else if (sourceProduct.containsRasterDataNode(TcwvConstants.PRIOR_U10_TPG_NAME)) {
            // for MERIS 4RP and OLCI: from TPG
            priorU10Band = sourceProduct.getRasterDataNode(TcwvConstants.PRIOR_U10_TPG_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_V10_BAND_NAME)) {
            // from ERA for MODIS
            priorV10Band = sourceProduct.getBand(TcwvConstants.PRIOR_V10_BAND_NAME);
        } else if (sourceProduct.containsRasterDataNode(TcwvConstants.PRIOR_V10_TPG_NAME)) {
            // for MERIS 4RP and OLCI: from TPG
            priorV10Band = sourceProduct.getRasterDataNode(TcwvConstants.PRIOR_V10_TPG_NAME);
        }
        if (sourceProduct.containsBand(TcwvConstants.PRIOR_WSP_BAND_NAME)) {
            // from ERA for MODIS
            priorWspBand = sourceProduct.getBand(TcwvConstants.PRIOR_WSP_BAND_NAME);
        }

        if (sensor == Sensor.MERIS) {
            atmTempBands = new RasterDataNode[TcwvConstants.MERIS_REF_PRESSURE_LEVELS.length];
            // MERIS: ordered from top to bottom!
            for (int i = atmTempBands.length-1; i >=0; i--) {
                final String bandName = "atmospheric_temperature_profile_pressure_level_" + (i + 1);
                atmTempBands[i] = sourceProduct.getRasterDataNode(bandName);
            }
        }
        if (sensor == Sensor.OLCI) {
            atmTempBands = new RasterDataNode[TcwvConstants.OLCI_REF_PRESSURE_LEVELS.length];
            // OLCI: ordered from bottom to top!
            for (int i = 0; i < atmTempBands.length; i++) {
                final String bandName = "atmospheric_temperature_profile_pressure_level_" + (i + 1);
                atmTempBands[i] = sourceProduct.getRasterDataNode(bandName);
            }
        }

        szaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[0]);
        vzaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[1]);
        saaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[2]);
        vaaBand = sourceProduct.getRasterDataNode(sensor.getTpgNames()[3]);

        altitudeBand = sourceProduct.getRasterDataNode(sensor.getAltitudeBandName());
        if (sensor.getSlpBandName() != null) {
            seaLevelPressBand = sourceProduct.getRasterDataNode(sensor.getSlpBandName());
        }

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

        tcwvBand = targetProduct.getBand(TcwvConstants.TCWV_TARGET_BAND_NAME);
        tcwvUncertaintyBand = targetProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_TARGET_BAND_NAME);
        tcwvQualityFlagBand = targetProduct.getBand(TcwvConstants.TCWV_QUALITY_FLAG_BAND_NAME);
        final Band tcwvSurfaceTypeFlagBand = targetProduct.getBand(TcwvConstants.SURFACE_TYPE_FLAG_BAND_NAME);
        stateVector1Band = targetProduct.getBand(TcwvConstants.TCWV_STATE_VECTOR1_BAND_NAME);
        stateVector2Band = targetProduct.getBand(TcwvConstants.TCWV_STATE_VECTOR2_BAND_NAME);
        costFunctionBand = targetProduct.getBand(TcwvConstants.TCWV_COST_FUNCTION_BAND_NAME);

        Tile[] landWinBandTiles = getSourceTiles(landWinBands, targetRectangle);
        Tile[] landAbsBandTiles = getSourceTiles(landAbsBands, targetRectangle);
        Tile[] oceanWinBandTiles = getSourceTiles(oceanWinBands, targetRectangle);
        Tile[] oceanAbsBandTiles = getSourceTiles(oceanAbsBands, targetRectangle);

        Tile[] landFluxWinBandTiles = null;
        Tile[] landFluxAbsBandTiles = null;
        Tile[] oceanFluxWinBandTiles = null;
        Tile[] oceanFluxAbsBandTiles = null;
        if (sensor == Sensor.MERIS) {
            landFluxWinBandTiles = getSourceTiles(landFluxWinBands, targetRectangle);
            landFluxAbsBandTiles = getSourceTiles(landFluxAbsBands, targetRectangle);
            oceanFluxWinBandTiles = getSourceTiles(oceanFluxWinBands, targetRectangle);
            oceanFluxAbsBandTiles = getSourceTiles(oceanFluxAbsBands, targetRectangle);
        }

        Tile[] atmTempTiles = null;
        if (atmTempBands != null) {
            atmTempTiles = getSourceTiles(atmTempBands, targetRectangle);
        }

        Tile szaTile = getSourceTile(szaBand, targetRectangle);
        Tile vzaTile = getSourceTile(vzaBand, targetRectangle);
        Tile saaTile = getSourceTile(saaBand, targetRectangle);
        Tile vaaTile = getSourceTile(vaaBand, targetRectangle);

        Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        Tile seaLevelPressTile = null;
        if (seaLevelPressBand != null) {
            seaLevelPressTile = getSourceTile(seaLevelPressBand, targetRectangle);
        }

        Tile pixelClassifTile = getSourceTile(pixelClassifBand, targetRectangle);
        Tile idepixClassifTile = null;
        if (sensor != Sensor.MODIS_TERRA && sensor != Sensor.MODIS_AQUA) {
            idepixClassifTile = getSourceTile(idepixClassifBand, targetRectangle);
        }

        Tile seaiceMaskTestTile = null;
        if (seaiceMaskTestBand != null) {
            seaiceMaskTestTile = getSourceTile(seaiceMaskTestBand, targetRectangle);
        }

        Tile priorT2mTile = getTcwvInputTile(priorT2mBand, targetRectangle);
        Tile priorMslTile = getTcwvInputTile(priorMslBand, targetRectangle);
        Tile priorTcwvTile = getTcwvInputTile(priorTcwvBand, targetRectangle);
        Tile priorU10Tile = getTcwvInputTile(priorU10Band, targetRectangle);
        Tile priorV10Tile = getTcwvInputTile(priorV10Band, targetRectangle);
        Tile priorWspTile = getTcwvInputTile(priorWspBand, targetRectangle);

        double[] landWinBandData = new double[landWinBandTiles.length];
        double[] landAbsBandData = new double[landAbsBandTiles.length];
        double[] oceanWinBandData = new double[oceanWinBandTiles.length];
        double[] oceanAbsBandData = new double[oceanAbsBandTiles.length];

        double[] landFluxWinBandData = null;
        double[] landFluxAbsBandData = null;
        double[] oceanFluxWinBandData = null;
        double[] oceanFluxAbsBandData = null;
        if (sensor == Sensor.MERIS) {
            landFluxWinBandData = new double[landWinBandTiles.length];
            landFluxAbsBandData = new double[landAbsBandTiles.length];
            oceanFluxWinBandData = new double[oceanWinBandTiles.length];
            oceanFluxAbsBandData = new double[oceanAbsBandTiles.length];
        }

        double[] atmTempData = null;
        if (atmTempTiles != null) {
            atmTempData = new double[atmTempTiles.length];
        }

        Tile[] winBandTiles;
        Tile[] absBandTiles;
        double[] winBandData;
        double[] absBandData;

        Tile[] fluxWinBandTiles = null;
        Tile[] fluxAbsBandTiles = null;
        double[] fluxWinBandData = null;
        double[] fluxAbsBandData = null;

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
//                if (x == 157 && y == 132) {
//                    System.out.println("x = " + x);
//                }

                // set to invalid if SZA > 75deg (RP, Jan 2020)
                final double sza = szaTile.getSampleDouble(x, y);
                final double szaR = sza * MathUtils.DTOR;
                final double csza = Math.cos(szaR);

                boolean isLand = mod35Used ? isMod35Land(x, y, pixelClassifTile) :
                        isIdepixLand(x, y, pixelClassifTile);

                winBandTiles = isLand ? landWinBandTiles : oceanWinBandTiles;
                absBandTiles = isLand ? landAbsBandTiles : oceanAbsBandTiles;

                boolean isValid = isValidNormalizedReflectances(x, y, csza, winBandTiles, absBandTiles, targetRectangle) &&
                        sza <= TcwvConstants.SZA_MAX_VALUE &&
                        (mod35Used || !pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_INVALID_BIT));
                isLand = isLand && isValid;
                boolean isSeaIce = false;
                if (sensor != Sensor.MODIS_TERRA && sensor != Sensor.MODIS_AQUA) {
                    isSeaIce = isValid && isIdepixSeaIce(x, y, idepixClassifTile);
                }
                // test 20201115: take sea ice from L3 HOAPS product collocated with source product
                if (seaiceMaskTestTile != null) {
                    isSeaIce = seaiceMaskTestTile.getSampleInt(x, y) == 11 || seaiceMaskTestTile.getSampleInt(x, y) == 12;
                }
                isLand = isLand || isSeaIce;  // sea ice should always be processed as land! (RP, Nov 2020)

                final boolean isCloud = isValid && (mod35Used ? isMod35Cloud(x, y, pixelClassifTile) :
                        isIdepixCloud(x, y, pixelClassifTile));

                final boolean isCoastline = isValid && (mod35Used ? isMod35Coastline(x, y, pixelClassifTile) :
                        isIdepixCoastline(x, y, pixelClassifTile));


                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_CLOUD, isCloud);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_SEA_ICE, isSeaIce);
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_UNDEFINED, !isValid);

                // declare as land also coastline pixels and pixels for which the reference reflectance (sensor dependent)
                // exceeds certain threshold. Then make sure that finally land/ocean/seaice are complementary!
                isLand = isLand || (isCoastline && applyLandForCoastlinesAndRivers(y, x, csza, targetRectangle));
                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_LAND, isLand);
                final boolean isOcean = isValid && !isLand && !isSeaIce;

                targetTiles.get(tcwvSurfaceTypeFlagBand).setSample(x, y, TcwvConstants.SURFACE_TYPE_OCEAN, isOcean);

                // update win/abs bands according to possible change of isLand (important for MODIS!):
                winBandTiles = isLand ? landWinBandTiles : oceanWinBandTiles;
                absBandTiles = isLand ? landAbsBandTiles : oceanAbsBandTiles;
                winBandData = isLand ? landWinBandData : oceanWinBandData;
                absBandData = isLand ? landAbsBandData : oceanAbsBandData;

                if (sensor == Sensor.MERIS) {
                    fluxWinBandTiles = isLand ? landFluxWinBandTiles : oceanFluxWinBandTiles;
                    fluxAbsBandTiles = isLand ? landFluxAbsBandTiles : oceanFluxAbsBandTiles;
                    fluxWinBandData = isLand ? landFluxWinBandData : oceanFluxWinBandData;
                    fluxAbsBandData = isLand ? landFluxAbsBandData : oceanFluxAbsBandData;
                }

                if (!isValid || isCloud || (!processOcean && !isLand)) {
                    setTcwvResultInvalid(x, y, targetTiles);
                } else {
                    // Preparing input data...
                    final double vzaR = vzaTile.getSampleDouble(x, y) * MathUtils.DTOR;
                    final double saaR = saaTile.getSampleDouble(x, y) * MathUtils.DTOR;
                    final double vaaR = vaaTile.getSampleDouble(x, y) * MathUtils.DTOR;
                    final double relAzi = 180. - Math.acos(Math.cos(saaR) * Math.cos(vaaR) +
                            Math.sin(saaR) * Math.sin(vaaR)) * MathUtils.RTOD;
                    final double amf = 1. / csza + 1. / Math.cos(vzaR);
                    final double altitude = altitudeTile.getSampleDouble(x, y);

                    // priors from TPs for MERIS, OLCI (RP Sep 2022):
                    // sea_level_pressure (OLCI), atm_press (MERIS) --> not new, but compute surface pressure now!
                    // total_columnar_water_vapour (new)
                    // horizontal_wind_vector_1 (new)
                    // horizontal_wind_vector_2 (new)
                    // atmospheric_temperature_profile_pressure_level_<i>, i=1,..,20 (MERIS), 1,..,25 (OLCI)

                    double t2m = temperature;
                    double priorWs = TcwvConstants.WS_INIT_VALUE;

                    // prior surface and sea level pressure:
                    double surfacePress = mslPressure;
                    double slp= mslPressure;
                    if (isLand) {
                        if (seaLevelPressTile != null) {
                            // MERIS, OLCI: from TPG
                            slp = seaLevelPressTile.getSampleDouble(x, y);
                        } else {
                            // MODIS
                            if (priorMslTile != null) {
                                // ERA Interim: Pa, e.g. 100500  --> divide by -100 to get negative hPa for current LUTs
                                slp = priorMslTile.getSampleDouble(x, y) / 100.0;
                            }
                        }
                        surfacePress = TcwvUtils.getSurfacePressure(slp, altitude);
                        t2m = priorT2mTile != null ? priorT2mTile.getSampleDouble(x, y) : temperature;

                        if (atmTempData != null) {
                            // MERIS, OLCI: from TPGs
                            for (int i = 0; i < atmTempData.length; i++) {
                                atmTempData[i] = atmTempTiles[i].getSampleDouble(x, y);
                            }
                            try {
                                t2m = TcwvUtils.getSurfaceTemperature(sensor, atmTempData, surfacePress);
                            } catch (IOException ignore) {
                            }
                        }
                    } else {
                        if (priorU10Tile != null && priorV10Tile != null) {
                            final double u10 = priorU10Tile.getSampleDouble(x, y);
                            final double v10 = priorV10Tile.getSampleDouble(x, y);
                            priorWs = Math.sqrt(u10 * u10 + v10 * v10);
                        } else if (priorWspTile != null) {
                            priorWs = priorWspTile.getSampleDouble(x, y);
                        }
                    }

                    final double priorAot = isLand ? TcwvConstants.AOT_FALLBACK_LAND : TcwvConstants.AOT_FALLBACK_OCEAN;

                    final double priorTcwv =
                            priorTcwvTile != null ? priorTcwvTile.getSampleDouble(x, y) : TcwvConstants.TCWV_INIT_VALUE;

                    normalizeSpectralInputBands(y, x, csza, winBandTiles, winBandData, fluxWinBandTiles);
                    normalizeSpectralInputBands(y, x, csza, absBandTiles, absBandData, fluxAbsBandTiles);

                    double priorAl0 = TcwvConstants.AL0_INIT_VALUE;
                    double priorAl1 = TcwvConstants.AL1_INIT_VALUE;
                    if (isLand) {
                        if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
                            // for MERIS, set to rad * PI / csza = refl_input*flux (RP 20200316):
                            priorAl0 = winBandData[0] * Math.PI / csza;
                            priorAl1 = winBandData[1] * Math.PI / csza;
                        } else if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
                            // for MERIS, set to rad * PI / csza = refl_input*flux (RP 20200316):
                            priorAl0 = winBandData[0] * Math.PI;
                            priorAl1 = winBandData[1] * Math.PI;
                        }
                    }

                    TcwvAlgorithmInput input = new TcwvAlgorithmInput(winBandData, absBandData, sza,
                            vzaTile.getSampleDouble(x, y), relAzi,
                            amf, priorAot, priorAl0, priorAl1,
                            t2m, surfacePress, priorWs, priorTcwv);

                    // 'ocean' parameters are null for land processing!
                    final TcwvResult result = tcwvAlgorithm.compute(sensor, landLut, oceanLut,
                            tcwvFunctionLand, tcwvFunctionOcean,
                            jacobiFunctionland, jacobiFunctionOcean,
                            input, isLand, isCoastline);

                    targetTiles.get(tcwvBand).setSample(x, y, result.getTcwv());
                    if (writeCostFunctionValue) {
                        targetTiles.get(costFunctionBand).setSample(x, y, result.getCost());
                    }
                    if (writeFullStateVector) {
                        targetTiles.get(stateVector1Band).setSample(x, y, result.getStateVector1());
                        targetTiles.get(stateVector2Band).setSample(x, y, result.getStateVector2());
                    }
                    targetTiles.get(tcwvUncertaintyBand).setSample(x, y, result.getTcwvUncertainty());

                    if (result.getTcwv() < TcwvConstants.TCWV_RETRIEVAL_TCWV_LOWER_LIMIT ||
                            result.getTcwv() > TcwvConstants.TCWV_RETRIEVAL_TCWV_UPPER_LIMIT ||
                            result.getCost() > TcwvConstants.TCWV_RETRIEVAL_COST_UPPER_LIMIT) {
                        setTcwvResultInvalid(x, y, targetTiles);

                    } else {
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
    }

    private void setTcwvResultInvalid(int x, int y, Map<Band, Tile> targetTiles) {
        targetTiles.get(tcwvBand).setSample(x, y, Float.NaN);
        targetTiles.get(tcwvUncertaintyBand).setSample(x, y, Float.NaN);
        targetTiles.get(tcwvQualityFlagBand).setSample(x, y, TcwvConstants.TCWV_INVALID, true);
        if (writeCostFunctionValue) {
            targetTiles.get(costFunctionBand).setSample(x, y, Float.NaN);
        }
        if (writeFullStateVector) {
            targetTiles.get(stateVector1Band).setSample(x, y, Float.NaN);
            targetTiles.get(stateVector2Band).setSample(x, y, Float.NaN);
        }
    }

    private boolean isValidNormalizedReflectances(int x, int y, double csza, Tile[] winBandTiles, Tile[] absBandTiles,
                                                  Rectangle targetRectangle) {
        for (int i = 0; i < winBandTiles.length; i++) {
            Tile fluxTile = null;
            if (sensor == Sensor.MERIS) {
                fluxTile = getSourceTile(sourceProduct.getBand(sensor.getFluxBandNames()[i]), targetRectangle);
            }
            final double normalizedSpectralValue = normalizeSpectralInputBand(y, x, csza, winBandTiles[i], fluxTile);
            if (normalizedSpectralValue < TcwvConstants.MIN_NORM_RAD_VALUE) {
                return false;
            }
        }
        for (int i = 0; i < absBandTiles.length; i++) {
            Tile fluxTile = null;
            if (sensor == Sensor.MERIS) {
                fluxTile = getSourceTile(sourceProduct.getBand(sensor.getFluxBandNames()[i]), targetRectangle);
            }
            final double normalizedSpectralValue = normalizeSpectralInputBand(y, x, csza, absBandTiles[i], fluxTile);
            if (normalizedSpectralValue < TcwvConstants.MIN_NORM_RAD_VALUE) {
                return false;
            }
        }

        return true;
    }

    private boolean applyLandForCoastlinesAndRivers(int y, int x, double csza, Rectangle targetRectangle) {
        final Tile minCoastNormRadTile =
                getSourceTile(sourceProduct.getBand(sensor.getMinCoastNormRadBandName()), targetRectangle);
        Tile minCoastFluxTile = null;
        if (sensor == Sensor.MERIS) {
            minCoastFluxTile = getSourceTile(sourceProduct.getBand(sensor.getMinCoastNormRadBandName()), targetRectangle);
        }
        final double minCoastNormRadValue = normalizeSpectralInputBand(y, x, csza, minCoastNormRadTile, minCoastFluxTile);

        return minCoastNormRadValue > sensor.getMinCoastNormRadValue();
    }

    private Tile[] getSourceTiles(RasterDataNode[] sourceBands, Rectangle rectangle) {
        Tile[] sourceTiles = new Tile[sourceBands.length];
        for (int i = 0; i < sourceBands.length; i++) {
            sourceTiles[i] = getSourceTile(sourceBands[i], rectangle);
        }
        return sourceTiles;
    }

    private Tile getTcwvInputTile(RasterDataNode sourceBand, Rectangle rectangle) {
        if (sourceBand != null) {
            return getSourceTile(sourceBand, rectangle);
        } else {
            return null;
        }
    }

    private void normalizeSpectralInputBands(int y, int x, double csza, Tile[] spectralBandTiles,
                                             double[] spectralBandData, Tile[] spectralFluxTiles) {
        for (int i = 0; i < spectralBandData.length; i++) {
            spectralBandData[i] = normalizeSpectralInputBand(y, x, csza, spectralBandTiles[i], spectralFluxTiles[i]);
        }
    }

    private double normalizeSpectralInputBand(int y, int x, double csza, Tile spectralBandTile, Tile spectralFluxTile) {
        // clarification of correct normalisation of input reflectances (email RP, 20190903):
        // - MODIS: refl_for_tcwv = refl_input / PI
        // - MERIS/OLCI: refl_for_tcwv = radiance / flux = refl_input * cos(sza) / PI , because
        //          refl_input = radiance * PI / (flux * cos(sza)), see RsMathUtils.radianceToReflectance(...)
        if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
            // this was wrong before!
            return spectralBandTile.getSampleDouble(x, y) / Math.PI;
        } else if (sensor == Sensor.OLCI) {
            // this was already correct before
            return spectralBandTile.getSampleDouble(x, y) * csza / Math.PI;  // PI was missing!! (OD, 20200318)
        } else {
            // MERIS 4RP:
            return spectralBandTile.getSampleDouble(x, y) / spectralFluxTile.getSampleDouble(x, y);
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

    private boolean isIdepixCoastline(int x, int y, Tile pixelClassifTile) {
        return pixelClassifTile.getSampleBit(x, y, TcwvConstants.IDEPIX_COASTLINE);
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

    private boolean isMod35Coastline(int x, int y, Tile pixelClassifTile) {
        return pixelClassifTile.getSampleBit(x, y, ModisMod35L2Constants.COASTAL_BIT_INDEX);
    }

    private static void validateSourceProduct(Sensor sensor, Product sourceProduct) {
        if (sensor != Sensor.MODIS_TERRA && sensor != Sensor.MODIS_AQUA) {
            if (!sourceProduct.containsBand(TcwvConstants.PIXEL_CLASSIF_BAND_NAME)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                        "pixel classification flag band '" +
                        TcwvConstants.PIXEL_CLASSIF_BAND_NAME + "'.");
            }
        }

        for (String bandName : sensor.getReflBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                        "mandatory band '" + bandName + "'.");
            }
        }
    }

    private void validateMod35Product() {
        if (mod35Product.getName().startsWith("MOD") && sensor == Sensor.MODIS_AQUA) {
            throw new OperatorException("Sensor is MODIS AQUA - does not fit to MODIS TERRA MOD35_L2 cloud product.");
        }

        if (mod35Product.getName().startsWith("MYD") && sensor == Sensor.MODIS_TERRA) {
            throw new OperatorException("Sensor is MODIS TERRA - does not fit to MODIS AQUA MYD35_L2 cloud product.");
        }

        if (mod35Product == null) {
            throw new OperatorException("MOD35 or MYD35 product missing - mandatory for TCWV retrieval from MODIS Terra/Aqua");
        }
        for (String bandName : TcwvConstants.MOD35_BAND_NAMES) {
            if (!mod35Product.containsBand(bandName)) {
                throw new OperatorException("MOD35 or MYD35 product is not valid, as it does not contain " +
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

        if (writeCostFunctionValue) {
            final Band costFunctionBand = targetProduct.addBand(TcwvConstants.TCWV_COST_FUNCTION_BAND_NAME,
                    ProductData.TYPE_FLOAT32);
            costFunctionBand.setDescription("TCWV retrieval cost function value");
            costFunctionBand.setNoDataValue(Float.NaN);
            costFunctionBand.setNoDataValueUsed(true);
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
            if (latBand != null && lonBand != null &&
                    latBand.getProduct().getSceneRasterWidth() >= 2 &&
                    latBand.getProduct().getSceneRasterHeight() >= 2 &&
                    lonBand.getProduct().getSceneRasterWidth() >= 2 &&
                    lonBand.getProduct().getSceneRasterHeight() >= 2) {
                targetProduct.setSceneGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));
            }
        }

        setTargetProduct(targetProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TcwvOp.class);
        }
    }
}
