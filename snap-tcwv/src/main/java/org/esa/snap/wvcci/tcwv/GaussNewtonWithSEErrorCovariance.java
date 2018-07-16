package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Provides covariance error computation for Gauss-Newton with standard error.
 * See breadboard: optimal_estimation_core_pure_python --> gauss_newton_se_ret_err_cov
 *
 * @author olafd
 */
public class GaussNewtonWithSEErrorCovariance implements ErrorCovariance {

    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {
        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix kkMatrixT = kkMatrix.transpose();

        final Matrix kkMatrixTDotSeiMatrix = kkMatrixT.times(seiMatrix);
        return kkMatrixTDotSeiMatrix.times(kkMatrix).inverse().getArray();
    }
}
