package org.esa.snap.wvcci.tcwv;

/**
 * Holder for TCWV algorithm input variables.
 *
 * @author olafd
 */
public class TcwvAlgorithmInput {

    private double[] rhoToaWin;
    private double[] rhoToaAbs;
    private double sza;
    private double vza;
    private double relAzi;
    private double amf;
    private double aot865;
    private double priorAot;
    private double priorAl0;
    private double priorAl1;
    private double priorT2m;
    private double priorMslPress;
    private double priorWsp;
    private double priorTcwv;

    TcwvAlgorithmInput(double[] rhoToaWin, double[] rhoToaAbs, double sza, double vza, double relAzi, double amf,
                       double aot865, double priorAot, double priorAl0, double priorAl1, double priorT2m,
                       double priorMslPress, double priorWsp, double priorTcwv) {
        this.rhoToaWin = rhoToaWin;
        this.rhoToaAbs = rhoToaAbs;
        this.sza = sza;
        this.vza = vza;
        this.relAzi = relAzi;
        this.amf = amf;
        this.aot865 = aot865;
        this.priorAot = priorAot;
        this.priorAl0 = priorAl0;
        this.priorAl1 = priorAl1;
        this.priorT2m = priorT2m;
        this.priorMslPress = priorMslPress;
        this.priorWsp = priorWsp;
        this.priorTcwv = priorTcwv;
    }

    public double[] getRhoToaWin() {
        return rhoToaWin;
    }

    public double[] getRhoToaAbs() {
        return rhoToaAbs;
    }

    public double getSza() {
        return sza;
    }

    public double getVza() {
        return vza;
    }

    public double getRelAzi() {
        return relAzi;
    }

    public double getAmf() {
        return amf;
    }

    public double getAot865() {
        return aot865;
    }

    public double getPriorAot() {
        return priorAot;
    }

    public double getPriorAl0() {
        return priorAl0;
    }

    public double getPriorAl1() {
        return priorAl1;
    }

    public double getPriorT2m() {
        return priorT2m;
    }

    public double getPriorMslPress() {
        return priorMslPress;
    }

    public double getPriorWsp() {
        return priorWsp;
    }

    public double getPriorTcwv() {
        return priorTcwv;
    }
}
