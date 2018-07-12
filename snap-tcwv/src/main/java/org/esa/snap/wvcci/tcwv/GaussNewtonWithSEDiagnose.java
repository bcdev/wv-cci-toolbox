package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 10:42
 *
 * @author olafd
 */
public class GaussNewtonWithSEDiagnose implements Diagnose {
    @Override
    public DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                                   double[][] sei, double[][] sai, double[][] sr) {

//         #retrieval error co-varince
//        gain = np.dot(ret_err_cov, np.dot(k.T, sei))
//        # averaging kernel
//        # aver=np.dot(gain,k)
//            aver = np.identity(x.size)
//        # cost function
//            cost = np.dot(y.T, np.dot(sei, y))
//            return gain, aver, cost

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
