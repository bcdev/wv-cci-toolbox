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
                                   double[][] sei, double[][] sai, double[][] sa) {

        final Matrix jacoLeftInverse = OptimalEstimationUtils.leftInverse(jaco);

        Matrix xMatrix = new Matrix(x.length, 1);
        for (int i = 0; i < x.length; i++) {
            xMatrix.set(i, 0, x[i]);
        }
        Matrix yMatrix = new Matrix(y.length, 1);
        for (int i = 0; i < y.length; i++) {
            yMatrix.set(i, 0, y[i]);
        }

        final Matrix incrX = jacoLeftInverse.times(yMatrix);
        xMatrix.minus(incrX).getArray();
        final double[] incrX0 = xMatrix.minus(incrX).getArray()[0];

        final double[] cnx = OptimalEstimationUtils.clip1D(a, b, incrX0);

        return new OeOperatorResult(cnx, incrX0, null, null);
    }

}
