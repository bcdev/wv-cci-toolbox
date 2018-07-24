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
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i + 1] <= arr[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isMontonicallyDecreasing(double[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i + 1] >= arr[i]) {
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

    public static double[] convert6Dto1DArray(double[][][][][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length * src[0][0][0].length
                * src[0][0][0][0].length * src[0][0][0][0][0].length];
        int index = 0;
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    for (int l = 0; l < src[0][0][0].length; l++) {
                        for (int m = 0; m < src[0][0][0][0].length; m++) {
                            for (int n = 0; n < src[0][0][0][0][0].length; n++) {
                                result[index++] = src[i][j][k][l][m][n];
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static double[] convert9Dto1DArray(double[][][][][][][][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length * src[0][0][0].length
                * src[0][0][0][0].length * src[0][0][0][0][0].length * src[0][0][0][0][0][0].length
                * src[0][0][0][0][0][0][0].length * src[0][0][0][0][0][0][0][0].length];
        int index = 0;
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    for (int l = 0; l < src[0][0][0].length; l++) {
                        for (int m = 0; m < src[0][0][0][0].length; m++) {
                            for (int n = 0; n < src[0][0][0][0][0].length; n++) {
                                for (int o = 0; o < src[0][0][0][0][0][0].length; o++) {
                                    for (int p = 0; p < src[0][0][0][0][0][0][0].length; p++) {
                                        for (int q = 0; q < src[0][0][0][0][0][0][0][0].length; q++) {
                                            result[index++] = src[i][j][k][l][m][n][o][p][q];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }


    static int[] getInt1DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayInt = getDataArray(DataType.INT, variable, Integer.class);
        return (int[]) (arrayInt != null ? arrayInt.copyToNDJavaArray() : null);
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

    static double[][][][][][][][][][] getDouble10DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][][][][][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    static double[][][][] change4DArrayLastToFirstDimension(double[][][][] src) {
        final int[] newDims = new int[]{src[0][0][0].length, src.length, src[0].length, src[0][0].length};
        double[][][][] result = new double[newDims[0]][newDims[1]][newDims[2]][newDims[3]];

        for (int i = 0; i < newDims[0]; i++) {
            for (int j = 0; j < newDims[1]; j++) {
                for (int k = 0; k < newDims[2]; k++) {
                    for (int l = 0; l < newDims[3]; l++) {
                        result[i][j][k][l] = src[j][k][l][i];
                    }
                }
            }
        }
        return result;
    }

    static double[][][][][][][] change7DArrayLastToFirstDimension(double[][][][][][][] src) {
        final int[] newDims = new int[]{src[0][0][0][0][0][0].length, src.length, src[0].length, src[0][0].length,
                src[0][0][0].length, src[0][0][0][0].length, src[0][0][0][0][0].length};
        double[][][][][][][] result =
                new double[newDims[0]][newDims[1]][newDims[2]][newDims[3]][newDims[4]][newDims[5]][newDims[6]];

        for (int i = 0; i < newDims[0]; i++) {
            for (int j = 0; j < newDims[1]; j++) {
                for (int k = 0; k < newDims[2]; k++) {
                    for (int l = 0; l < newDims[3]; l++) {
                        for (int m = 0; m < newDims[4]; m++) {
                            for (int n = 0; n < newDims[5]; n++) {
                                for (int o = 0; o < newDims[6]; o++) {
                                    result[i][j][k][l][m][n][o] = src[j][k][l][m][n][o][i];
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    static double[][][][][][][][][][] change10DArrayLastToFirstDimension(double[][][][][][][][][][] src) {
        final int[] newDims = new int[]{src[0][0][0][0][0][0][0][0][0].length, src.length, src[0].length, src[0][0].length,
                src[0][0][0].length, src[0][0][0][0].length, src[0][0][0][0][0].length,
                src[0][0][0][0][0][0].length, src[0][0][0][0][0][0][0].length, src[0][0][0][0][0][0][0][0].length,};
        double[][][][][][][][][][] result =
                new double[newDims[0]][newDims[1]][newDims[2]][newDims[3]][newDims[4]][newDims[5]][newDims[6]]
                        [newDims[7]][newDims[8]][newDims[9]];

        for (int i = 0; i < newDims[0]; i++) {
            for (int j = 0; j < newDims[1]; j++) {
                for (int k = 0; k < newDims[2]; k++) {
                    for (int l = 0; l < newDims[3]; l++) {
                        for (int m = 0; m < newDims[4]; m++) {
                            for (int n = 0; n < newDims[5]; n++) {
                                for (int o = 0; o < newDims[6]; o++) {
                                    for (int p = 0; p < newDims[7]; p++) {
                                        for (int q = 0; q < newDims[8]; q++) {
                                            for (int r = 0; r < newDims[9]; r++) {
                                                result[i][j][k][l][m][n][o][p][q][r] = src[j][k][l][m][n][o][p][q][r][i];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static Array getDataArray(DataType type, Variable variable, Class clazz) throws IOException {
        final int[] origin = new int[variable.getRank()];
        final int[] shape = variable.getShape();
        Array array;
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
