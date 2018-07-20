package org.esa.snap.wvcci.tcwv;

/**
 * Function object mapping the Python breadboard equivalent:
 * def fnc(x):
 * return func(oec.clipper(a, b, x), fparams) - y
 *
 * @author olafd
 */
public class TcwvForwardFunction implements TcwvFunction {

    private double[] a;
    private double[] b;
    private TcwvFunction func;

    TcwvForwardFunction(double[] a, double[] b, TcwvFunction func) {
        this.a = a;
        this.b = b;
        this.func = func;
    }

    @Override
    public double[] f(double[] x, double[] params) {
        final double[] clippedResult = func.f(OptimalEstimationUtils.clip1D(a, b, x), params);

        double[] clippedDiffResult = new double[clippedResult.length];
        for (int i = 0; i < clippedDiffResult.length; i++) {
            clippedDiffResult[i] = clippedResult[i];
        }
        return clippedDiffResult;
    }
}
