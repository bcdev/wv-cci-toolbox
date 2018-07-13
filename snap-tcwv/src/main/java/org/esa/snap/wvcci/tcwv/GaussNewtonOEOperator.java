package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Optimal estmation with Gauss Newton
 * See breadboard code:
 *      optimal_estimation_core_pure_python --> optimal_estimation_gauss_newton_operator
 *
 * @author olafd
 */
public class GaussNewtonOEOperator implements OEOperator {

    @Override
    public OeOperatorResult result(double[] a, double[] b, double[] x, double[] y, double[][] jaco,
                                   double[][] sei, double[][] sai, double[] xa) {



//        kt_sei = np.dot(k.T, sei)
        Matrix jacoMatrix = new Matrix(jaco);
        Matrix jacoMatrixT = jacoMatrix.transpose();
        final Matrix saiMatrix = new Matrix(sai);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix jacoMatrixTDotSeiMatrix = jacoMatrixT.times(seiMatrix);
//        kt_sei_k = (np.dot(kt_sei, k))
        final Matrix jacoTransDotJacoMatrix = jacoMatrixTDotSeiMatrix.times(jacoMatrix);
//        # inverse retrieval error co-variance
//            ret_err_cov_i = sai + kt_sei_k
        final Matrix errCovMatrix = saiMatrix.plus(jacoTransDotJacoMatrix);
//        # retrieval error co-variance
//            ret_err_cov = inverse(ret_err_cov_i)
        final Matrix errCovMatrixInverse = errCovMatrix.inverse();
//            kt_sei_y = np.dot(kt_sei, y)
        Matrix yMatrix = new Matrix(y.length, 1);
        for (int i = 0; i < y.length; i++) {
            yMatrix.set(i, 0, y[i]);
        }
        final Matrix jacoMatrixTDotSeiMatrixDotY = jacoMatrixTDotSeiMatrix.times(yMatrix);
//            sai_dx = np.dot(sai, xa - x)
        Matrix xMatrix = new Matrix(x.length, 1);
        for (int i = 0; i < x.length; i++) {
            xMatrix.set(i, 0, x[i]);
        }
        Matrix xaMatrix = new Matrix(xa.length, 1);
        for (int i = 0; i < xa.length; i++) {
            xaMatrix.set(i, 0, xa[i]);
        }
        final Matrix xDiffMatrix = xaMatrix.minus(xMatrix);
        final Matrix saiDxMatrix = saiMatrix.times(xDiffMatrix);

//            incr_x = np.dot(ret_err_cov, kt_sei_y - sai_dx)
        final Matrix seiSaiDiffMatrix = jacoMatrixTDotSeiMatrixDotY.minus(saiDxMatrix);
        final Matrix incrXMatrix = errCovMatrixInverse.times(seiSaiDiffMatrix);
        final double[] incrXMatrixArr = incrXMatrix.transpose().getArray()[0];
//
//        cnx = clipper(a, b, x - incr_x)
        final Matrix incrXMinus = xMatrix.minus(incrXMatrix);
        final double[] incrX0 = incrXMinus.transpose().getArray()[0];
        final double[] cnx = OptimalEstimationUtils.clip1D(a, b, incrX0);

//        return cnx, incr_x, ret_err_cov_i, ret_err_cov

        return new OeOperatorResult(cnx, incrXMatrixArr, jacoTransDotJacoMatrix.getArray(), errCovMatrixInverse.getArray());
    }

}
