package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.LookupTable;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 17.07.2018
 * Time: 10:24
 *
 * @author olafd
 */
public class TcwvInterpolation {

    public TcwvFunction lut2Function(double[][] luts, final double[][] axes)  {

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        TcwvFunction tcwvFunction = (x, params) -> {
            double[] values = new double[luts.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = lookupTables[i].getValue(x);
            }
            return values;
        };

        return tcwvFunction;
    }

    public JacobiFunction jacobiLut2Function(double[][] luts, final double[][] axes, int ny, int nx)  {

        // luts: 8*6*400*100: we will store 8 (6*400*100) LUTs, each one holding one Jacobi element

        if (luts.length != ny*nx) {
            // should never happen!
            throw new IllegalStateException("Jacobi matrix dimensions do not match.");
        }

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        JacobiFunction lutJacobiFunction = (x, params) -> {
            double[] values = new double[luts.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = lookupTables[i].getValue(x);
            }

            double[][] jaco = new double[ny][nx];
            // resort as ny*nx array...
            int index = 0;
            for (int i = 0; i < ny; i++) {
                for (int j = 0; j < nx; j++) {
                    jaco[i][j] = values[index++];
                }
            }

            return jaco;
        };

        return lutJacobiFunction;
    }


}
