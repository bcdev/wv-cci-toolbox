package org.esa.snap.wvcci.tcwv;

import Jama.Matrix;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.07.2018
 * Time: 10:36
 *
 * @author olafd
 */
public class DiagnoseResult {

    private Matrix gain;
    private Matrix average;
    private double cost;

    public DiagnoseResult(Matrix gain, Matrix average, double cost) {
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
