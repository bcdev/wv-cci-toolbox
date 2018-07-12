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
public class GaussNewtonDiagnose implements Diagnose {

    @Override
    public DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                                   double[][] sei, double[][] sai, double[][] sr) {

//        '''
//        Calculates Gain, averagiong kernel matrix and cost
//        :param y:
//        :param x:
//        :param k:
//        :return:
//            '''
//        # gain matrix
//            gain = left_inverse(k)
//        # averaging kernel
//            aver = np.identity(x.size)
//        # cost function
//            cost = np.dot(y.T, y)
//            return gain, aver, cost

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
