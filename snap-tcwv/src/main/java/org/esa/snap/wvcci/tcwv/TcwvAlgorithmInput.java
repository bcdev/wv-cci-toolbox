package org.esa.snap.wvcci.tcwv;

/**
 * Holder for TCWV algorithm input variables.
 *
 * @author olafd
 */
class TcwvAlgorithmInput {

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

    double[] getRhoToaWin() {
        return rhoToaWin;
    }

    double[] getRhoToaAbs() {
        return rhoToaAbs;
    }

    double getSza() {
        return sza;
    }

    double getVza() {
        return vza;
    }

    double getRelAzi() {
        return relAzi;
    }

    double getAmf() {
        return amf;
    }

    double getAot865() {
        return aot865;
    }

    double getPriorAot() {
        return priorAot;
    }

    double getPriorAl0() {
        return priorAl0;
    }

    double getPriorAl1() {
        return priorAl1;
    }

    double getPriorT2m() {
        return priorT2m;
    }

    double getPriorMslPress() {
        return priorMslPress;
    }

    double getPriorWsp() {
        return priorWsp;
    }

    double getPriorTcwv() {
        return priorTcwv;
    }
}
