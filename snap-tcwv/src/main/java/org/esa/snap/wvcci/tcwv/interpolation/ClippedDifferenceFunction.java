package org.esa.snap.wvcci.tcwv.interpolation;

import org.esa.snap.wvcci.tcwv.oe.OptimalEstimationUtils;
import org.esa.snap.wvcci.tcwv.TcwvFunction;

/**
 * Function object mapping the Python breadboard equivalent:
 * def fnc(x):
 * return func(oec.clipper(a, b, x), fparams) - y
 *
 * @author olafd
 */
public class ClippedDifferenceFunction implements TcwvFunction {

    private double[] a;
    private double[] b;
    private double[] yDiff;
    private TcwvFunction func;

    public ClippedDifferenceFunction(double[] a, double[] b, TcwvFunction func, double[] yDiff) {
        this.a = a;
        this.b = b;
        this.func = func;
        this.yDiff = yDiff;
    }

    @Override
    public double[] f(double[] x, double[] params) {
        final double[] clippedResult = func.f(OptimalEstimationUtils.clip1D(a, b, x), params);

        double[] clippedDiffResult = new double[clippedResult.length];
        for (int i = 0; i < clippedDiffResult.length; i++) {
            clippedDiffResult[i] = clippedResult[i] - yDiff[i];
        }
        return clippedDiffResult;
    }
}
