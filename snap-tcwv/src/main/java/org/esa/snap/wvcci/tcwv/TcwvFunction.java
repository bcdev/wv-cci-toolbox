package org.esa.snap.wvcci.tcwv;

/**
 * Interface for CCI TCWV functions.
 *
 * @author olafd
 */
public interface TcwvFunction {

    /**
     * Parametric multivariate function f: R^n --> R^m.
     * Here this may represent a 'forward function' from 'state vector' to 'measurements' with
     *    n = number of state variables
     *    m = number of measurements
     *
     * @param x - double array of state variables
     * @param params - double array of parameters
     *
     * @return f - double array of measurements
     */
    double[] f(double[] x, double[] params);
}
