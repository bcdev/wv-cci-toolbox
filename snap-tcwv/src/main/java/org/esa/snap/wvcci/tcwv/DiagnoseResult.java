package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * Holder for OE diagnose result.
 *
 * @author olafd
 */
public class DiagnoseResult {

    private Matrix gain;
    private Matrix average;
    private double cost;

    DiagnoseResult(Matrix gain, Matrix average, double cost) {
        this.gain = gain;
        this.average = average;
        this.cost = cost;
    }

    public Matrix getGain() {
        return gain;
    }

    public Matrix getAverage() {
        return average;
    }

    public double getCost() {
        return cost;
    }
}
