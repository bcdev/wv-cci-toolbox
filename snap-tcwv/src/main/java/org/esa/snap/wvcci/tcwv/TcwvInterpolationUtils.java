package org.esa.snap.wvcci.tcwv;

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
}
