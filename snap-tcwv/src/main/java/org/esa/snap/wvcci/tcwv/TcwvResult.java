package org.esa.snap.wvcci.tcwv;

/**
 * Holder for TCWV result, flag(s) etc
 *
 * @author olafd
 */
public class TcwvResult {

    private double tcwv;
    // todo: add flags etc. if needed

    public TcwvResult(double tcwv) {
        this.tcwv = tcwv;
    }

    public void setTcwv(double tcwv) {
        this.tcwv = tcwv;
    }

    public double getTcwv() {
        return tcwv;
    }
}
