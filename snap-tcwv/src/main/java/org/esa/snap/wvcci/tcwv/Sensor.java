package org.esa.snap.wvcci.tcwv;

import static org.esa.snap.wvcci.tcwv.TcwvConstants.*;

/**
 * Enumeration for supported sensors for TCWV retrieval
 *
 * @author olafd
 */
public enum Sensor {
    MERIS("MERIS",
            MERIS_RAD_BAND_NAMES,
            MERIS_LAND_RAD_WIN_BAND_NAMES,
            MERIS_LAND_RAD_ABS_BAND_NAMES,
            MERIS_OCEAN_RAD_WIN_BAND_NAMES,
            MERIS_OCEAN_RAD_ABS_BAND_NAMES,
            MERIS_FLUX_BAND_NAMES,
            MERIS_LAND_FLUX_WIN_BAND_NAMES,
            MERIS_LAND_FLUX_ABS_BAND_NAMES,
            MERIS_OCEAN_FLUX_WIN_BAND_NAMES,
            MERIS_OCEAN_FLUX_ABS_BAND_NAMES,
//            MERIS_REFL_BAND_NAMES,
//            MERIS_LAND_REFL_WIN_BAND_NAMES,
//            MERIS_LAND_REFL_ABS_BAND_NAMES,
//            MERIS_OCEAN_REFL_WIN_BAND_NAMES,
//            MERIS_OCEAN_REFL_ABS_BAND_NAMES,
            MERIS_ALTITUDE_BAND_NAME,
            MERIS_SLP_BAND_NAME,
            MERIS_MIN_COAST_NORM_RAD_BAND_NAME,
            MERIS_MIN_COAST_NORM_RAD_VALUE,
            MERIS_MIN_COAST_FLUX_BAND_NAME,
            MERIS_TPG_NAMES,
            MERIS_OCEAN_LUT_NC_FILENAME,
            MERIS_LAND_LUT_NC_FILENAME,
            MERIS_LAND_SE,
            MERIS_OCEAN_SE,
            MERIS_LAND_RECT_CORR,
            MERIS_OCEAN_RECT_CORR,
            MERIS_LAND_SNR,
            MERIS_OCEAN_SNR,
            MERIS_LAND_INTERPOL_ERROR,
            MERIS_OCEAN_INTERPOL_ERROR,
            MERIS_CWVL_RECT_CORR),
    MODIS_TERRA("MODIS_TERRA",
            MODIS_REFL_BAND_NAMES,
            MODIS_LAND_REFL_WIN_BAND_NAMES,
            MODIS_LAND_REFL_ABS_BAND_NAMES,
            MODIS_OCEAN_REFL_WIN_BAND_NAMES,
            MODIS_OCEAN_REFL_ABS_BAND_NAMES,
            null,
            null,
            null,
            null,
            null,
            MODIS_ALTITUDE_BAND_NAME,
            null,
            MODIS_MIN_COAST_NORM_RAD_BAND_NAME,
            MODIS_MIN_COAST_NORM_RAD_VALUE,
            null,
            MODIS_TPG_NAMES,
            MODIS_TERRA_OCEAN_LUT_NC_FILENAME,
            MODIS_TERRA_LAND_LUT_NC_FILENAME,
            MODIS_LAND_SE,
            MODIS_OCEAN_SE,
            MODIS_LAND_RECT_CORR,
            MODIS_OCEAN_RECT_CORR,
            MODIS_LAND_SNR,
            MODIS_OCEAN_SNR,
            MODIS_LAND_INTERPOL_ERROR,
            MODIS_OCEAN_INTERPOL_ERROR,
            MODIS_CWVL_RECT_CORR),
    MODIS_AQUA("MODIS_AQUA",
            MODIS_REFL_BAND_NAMES,
            MODIS_LAND_REFL_WIN_BAND_NAMES,
            MODIS_LAND_REFL_ABS_BAND_NAMES,
            MODIS_OCEAN_REFL_WIN_BAND_NAMES,
            MODIS_OCEAN_REFL_ABS_BAND_NAMES,
            null,
            null,
            null,
            null,
            null,
            MODIS_ALTITUDE_BAND_NAME,
            null,
            MODIS_MIN_COAST_NORM_RAD_BAND_NAME,
            MODIS_MIN_COAST_NORM_RAD_VALUE,
            null,
            MODIS_TPG_NAMES,
            MODIS_AQUA_OCEAN_LUT_NC_FILENAME,
            MODIS_AQUA_LAND_LUT_NC_FILENAME,
            MODIS_LAND_SE,
            MODIS_OCEAN_SE,
            MODIS_LAND_RECT_CORR,
            MODIS_OCEAN_RECT_CORR,
            MODIS_LAND_SNR,
            MODIS_OCEAN_SNR,
            MODIS_LAND_INTERPOL_ERROR,
            MODIS_OCEAN_INTERPOL_ERROR,
            MODIS_CWVL_RECT_CORR),
    OLCI("OLCI",
            OLCI_REFL_BAND_NAMES,
            OLCI_LAND_REFL_WIN_BAND_NAMES,
            OLCI_LAND_REFL_ABS_BAND_NAMES,
            OLCI_OCEAN_REFL_WIN_BAND_NAMES,
            OLCI_OCEAN_REFL_ABS_BAND_NAMES,
            null,
            null,
            null,
            null,
            null,
            OLCI_ALTITUDE_BAND_NAME,
            OLCI_SLP_BAND_NAME,
            OLCI_MIN_COAST_NORM_RAD_BAND_NAME,
            OLCI_MIN_COAST_NORM_RAD_VALUE,
            null,
            OLCI_TPG_NAMES,
            OLCI_OCEAN_LUT_NC_FILENAME,
            OLCI_LAND_LUT_NC_FILENAME,
            OLCI_LAND_SE,
            OLCI_OCEAN_SE,
            OLCI_LAND_RECT_CORR,
            OLCI_OCEAN_RECT_CORR,
            OLCI_LAND_SNR,
            OLCI_OCEAN_SNR,
            OLCI_LAND_INTERPOL_ERROR,
            OLCI_OCEAN_INTERPOL_ERROR,
            OLCI_CWVL_RECT_CORR);

    private String name;
    private String[] reflBandNames;
    private String[] landWinBandNames;
    private String[] landAbsBandNames;
    private String[] oceanWinBandNames;
    private String[] oceanAbsBandNames;
    private String[] fluxBandNames;
    private String[] landFluxWinBandNames;
    private String[] landFluxAbsBandNames;
    private String[] oceanFluxWinBandNames;
    private String[] oceanFluxAbsBandNames;
    private String altitudeBandName;
    private String slpBandName;
    private String minCoastNormRadBandName;
    private double minCoastNormRadValue;
    private String minCoastFluxBandName;
    private String[] tpgNames;
    private String oceanLutName;
    private String landLutName;
    private double[][] landSe;
    private double[][] oceanSe;
    private double[][] landRectCorr;
    private double[][] oceanRectCorr;
    private double landSnr;
    private double oceanSnr;
    private double[] landInterpolError;
    private double[] oceanInterpolError;
    private double[] cwvlRectCorr;

    Sensor(String name,
           String[] reflBandNames,
           String[] landWinBandNames,
           String[] landAbsBandNames,
           String[] oceanWinBandNames,
           String[] oceanAbsBandNames,
           String[] fluxBandNames,
           String[] landFluxWinBandNames,
           String[] landFluxAbsBandNames,
           String[] oceanFluxWinBandNames,
           String[] oceanFluxAbsBandNames,
           String altitudeBandName,
           String slpBandName,
           String minCoastNormRadBandName,
           double minCoastNormRadValue,
           String minCoastFluxBandName,
           String[] tpgNames,
           String oceanLutName, String landLutName, double[][] landSe, double[][] oceanSe,
           double[][] landRectCorr, double[][] oceanRectCorr,
           double landSnr, double oceanSnr,
           double[] landInterpolError, double[] oceanInterpolError,
           double[] cwvlRectCorr) {
        this.name = name;
        this.reflBandNames = reflBandNames;
        this.landWinBandNames = landWinBandNames;
        this.landAbsBandNames = landAbsBandNames;
        this.oceanWinBandNames = oceanWinBandNames;
        this.oceanAbsBandNames = oceanAbsBandNames;
        this.fluxBandNames = fluxBandNames;
        this.landFluxWinBandNames = landFluxWinBandNames;
        this.landFluxAbsBandNames = landFluxAbsBandNames;
        this.oceanFluxWinBandNames = oceanFluxWinBandNames;
        this.oceanFluxAbsBandNames = oceanFluxAbsBandNames;
        this.altitudeBandName = altitudeBandName;
        this.slpBandName = slpBandName;
        this.minCoastNormRadBandName = minCoastNormRadBandName;
        this.minCoastNormRadValue = minCoastNormRadValue;
        this.minCoastFluxBandName = minCoastFluxBandName;
        this.tpgNames = tpgNames;
        this.oceanLutName = oceanLutName;
        this.landLutName = landLutName;
        this.landSe = landSe;
        this.oceanSe = oceanSe;
        this.landRectCorr = landRectCorr;
        this.oceanRectCorr = oceanRectCorr;
        this.landSnr = landSnr;
        this.oceanSnr = oceanSnr;
        this.landInterpolError = landInterpolError;
        this.oceanInterpolError = oceanInterpolError;
        this.cwvlRectCorr = cwvlRectCorr;
    }

    public String getName() {
        return name;
    }

    public String[] getReflBandNames() {
        return reflBandNames;
    }

    public String[] getLandWinBandNames() {
        return landWinBandNames;
    }

    public String[] getLandAbsBandNames() {
        return landAbsBandNames;
    }

    public String[] getOceanWinBandNames() {
        return oceanWinBandNames;
    }

    public String[] getOceanAbsBandNames() {
        return oceanAbsBandNames;
    }

    public String[] getFluxBandNames() {
        return fluxBandNames;
    }

    public String[] getLandFluxWinBandNames() {
        return landFluxWinBandNames;
    }

    public String[] getLandFluxAbsBandNames() {
        return landFluxAbsBandNames;
    }

    public String[] getOceanFluxWinBandNames() {
        return oceanFluxWinBandNames;
    }

    public String[] getOceanFluxAbsBandNames() {
        return oceanFluxAbsBandNames;
    }

    public String getAltitudeBandName() {
        return altitudeBandName;
    }

    public String getSlpBandName() {
        return slpBandName;
    }

    public String getMinCoastNormRadBandName() {
        return minCoastNormRadBandName;
    }

    public double getMinCoastNormRadValue() {
        return minCoastNormRadValue;
    }

    public String[] getTpgNames() {
        return tpgNames;
    }

    public String getOceanLutName() {
        return oceanLutName;
    }

    public String getLandLutName() {
        return landLutName;
    }

    public double[][] getLandSe() {
        return landSe;
    }

    public double[][] getOceanSe() {
        return oceanSe;
    }

    public double[][] getLandRectCorr() {
        return landRectCorr;
    }

    public double[][] getOceanRectCorr() {
        return oceanRectCorr;
    }

    public double getLandSnr() {
        return landSnr;
    }

    public double getOceanSnr() {
        return oceanSnr;
    }

    public double[] getLandInterpolError() {
        return landInterpolError;
    }

    public double[] getOceanInterpolError() {
        return oceanInterpolError;
    }

    public double[] getCwvlRectCorr() {
        return cwvlRectCorr;
    }
}
