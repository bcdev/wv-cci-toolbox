package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Utility methods for optimal estimation algorithm
 *
 * @author olafd
 */
public class OptimalEstimationUtils {

    public static double[] clip1D(double[] a, double[] b, double[] x) {
        double[] clipped = new double[a.length];
        for (int i = 0; i < x.length; i++) {
            clipped[i] = Math.min(Math.max(x[i], a[i]), b[i]);
        }
        return clipped;
    }

    public static double[][] getNumericalJacobi(double[] a, double[] b, double[] x, TcwvFunction func,
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
            final double[] fp = func.f(clip1D(a, b, dxp), fparams);
            final double[] fm = func.f(clip1D(a, b, dxm), fparams);
            for (int iy = 0; iy < ny; iy++) {
                dyy[iy] = fp[iy] - fm[iy];
                jac[iy][ix] = 0.5 * dyy[iy] / dx[ix];
            }
        }

        return jac;
    }

    public static double norm(double[] src) {
        // return (inn * inn).mean()
        double norm = 0.0;
        for (int i = 0; i < src.length; i++) {
            norm += src[i] * src[i];
        }
        return norm / src.length;
    }

    public static double normErrorWeighted(double[] ix, double[][] sri) {
        // return np.dot(ix.T, np.dot(sri, ix))
        Matrix ixMatrix = new Matrix(ix.length, 1);
        for (int i = 0; i < ix.length; i++) {
            ixMatrix.set(i, 0, ix[i]);
        }
        final Matrix sriMatrix = new Matrix(sri);
        final Matrix sriDotIxMatrix = sriMatrix.times(ixMatrix);
        return ixMatrix.transpose().times(sriDotIxMatrix).get(0, 0);
    }

    public static Matrix leftInverse(double[][] src) {
        // return np.dot(inverse(np.dot(inn.T, inn)), inn.T)
        final Matrix srcMatrix = new Matrix(src);
        final Matrix srcMatrixT = srcMatrix.transpose();
        return ((srcMatrixT.times(srcMatrix)).inverse()).times(srcMatrixT);
    }

}
