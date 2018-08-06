package org.esa.snap.wvcci.tcwv.interpolation;

import org.esa.snap.core.util.math.LookupTable;
import org.esa.snap.wvcci.tcwv.TcwvFunction;
import org.esa.snap.wvcci.tcwv.TcwvLandLut;
import org.esa.snap.wvcci.tcwv.TcwvOceanLut;

/**
 * Class providing methods related to lookup table access and interpolation
 *
 * @author olafd
 */
public class TcwvInterpolation {

    /**
     * Provides a {@link TcwvFunction} object implementing the 'lut2func' logic from Python breadboard.
     *
     * @param luts - array of lookup tables, each LUT is a 1D double array here
     * @param axes - array of axes, each axis is a 1D double array here
     *
     * @return TCWV function
     */
    public static TcwvFunction lut2Function(double[][] luts, final double[][] axes) {

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        return (x, params) -> {
            double[] values = new double[luts.length];
            double[] xNew;
            xNew = TcwvInterpolationUtils.concat1DArrays(x, params);
            for (int i = 0; i < lookupTables.length; i++) {
                values[i] = lookupTables[i].getValue(xNew);
            }
            return values;
        };
    }

    /**
     * Provides a {@link JacobiFunction} object implementing the 'jlut2func' logic from Python breadboard.
     *
     * @param luts - array of lookup tables, each LUT is a 1D double array here
     * @param axes - array of axes, each axis is a 1D double array here
     * @param ny - y dimension of Jacobi matrix
     * @param nx - x dimension of Jacobi matrix
     *
     * @return Jacobi function
     */
    public static JacobiFunction jacobiLut2Function(double[][] luts, final double[][] axes, int ny, int nx) {

        // e.g. 8*6*400*100: we will store 8 (6*400*100) LUTs, each one holding one Jacobi element

        if (luts.length != ny * nx) {
            // should never happen!
            throw new IllegalStateException("Jacobi matrix dimensions do not match.");
        }

        LookupTable[] lookupTables = new LookupTable[luts.length];
        for (int i = 0; i < luts.length; i++) {
            lookupTables[i] = new LookupTable(luts[i], axes);
        }

        return (x, params) -> {

            double[] values = new double[luts.length];
            double[] xNew = TcwvInterpolationUtils.concat1DArrays(x, params);
            for (int i = 0; i < lookupTables.length; i++) {
                values[i] = lookupTables[i].getValue(xNew);
            }

            // resort as ny * 3 array, ignore rest in nx dimension, as in breadboard (todo: ask RP what this means...)
            // ny=4, nx=6 --> ny=4, nx=3     (MODIS ocean)
            // ny=5, nx=9 --> ny=5, nx=3     (MODIS land)
            // ny=3, nx=6 --> ny=3, nx=3      (MERIS ocean)
            // ny=3, nx=9 --> ny=3, nx=3      (MERIS land)
            double[][] jaco = new double[ny][3];
            int index = 0;
            for (int i = 0; i < ny; i++) {
                for (int j = 0; j < 3; j++) {
                    jaco[i][j] = values[index++];
                }
                index += (nx-3);
            }

            return jaco;
        };
    }

    /**
     * Wrapper providing a TCWV forward function for ocean
     *
     * @param tcwvOceanLut - LUT for ocean, provided as a {@link TcwvOceanLut}
     *
     * @return TCWV function
     */
    public static TcwvFunction getForwardFunctionOcean(TcwvOceanLut tcwvOceanLut) {
        // e.g. 6*6*11*11*9*9*3 --> 3*6*6*11*11*9*9 :
        // we will store 3 (6*6*11*11*9*9) LUTs, each one holding one element for one band
        final double[][][][][][][] lutArraySwapped =
                TcwvInterpolationUtils.change7DArrayLastToFirstDimension(tcwvOceanLut.getLutArray());
        final double[][] lutArrays1D = new double[lutArraySwapped.length][];
        for (int i = 0; i < lutArraySwapped.length; i++) {
            lutArrays1D[i] = TcwvInterpolationUtils.convert6Dto1DArray(lutArraySwapped[i]);
        }

        final double[][] axes = tcwvOceanLut.getAxes();
        // Python: self._forward
        return lut2Function(lutArrays1D, axes);
    }

    /**
     * Wrapper providing a TCWV forward function for land
     *
     * @param tcwvLandLut - LUT for land, provided as a {@link TcwvLandLut}
     *
     * @return TCWV function
     */
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

    /**
     * Wrapper providing a Jacobi forward function for ocean
     *
     * @param tcwvOceanLut - LUT for ocean, provided as a {@link TcwvOceanLut}
     *
     * @return Jacobi function
     */
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

    /**
     * Wrapper providing a Jacobi forward function for land
     *
     * @param tcwvLandLut - LUT for land, provided as a {@link TcwvLandLut}
     *
     * @return Jacobi function
     */
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
