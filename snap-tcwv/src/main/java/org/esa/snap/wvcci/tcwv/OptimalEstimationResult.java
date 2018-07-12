package org.esa.snap.wvcci.tcwv;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 10.07.2018
 * Time: 10:14
 *
 * @author olafd
 */
public class OptimalEstimationResult {

    private double[] xn;
    private double[][] kk;
    private boolean success;
    private int ii;
    private double[][] sr;
    private DiagnoseResult diagnoseResult;

    public OptimalEstimationResult(double[] xn, double[][] kk, boolean success, int ii, double[][] sr, DiagnoseResult diagnoseResult) {
        this.xn = xn;
        this.kk = kk;
        this.success = success;
        this.ii = ii;
        this.sr = sr;
        this.diagnoseResult = diagnoseResult;
    }

    public double[] getXn() {
        return xn;
    }

    public double[][] getKk() {
        return kk;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getIi() {
        return ii;
    }

    public double[][] getSr() {
        return sr;
    }

    public DiagnoseResult getDiagnoseResult() {
        return diagnoseResult;
    }
}
