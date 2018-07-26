package org.esa.snap.wvcci.tcwv.interpolation;

/**
 * Function object providing a Jacobi matrix
 *
 * @author olafd
 */
public interface JacobiFunction {

    double[][] f(double[] x, double[] params);
}
