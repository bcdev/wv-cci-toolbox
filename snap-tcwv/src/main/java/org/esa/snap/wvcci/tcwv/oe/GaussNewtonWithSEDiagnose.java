package org.esa.snap.wvcci.tcwv.oe;

import Jama.Matrix;

/**
 * Provides diagnose for Gauss-Newton with standard error.
 * See breadboard: optimal_estimation_core_pure_python --> gauss_newton_gain_aver_cost_with_se(a, b, x, y, k, sei)
 *
 * @author olafd
 */
public class GaussNewtonWithSEDiagnose implements Diagnose {
    @Override
    public DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                                   double[][] sei, double[][] sai, double[][] sr) {

        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix srMatrix = new Matrix(sr);
        final Matrix kkMatrixT = kkMatrix.transpose();
        Matrix ynMatrix = new Matrix(yn.length, 1);
        for (int i = 0; i < yn.length; i++) {
            ynMatrix.set(i, 0, yn[i]);
        }
        final Matrix ynMatrixT = ynMatrix.transpose();

        final Matrix gain = srMatrix.times(kkMatrixT.times(seiMatrix));
        final Matrix aver = Matrix.identity(xa.length, xa.length);
        final double cost = ynMatrixT.times(seiMatrix.times(ynMatrix)).get(0, 0);

        return new DiagnoseResult(gain, aver, cost);
    }
}
