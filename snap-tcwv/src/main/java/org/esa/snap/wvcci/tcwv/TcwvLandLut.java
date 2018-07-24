package org.esa.snap.wvcci.tcwv;

/**
 * TCWV LUT object for land.
 *
 * @author olafd
 */
public class TcwvLandLut {

    // currently see e.g. land_core_meris.nc4 from CAWA
    // see new json LUTs later!

    private double[] wvc;
    private double[] aot;
    private double[] azi;
    private double[] vie;
    private double[] suz;
    private int[] jaco;

    private double[][][][][][][][][][] lutArray;   // 10D for land, 7D for ocean
    private double[][][][][][][][][][] jlutArray;   // 10D for land, 7D for ocean

    // land specific:
    private double[] al0;
    private double[] al1;
    private double[] prs;
    private double[] tmp;

    TcwvLandLut(double[] wvc, double[] aot, double[] azi, double[] vie, double[] suz, int[] jaco,
                       double[][][][][][][][][][] lutArray, double[][][][][][][][][][] jlutArray,
                       double[] al0, double[] al1, double[] prs, double[] tmp) {
        this.wvc = wvc;
        this.aot = aot;
        this.azi = azi;
        this.vie = vie;
        this.suz = suz;
        this.jaco = jaco;
        this.lutArray = lutArray;
        this.jlutArray = jlutArray;
        this.al0 = al0;
        this.al1 = al1;
        this.prs = prs;
        this.tmp = tmp;
    }

    public double[] getWvc() {
        return wvc;
    }

    public double[] getAot() {
        return aot;
    }

    public double[] getAzi() {
        return azi;
    }

    public double[] getVie() {
        return vie;
    }

    public double[] getSuz() {
        return suz;
    }

    public int[] getJaco() {
        return jaco;
    }

    public double[][][][][][][][][][] getLutArray() {
        return lutArray;
    }

    public double[][][][][][][][][][] getJlutArray() {
        return jlutArray;
    }

    public double[] getAl0() {
        return al0;
    }

    public double[] getAl1() {
        return al1;
    }

    public double[] getPrs() {
        return prs;
    }

    public double[] getTmp() {
        return tmp;
    }

    public double[][] getAxes() {
        return new double[][]{wvc, al0, al1, aot, prs, tmp, azi, vie, suz};
    }
}
