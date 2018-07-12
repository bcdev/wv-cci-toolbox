package org.esa.snap.wvcci.tcwv;

/**
 * Function object providing a 'LUT Jacoby' function from a Jacobi LUT generated from the original LUT.
 * See Python breadboard:
 * lut2jacobian_lut.py --> generate_jacobian_lut, jlut2func
 *
 * @author olafd
 */
public class LutJacobiFunction implements JacobiFunction {

    // todo

    public LutJacobiFunction() {
    }

    @Override
    public double[][] f(double[] x, double[] params) {
        return null;
    }
}
