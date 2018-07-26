package org.esa.snap.wvcci.tcwv.oe;

/**
 * Interface for result diagnostics.
 *
 * @author olafd
 */
public interface Diagnose {

    DiagnoseResult diagnose(double[] xn, double[] yn, double[][] kk, double[] xa,
                            double[][] sei, double[][] sai, double[][] sr);
}
