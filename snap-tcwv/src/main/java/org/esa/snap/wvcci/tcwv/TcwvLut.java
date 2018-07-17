package org.esa.snap.wvcci.tcwv;

/**
 * TCWV LUT object
 * To change this template use File | Settings | File Templates.
 * Date: 17.07.2018
 * Time: 13:40
 *
 * @author olafd
 */
public class TcwvLut {

//    geo: aot,prs,tmp,azi,vie,suz
//    al0 is albedo at winband [0]
//    al1 is albedo at winband [1]
//    aot is aeros optical thickness at winband [1]
//    prs is surface pressure in hPa
//    tmp is 2m temperature is K
//    wvc is sqrt of wvc

    // currently see e.g. land_core_meris.nc4 from CAWA
    // see new json LUTs later!

    private double[] al0;
    private double[] al1;
    private double[] aot;
    private double[] azi;
    private double[] prs;
    private double[] suz;
    private double[] tmp;
    private double[] vie;
    private double[] wvc;
    private double[][][][][][][][][][] lutArray;   // 10D
    private double[][][][][][][][][][] jlutArray;   // 10D

    public TcwvLut(double[] al0, double[] al1, double[] aot,
                   double[] azi, double[] prs, double[] suz,
                   double[] tmp, double[] vie, double[] wvc,
                   double[][][][][][][][][][] lutArray,
                   double[][][][][][][][][][] jlutArray) {
        this.al0 = al0;
        this.al1 = al1;
        this.aot = aot;
        this.azi = azi;
        this.prs = prs;
        this.suz = suz;
        this.tmp = tmp;
        this.vie = vie;
        this.wvc = wvc;
        this.lutArray = lutArray;
        this.jlutArray = jlutArray;
    }

    public double[] getAl0() {
        return al0;
    }

    public double[] getAl1() {
        return al1;
    }

    public double[] getAot() {
        return aot;
    }

    public double[] getAzi() {
        return azi;
    }

    public double[] getPrs() {
        return prs;
    }

    public double[] getSuz() {
        return suz;
    }

    public double[] getTmp() {
        return tmp;
    }

    public double[] getVie() {
        return vie;
    }

    public double[] getWvc() {
        return wvc;
    }

    public double[][][][][][][][][][] getLutArray() {
        return lutArray;
    }

    public double[][][][][][][][][][] getJlutArray() {
        return jlutArray;
    }
}
