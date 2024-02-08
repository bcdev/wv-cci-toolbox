package org.esa.snap.wvcci.tcwv.l3;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

/**
 * Operator for post-processing of HOAPS SSMI TCWV L3 daily products.
 *
 * Current actions are:
 * - rename wvpa --> tcwv
 * - remove satm flag band --> todo: clarify
 * - rename stdv --> tcwv_uncertainty
 * - rename numo --> tcwv_counts
 * - filter with L3 LC land/water mask
 *
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.Tcwv.L3.SSMIPostProcessing", version = "0.8",
        authors = "O.Danne",
        internal = true,
        description = "Operator for post-processing of HOAPS SSMI TCWV L3 daily products.")
public class L3SsmiPostProcessOp extends L3PostProcessOp {

    @Override
    public void initialize() throws OperatorException {
        super.initialize();

        validateSourceProduct(sourceProduct, TcwvConstants.TCWV_HOAPS_BAND_NAME);

        tcwvSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_HOAPS_BAND_NAME);
        tcwvUncertaintySourceBand = sourceProduct.getBand(TcwvConstants.TCWV_SIGMA_HOAPS_BAND_NAME);
        tcwvCountsSourceBand = sourceProduct.getBand(TcwvConstants.NUM_OBS_HOAPS_BAND_NAME);

        createTargetProduct(TcwvConstants.TCWV_HOAPS_BAND_NAME,
                            TcwvConstants.TCWV_SIGMA_HOAPS_BAND_NAME,
                            TcwvConstants.NUM_OBS_HOAPS_BAND_NAME);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3SsmiPostProcessOp.class);
        }
    }
}
