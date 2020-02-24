package org.esa.snap.wvcci.tcwv;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.MergeOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.util.TcwvUtils;

/**
 * Merges MODIS L1b (5 bands of MOD021KM) and EraInterim intermediate products in Water_Vapour_cci TCWV chain.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.MergeModisL1bEraInterim", version = "0.8",
        authors = "O.Danne",
        internal = true,
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2020 by Brockmann Consult",
        description = "Merges MODIS L1b (the 5 required bands of MOD021KM) and EraInterim intermediate products in " +
                "Water_Vapour_cci TCWV chain.")
public class MergeModisL1bEraInterimOp extends Operator {

    @Parameter(description = "The list of EraInterim bands to copy to target product.",
            defaultValue = "t2m,msl,tcwv")
    private String[] eraInterimBandsToCopy = {"t2m","msl","tcwv"};

    @Parameter(defaultValue = "true",
            label = " Process only products with DayNightFlag = 'Day'")
    private boolean processDayProductsOnly;

    
    @SourceProduct(description = "IdePix product")
    private Product l1bProduct;

    @SourceProduct(description = "EraInterim product")
    private Product eraInterimProduct;


    @Override
    public void initialize() throws OperatorException {

        if (processDayProductsOnly) {
            TcwvUtils.checkIfMod021KMDayProduct(l1bProduct);
        }

        validateL1bProduct();
        ProductUtils.copyGeoCoding(l1bProduct, eraInterimProduct);
        ProductUtils.copyGeoCoding(l1bProduct, eraInterimProduct);

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProducts(eraInterimProduct);
        mergeOp.setSourceProduct("masterProduct", l1bProduct);

        final MergeOp.NodeDescriptor inclModL1bDescriptor = new MergeOp.NodeDescriptor();
        inclModL1bDescriptor.setNamePattern(".*");
        inclModL1bDescriptor.setProductId("masterProduct");

        final MergeOp.NodeDescriptor[] includeEraInterimDescriptor =
                new MergeOp.NodeDescriptor[eraInterimBandsToCopy.length+1];
        includeEraInterimDescriptor[0] = new MergeOp.NodeDescriptor();
        includeEraInterimDescriptor[0].setProductId("masterProduct");
        includeEraInterimDescriptor[0].setNamePattern(".*");
        for (int i = 1; i < includeEraInterimDescriptor.length; i++) {
            includeEraInterimDescriptor[i] = new MergeOp.NodeDescriptor();
            includeEraInterimDescriptor[i].setProductId("sourceProduct.1");
            includeEraInterimDescriptor[i].setNamePattern(eraInterimBandsToCopy[i-1]);
        }
        mergeOp.setParameterDefaultValues();
        mergeOp.setParameter("includes", includeEraInterimDescriptor);

        setTargetProduct(mergeOp.getTargetProduct());
    }

    private void validateL1bProduct() {
        for (String bandName : Sensor.MODIS_TERRA.getReflBandNames()) {
            if (!l1bProduct.containsBand(bandName)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                                                    "mandatory band '" + bandName + "'.");
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeModisL1bEraInterimOp.class);
        }
    }
}
