package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Gauss Newton Operator with standard error
 * See breadboard code:
 *      optimal_estimation_core_pure_python --> gauss_newton_operator_with_se
 *
 * @author olafd
 */
public class GaussNewtonWithSEOperator implements OEOperator {

    @Override
    public OeOperatorResult result(double[] a, double[] b, double[] x, double[] y, double[][] jaco,
                                   double[][] sei, double[][] sai, double[] xa) {

//        :param a: lower limit of x np.array with 1 dimension
//        :param b: upper limit of x np.array with same length  as a
//        :param x: state vector
//        :param y: fnc(x)
//        :param k: dfnc(x)
//        :param sei: inverse of measurement error co-variance
//        :return: cnx (clipped) root of fnc for the linear case, last y=fnc(x), last increment of x, last
//            retrieval error co.-variance
//            '''

//        #print 'sei',sei
//            kt_sei = np.dot(k.T, sei)
        Matrix jacoMatrix = new Matrix(jaco);
        Matrix jacoMatrixT = jacoMatrix.transpose();
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix jacoMatrixTDotSeiMatrix = jacoMatrixT.times(seiMatrix);

//        #print 'kt_sei',kt_sei
//            ret_err_cov_i = (np.dot(kt_sei, k))
        final Matrix errCovMatrix = jacoMatrixTDotSeiMatrix.times(jacoMatrix);
//        #print 'ret_err_cov_i',ret_err_cov_i
//            ret_err_cov = inverse(ret_err_cov_i)
        final Matrix errCovMatrixInverse = errCovMatrix.inverse();
//        #print 'ret_err_cov',ret_err_cov
//            kt_sei_y = np.dot(kt_sei, y)
        Matrix yMatrix = new Matrix(y.length, 1);
        for (int i = 0; i < y.length; i++) {
            yMatrix.set(i, 0, y[i]);
        }
        final Matrix jacoMatrixTDotSeiMatrixDotY = jacoMatrixTDotSeiMatrix.times(yMatrix);
//        #print 'kt_sei_y',kt_sei_y
//            incr_x = np.dot(ret_err_cov, kt_sei_y)
        final Matrix incrXMatrix = errCovMatrixInverse.times(jacoMatrixTDotSeiMatrixDotY);
        final double[] incrXMatrixArr = incrXMatrix.transpose().getArray()[0];
//        #print 'nx',x - incr_x
//            cnx = clipper(a, b, x - incr_x)
        Matrix xMatrix = new Matrix(x.length, 1);
        for (int i = 0; i < x.length; i++) {
            xMatrix.set(i, 0, x[i]);
        }
        final Matrix incrXMinus = xMatrix.minus(incrXMatrix);
        final double[] incrX0 = incrXMinus.transpose().getArray()[0];
        final double[] cnx = OptimalEstimationUtils.clip1D(a, b, incrX0);

//        #print 'cnx',cnx
//            return cnx, incr_x, ret_err_cov_i, ret_err_cov

        return new OeOperatorResult(cnx, incrXMatrixArr, errCovMatrix.getArray(), errCovMatrixInverse.getArray());
    }

}
