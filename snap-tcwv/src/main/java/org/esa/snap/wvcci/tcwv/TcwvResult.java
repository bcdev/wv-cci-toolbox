package org.esa.snap.wvcci.tcwv;

/**
 * Holder for TCWV result, flag(s) etc
 *
 * @author olafd
 */
public class TcwvResult {

    private double tcwv;
    private double stateVector1;    // 'albedo 1'
    private double stateVector2;    // 'albedo 2' over land, wind speed over ocean
    // todo: add flags etc. if needed (to be discussed)

    TcwvResult(double tcwv) {
        new TcwvResult(tcwv, Double.NaN, Double.NaN);
    }

    public TcwvResult(double tcwv, double stateVector1, double stateVector2) {
        this.tcwv = tcwv;
        this.stateVector1 = stateVector1;
        this.stateVector2 = stateVector2;
    }

    public void setTcwv(double tcwv) {
        this.tcwv = tcwv;
    }

    public double getTcwv() {
        return tcwv;
    }

    public double getStateVector1() {
        return stateVector1;
    }

    public void setStateVector1(double stateVector1) {
        this.stateVector1 = stateVector1;
    }

    public double getStateVector2() {
        return stateVector2;
    }

    public void setStateVector2(double stateVector2) {
        this.stateVector2 = stateVector2;
    }
}
