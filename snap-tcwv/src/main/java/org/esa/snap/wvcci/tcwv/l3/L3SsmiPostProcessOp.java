package org.esa.snap.wvcci.tcwv.l3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.TcwvConstants;
import org.esa.snap.wvcci.tcwv.TcwvIO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

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

        validateSourceProduct(sourceProduct, TcwvConstants.TCWV_SSMI_MEAN_BAND_NAME);

        tcwvSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_SSMI_MEAN_BAND_NAME);
        tcwvUncertaintySourceBand = sourceProduct.getBand(TcwvConstants.TCWV_SSMI_UNCERTAINTY_MEAN_BAND_NAME);
        tcwvCountsSourceBand = sourceProduct.getBand(TcwvConstants.TCWV_SSMI_COUNTS_BAND_NAME);

        createTargetProduct(TcwvConstants.TCWV_SSMI_MEAN_BAND_NAME,
                            TcwvConstants.TCWV_SSMI_UNCERTAINTY_MEAN_BAND_NAME,
                            TcwvConstants.TCWV_SSMI_COUNTS_BAND_NAME);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(L3SsmiPostProcessOp.class);
        }
    }
}
