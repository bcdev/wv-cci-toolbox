package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Class providing the Optimal Estimation algorithm, i.e. the inversion.
 * To change this template use File | Settings | File Templates.
 * Date: 10.07.2018
 * Time: 10:10
 *
 * @author olafd
 */
public class OptimalEstimation {


//    erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=i,
//                   maxiter=100)

    /**
     * Provides the inverse of a function.
     *
     * @param method - inversion method
     * @param a      - lower bound of state
     * @param b      - upper bound of state
     * @param se     - measurement error covariance
     * @param sa     - prior error covariance
     * @param xa     - prior knowledge
     * @param yy     - 'forward function' output  --> 'measurements'
     * @return - double[] the inverse function
     */
    public static OptimalEstimationResult invert(InversionMethod method,
                                                 double[] a, double b[],
                                                 double[][] se,
                                                 double[][] sa,
                                                 double[] xa,
                                                 double[] yy) {

//        inv_func = my_inverter(FUNC[func_key], AA[func_key], BB[func_key], dtype=dtype)

        // some default settings from breadboard:
        final double eps = 0.01;
        final int maxiter = 20;
        double[] firstGuessVector = new double[a.length];
        for (int i = 0; i < firstGuessVector.length; i++) {
            firstGuessVector[i] = 0.5 * (a[i] + b[i]);
        }

        double[][] sei;
        double[][] sai;
        if (method == InversionMethod.NEWTON) {
            sei = new double[yy.length][yy.length];
            sai = new double[a.length][a.length];
        } else if (method == InversionMethod.NEWTON_SE) {
            sai = new double[a.length][a.length];
            sei = Matrix.constructWithCopy(se).inverse().getArray();
        } else {
            sai = Matrix.constructWithCopy(sa).inverse().getArray();
            sei = Matrix.constructWithCopy(se).inverse().getArray();
        }

//        xxx, jjj, ccc, nnn, aaa, ggg, sss, cst = my_optimizer(a=a, b=b
//                , y=yyy, xa=xa, fg=fg
//                , sei=sei, sai=sai
//                , eps=eps, maxiter=maxiter, method=method
//                , func=func, fparams=fparams, jaco=jaco, jparams=jparams
//                , full=full, dtype=dtype)

        return optimize(a, b, yy, xa, firstGuessVector, sei, sai, eps, maxiter, method);
    }

    private static OptimalEstimationResult optimize(double[] a, double[] b,
                                                    double[] yy, double[] xa,
                                                    double[] firstGuessVector,
                                                    double[][] sei, double[][] sai,
                                                    double eps, int maxiter,
                                                    InversionMethod method) {

        // todo
        return null;
    }
}
