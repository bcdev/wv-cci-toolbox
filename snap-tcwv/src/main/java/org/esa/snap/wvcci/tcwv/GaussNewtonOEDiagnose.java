package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Provides diagnose for OE with Gauss-Newton.
 * See breadboard: optimal_estimation_core_pure_python --> oe_gain_aver_cost(x, y, k, xa, sei, sai, ret_err_cov)
 *
 * @author olafd
 */
public class GaussNewtonOEDiagnose implements Diagnose {
    @Override
    public DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                                   double[][] sei, double[][] sai, double[][] sr) {

        final Matrix kkMatrix = new Matrix(kk);
        final Matrix seiMatrix = new Matrix(sei);
        final Matrix saiMatrix = new Matrix(sai);
        final Matrix srMatrix = new Matrix(sr);
        final Matrix kkMatrixT = kkMatrix.transpose();
        Matrix xaMatrix = new Matrix(xa.length, 1);
        for (int i = 0; i < xa.length; i++) {
            xaMatrix.set(i, 0, xa[i]);
        }
        Matrix xnMatrix = new Matrix(xn.length, 1);
        for (int i = 0; i < xn.length; i++) {
            xnMatrix.set(i, 0, xn[i]);
        }
        Matrix ynMatrix = new Matrix(yn.length, 1);
        for (int i = 0; i < yn.length; i++) {
            ynMatrix.set(i, 0, yn[i]);
        }
        final Matrix xDiffMatrix = xaMatrix.minus(xnMatrix);
        final Matrix xDiffMatrixT = xDiffMatrix.transpose();
        final Matrix sum1 = xDiffMatrixT.times(saiMatrix.times(xDiffMatrix));
        final Matrix ynMatrixT = ynMatrix.transpose();
        final Matrix sum2 = ynMatrixT.times(seiMatrix.times(ynMatrix));

        final Matrix gain = srMatrix.times(kkMatrixT.times(seiMatrix));
        final Matrix aver = gain.times(kkMatrix);
        final double cost = sum1.plus(sum2).get(0, 0);

        return new DiagnoseResult(gain, aver, cost);
    }
}
