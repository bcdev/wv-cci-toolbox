package org.esa.snap.wvcci.tcwv;

/**
 * TCWV LUT object for ocean.
 *
 * @author olafd
 */
public class TcwvOceanLut {

    // currently see e.g. ocean_core_meris.nc4 from CAWA
    // see new json LUTs later!

    private double[] wvc;
    private double[] aot;
    private double[] azi;
    private double[] vie;
    private double[] suz;
    private int[] jaco;
    private double[][][][][][][] lutArray;   // 10D for land, 7D for ocean
    private double[][][][][][][] jlutArray;   // 10D for land, 7D for ocean

    // ocean specific:
    private double[] wsp;

    public TcwvOceanLut(double[] wvc, double[] aot, double[] azi, double[] vie, double[] suz, int[] jaco,
                        double[][][][][][][] lutArray, double[][][][][][][] jlutArray,
                        double[] wsp) {
        this.wvc = wvc;
        this.aot = aot;
        this.azi = azi;
        this.vie = vie;
        this.suz = suz;
        this.jaco = jaco;
        this.lutArray = lutArray;
        this.jlutArray = jlutArray;
        this.wsp = wsp;
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

    public double[][][][][][][] getLutArray() {
        return lutArray;
    }

    public double[][][][][][][] getJlutArray() {
        return jlutArray;
    }

    public double[] getWsp() {
        return wsp;
    }

    public double[][] getAxes() {
        return new double[][]{wvc, aot, wsp, azi, vie, suz};
    }
}

