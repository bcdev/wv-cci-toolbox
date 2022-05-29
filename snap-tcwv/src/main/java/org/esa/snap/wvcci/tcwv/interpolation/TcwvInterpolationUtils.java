package org.esa.snap.wvcci.tcwv.interpolation;

import org.esa.snap.wvcci.tcwv.Sensor;
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

    /**
     * Chaecks if input array is monotonically increasing
     *
     * @param arr - input array of doubles
     * @return boolean
     */
    public static boolean isMontonicallyIncreasing(double[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i + 1] <= arr[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Chaecks if input array is monotonically decreasing
     *
     * @param arr - input array of doubles
     * @return boolean
     */
    public static boolean isMontonicallyDecreasing(double[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i + 1] >= arr[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts 2D input array to 1D array
     *
     * @param src - 2D input array of doubles
     * @return 1D result array
     */
    public static double[] convert2Dto1DArray(double[][] src) {
        double[] result = new double[src.length * src[0].length];
        int index = 0;
        for (double[] srcSubArr : src) {
            for (int j = 0; j < src[0].length; j++) {
                result[index++] = srcSubArr[j];
            }
        }
        return result;
    }

    /**
     * Converts 3D input array to 1D array
     *
     * @param src - 3D input array of doubles
     * @return 1D result array
     */
    public static double[] convert3Dto1DArray(double[][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length];
        int index = 0;
        for (double[][] srcSubArr : src) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    result[index++] = srcSubArr[j][k];
                }
            }
        }
        return result;
    }

    /**
     * Converts 4D input array to 1D array
     *
     * @param src - 4D input array of doubles
     * @return 1D result array
     */
    public static double[] convert4Dto1DArray(double[][][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length * src[0][0][0].length];
        int index = 0;
        for (double[][][] srcSubArr : src) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    for (int l = 0; l < src[0][0][0].length; l++) {
                        result[index++] = srcSubArr[j][k][l];
                    }
                }
            }
        }
        return result;
    }

    /**
     * Converts 6D input array to 1D array
     *
     * @param src - 6D input array of doubles
     * @return 1D result array
     */
    public static double[] convert6Dto1DArray(double[][][][][][] src) {
        double[] result = new double[src.length * src[0].length * src[0][0].length * src[0][0][0].length
                * src[0][0][0][0].length * src[0][0][0][0][0].length];
        int index = 0;
        for (double[][][][][] srcSubArr : src) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    for (int l = 0; l < src[0][0][0].length; l++) {
                        for (int m = 0; m < src[0][0][0][0].length; m++) {
                            for (int n = 0; n < src[0][0][0][0][0].length; n++) {
                                result[index++] = srcSubArr[j][k][l][m][n];
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Converts 9D input array to 1D array
     *
     * @param src    - 9D input array of doubles
     * @param sensor
     * @return 1D result array
     */
    public static double[] convert9Dto1DArray(double[][][][][][][][][] src, Sensor sensor) {
        double[] result = new double[src.length * src[0].length * src[0][0].length * src[0][0][0].length
                * src[0][0][0][0].length * src[0][0][0][0][0].length * src[0][0][0][0][0][0].length
                * src[0][0][0][0][0][0][0].length * src[0][0][0][0][0][0][0][0].length];
        int index = 0;

        for (double[][][][][][][][] srcSubArr : src) {
            for (int j = 0; j < src[0].length; j++) {
                for (int k = 0; k < src[0][0].length; k++) {
                    for (int l = 0; l < src[0][0][0].length; l++) {
                        for (int m = 0; m < src[0][0][0][0].length; m++) {
                            for (int n = 0; n < src[0][0][0][0][0].length; n++) {
                                for (int o = 0; o < src[0][0][0][0][0][0].length; o++) {
                                    for (int p = 0; p < src[0][0][0][0][0][0][0].length; p++) {
                                        for (int q = 0; q < src[0][0][0][0][0][0][0][0].length; q++) {
                                            // fix for CAWA MODIS land LUTs:
                                            // todo: remove after LUT update, June 2019
//                                            if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
//                                                if (n == 2) {
//                                                    srcSubArr[j][k][l][m][n][o][p][q] =
//                                                            srcSubArr[j][k][l][m][1][o][p][q];
//                                                }
//                                            }
                                            result[index++] = srcSubArr[j][k][l][m][n][o][p][q];
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

    /**
     * Extracts 1D Netcdf integer variable as primitive 1D int[]
     *
     * @param variable - 1D Netcdf integer variable
     * @return - int[]
     * @throws IOException -
     */
    public static int[] getInt1DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayInt = getDataArray(DataType.INT, variable, Integer.class);
        return (int[]) (arrayInt != null ? arrayInt.copyToNDJavaArray() : null);
    }

    /**
     * Extracts 1D Netcdf double variable as primitive 1D double[]
     *
     * @param variable - 1D Netcdf integer variable
     * @return - double[]
     * @throws IOException -
     */
    public static double[] getDouble1DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    /**
     * Extracts 4D Netcdf double variable as primitive 4D double[][][][]
     *
     * @param variable - 4D Netcdf integer variable
     * @return - double[][][][]
     * @throws IOException -
     */
    public static double[][][][] getDouble4DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    /**
     * Extracts 7D Netcdf double variable as primitive 7D double[][][][][][][]
     *
     * @param variable - 7D Netcdf integer variable
     * @return - double[][][][][][][]
     * @throws IOException -
     */
    public static double[][][][][][][] getDouble7DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    /**
     * Extracts 10D Netcdf double variable as primitive 10D double[][][][][][][][][][]
     *
     * @param variable - 10D Netcdf integer variable
     * @return - double[][][][][][][][][][]
     * @throws IOException -
     */
    public static double[][][][][][][][][][] getDouble10DArrayFromNetcdfVariable(Variable variable) throws IOException {
        final Array arrayDouble = getDataArray(DataType.DOUBLE, variable, Double.class);
        return (double[][][][][][][][][][]) (arrayDouble != null ? arrayDouble.copyToNDJavaArray() : null);
    }

    /**
     * Switches last dimension of 4D input array to the first dimension
     *
     * @param src - 4D input array of doubles
     * @return - rearranged array
     */
    public static double[][][][] change4DArrayLastToFirstDimension(double[][][][] src) {
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

    /**
     * Switches last dimension of 7D input array to the first dimension
     *
     * @param src - 7D input array of doubles
     * @return - rearranged array
     */
    public static double[][][][][][][] change7DArrayLastToFirstDimension(double[][][][][][][] src) {
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

    /**
     * Switches last dimension of 10D input array to the first dimension
     *
     * @param src - 10D input array of doubles
     * @return - rearranged array
     */
    public static double[][][][][][][][][][] change10DArrayLastToFirstDimension(double[][][][][][][][][][] src) {
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

    /**
     * Concatenates two 1D input arrays of doubles
     *
     * @param arr1 - first array
     * @param arr2 second array
     * @return - concatenated array
     */
    public static double[] concat1DArrays(double[] arr1, double[] arr2) {
        double[] xNew;
        if (arr2 != null) {
            xNew = new double[arr1.length + arr2.length];
            System.arraycopy(arr1, 0, xNew, 0, arr1.length);
            System.arraycopy(arr2, 0, xNew, arr1.length, arr2.length);
        } else {
            xNew = new double[arr1.length];
            System.arraycopy(arr1, 0, xNew, 0, arr1.length);
        }
        return xNew;
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
