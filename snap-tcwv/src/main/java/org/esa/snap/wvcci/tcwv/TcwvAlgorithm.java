package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.interpolation.JacobiFunction;
import org.esa.snap.wvcci.tcwv.oe.*;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

/**
 * Implementation of TCWV algorithm follwing SE Python breadboard (CAWA heritage)
 *
 * @author olafd
 */
public class TcwvAlgorithm {

    /**
     * Provides computation of final TCWV from given input
     *
     * @param sensor              - the sensor (MERIS, MODIS, or OLCI)
     * @param landLut             - lookup table for land pixels for given sensor
     * @param oceanLut            - lookup table for ocean pixels for given sensor
     * @param tcwvFunctionLand    - TCWB function object for land pixels
     * @param tcwvFunctionOcean   - TCWB function object for ocean pixels
     * @param jacobiFunctionLand  - Jacobi function object for land pixels
     * @param jacobiFunctionOcean - Jacobi function object for ocean pixels
     * @param input               - object with all required input variables
     * @param isLand              - land/water flag
     * @return {@link TcwvResult}: TCWV, and possibly additional information
     */
    public TcwvResult compute(Sensor sensor,
                              TcwvLandLut landLut, TcwvOceanLut oceanLut,
                              TcwvFunction tcwvFunctionLand, TcwvFunction tcwvFunctionOcean,
                              JacobiFunction jacobiFunctionLand, JacobiFunction jacobiFunctionOcean,
                              TcwvAlgorithmInput input, boolean isLand) {

        return compute(sensor, landLut, oceanLut, tcwvFunctionLand, tcwvFunctionOcean,
                jacobiFunctionLand, jacobiFunctionOcean, input, isLand, false);
    }

    /**
     * Provides computation of final TCWV from given input
     *
     * @param sensor              - the sensor (MERIS, MODIS, or OLCI)
     * @param landLut             - lookup table for land pixels for given sensor
     * @param oceanLut            - lookup table for ocean pixels for given sensor
     * @param tcwvFunctionLand    - TCWB function object for land pixels
     * @param tcwvFunctionOcean   - TCWB function object for ocean pixels
     * @param jacobiFunctionLand  - Jacobi function object for land pixels
     * @param jacobiFunctionOcean - Jacobi function object for ocean pixels
     * @param input               - object with all required input variables
     * @param isLand              - land/water flag
     * @param isCoastline         - coastline flag
     * @return {@link TcwvResult}: TCWV, and possibly additional information
     */
    public TcwvResult compute(Sensor sensor,
                              TcwvLandLut landLut, TcwvOceanLut oceanLut,
                              TcwvFunction tcwvFunctionLand, TcwvFunction tcwvFunctionOcean,
                              JacobiFunction jacobiFunctionLand, JacobiFunction jacobiFunctionOcean,
                              TcwvAlgorithmInput input, boolean isLand, boolean isCoastline) {

        return isLand ? computeTcwvLand(sensor, input, landLut, tcwvFunctionLand, jacobiFunctionLand, isCoastline) :
                computeTcwvOcean(sensor, input, oceanLut, tcwvFunctionOcean, jacobiFunctionOcean);
    }

    private TcwvResult computeTcwvLand(Sensor sensor, TcwvAlgorithmInput input, TcwvLandLut landLut,
                                       TcwvFunction tcwvFunction, JacobiFunction jacobiFunction, boolean isCoastline) {

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
            if (sensor == Sensor.MERIS || sensor == Sensor.OLCI ||
                    sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
                // run this also for MODIS land !! (RP 20190410)
                // for ocean it makes no difference as a,b are always 0,1
                mes[input.getRhoToaWin().length + i] = rectifyAndO2Correct(sensor, input.getRhoToaWin(),
                        input.getRhoToaAbs(), i,
                        Math.sqrt(input.getAmf()), true);
            } else {
                // this is equal to the output of rectifyAndO2Correct in case of a,b = 0,1
                mes[input.getRhoToaWin().length + i] =
                        -1.0 * Math.log(input.getRhoToaAbs()[i] / input.getRhoToaWin()[input.getRhoToaWin().length - 1]) /
                                Math.sqrt(input.getAmf());
            }

        }

        double[] par = new double[6];
        par[0] = input.getPriorAot();
        par[1] = -Math.log(input.getPriorMslPress());
        par[2] = input.getPriorT2m();
        par[3] = input.getRelAzi();
        par[4] = input.getVza();
        par[5] = input.getSza();

        double[] xa = new double[3];
        xa[0] = Math.sqrt(input.getPriorTcwv());
        xa[1] = input.getPriorAl0();
        xa[2] = input.getPriorAl1();
        // finally clip (see cowa_core.py, prepare_data):
        xa = OptimalEstimationUtils.clip1D(a, b, xa);

        double[][] se = sensor.getLandSe();
        for (int i = 0; i < input.getRhoToaWin().length; i++) {
            se[i][i] = 1.0 / (sensor.getLandSnr() * sensor.getLandSnr());
        }
        // introduce per-pixel uncertainty for abs bands as provided by RP Jan 2020:
        for (int i = 0; i < input.getRhoToaAbs().length; i++) {
            int j = input.getRhoToaWin().length + i;
            se[j][j] = TcwvUtils.computePseudoAbsorptionMeasurementVariance(sensor.getLandSnr(),
                    sensor.getLandInterpolError()[i],
                    input.getAmf());
        }

        double[][] sa = TcwvConstants.SA_LAND;
        if (sensor == Sensor.MERIS) {
            sa = TcwvConstants.MERIS_SA_LAND;
        }
        // RP March 2020:
        if (isCoastline) {
            sa[0][0] = TcwvConstants.SA_OCEAN[0][0];
        }

//        OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);
        OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, null);  // test!!!

        return getTcwvResult(a, xa, se, sa, oe, sensor);
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
            if (sensor == Sensor.MERIS || sensor == Sensor.OLCI ||
                    sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
//                if (sensor == Sensor.MERIS || sensor == Sensor.OLCI) {
                mes[input.getRhoToaWin().length + i] = rectifyAndO2Correct(sensor, input.getRhoToaWin(),
                        input.getRhoToaAbs(), i,
                        Math.sqrt(input.getAmf()), false);
            } else {
                // this is equal to the output of rectifyAndO2Correct in case of a,b = 0,1. Therefore ok for MODIS.
                // Python:
                //            self.mes[len(self.wb) + ich] = -np.log(
                //                    data['rtoa'][ch] /
                //                            data['rtoa'][self.wb[-1]]) / np.sqrt(data['amf'])
                mes[input.getRhoToaWin().length + i] =
                        -1.0 * Math.log(input.getRhoToaAbs()[i] / input.getRhoToaWin()[input.getRhoToaWin().length - 1]) /
                                Math.sqrt(input.getAmf());
            }
        }

        double[] par = new double[3];
        par[0] = input.getRelAzi();
        par[1] = input.getVza();
        par[2] = input.getSza();

        double[] xa = new double[3];
        xa[0] = Math.sqrt(input.getPriorTcwv());
        xa[1] = input.getPriorAot();
        xa[2] = input.getPriorWsp();
        // finally clip (see cowa_core.py, prepare_data):
        xa = OptimalEstimationUtils.clip1D(a, b, xa);

        final double[][] se = sensor.getOceanSe();
        for (int i = 0; i < input.getRhoToaWin().length; i++) {
            se[i][i] = 1.0 / (sensor.getOceanSnr() * sensor.getOceanSnr());
        }
        // introduce per-pixel uncertainty for abs bands as provided by RP Jan 2020:
        for (int i = 0; i < input.getRhoToaAbs().length; i++) {
            int j = input.getRhoToaWin().length + i;
            se[j][j] = TcwvUtils.computePseudoAbsorptionMeasurementVariance(sensor.getOceanSnr(),
                    sensor.getOceanInterpolError()[i],
                    input.getAmf());
        }

        final double[][] sa = TcwvConstants.SA_OCEAN;
        if (sensor == Sensor.MODIS_TERRA || sensor == Sensor.MODIS_AQUA) {
            sa[2][2] = TcwvConstants.SA_OCEAN_2_2_MODIS;   // differs from default
            se[1][1] = 100.0; // switching off 17 over ocean (too much noise), see demo_modis_processor.py l.312
        }

        OptimalEstimation oe = new OptimalEstimation(tcwvFunction, a, b, mes, par, jacobiFunction);

        return getTcwvResult(a, xa, se, sa, oe, sensor);
    }

    double rectifyAndO2Correct(Sensor sensor, double[] rhoWb, double[] rhoAb, int absBandIndex,
                               double samf, boolean isLand) {

        double[][] rectCorr = isLand ? sensor.getLandRectCorr() : sensor.getOceanRectCorr();    // a, b

        final double a = rectCorr[absBandIndex][0];
        final double b = rectCorr[absBandIndex][1];
        double[] cwvl = sensor.getCwvlRectCorr();       // first win bands, then abs bands

        double ref;
        if (rhoWb.length == 1) {
            ref = rhoWb[0];
        } else {
            final double dwvl = cwvl[1] - cwvl[0];
            final double drho = rhoWb[1] - rhoWb[0];
            if (Math.abs(dwvl) > 1.E-5) {
                ref = rhoWb[0] + drho * (cwvl[rhoWb.length + absBandIndex] - cwvl[0]) / dwvl;
            } else {
                ref = rhoWb[0];
            }
        }
        return -(a + b * Math.log(rhoAb[absBandIndex] / ref) / samf);
    }

    private TcwvResult getTcwvResult(double[] a, double[] xa, double[][] se, double[][] sa,
                                     OptimalEstimation oe, Sensor sensor) {
        // now includes uncertainty
        OptimalEstimationResult result = oe.invert(InversionMethod.OE, a, se, sa, xa, OEOutputMode.FULL);
        final double resultTcwv = Math.pow(result.getXn()[0], 2.0);
        double resultTcwvUncertainty = 0.0;
        if (result.getSr() != null) {
            resultTcwvUncertainty = result.getSr()[0][0];
        }
        final double resultAot1 = result.getXn()[1];
        final double resultAot2 = result.getXn()[2];

        final double cost = result.getDiagnoseResult().getCost();
        return new TcwvResult(resultTcwv, resultTcwvUncertainty, cost, resultAot1, resultAot2);
    }
}
