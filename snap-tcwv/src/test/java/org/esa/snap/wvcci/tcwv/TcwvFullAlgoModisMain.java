package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;

import java.io.IOException;


public class TcwvFullAlgoModisMain {

    public static void main(String[] args) throws IOException {
//        computeOcean();
        computeLand_modis();
        computeLand_modis_2();
        computeLand_meris();
    }

//    private static void computeOcean() throws IOException {
//        final Sensor sensor = Sensor.MODIS_AQUA;
//        TcwvAlgorithm algorithm = new TcwvAlgorithm();
//
//        String auxdataPath = TcwvIO.installAuxdataLuts();
//        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_AQUA);
//        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_AQUA);
//        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
//        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);
//
//        double[] rhoToaWin = new double[]{0.089};
//        double[] rhoToaAbs = new double[]{0.083, 0.051, 0.063};
//        double sza = 56.0;
//        double vza = 158.0;
//        double relAzi = 118.03159332;
//        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
//        double aot865 = 0.1;
//        double priorAot = 0.15;
//        double priorAl0 = 0.13;
//        double priorAl1 = 0.13;
//        double priorT2m = Double.NaN;            // not needed for ocean
//        double priorMslPress = Double.NaN;       // not needed for ocean
//        double priorWsp = 7.5;
//        double priorTcwv = 30.0;
//        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
//                priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
//                priorWsp, priorTcwv);
//        final TcwvResult result = algorithm.compute(sensor,
//                landLut, oceanLut,
//                null, tcwvFunctionOcean,
//                null, jacobiFunctionOcean,
//                input, false);
//
//        System.out.println("Result ocean: " + result.getTcwv());
//    }

    private static void computeLand_modis() throws IOException {
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        String auxdataPath = TcwvIO.installAuxdataLuts();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double[] rhoToaWin = new double[]{0.165, 0.19};
        double[] rhoToaAbs = new double[]{0.14, 0.008, 0.1};
        double sza = 55.9;
        double saa = 166.8;
        double vaa = -94.9;
        double vza = 1.3;
        double relAzi = 180. - Math.abs(saa - vaa);
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;
        double priorMslPress = -1013.25;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                landLut, oceanLut,
                tcwvFunctionLand, null,
                jacobiFunctionland, null,
                input, true);

        System.out.println("Result land MODIS: " + result.getTcwv());
    }

    private static void computeLand_modis_2() throws IOException {
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        String auxdataPath = TcwvIO.installAuxdataLuts();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double[] rhoToaWin = new double[]{0.05588217,  0.06197434};
        double[] rhoToaAbs = new double[]{0.10987211,  0.33038937,  0.22174702};
        double sza = 6.23843994e+01;
        double saa = 166.8;
        double vaa = -94.9;
        double vza = 2.59459991e+01;
        double relAzi = 1.18127800e+02;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;
        double priorMslPress = -1013.25;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("Result land MODIS 2: " + result.getTcwv());
    }

    private static void computeLand_meris() throws IOException {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        String auxdataPath = TcwvIO.installAuxdataLuts();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double[] rhoToaWin = new double[]{0.165, 0.19};
        double[] rhoToaAbs = new double[]{0.14, 0.008, 0.1};
        double sza = 55.9;
        double saa = 166.8;
        double vaa = -94.9;
        double vza = 1.3;
        double relAzi = 180. - Math.abs(saa - vaa);
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;
        double priorMslPress = -1013.25;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("Result land MERIS: " + result.getTcwv());
    }

}