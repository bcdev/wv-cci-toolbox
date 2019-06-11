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
          MERIS_REFL_WIN_BAND_NAMES,
          MERIS_REFL_ABS_BAND_NAMES,
          MERIS_TPG_NAMES,
          MERIS_OCEAN_LUT_NC_FILENAME,
          MERIS_LAND_LUT_NC_FILENAME,
          MERIS_LAND_SE,
          MERIS_OCEAN_SE,
          MERIS_LAND_RECT_CORR,
          MERIS_OCEAN_RECT_CORR,
          MERIS_CWVL_RECT_CORR),
    MODIS_TERRA("MODIS_TERRA",
//                MODIS_LAND_REFL_BAND_NAMES,
//                MODIS_LAND_REFL_WIN_BAND_NAMES,
                MODIS_REFL_BAND_NAMES,
                MODIS_REFL_WIN_BAND_NAMES,
                MODIS_REFL_ABS_BAND_NAMES,
                MODIS_TPG_NAMES,
                MODIS_TERRA_OCEAN_LUT_NC_FILENAME,
                MODIS_TERRA_LAND_LUT_NC_FILENAME,
                MODIS_LAND_SE,
                MODIS_OCEAN_SE,
                MODIS_LAND_RECT_CORR,
                MODIS_OCEAN_RECT_CORR,
                MODIS_CWVL_RECT_CORR),
    MODIS_AQUA("MODIS_AQUA",
//               MODIS_OCEAN_REFL_BAND_NAMES,
//               MODIS_OCEAN_REFL_WIN_BAND_NAMES,
               MODIS_REFL_BAND_NAMES,
               MODIS_REFL_WIN_BAND_NAMES,
               MODIS_REFL_ABS_BAND_NAMES,
               MODIS_TPG_NAMES,
               MODIS_AQUA_OCEAN_LUT_NC_FILENAME,
               MODIS_AQUA_LAND_LUT_NC_FILENAME,
               MODIS_LAND_SE,
               MODIS_OCEAN_SE,
               MODIS_LAND_RECT_CORR,
               MODIS_OCEAN_RECT_CORR,
               MODIS_CWVL_RECT_CORR),
    OLCI("OLCI",
         OLCI_REFL_BAND_NAMES,
         OLCI_REFL_WIN_BAND_NAMES,
         OLCI_REFL_ABS_BAND_NAMES,
         OLCI_TPG_NAMES,
         OLCI_OCEAN_LUT_NC_FILENAME,
         OLCI_LAND_LUT_NC_FILENAME,
         OLCI_LAND_SE,
         OLCI_OCEAN_SE,
         OLCI_LAND_RECT_CORR,
         OLCI_OCEAN_RECT_CORR,
         OLCI_CWVL_RECT_CORR);

    private String name;
    private String[] reflBandNames;
    private String[] winBandNames;
    private String[] absBandNames;
    private String[] tpgNames;
    private String oceanLutName;
    private String landLutName;
    private double[][] landSe;
    private double[][] oceanSe;
    private double[][] landRectCorr;
    private double[][] oceanRectCorr;
    private double[] cwvlRectCorr;

    Sensor(String name, String[] reflBandNames, String[] winBandNames, String[] absBandNames, String[] tpgNames,
           String oceanLutName, String landLutName, double[][] landSe, double[][] oceanSe,
           double[][] landRectCorr, double[][] oceanRectCorr, double[] cwvlRectCorr) {
        this.name = name;
        this.reflBandNames = reflBandNames;
        this.winBandNames = winBandNames;
        this.absBandNames = absBandNames;
        this.tpgNames = tpgNames;
        this.oceanLutName = oceanLutName;
        this.landLutName = landLutName;
        this.landSe = landSe;
        this.oceanSe = oceanSe;
        this.landRectCorr = landRectCorr;
        this.oceanRectCorr = oceanRectCorr;
        this.cwvlRectCorr = cwvlRectCorr;
    }

    public String getName() {
        return name;
    }

    public String[] getReflBandNames() {
        return reflBandNames;
    }

    public String[] getWinBandNames() {
        return winBandNames;
    }

    public String[] getAbsBandNames() {
        return absBandNames;
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

    public double[] getCwvlRectCorr() {
        return cwvlRectCorr;
    }}
