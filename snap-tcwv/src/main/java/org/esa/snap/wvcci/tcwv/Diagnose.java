package org.esa.snap.wvcci.tcwv;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 10:35
 *
 * @author olafd
 */
public interface Diagnose {

    DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                            double[][] sei, double[][] sai, double[][] sr);
}
