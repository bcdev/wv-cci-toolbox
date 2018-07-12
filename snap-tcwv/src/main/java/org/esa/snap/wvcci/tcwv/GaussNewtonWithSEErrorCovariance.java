package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 13:23
 *
 * @author olafd
 */
public class GaussNewtonWithSEErrorCovariance implements ErrorCovariance {

    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {

//        def gauss_newton_se_ret_err_cov_i(k,sei):
//            kt_sei = np.dot(k.T, sei)
//            # inverse retrieval error co-variance
//            kt_sei_k = np.dot(kt_sei, k)
//            return kt_sei_k
//        def gauss_newton_se_ret_err_cov(k,sei):
//            return inverse(gauss_newton_se_ret_err_cov_i(k,sei))

        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix kkMatrixT = kkMatrix.transpose();

        final Matrix kkMatrixTDotSeiMatrix = kkMatrixT.times(seiMatrix);
        return kkMatrixTDotSeiMatrix.times(kkMatrix).inverse().getArray();
    }
}
