package org.esa.snap.wvcci.tcwv;

/**
 * Object holding result from OE operator.
 *
 * @author olafd
 */
public class OeOperatorResult {

    private double[] cnx;
    private double[] incrX;
    private double[][] retErrCovI;
    private double[][] retErrCov;

    /**
     * Result from OE operator
     *
     * @param cnx - (clipped) optimal solution for  fnc-1 for the linear case
     * @param incrX - last y=fnc(x)
     * @param retErrCovI - last increment of x
     * @param retErrCov - last retrieval error covariance
     */
    OeOperatorResult(double[] cnx, double[] incrX, double[][] retErrCovI, double[][] retErrCov) {
        this.cnx = cnx;
        this.incrX = incrX;
        this.retErrCovI = retErrCovI;
        this.retErrCov = retErrCov;
    }

    public double[] getCnx() {
        return cnx;
    }

    public double[] getIncrX() {
        return incrX;
    }

    public double[][] getRetErrCovI() {
        return retErrCovI;
    }

    public double[][] getRetErrCov() {
        return retErrCov;
    }
}
