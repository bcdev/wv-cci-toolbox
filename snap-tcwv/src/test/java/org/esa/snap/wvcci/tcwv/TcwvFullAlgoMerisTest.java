package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolation;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;


public class TcwvFullAlgoMerisTest {

    private TcwvLandLut landLut;
    private TcwvOceanLut oceanLut;
    private TcwvFunction tcwvFunctionLand;
    private JacobiFunction jacobiFunctionland;
    private TcwvFunction tcwvFunctionOcean;
    private JacobiFunction jacobiFunctionOcean;

    @Before
    public void setUp() throws Exception {
        final Path auxdataPath = TcwvIO.installAuxdata();

        landLut = TcwvIO.readLandLookupTable(auxdataPath.toString(), Sensor.MERIS);
        oceanLut = TcwvIO.readOceanLookupTable(auxdataPath.toString(), Sensor.MERIS);
        tcwvFunctionLand = TcwvInterpolation.getForwardFunctionLand(landLut);
        jacobiFunctionland = TcwvInterpolation.getJForwardFunctionLand(landLut);
        tcwvFunctionOcean = TcwvInterpolation.getForwardFunctionOcean(oceanLut);
        jacobiFunctionOcean = TcwvInterpolation.getJForwardFunctionOcean(oceanLut);
    }

    // Java version of tests from Python breadboard 'test_cawa.py', which in return is a standalone version
    // of GPF operator code 'cawa_tcwv_meris_op.py' for a single test pixel,
    // using corresponding LUTs from CAWA.
    // Towards a Water_Vapour_cci Java operator, we have to test:
    //     - MERIS ocean
    //     - MODIS land
    //

    @Test
    public void testComputeTcwv_meris_land() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        double[] rhoToaWin = new double[]{0.192909659591, 0.191403549212};
        double[] rhoToaAbs = new double[]{0.15064498747946906};
        double sza = 52.9114494;
        double vza = 27.0720062;
        double relAzi = 44.8835754;
        double amf = 1./Math.cos(sza* MathUtils.DTOR) + 1./Math.cos(vza* MathUtils.DTOR);
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = 303.0;
        double priorMslPress = -1013.25/100.0;  // todo: to be fixed by RP
        double priorWsp = Double.NaN;     // not needed for land
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    tcwvFunctionLand, tcwvFunctionOcean,
                                                    jacobiFunctionland, jacobiFunctionOcean,
                                                    input, true);
        
        assertEquals(7.16992, result.getTcwv(), 1.E-6);
    }

    @Test
    public void testComputeTcwv_meris_ocean() {
        final Sensor sensor = Sensor.MERIS;
        TcwvAlgorithm algorithm = new TcwvAlgorithm();

        double[] rhoToaWin = new double[]{0.190342278759, 0.189699328631};
        double[] rhoToaAbs = new double[]{0.129829271947};
        double sza = 61.435791;
        double vza = 28.435095;
        double relAzi = 135.61277770996094;
        double amf = 3.22861762089;
        double aot865 = 0.1;
        double priorAot = 0.15;
        double priorAl0 = 0.13;
        double priorAl1 = 0.13;
        double priorT2m = Double.NaN;            // not needed for ocean
        double priorMslPress = Double.NaN;       // not needed for ocean
        double priorWsp = 7.5;
        double priorTcwv = 30.0;
        TcwvAlgorithmInput input = new TcwvAlgorithmInput(rhoToaWin, rhoToaAbs, sza, vza, relAzi, amf, aot865,
                                                          priorAot, priorAl0, priorAl1, priorT2m, priorMslPress,
                                                          priorWsp, priorTcwv);
        final TcwvResult result = algorithm.compute(sensor,
                                                    landLut, oceanLut,
                                                    tcwvFunctionLand, tcwvFunctionOcean,
                                                    jacobiFunctionland, jacobiFunctionOcean,
                                                    input, false);

        assertEquals(28.007107, result.getTcwv(), 1.E-6);
    }

}