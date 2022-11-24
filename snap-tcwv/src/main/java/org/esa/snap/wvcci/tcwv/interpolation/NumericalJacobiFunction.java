package org.esa.snap.wvcci.tcwv.interpolation;

import org.esa.snap.wvcci.tcwv.TcwvFunction;
import org.esa.snap.wvcci.tcwv.oe.OptimalEstimationUtils;

import static org.esa.snap.wvcci.tcwv.oe.OptimalEstimationUtils.clip1D;

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
        return getNumericalJacobi(a, b, x, fnc, params, x.length, y.length, delta);
    }

    private double[][] getNumericalJacobi(double[] a, double[] b, double[] x, TcwvFunction func,
                                                double[] fparams, int nx, int ny, double delta) {

        double[] dx = new double[a.length];
        for (int i = 0; i < dx.length; i++) {
            dx[i] = (b[i] - a[i]) * delta;
        }
        double[][] jac = new double[ny][nx];
        double[] dxm = new double[nx];
        double[] dxp = new double[nx];
        double[] dyy = new double[ny];

        for (int ix = 0; ix < nx; ix++) {
            System.arraycopy(x, 0, dxm, 0, x.length);
            System.arraycopy(x, 0, dxp, 0, x.length);
            dxm[ix] = x[ix] - dx[ix];
            dxp[ix] = x[ix] + dx[ix];
            final double[] fp = func.f(OptimalEstimationUtils.clip1D(a, b, dxp), fparams);
            final double[] fm = func.f(OptimalEstimationUtils.clip1D(a, b, dxm), fparams);
            for (int iy = 0; iy < ny; iy++) {
                dyy[iy] = fp[iy] - fm[iy];
                jac[iy][ix] = 0.5 * dyy[iy] / dx[ix];
            }
        }

        return jac;
    }
}
