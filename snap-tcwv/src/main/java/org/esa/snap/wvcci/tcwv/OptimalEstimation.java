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
    private JacobiFunction jfunc;
    private double[] a;
    private double[] b;
    private double[] xx;
    private double[] params;

    /**
     * @param func   - 'forward function' from 'state vector' to 'measurements' (e.g. as in lut2func.py)
     * @param a      - lower bound of state
     * @param b      - upper bound of state
     * @param xx     - state vector (e.g. LUT)
     * @param params - optional input parameters for func
     * @param jfunc  - corresponding 'Jacobi function' (see LutJacobiFunction)
     */
    public OptimalEstimation(TcwvFunction func,
                             double[] a, double[] b, double[] xx, double[] params,
                             JacobiFunction jfunc) {
        this.func = func;
        this.a = a;
        this.b = b;
        this.xx = xx;
        this.params = params;
        this.jfunc = jfunc;
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
                                          double[] xa,
                                          OEOutputMode outputMode) {

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

        return optimize(yy, xa, sei, sai, sa, method, outputMode);
    }


    /**
     * Java version of RPs optimal_estimation_py3 --> my_optimizer
     *
     * @param yy - measurement
     * @param xa - prior state
     * @param sei - inverse of measurement error covariance matrix
     * @param sai - inverse of prior error covariance matrix
     * @param sa - prior error covariance
     * @param method - inversion method
     * @param outputMode - output mode
     *
     * @return OptimalEstimationResult
     */
    private OptimalEstimationResult optimize(double[] yy, double[] xa,
                                             double[][] sei, double[][] sai, double[][] sa,
                                             InversionMethod method,
                                             OEOutputMode outputMode) {

        // some default settings from breadboard (same as in Cawa):
        final int maxiter = 20;
        final double delta = 0.001;
        final double epsy = 0.000001;
        double[] firstGuessVector = new double[a.length];
        for (int i = 0; i < firstGuessVector.length; i++) {
            firstGuessVector[i] = 0.5 * (a[i] + b[i]);
        }

        final ClippedDifferenceFunction fnc = new ClippedDifferenceFunction(a, b, func, yy);
        if (jfunc == null) {
            jfunc = new NumericalJacobiFunction(a, b, fnc, yy, delta);
        }

        OEOperator operator;
        Diagnose diagnose;
        ErrorCovariance retErrCov;
        switch (method) {
            case NEWTON:
                // Gauss Newton Step
                operator = new GaussNewtonOperator();
                retErrCov = new GaussNewtonErrorCovariance();
                diagnose = new GaussNewtonDiagnose();
                break;
            case NEWTON_SE:
                // Gauss Newton with measurement error
                operator = new GaussNewtonWithSEOperator();
                retErrCov = new GaussNewtonWithSEErrorCovariance();
                diagnose = new GaussNewtonWithSEDiagnose();
                break;
            case OE:
                 // Gauss newton Optimal Estimation
                operator = new GaussNewtonOEOperator();
                retErrCov = new GaussNewtonOEErrorCovariance();
                diagnose = new GaussNewtonOEDiagnose();
                break;
            default:
                throw new IllegalArgumentException("Method '" + method.getName() + "' not supported.");
        }

//        # prior as first guess ...
        double[] xn;
        if (xa != null) {
            xn = xa;
        } else {
            xn = firstGuessVector;
        }
        xn = firstGuessVector;

//        ### Do the iteration
        int ii = 0;
        double[] yn;
        double[][] kk;
        OeOperatorResult result = null;
        boolean convergence = false;
        while (ii < maxiter) {
            ii++;
            yn = fnc.f(xn, params);
            kk = jfunc.f(xn, params);
            result = operator.result(a, b, xn, yn, kk, sei, sai, sa);
            if (method == InversionMethod.NEWTON) {
                if (OptimalEstimationUtils.norm(yn) < epsy) {
                    convergence = true;
                    break;
                }
            } else {
                final double[][] sri = result.getRetErrCovI();
                if (OptimalEstimationUtils.normErrorWeighted(result.getIncrX(), sri) < epsy) {
                    convergence = true;
                    break;
                }
            }
        }

        yn = fnc.f(xn, params);
        kk = jfunc.f(xn, params);
        double[][] sr;
        DiagnoseResult diagnoseResult;

        switch (outputMode) {
            case BASIC:
                return new OptimalEstimationResult(xn, kk, convergence, ii, null, null);
            case FULL:
                sr = result.getRetErrCov();
                diagnoseResult = diagnose.diagnose(xn, yn, kk, xa, sei, sai, sr);
                return new OptimalEstimationResult(xn, kk, convergence, ii, sr, diagnoseResult);
            case EXTENDED:
                sr = retErrCov.compute(kk, sei, sai);
                diagnoseResult = diagnose.diagnose(xn, yn, kk, xa, sei, sai, sr);
                return new OptimalEstimationResult(xn, kk, convergence, ii, sr, diagnoseResult);
            default:
                throw new IllegalArgumentException("Output mode '" + outputMode.getName() + "' not supported.");
        }
    }

}
