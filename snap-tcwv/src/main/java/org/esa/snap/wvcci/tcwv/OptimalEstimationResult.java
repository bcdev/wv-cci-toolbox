package org.esa.snap.wvcci.tcwv;

/**
 * Object holding result from optimal estimation algorithm.
 *
 * @author olafd
 */
public class OptimalEstimationResult {

    private double[] xn;
    private double[][] kk;
    private boolean convergence;
    private int ii;
    private double[][] sr;
    private DiagnoseResult diagnoseResult;

    OptimalEstimationResult(double[] xn, double[][] kk, boolean convergence, int ii, double[][] sr, DiagnoseResult diagnoseResult) {
        this.xn = xn;
        this.kk = kk;
        this.convergence = convergence;
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

    public boolean isConvergence() {
        return convergence;
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
