package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Provides diagnose for Gauss-Newton.
 * See breadboard: optimal_estimation_core_pure_python --> gauss_newton_gain_aver_cost(x, y, k)
 *
 * @author olafd
 */
public class GaussNewtonDiagnose implements Diagnose {

    @Override
    public DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                                   double[][] sei, double[][] sai, double[][] sr) {

        final Matrix gain = OptimalEstimationUtils.leftInverse(kk);
        final Matrix aver = Matrix.identity(xa.length, xa.length);
        Matrix ynMatrix = new Matrix(yn.length, 1);
        for (int i = 0; i < yn.length; i++) {
            ynMatrix.set(i, 0, yn[i]);
        }
        final Matrix ynMatrixT = ynMatrix.transpose();
        final double cost = ynMatrixT.times(ynMatrix).get(0, 0);

        return new DiagnoseResult(gain, aver, cost);
    }
}
