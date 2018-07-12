package org.esa.snap.wvcci.tcwv;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 13:18
 *
 * @author olafd
 */
public interface ErrorCovariance {

    double[][] compute(double[][] kk, double[][] sei, double[][] sai);
}
