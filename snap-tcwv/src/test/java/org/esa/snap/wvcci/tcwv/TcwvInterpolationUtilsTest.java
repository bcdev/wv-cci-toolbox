package org.esa.snap.wvcci.tcwv;

import org.junit.Test;

import static org.junit.Assert.*;


public class TcwvInterpolationUtilsTest {


    @Test
    public void testIsMonotonicallyIncreasing() {
        double[] a = new double[]{0, 1, 2, 3, 4, 5};
        assertTrue(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{0, 0, 1, 2, 3, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{0, 1, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{0, 0, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{4, 4, 3, 2, 1, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{4, 3, 2, 1, 0, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));

        a = new double[]{5, 4, 3, 2, 1, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyIncreasing(a));
    }

    @Test
    public void testIsMonotonicallyDecreasing() {
        double[] a = new double[]{0, 1, 2, 3, 4, 5};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 0, 1, 2, 3, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 1, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{0, 0, 2, 3, 4, 4};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{4, 4, 3, 2, 1, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{4, 3, 2, 1, 0, 0};
        assertFalse(TcwvInterpolationUtils.isMontonicallyDecreasing(a));

        a = new double[]{5, 4, 3, 2, 1, 0};
        assertTrue(TcwvInterpolationUtils.isMontonicallyDecreasing(a));
    }


    @Test
    public void testChange4DArrayLastToFirstDimension() {
        double[][][][] src4DArr = new double[5][2][4][3];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        src4DArr[i][j][k][l] = (double) index++;
                    }
                }
            }
        }

        final double[][][][] result4DArr = TcwvInterpolationUtils.change4DArrayLastToFirstDimension(src4DArr);
        assertNotNull(result4DArr);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        assertEquals(result4DArr[l][i][j][k], src4DArr[i][j][k][l], 1.E-8);
                    }
                }
            }
        }

    }

    @Test
    public void testChange7DArrayLastToFirstDimension() {
        double[][][][][][][] src7DArr = new double[5][2][4][3][1][6][7];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        for (int m = 0; m < 1; m++) {
                            for (int n = 0; n < 6; n++) {
                                for (int o = 0; o < 7; o++) {
                                    src7DArr[i][j][k][l][m][n][o] = (double) index++;
                                }
                            }
                        }
                    }
                }
            }
        }

        final double[][][][][][][] result7DArr = TcwvInterpolationUtils.change7DArrayLastToFirstDimension(src7DArr);
        assertNotNull(result7DArr);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        for (int m = 0; m < 1; m++) {
                            for (int n = 0; n < 6; n++) {
                                for (int o = 0; o < 7; o++) {
                                    assertEquals(result7DArr[o][i][j][k][l][m][n], src7DArr[i][j][k][l][m][n][o], 1.E-8);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testChange10DArrayLastToFirstDimension() {
        double[][][][][][][][][][] src10DArr = new double[5][2][4][3][1][6][7][8][9][5];
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        for (int m = 0; m < 1; m++) {
                            for (int n = 0; n < 6; n++) {
                                for (int o = 0; o < 7; o++) {
                                    for (int p = 0; p < 8; p++) {
                                        for (int q = 0; q < 9; q++) {
                                            for (int r = 0; r < 5; r++) {
                                                src10DArr[i][j][k][l][m][n][o][p][q][r] = (double) index++;
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

        final double[][][][][][][][][][] result10DArr = TcwvInterpolationUtils.change10DArrayLastToFirstDimension(src10DArr);

        assertNotNull(result10DArr);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 3; l++) {
                        for (int m = 0; m < 1; m++) {
                            for (int n = 0; n < 6; n++) {
                                for (int o = 0; o < 7; o++) {
                                    for (int p = 0; p < 8; p++) {
                                        for (int q = 0; q < 9; q++) {
                                            for (int r = 0; r < 5; r++) {
                                                assertEquals(result10DArr[r][i][j][k][l][m][n][o][p][q],
                                                             src10DArr[i][j][k][l][m][n][o][p][q][r], 1.E-8);
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
    }
}