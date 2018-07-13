package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Gauss Newton Operator
 * See breadboard code:
 *      optimal_estimation_core_pure_python --> gauss_newton_operator
 *
 * @author olafd
 */
public class GaussNewtonOperator implements OEOperator {

    @Override
    public OeOperatorResult result(double[] a, double[] b, double[] x, double[] y, double[][] jaco,
                                   double[][] sei, double[][] sai, double[] xa) {

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
        final Matrix incrXMinus = xMatrix.minus(incrX);
        final double[] incrX0 = incrXMinus.transpose().getArray()[0];

        final double[] cnx = OptimalEstimationUtils.clip1D(a, b, incrX0);

        return new OeOperatorResult(cnx, incrX0, null, null);
    }

}
