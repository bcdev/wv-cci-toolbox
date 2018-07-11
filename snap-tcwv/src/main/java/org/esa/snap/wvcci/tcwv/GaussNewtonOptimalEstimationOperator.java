package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Optimal estmation with Gauss Newton
 * See breadboard code:
 *      optimal_estimation_core_pure_python --> optimal_estimation_gauss_newton_operator
 *
 * @author olafd
 */
public class GaussNewtonOptimalEstimationOperator implements OEOperator {

    @Override
    public OeOperatorResult result(double[] a, double[] b, double[] x, double[] y, double[][] jaco,
                                   double[][] sei, double[][] sai, double[][] sa) {

        // todo: adapt from GaussNewtonOperator!

        final Matrix jacoLeftInverse = leftInverse(jaco);
        final Matrix xMatrix = new Matrix(x, 1);
        final Matrix yMatrix = new Matrix(y, 1);
        final Matrix incrX = jacoLeftInverse.times(yMatrix);
        xMatrix.minus(incrX).getArray();
        final double[] incrX0 = xMatrix.minus(incrX).getArray()[0];

        final double[] cnx = OptimalEstimation.clip1D(a, b, incrX0);

        return new OeOperatorResult(cnx, incrX0, null, null);
    }

    private static Matrix leftInverse(double[][] src) {
        // todo: move to some utils!
        // return np.dot(inverse(np.dot(inn.T, inn)), inn.T)
        final Matrix srcMatrix = new Matrix(src);
        final Matrix srcMatrixT = srcMatrix.transpose();
        return ((srcMatrix.times(srcMatrixT)).inverse()).times(srcMatrixT);
    }
}
