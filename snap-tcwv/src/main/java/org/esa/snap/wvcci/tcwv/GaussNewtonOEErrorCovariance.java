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
public class GaussNewtonOEErrorCovariance implements ErrorCovariance {
    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {

//        def oe_ret_err_cov_i(k,sei,sai):
//            kt_sei = np.dot(k.T, sei)
//            kt_sei_k = np.dot(kt_sei, k)
//            # inverse retrieval error co-variance
//            ret_err_cov_i = sai + kt_sei_k
//            return ret_err_cov_i
//
//        def oe_ret_err_cov(k,sei,sai):
//            return inverse(oe_ret_err_cov_i(k,sei,sai)) 

        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix saiMatrix = new Matrix(sai);
        final Matrix kkMatrixT = kkMatrix.transpose();

        final Matrix kkMatrixTDotSeiMatrix = kkMatrixT.times(seiMatrix);
        final Matrix kkMatrixTDotSeiDotKMatrix = kkMatrixTDotSeiMatrix.times(kkMatrix);

        return saiMatrix.plus(kkMatrixTDotSeiDotKMatrix).getArray();
    }
}
