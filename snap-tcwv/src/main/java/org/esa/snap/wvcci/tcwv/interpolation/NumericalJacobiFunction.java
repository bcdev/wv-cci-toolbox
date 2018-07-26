package org.esa.snap.wvcci.tcwv.interpolation;

import org.esa.snap.wvcci.tcwv.oe.OptimalEstimationUtils;

/**
 * Numerical Jacobi Function:
 * See breadboard: optimal_estimation.py --> numerical_jacoby
 *
 * @author olafd
 */
public class NumericalJacobiFunction implements JacobiFunction {

    private double[] a;
    private double[] b;
    private double[] y;
    private ClippedDifferenceFunction fnc;
    private double delta;

    public NumericalJacobiFunction(double[] a, double[] b, ClippedDifferenceFunction fnc, double[] y, double delta) {
        this.a = a;
        this.b = b;
        this.fnc = fnc;
        this.y = y;
        this.delta = delta;
    }

    @Override
    public double[][] f(double[] x, double[] params) {
        return OptimalEstimationUtils.getNumericalJacobi(a, b, x, fnc, params, x.length, y.length, delta);
    }
}
