package org.esa.snap.wvcci.tcwv.oe;

import Jama.Matrix;

/**
 * Provides covariance error computation for OE with Gauss-Newton.
 * See breadboard: optimal_estimation_core_pure_python --> oe_ret_err_cov
 *
 * @author olafd
 */
public class GaussNewtonOEErrorCovariance implements ErrorCovariance {

    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {
        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix saiMatrix = new Matrix(sai);
        final Matrix kkMatrixT = kkMatrix.transpose();

        final Matrix kkMatrixTDotSeiMatrix = kkMatrixT.times(seiMatrix);
        final Matrix kkMatrixTDotSeiDotKMatrix = kkMatrixTDotSeiMatrix.times(kkMatrix);

        return saiMatrix.plus(kkMatrixTDotSeiDotKMatrix).getArray();
    }
}
