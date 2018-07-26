package org.esa.snap.wvcci.tcwv.oe;

/**
 * Interface for covariance error computation in OE.
 *
 * @author olafd
 */
public interface ErrorCovariance {

    double[][] compute(double[][] kk, double[][] sei, double[][] sai);
}
