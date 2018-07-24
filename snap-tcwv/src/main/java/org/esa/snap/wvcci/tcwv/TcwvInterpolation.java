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

    public static TcwvFunction lut2Function(double[][] luts, final double[][] axes) {

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        TcwvFunction tcwvFunction = (x, params) -> {
            double[] values = new double[luts.length];
            double[] xNew;
            xNew = concatXandParams(x, params);
            for (int i = 0; i < lookupTables.length; i++) {
                values[i] = lookupTables[i].getValue(xNew);
            }
            return values;
        };

        return tcwvFunction;
    }

    private static double[] concatXandParams(double[] x, double[] params) {
        double[] xNew;
        if (params != null) {
            xNew = new double[x.length + params.length];
            System.arraycopy(x, 0, xNew, 0, x.length);
            System.arraycopy(params, 0, xNew, x.length, params.length);
        } else {
            xNew = new double[x.length];
            System.arraycopy(x, 0, xNew, 0, x.length);
        }
        return xNew;
    }

    public static JacobiFunction jacobiLut2Function(double[][] luts, final double[][] axes, int ny, int nx) {

        // luts: 8*6*400*100: we will store 8 (6*400*100) LUTs, each one holding one Jacobi element

        if (luts.length != ny * nx) {
            // should never happen!
            throw new IllegalStateException("Jacobi matrix dimensions do not match.");
        }

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        JacobiFunction lutJacobiFunction = (x, params) -> {

            double[] values = new double[luts.length];
            double[] xNew = concatXandParams(x, params);
            for (int i = 0; i < lookupTables.length; i++) {
                values[i] = lookupTables[i].getValue(xNew);
            }

            // resort as ny * ny array, skip rest in x dimension, as in BB (todo: ask RP what this means...)
            double[][] jaco = new double[ny][ny];
            int index = 0;
            for (int i = 0; i < ny; i++) {
                for (int j = 0; j < ny; j++) {
                    jaco[i][j] = values[index++];
                }
                index += (nx-ny);
            }

            return jaco;
        };

        return lutJacobiFunction;
    }

    public static TcwvFunction getForwardFunctionOcean(TcwvOceanLut tcwvOceanLut) {
        // 6*6*11*11*9*9*3 --> 3*6*6*11*11*9*9 :
        // we will store 3 (6*6*11*11*9*9) LUTs, each one holding one element for one band
        final double[][][][][][][] lutArraySwapped =
                TcwvInterpolationUtils.change7DArrayLastToFirstDimension(tcwvOceanLut.getLutArray());
        final double[][] lutArrays1D = new double[lutArraySwapped.length][];
        for (int i = 0; i < lutArraySwapped.length; i++) {
            lutArrays1D[i] = TcwvInterpolationUtils.convert6Dto1DArray(lutArraySwapped[i]);
        }

        final double[][] axes = tcwvOceanLut.getAxes();
        // Python: self._forward
//        return lut2Function(lutArrays1D, axes);
        return lut2Function(lutArrays1D, axes);
    }

    public static TcwvFunction getForwardFunctionLand(TcwvLandLut tcwvLandLut) {
        // same as for ocean, but 10D
        final double[][][][][][][][][][] lutArraySwapped =
                TcwvInterpolationUtils.change10DArrayLastToFirstDimension(tcwvLandLut.getLutArray());
        final double[][] lutArrays1D = new double[lutArraySwapped.length][];
        for (int i = 0; i < lutArraySwapped.length; i++) {
            lutArrays1D[i] = TcwvInterpolationUtils.convert9Dto1DArray(lutArraySwapped[i]);
        }

        final double[][] axes = tcwvLandLut.getAxes();
        return lut2Function(lutArrays1D, axes);
    }

    public static JacobiFunction getJForwardFunctionOcean(TcwvOceanLut tcwvOceanLut) {
        // 6*6*11*11*9*9*18 --> 18*6*6*11*11*9*9 :
        // we will store 18 (6*6*11*11*9*9) LUTs, each one holding one Jacobi element
        final double[][][][][][][] jlutArraySwapped =
                TcwvInterpolationUtils.change7DArrayLastToFirstDimension(tcwvOceanLut.getJlutArray());
        final double[][] jlutArrays1D = new double[jlutArraySwapped.length][];
        for (int i = 0; i < jlutArraySwapped.length; i++) {
            jlutArrays1D[i] = TcwvInterpolationUtils.convert6Dto1DArray(jlutArraySwapped[i]);
        }

        // Python: self._jacobi
        return jacobiLut2Function(jlutArrays1D,
                                  tcwvOceanLut.getAxes(),   // jaxes = axes
                                  tcwvOceanLut.getJaco()[0],
                                  tcwvOceanLut.getJaco()[1]);
    }

    public static JacobiFunction getJForwardFunctionLand(TcwvLandLut tcwvLandLut) {
        // same as for ocean, but 10D
        final double[][][][][][][][][][] jlutArraySwapped =
                TcwvInterpolationUtils.change10DArrayLastToFirstDimension(tcwvLandLut.getJlutArray());
        final double[][] jlutArrays1D = new double[jlutArraySwapped.length][];
        for (int i = 0; i < jlutArraySwapped.length; i++) {
            jlutArrays1D[i] = TcwvInterpolationUtils.convert9Dto1DArray(jlutArraySwapped[i]);
        }

        return jacobiLut2Function(jlutArrays1D,
                                  tcwvLandLut.getAxes(),   // jaxes = axes
                                  tcwvLandLut.getJaco()[0],
                                  tcwvLandLut.getJaco()[1]);
    }
}
