package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TcwvCowaConsolidatedCciLutsForOnePixelTest {

    private String auxdataPath;

    @Before
    public void setUp() throws Exception {
        auxdataPath = TcwvIO.installAuxdataLuts();
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_land() {
        // todo: extract MERIS landpixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        final double sza = 45.286419;     // data['geo']['SZA'].flat[99]     , 'sun_zenith'
        final double vza = 32.795418;     // data['geo']['OZA'].flat[99]     , 'view_zenith'
        final double saa = 185.191369;    // data['geo']['SAA'].flat[99]     , 'sun_azimuth'
        final double vaa = 306.755;       // data['geo']['OAA'].flat[99]     , 'view_azimuth'

        final double reflectance_13 = 0.00818;  // data['rad'][13].flat[99]
        final double reflectance_14 = 0.00784;  // data['rad'][14].flat[99]
        final double reflectance_15 = 0.00578;  // data['rad'][15].flat[99]

        // def azi2azid(sa,va):
        //     return acosd(cosd(sa)*cosd(va)+sind(sa)*sind(va))
        // out['ADA']=azi2azid(out['SAA'],out['OAA'])
        // 'azi':   180.-data['geo']['ADA'].flat[i]
        final double relAzi = 180. - Math.acos(Math.cos(saa* MathUtils.DTOR) * Math.cos(vaa* MathUtils.DTOR) +
                                                       Math.sin(saa* MathUtils.DTOR) * Math.sin(vaa* MathUtils.DTOR)) * MathUtils.RTOD;

//        double relAziDeg = 58.436;   // todo: test for getting this value from saa, vaa above
//        double relAzi = relAziDeg * MathUtils.DTOR;
//        double relAzi = relAziDeg;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double priorAot = 0.15;        // todo: implement and test mapping nan --> 0.15, we have no aot for MERIS:
        // inp['aot'] = ternary(np.isfinite(data['aot'].flat[i]), data['aot'].flat[i],
        //                      DEMO_CONFIG['PROCESSING']['land_aot_fallback'])

        double aot865 = priorAot;   // todo: do we still need this?

        //// are these ever used in breadboard?? // todo check with Rene
        double pressUnc = TcwvConstants.MERIS_PRESSURE_UNCERTAINTY;      // DEMO_CONFIG['PROCESSING']['pressure_uncertainty']
        double priorAotUnc = TcwvConstants.MERIS_AOT_FALLBACK_UNCERTAINTY;
        double tmpUnc = TcwvConstants.MERIS_TEMPERATURE_UNCERTAINTY;
        double wvlUnc = TcwvConstants.MERIS_SPECTRAL_UNCERTAINTY;
        ////

        double priorAl0 = reflectance_13 * Math.PI;   // data['rad'][13].flat[99]*np.pi
        double priorAl1 = reflectance_14 * Math.PI;   // data['rad'][14].flat[99]*np.pi
        double priorT2m = 279.78384;    // 't2m' from ERA, data['tem'].flat[99]

        double priorMslPress = -Math.log(998.3356);  // 'msl' from ERA, data['prs'].flat[99]
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 15.4964;       // 'tcwv' from ERA, data['tcv'].flat[99]

        double[] rhoToaWin = new double[]{reflectance_13, reflectance_14};  // bands 13, 14
        double[] rhoToaAbs = new double[]{reflectance_15};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("MERIS LAND result.getTcwv() = " + result.getTcwv());
        // Java: 16.4272978
        // Java: 16.2862 (with new L1 uncertainty estimates)
        // Cowa Python: 16.46472
//        assertEquals(16.2862, result.getTcwv(), 0.05);
//        assertEquals(1.12, result.getTcwvUncertainty(), 0.01);
    }

    @Test
    public void testCowaConsolidatedCciLuts_meris_ocean() {
        // todo: extract MERIS ocean pixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MERIS);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 30.0;
        double vza = 20.0;
        double relAzi = 10.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 8.0;
        double priorTcwv = 25.0;

        double[] rhoToaWin = new double[]{0.0317360866889, 0.0313907463};  // bands 13, 14
        double[] rhoToaAbs = new double[]{0.021355692};   // band 15

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MERIS OCEAN result.getTcwv() = " + result.getTcwv());
        // Java: 19.54744
        // Cowa Python: 19.35311
//        assertEquals(19.35311, result.getTcwv(), 0.5);
//        assertEquals(0.597, result.getTcwvUncertainty(), 0.001);
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_land() {
        // todo: extract OLCI land pixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double sza = 30.;
        double vza = 20.0;
        double relAzi = 170.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 280.0;

        double priorMslPress = -Math.log(1005.0);
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 15.0;

        double[] rhoToaWin = new double[]{0.042, 0.042};  // bands 18, 21
        double[] rhoToaAbs = new double[]{0.031, 0.013};   // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionland, null,
                                                    input, true);

        System.out.println("OLCI LAND result.getTcwv() = " + result.getTcwv());
        // Java: 13.7652
        // Java: 14.012 (with new L1 uncertainty estimates)
        // Cowa Python: 13.7686
//         assertEquals(14.012, result.getTcwv(), 0.01);
//        assertEquals(13.918, result.getTcwv(), 0.01);  // with refl uncertainty estimate of 2% instead of fix SE matrix
        // uncertainty:
//        assertEquals(0.853, result.getTcwvUncertainty(), 0.001);
    }

    @Test
    public void testCowaConsolidatedCciLuts_olci_ocean() {
        // todo: extract OLCI ocean pixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.OLCI;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.OLCI);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 30.0;
        double vza = 20.0;
        double relAzi = 10.0;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 8.0;
        double priorTcwv = 25.0;

        double[] rhoToaWin = new double[]{0.031};  // band 18 only!!
        double[] rhoToaAbs = new double[]{0.021, 0.007};   // bands 19, 20

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);

        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("OLCI OCEAN result.getTcwv() = " + result.getTcwv());
        // Java: 22.13765
        // Cowa Python: 22.18858
//        assertEquals(22.18858, result.getTcwv(), 0.1);
//        assertEquals(0.042, result.getTcwvUncertainty(), 0.001);
    }

    @Test
    public void testCowaConsolidatedCciLuts_modis_land() {
        // todo: extract MODIS land pixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvLandLut landLut = TcwvIO.readLandLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        JacobiFunction jacobiFunctionLand = TcwvInterpolation.getJForwardFunctionLand(landLut);

        double aot865 = 0.1;
        double relAzi = 53.56640624999997;
        double priorMslPress = -Math.log(972.3643431194063);
        double priorAl0 = 0.13471372425556183;
        double priorAl1 = 0.18240199983119965;
        double priorTcwv = 22.3887708167446;
        double sza = 44.02040100097656;
        double priorT2m = 289.367343379847;
        double vza = 37.68560028076172;

        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);

        double[] rhoToaWin = new double[]{0.042880710235182445, 0.05806035980596497};
        double[] rhoToaAbs = new double[]{0.034583598016895215, 0.012255925435128554, 0.0202874049561323};

        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          aot865, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          Double.NaN, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, null,
                                                    tcwvFunctionLand, null,
                                                    jacobiFunctionLand, null,
                                                    input, true);

        System.out.println("MODIS TERRA LAND result.getTcwv() = " + result.getTcwv());
//        assertEquals(17.63, result.getTcwv(), 0.2);  // MODIS update RP, 20190902
        // Python result: 17.63 (new test_cowa_land_4, OD, 20191128), Java result: 17.74
//        assertEquals(0.292, result.getTcwvUncertainty(), 0.001); // with MODIS_LAND_SE from 20190902
//        assertEquals(0.046, result.getCost(), 0.001);  // with MODIS_LAND_SE from 20190902
    }

    @Test
    public void testCowaConsolidatedCciLuts_modis_ocean() {
        // todo: extract MODIS ocean pixel from RP package 'consolidated_cci_luts', 20200113
        final Sensor sensor = Sensor.MODIS_TERRA;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();
        TcwvOceanLut oceanLut = TcwvIO.readOceanLookupTable(auxdataPath, Sensor.MODIS_TERRA);
        TcwvFunction tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        JacobiFunction jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);

        double sza = 50.97;
        double saa = 34.2362;
        double saaR = saa*MathUtils.DTOR;
        double vza = 6.7142;
        double vaa = 99.53059;
        double vaaR = vaa*MathUtils.DTOR;
        double relAzi = 180. - Math.acos(Math.cos(saaR)*Math.cos(vaaR) + Math.sin(saaR)*Math.sin(vaaR))*MathUtils.RTOD;
        double amf = 1. / Math.cos(sza * MathUtils.DTOR) + 1. / Math.cos(vza * MathUtils.DTOR);   // should be 2.610949

        // the rhoToa must be RefSB_* / Math.PI !!!
        double[] refSBWin = new double[]{0.00949};  // RefSB_2
        double[] refSBAbs = new double[]{0.00694, 0.0048, 0.00526};   //  RefSB_17, RefSB_18, RefSB_19
        double[] rhoToaWin = new double[1];
        double[] rhoToaAbs = new double[3];
        rhoToaWin[0] = refSBWin[0]/Math.PI;
        for (int i = 0; i < rhoToaAbs.length; i++) {
            rhoToaAbs[i] = refSBAbs[i]/Math.PI;
        }

        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 293.9698;

        double priorMslPress = 1013.25;  // not needed for water
        double priorWsp = 7.5;
        double priorTcwv = 18.0153;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    null, oceanLut,
                                                    null, tcwvFunctionOcean,
                                                    null, jacobiFunctionOcean,
                                                    input, false);

        System.out.println("MODIS TERRA OCEAN result.getTcwv() = " + result.getTcwv());
//        assertEquals(33.86, result.getTcwv(), 0.2);
//        assertEquals(22.4, result.getTcwv(), 0.2);  // MODIS update RP, 20190902
        // Python result: 22.227 (new test_cowa_ocean_4, OD, 20191128)
//        assertEquals(3.582, result.getTcwvUncertainty(), 0.001); // Python: 3.39
    }

}