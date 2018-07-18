package org.esa.snap.wvcci.tcwv;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * Utility methods for TCWV interpolation
 *
 * @author olafd
 */
public class TcwvInterpolationUtils {

    public static boolean isMontonicallyIncreasing(double[] arr) {
        for(int i=0 ; i < arr.length-1; i++) {
            if (arr[i+1] <= arr[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isMontonicallyDecreasing(double[] arr) {
        for(int i=0 ; i < arr.length-1; i++) {
            if (arr[i+1] >= arr[i]) {
                return false;
            }
        }
        return true;
    }

    public static double[] convert2Dto1DArray(double[][] src) {
        double[] result = new double[src.length * src[0].length];
        int index = 0;
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {
                      result[index++] = src[i][j];
            }
        }
        return result;
    }

    public static double[] convert3Dto1DArray(double[][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length];
        int index = 0;
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    result[index++] = src[i][j][k];
                }
            }
        }
        return result;
    }


    static int[] getShort1DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayInt = getDataArray(DataType.INT, variable, Integer.class);
        return (int[]) arrayInt.copyToNDJavaArray();
    }

    static double[] getDouble1DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    static double[][] getDouble2DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    static double[][][][] getDouble4DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    static double[][][][][][][] getDouble7DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    static float[][] getFloat2DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayFloat = getDataArray(DataType.FLOAT, variable, Float.class);
        return (float[][]) (arrayFloat != null ? arrayFloat.copyToNDJavaArray() : null);
    }

    private static Array getDataArray(DataType type, Variable variable, Class clazz) throws IOException {
        final int[] origin = new int[variable.getRank()];
        final int[] shape = variable.getShape();
        Array array = null;
        try {
            array = variable.read(new Section(origin, shape));
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (array != null) {
            return Array.factory(type, shape, array.get1DJavaArray(clazz));
        }
        return null;
    }
}
