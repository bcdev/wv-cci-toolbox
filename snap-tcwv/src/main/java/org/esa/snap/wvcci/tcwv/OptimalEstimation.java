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

    private TcwvFunction func;
    private JacobyFunction jfunc;
    private double[] a;
    private double[] b;
    private double[] xx;
    private double[] params;
    double[][] jaco;

    /**
     * @param func   - 'forward function' from 'state vector' to 'measurements' (e.g. as in lut2func.py)
     * @param jfunc  - corresponding 'Jacoby function' (see LutJacobyFunction)
     * @param a      - lower bound of state
     * @param b      - upper bound of state
     * @param xx     - state vector (e.g. LUT)
     * @param params - optional input parameters for func
     */
    public OptimalEstimation(TcwvFunction func,
                             JacobyFunction jfunc,
                             double[] a, double[] b, double[] xx, double[] params, double[][] jaco) {
        this.func = func;
        this.jfunc = jfunc;
        this.a = a;
        this.b = b;
        this.xx = xx;
        this.params = params;
        this.jaco = jaco;
    }


    /**
     * Provides the inverse of a function.
     *
     * @param method - inversion method
     * @param se     - measurement error covariance
     * @param sa     - prior error covariance
     * @param xa     - prior knowledge
     * @return - double[] the inverse function
     */
    public OptimalEstimationResult invert(InversionMethod method,
                                          double[][] se,
                                          double[][] sa,
                                          double[] xa) {

//        inv_func = my_inverter(FUNC[func_key], AA[func_key], BB[func_key], dtype=dtype
//        erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=i,maxiter=100)

        final double[] yy = func.f(xx, params);

        double[][] sei;
        double[][] sai;
        if (method == InversionMethod.NEWTON) {
            sei = new double[yy.length][yy.length];
            sai = new double[a.length][a.length];
        } else if (method == InversionMethod.NEWTON_SE) {
            sai = new double[a.length][a.length];
            sei = new Matrix(se).inverse().getArray();
        } else {
            sai = new Matrix(sa).inverse().getArray();
            sei = new Matrix(se).inverse().getArray();
        }

//        xxx, jjj, ccc, nnn, aaa, ggg, sss, cst = my_optimizer(a=a, b=b
//                , y=yyy, xa=xa, fg=fg
//                , sei=sei, sai=sai
//                , eps=eps, maxiter=maxiter, method=method
//                , func=func, fparams=fparams, jaco=jaco, jparams=jparams
//                , full=full, dtype=dtype)

        return optimize(func, a, b, yy, xa, sei, sai, sa, jaco, method);
    }


    private OptimalEstimationResult optimize(TcwvFunction func,
                                             double[] a, double[] b,
                                             double[] yy, double[] xa,
                                             double[][] sei, double[][] sai, double[][] sa,
                                             double[][] jaco,
                                             InversionMethod method) {

        // some default settings from breadboard (same as in Cawa):
        final double eps = 0.01;
        final int maxiter = 20;
        final double delta = 0.001;
        final double epsx = 0.000001;
        final double epsy = 0.000001;
        double[] firstGuessVector = new double[a.length];
        for (int i = 0; i < firstGuessVector.length; i++) {
            firstGuessVector[i] = 0.5 * (a[i] + b[i]);
        }

        final ClippedDifferenceFunction fnc = new ClippedDifferenceFunction(a, b, func, yy);
        if (jaco == null) {
            jaco = getNumericalJacoby(a, b, yy, fnc, params, xx.length, yy.length, delta);
        } else {
            jaco = jfunc.f(yy, params);
        }

        OEOperator operator;
        switch (method) {
            case NEWTON:
//                # Gauss Newton Step
//                def operator(x, y, k):
//                    return oec.gauss_newton_operator(a, b, x, y, k)
                operator = new GaussNewtonOperator();
//
//                def reterrcov(k):
//                    return np.zeros((k.shape[1], k.shape[1]))

//
//                def diagnose(x, y, k, sr):
//                    return oec.gauss_newton_gain_aver_cost(x, y, k)

                break;
            case NEWTON_SE:
//                # Gauss Newton with measurement error
//                def operator(x, y, k):
//                    return oec.gauss_newton_operator_with_se(a, b, x, y, k, sei)
                operator = new GaussNewtonWithSEOperator();
//
//                def reterrcov(k):
//                    return oec.gauss_newton_se_ret_err_cov(k, sei)
//
//                def diagnose(x, y, k, sr):
//                    return oec.gauss_newton_se_gain_aver_cost(x, y, k, sei, sr)
                break;
            case OE:
//                 # Gauss newton Optimal Estimation
//                def operator(x, y, k):
//                    return oec.optimal_estimation_gauss_newton_operator(a, b, x, y, k, sei, sai, xa)
                operator = new GaussNewtonOptimalEstimationOperator();
//
//                def reterrcov(k):
//                    return oec.oe_ret_err_cov(k, sei, sai)
//
//                def diagnose(x, y, k, sr):
//                    return oec.oe_gain_aver_cost(x, y, k, xa, sei, sai, sr)
                break;
            default:
                throw new IllegalArgumentException("Method '" + method.getName() + "' not supported.");
        }

//        # prior as first guess ...
//        if fg is None:
//            xn = xa
//        else:
//            xn = fg

        double[] xn;
        if (firstGuessVector != null) {
            xn = firstGuessVector;
        } else {
            xn = xa;
        }

//        ### Do the iteration
//                yn = fnc(xn)
//        ii, conv = 0, False
//        while True:
//            ii += 1
//            # Iteration step
//            yn = fnc(xn)
//            kk = dfnc(xn)
//            xn, ix, sri, sr = operator(xn, yn, kk)
//            # print diagnose(xn, yn, kk, sr)[2]
//
//            # Rodgers convergence criteria
//                    eps = 0.000001
//            if method in (1, 2, 4, 5):
//                if oec.norm_error_weighted_x(ix, sri) < eps * ix.size:
//                    conv = True
//                    break
//
//            # only aplicable if *no* prior knowledge is used
//            if method in (0, 1, 3):
//                if oec.norm_y(yn) < epsy:
//                    conv = True
//                    break
//
//            # if maxiter is reached,  no-converge and stop
//            if ii > maxiter:
//                conv = False
//                break

        int ii = 0;
        double[] yn;
        double[][] kk;
        while (ii < maxiter) {
            ii++;
            yn = fnc.f(xn, params);
            kk = jfunc.f(xn, params);
            OeOperatorResult result = operator.result(a, b, xn, yn, kk, sei, sai, sa);
            if (method == InversionMethod.NEWTON) {
//                if oec.norm_y(yn) < epsy:
//                    conv = True
//                    break
                if (norm(yn) < epsy) {
                    break;
                }
            } else {
//                if oec.norm_error_weighted_x(ix, sri) < eps * ix.size:
//                    conv = True
//                    break
                if (normErrorWeighted(result.getIncrX(), result.getRetErrCovI()) < epsy) {
                    break;
                }
            }
        }

//         # calculate latest yn, kk, sr
//        yn = fnc(xn)
//        kk = dfnc(xn)
//        sr = reterrcov(kk)
//        # calculate diagnose quantities
//                gg, av, co = diagnose(xn, yn, kk, sr)
//        return xn, kk, conv, ii, av, gg, sr, co

        yn = fnc.f(xn, params);
        kk = jfunc.f(xn, params);

        if (ii >= maxiter) {
             // todo: how to handle?
            return null;
        } else {
            return new OptimalEstimationResult();
        }

    }

    static double[] clip1D(double[] a, double[] b, double[] x) {
        double[] clipped = new double[a.length];
        for (int i = 0; i < x.length; i++) {
            clipped[i] = Math.min(Math.max(x[i], a[i]), b[i]);
        }
        return clipped;
    }

    static double[][] getNumericalJacoby(double[] a, double[] b, double[] x, TcwvFunction func,
                                         double[] fparams, int nx, int ny, double delta) {

        double[] dx = new double[a.length];
        for (int i = 0; i < dx.length; i++) {
            dx[i] = (b[i] - a[i]) / 2.0;
        }
        double[][] jac = new double[ny][nx];
        double[] dxm = new double[nx];
        double[] dxp = new double[nx];
        double[] dyy = new double[ny];
        for (int ix = 0; ix < nx; ix++) {
            dxm[ix] = x[ix] - dx[ix];
            dxp[ix] = x[ix] + dx[ix];
            final double[] fp = func.f(clip1D(a, b, dxp), fparams);
            final double[] fm = func.f(clip1D(a, b, dxm), fparams);
            for (int iy = 0; iy < ny; iy++) {
                dyy[iy] = fp[iy] - fm[iy];
                jac[iy][ix] = 0.5 * dyy[iy] / dx[ix];
            }
        }

        return jac;
    }

    static double norm(double[] src) {
        // return (inn * inn).mean()
        double norm = 0.0;
        for (int i = 0; i < src.length; i++) {
            norm += src[i] * src[i];
        }
        return norm / src.length;
    }

    static double normErrorWeighted(double[] ix, double[][] sri) {
        // return np.dot(ix.T, np.dot(sri, ix))
        Matrix ixMatrix = new Matrix(ix.length, 1);
        for (int i = 0; i < ix.length; i++) {
            ixMatrix.set(i, 0, ix[i]);
        }
        final Matrix sriMatrix = new Matrix(sri);
        final Matrix sriDotIxMatrix = sriMatrix.times(ixMatrix);
        return ixMatrix.transpose().times(sriDotIxMatrix).get(0, 0);
    }

}
