package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.oe.InversionMethod;
import org.esa.snap.wvcci.tcwv.oe.OEOutputMode;
import org.esa.snap.wvcci.tcwv.oe.OptimalEstimation;
import org.esa.snap.wvcci.tcwv.oe.OptimalEstimationResult;

/**
 * Implementation of TCWV algorithm follwing SE Python breadboard (CAWA heritage)
 *
 * @author olafd
 */
public class TcwvAlgorithm {

    /**
     * Provides computation of final TCWV from given input
     *
     * @param sensor - the sensor (MERIS, MODIS, or OLCI)
     * @param landLut - lookup table for land pixels for given sensor
     * @param oceanLut - lookup table for ocean pixels for given sensor
     * @param tcwvFunctionLand - TCWB function object for land pixels
     * @param tcwvFunctionOcean - TCWB function object for ocean pixels
     * @param jacobiFunctionLand - Jacobi function object for land pixels
     * @param jacobiFunctionOcean - Jacobi function object for ocean pixels
     * @param input - object with all required input variables
     * @param isLand - land/water flag
     *
     * @return {@link TcwvResult}: TCWV, and possibly additional information
     */
    public TcwvResult compute(Sensor sensor,
                              TcwvLandLut landLut, TcwvOceanLut oceanLut,
                              TcwvFunction tcwvFunctionLand, TcwvFunction tcwvFunctionOcean,
                              JacobiFunction jacobiFunctionLand, JacobiFunction jacobiFunctionOcean,
                              TcwvAlgorithmInput input, boolean isLand) {

        return isLand ? computeTcwvLand(sensor, input, landLut, tcwvFunctionLand, jacobiFunctionLand) :
                computeTcwvOcean(sensor, input, oceanLut, tcwvFunctionOcean, jacobiFunctionOcean);
    }

    private TcwvResult computeTcwvLand(Sensor sensor, TcwvAlgorithmInput input, TcwvLandLut landLut,
                                       TcwvFunction tcwvFunction, JacobiFunction jacobiFunction) {

        final double[] wvc = landLut.getWvc();
        final double[] al0 = landLut.getAl0();
        final double[] al1 = landLut.getAl1();
        final double[] a = {wvc[0], al0[0], al1[0]}; // constant for all retrievals!
        final double[] b = {wvc[wvc.length - 1], al0[al0.length - 1], al1[al1.length - 1]};

        // see cawa_tcwv_land.py --> _do_inversion:
        double[] mes = new double[input.getRhoToaWin().length + input.getRhoToaAbs().length];
        for (int i = 0; i < input.getRhoToaWin().length; i++) {
            mes[i] = input.getRhoToaWin()[i];
        }
        for (int i = 0; i < input.getRhoToaAbs().length; i++) {
            mes[input.getRhoToaWin().length + i] =
                    -1.0*Math.log(input.getRhoToaAbs()[i]/input.getRhoToaWin()[input.getRhoToaWin().length-1]) /
                            Math.sqrt(input.getAmf());
        }

        double[] par = new double[6];
        par[0] = input.getAot865();
        par[1] = input.getPriorMslPress();
        par[2] = input.getPriorT2m();
        par[3] = input.getRelAzi();
        par[4] = input.getVza();
        par[5] = input.getSza();

        final double[] xa = new double[3];
        xa[0] = Math.sqrt(input.getPriorTcwv());
        xa[1] = input.getPriorAl0();
        xa[2] = input.getPriorAl1();

        final double[][] se = sensor.getSe();
        final double[][] sa = TcwvConstants.SA_LAND;

        OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, a, se, sa, xa, OEOutputMode.BASIC, 3);
        final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
//        final double resultAot = result.getXn()[1];
//        final double resultWsp = result.getXn()[2];

        return new TcwvResult(resultTcwv);
    }

    private TcwvResult computeTcwvOcean(Sensor sensor, TcwvAlgorithmInput input, TcwvOceanLut oceanLut,
                                        TcwvFunction tcwvFunction, JacobiFunction jacobiFunction) {

        final double[] wvc = oceanLut.getWvc();
        final double[] aot = oceanLut.getAot();
        final double[] wsp = oceanLut.getWsp();
        final double[] a = {wvc[0], aot[0], wsp[0]}; // constant for all retrievals!
        final double[] b = {wvc[wvc.length - 1], aot[aot.length - 1], wsp[wsp.length - 1]};

        double[] mes = new double[input.getRhoToaWin().length + input.getRhoToaAbs().length];
        for (int i = 0; i < input.getRhoToaWin().length; i++) {
            mes[i] = input.getRhoToaWin()[i];
        }
        for (int i = 0; i < input.getRhoToaAbs().length; i++) {
//            self.mes[len(self.wb) + ich] = -np.log(
//                    data['rtoa'][ch] /
//                            data['rtoa'][self.wb[-1]]) / np.sqrt(data['amf'])
            mes[input.getRhoToaWin().length + i] =
                    -1.0*Math.log(input.getRhoToaAbs()[i]/input.getRhoToaWin()[input.getRhoToaWin().length-1]) /
                            Math.sqrt(input.getAmf());
        }

        double[] par = new double[3];
        par[0] = input.getRelAzi();
        par[1] = input.getVza();
        par[2] = input.getSza();

        final double[] xa = new double[3];
        xa[0] = Math.sqrt(input.getPriorTcwv());
        xa[1] = input.getPriorAot();
        xa[2] = input.getPriorWsp();

        final double[][] se = sensor.getSe();
        final double[][] sa = TcwvConstants.SA_OCEAN;

        OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, a, se, sa, xa, OEOutputMode.BASIC, 3);
        final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
//        final double resultAot = result.getXn()[1];
//        final double resultWsp = result.getXn()[2];

        return new TcwvResult(resultTcwv);
    }
}
