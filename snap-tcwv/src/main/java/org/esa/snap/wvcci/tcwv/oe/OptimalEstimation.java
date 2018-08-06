package org.esa.snap.wvcci.tcwv.oe;

import Jama.Matrix;
import org.esa.snap.wvcci.tcwv.TcwvFunction;
import org.esa.snap.wvcci.tcwv.interpolation.ClippedDifferenceFunction;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.NumericalJacobiFunction;

/**
 * Class providing the Optimal Estimation algorithm, i.e. the inversion.
 *
 * @author olafd
 */
public class OptimalEstimation {

    private ClippedDifferenceFunction clippedDiffFunc;
    private JacobiFunction jfunc;
    private double[] a;
    private double[] b;
    private double[] params;

    /**
     * @param func   - 'forward function' from 'state vector' to 'measurements' (e.g. as in lut2func.py)
     * @param a      - lower bound of state
     * @param b      - upper bound of state
     * @param yy     - measurements vector
     * @param params - optional input parameters for func
     * @param jfunc  - corresponding 'Jacobi function' (see LutJacobiFunction)
     */
    public OptimalEstimation(TcwvFunction func,
                             double[] a, double[] b, double[] yy, double[] params,
                             JacobiFunction jfunc) {
        this.a = a;
        this.b = b;
        this.params = params;
        this.jfunc = jfunc;

        clippedDiffFunc = new ClippedDifferenceFunction(a, b, func, yy);
        if (jfunc == null) {
            double DELTA = 0.001;
            this.jfunc = new NumericalJacobiFunction(a, b, clippedDiffFunc, yy, DELTA);
        }
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
                                          double[] y,
                                          double[][] se,
                                          double[][] sa,
                                          double[] xa,
                                          OEOutputMode outputMode,
                                          int maxiter) {

        double[][] sei;
        double[][] sai;
        if (method == InversionMethod.NEWTON) {
            sei = new double[y.length][y.length];
            sai = new double[a.length][a.length];
        } else if (method == InversionMethod.NEWTON_SE) {
            sai = new double[a.length][a.length];
            sei = new Matrix(se).inverse().getArray();
        } else {
            sai = new Matrix(sa).inverse().getArray();
            sei = new Matrix(se).inverse().getArray();
        }

        return optimize(xa, sei, sai, method, outputMode, maxiter);
    }

    /**
     * Java version of RPs optimal_estimation_py3 --> my_optimizer
     *
     * @param xa - prior state
     * @param sei - inverse of measurement error covariance matrix
     * @param sai - inverse of prior error covariance matrix
     * @param method - inversion method
     * @param outputMode - output mode
     *
     * @return OptimalEstimationResult
     */
    private OptimalEstimationResult optimize(double[] xa,
                                             double[][] sei, double[][] sai,
                                             InversionMethod method,
                                             OEOutputMode outputMode,
                                             int maxiter) {

        double[] firstGuessVector = new double[a.length];
        for (int i = 0; i < firstGuessVector.length; i++) {
            firstGuessVector[i] = 0.5 * (a[i] + b[i]);
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

        // if available, prior as first guess ...
        double[] xn;
//        if (xa != null) {
//            xn = xa;
//        } else {
//            xn = firstGuessVector;
//        }

        // actual CAWA Python code, but seems to be wrong?! Here we always take the first guess...
//        if fg is None:
//            xn = xa
//        else:
//             xn = fg
        if (firstGuessVector == null) {
            xn = xa;
        } else {
            xn = firstGuessVector;
        }

        // Do the iteration
        int ii = 0;
        double[] yn;
        double[][] kk;
        OeOperatorResult result = null;
        boolean convergence = false;
        while (ii <= maxiter) {
            ii++;
            yn = clippedDiffFunc.f(xn, params);
            kk = jfunc.f(xn, params);

            result = operator.result(a, b, xn, yn, kk, sei, sai, xa);
            xn = result.getCnx();
            double EPSY = 0.000001;
            if (method == InversionMethod.NEWTON) {
                if (OptimalEstimationUtils.norm(yn) < EPSY) {
                    convergence = true;
                    break;
                }
            } else {
                final double[][] sri = result.getRetErrCovI();
                final double normErrorWeighted = OptimalEstimationUtils.normErrorWeighted(result.getIncrX(), sri);
                if (normErrorWeighted < EPSY) {
                    convergence = true;
                    break;
                }
            }
        }

        yn = clippedDiffFunc.f(xn, params);
        kk = jfunc.f(xn, params);
        double[][] sr;
        DiagnoseResult diagnoseResult;

        switch (outputMode) {
            case BASIC:
                return new OptimalEstimationResult(xn, kk, convergence, ii, null, null);
            case FULL:
                if (result != null && result.getRetErrCov() != null) {
                    sr = result.getRetErrCov();
                    diagnoseResult = diagnose.diagnose(xn, yn, kk, xa, sei, sai, sr);
                    return new OptimalEstimationResult(xn, kk, convergence, ii, sr, diagnoseResult);
                } else {
                    return new OptimalEstimationResult(xn, kk, convergence, ii, null, null);
                }
            case EXTENDED:
                sr = retErrCov.compute(kk, sei, sai);
                diagnoseResult = diagnose.diagnose(xn, yn, kk, xa, sei, sai, sr);
                return new OptimalEstimationResult(xn, kk, convergence, ii, sr, diagnoseResult);
            default:
                throw new IllegalArgumentException("Output mode '" + outputMode.getName() + "' not supported.");
        }
    }

}
