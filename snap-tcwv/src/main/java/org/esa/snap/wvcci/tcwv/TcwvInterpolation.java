package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.gpf.OperatorException;
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

    public TcwvFunction jacobiLut2Function(double[][] luts, final double[][] axes, int nx, int ny)  {

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


}
