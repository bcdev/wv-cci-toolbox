package org.esa.snap.wvcci.tcwv;

/**
 * Function object mapping the Python breadboard equivalent:
 //        # numerical derivation of fnc (central differential ...)
 //        if jaco is None:
 //           def dfnc(x):
 //              return numerical_jacoby(a, b, x, fnc, x.size, y.size, delta, dtype=dtype)
 //        else:
 //           def dfnc(x):
 //              return jaco(x, jparams)
 *
 * @author olafd
 */
public class NumericalJacobyFunction implements  JacobyFunction {

    private double[] a;
    private double[] b;
    private double[] y;
    private ClippedDifferenceFunction fnc;
    private double[][] jaco;
    private double delta;

    public NumericalJacobyFunction(double[] a, double[] b, ClippedDifferenceFunction fnc, double[] y, double delta) {
        this.a = a;
        this.b = b;
        this.fnc = fnc;
        this.y = y;
        this.delta = delta;
    }

    @Override
    public double[][] f(double[] x, double[] params) {
        return OptimalEstimation.getNumericalJacoby(a, b, x, fnc, params, x.length, y.length, delta);
    }
}
