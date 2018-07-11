package org.esa.snap.wvcci.tcwv;

/**
 * Function object providing a Jacoby matrix
 *
 * @author olafd
 */
public interface JacobyFunction {

    public double[][] f(double[] x, double[] params);
}
