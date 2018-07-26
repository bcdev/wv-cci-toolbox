package org.esa.snap.wvcci.tcwv.oe;

/**
 * Provides covariance error computation for Gauss-Newton.
 * See breadboard: optimal_estimation_core_pure_python --> gauss_newton_ret_err_cov
 *
 * @author olafd
 */
public class GaussNewtonErrorCovariance implements ErrorCovariance {

    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {
        return new double[kk.length][kk.length];
    }
}
