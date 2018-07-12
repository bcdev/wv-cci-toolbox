package org.esa.snap.wvcci.tcwv;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 13:23
 *
 * @author olafd
 */
public class GaussNewtonErrorCovariance implements ErrorCovariance {

    @Override
    public double[][] compute(double[][] kk, double[][] sei, double[][] sai) {
        return new double[kk.length][kk.length];
    }
}
