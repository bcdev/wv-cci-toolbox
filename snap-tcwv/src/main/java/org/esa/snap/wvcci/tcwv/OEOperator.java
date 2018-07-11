package org.esa.snap.wvcci.tcwv;

/**
 * Optimal Estimation Operator interface for different OE methods
 *
 * @author olafd
 */
public interface OEOperator {

    /**
     * Provides optimal estimation result object
     *
     * @param a - lower limit of x np.array with 1 dimension
     * @param b - upper limit of x np.array with same length  as a
     * @param x - state vector
     * @param y - fnc(x)
     * @param jaco - dfnc(x) (Jacobian)
     * @param sei - inverse of measurement error co-variance
     * @param sai - inverse of prior error co-variance
     * @param sa  - prior
     *
     * @return  OeOperatorResult result:
     * - cnx (clipped) optimal solution for  fnc-1 for the linear case,
     * last y=fnc(x),
     * last increment of x,
     * last retrieval error co.-variance
     */
    OeOperatorResult result(double[] a, double[] b, double[] x, double[] y,
                            double[][] jaco, double[][] sei, double[][] sai, double[][] sa);
}
