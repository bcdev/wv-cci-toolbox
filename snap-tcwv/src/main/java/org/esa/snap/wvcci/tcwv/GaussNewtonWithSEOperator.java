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

        Matrix jacoMatrix = new Matrix(jaco);
        Matrix jacoMatrixT = jacoMatrix.transpose();
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix jacoMatrixTDotSeiMatrix = jacoMatrixT.times(seiMatrix);

        final Matrix errCovMatrix = jacoMatrixTDotSeiMatrix.times(jacoMatrix);
        final Matrix errCovMatrixInverse = errCovMatrix.inverse();
        Matrix yMatrix = new Matrix(y.length, 1);
        for (int i = 0; i < y.length; i++) {
            yMatrix.set(i, 0, y[i]);
        }
        final Matrix jacoMatrixTDotSeiMatrixDotY = jacoMatrixTDotSeiMatrix.times(yMatrix);
        final Matrix incrXMatrix = errCovMatrixInverse.times(jacoMatrixTDotSeiMatrixDotY);
        final double[] incrXMatrixArr = incrXMatrix.transpose().getArray()[0];
        Matrix xMatrix = new Matrix(x.length, 1);
        for (int i = 0; i < x.length; i++) {
            xMatrix.set(i, 0, x[i]);
        }
        final Matrix incrXMinus = xMatrix.minus(incrXMatrix);
        final double[] incrX0 = incrXMinus.transpose().getArray()[0];
        final double[] cnx = OptimalEstimationUtils.clip1D(a, b, incrX0);

        return new OeOperatorResult(cnx, incrXMatrixArr, errCovMatrix.getArray(), errCovMatrixInverse.getArray());
    }

}
