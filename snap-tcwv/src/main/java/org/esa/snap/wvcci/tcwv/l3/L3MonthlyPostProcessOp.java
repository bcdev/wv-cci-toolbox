package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

/**
 * Operator for post-processing of MERIS/MODIS/OLCI TCWV L3 monthly products.
 * Currently no differences to daily post-processing.
 *
 * Current actions are:
 * - rename tcwv_mean --> tcwv
 * - remove tcwv_sigma
 * - rename tcwv_uncertainty_mean --> tcwv_uncertainty
 * - remove tcwv_uncertainty_sigma
 * - filter with L3 LC land/water mask
 *
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.MonthlyPostProcessing", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for post-processing of TCWV L3 daily products.")
public class L3MonthlyPostProcessOp extends L3PostProcessOp {

    @Override
    public void initialize() throws OperatorException {
        super.initialize();

        validateSourceProduct(sourceProduct, TcwvConstants.TCWV_MEAN_BAND_NAME);

        tcwvSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_MEAN_BAND_NAME);
        tcwvUncertaintySourceBand = sourceProduct.getBand(TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME);
        tcwvCountsSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_COUNTS_TARGET_BAND_NAME);

        createTargetProduct(TcwvConstants.TCWV_MEAN_BAND_NAME,
                            TcwvConstants.TCWV_UNCERTAINTY_L3_BAND_NAME,
                            TcwvConstants.TCWV_COUNTS_TARGET_BAND_NAME);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3MonthlyPostProcessOp.class);
        }
    }
}
