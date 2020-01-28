package org.esa.snap.wvcci.tcwv;

import static org.esa.snap.wvcci.tcwv.TcwvConstants.*;

/**
 * Enumeration for supported sensors for TCWV retrieval
 *
 * @author olafd
 */
public enum Sensor {
    MERIS("MERIS",
          MERIS_REFL_BAND_NAMES,
          MERIS_LAND_REFL_WIN_BAND_NAMES,
          MERIS_LAND_REFL_ABS_BAND_NAMES,
          MERIS_OCEAN_REFL_WIN_BAND_NAMES,
          MERIS_OCEAN_REFL_ABS_BAND_NAMES,
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
//                MODIS_LAND_REFL_BAND_NAMES,
//                MODIS_LAND_REFL_WIN_BAND_NAMES,
                MODIS_REFL_BAND_NAMES,
                MODIS_LAND_REFL_WIN_BAND_NAMES,
                MODIS_LAND_REFL_ABS_BAND_NAMES,
                MODIS_OCEAN_REFL_WIN_BAND_NAMES,
                MODIS_OCEAN_REFL_ABS_BAND_NAMES,
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
//               MODIS_OCEAN_REFL_BAND_NAMES,
//               MODIS_OCEAN_REFL_WIN_BAND_NAMES,
               MODIS_REFL_BAND_NAMES,
               MODIS_LAND_REFL_WIN_BAND_NAMES,
               MODIS_LAND_REFL_ABS_BAND_NAMES,
               MODIS_OCEAN_REFL_WIN_BAND_NAMES,
               MODIS_OCEAN_REFL_ABS_BAND_NAMES,
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

    Sensor(String name, String[] reflBandNames,
           String[] landWinBandNames, String[] landAbsBandNames,
           String[] oceanWinBandNames, String[] oceanAbsBandNames,
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
    }}
