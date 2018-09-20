package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.MergeOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.wvcci.tcwv.Sensor;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

/**
 * TCWV main operator for Water_Vapour_cci.
 * Authors: R.Preusker (Python breadboard), O.Danne (Java conversion), 2018
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ESACCI.MergeIdepixEraInterim", version = "0.8",
        authors = "O.Danne",
        internal = true,
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Brockmann Consult",
        description = "Merges Idepix and EraInterim intermediate products in Water_Vapour_cci TCWV chain.")
public class MergeIdepixEraInterimOp extends Operator {

    @Parameter(valueSet = {"MERIS", "MODIS_AQUA", "MODIS_TERRA", "OLCI"},
            description = "The sensor (MERIS, MODIS or OLCI).")
    private Sensor sensor;

    @Parameter(description = "The list of EraInterim bands to copy to target product.",
            defaultValue = "t2m,msl,tcwv")
    private String[] eraInterimBandsToCopy = {"t2m","msl","tcwv"};

    
    @SourceProduct(description = "IdePix product")
    private Product idepixProduct;

    @SourceProduct(description = "EraInterim product")
    private Product eraInterimProduct;


    @Override
    public void initialize() throws OperatorException {

        validateIdepixProduct();
        ProductUtils.copyGeoCoding(idepixProduct, eraInterimProduct);
        eraInterimProduct.setSceneGeoCoding(idepixProduct.getSceneGeoCoding());

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProducts(eraInterimProduct);
        mergeOp.setSourceProduct("masterProduct", idepixProduct);

        final MergeOp.NodeDescriptor inclIdepixDescriptor = new MergeOp.NodeDescriptor();
        inclIdepixDescriptor.setNamePattern(".*");
        inclIdepixDescriptor.setProductId("masterProduct");

        final MergeOp.NodeDescriptor[] includeDescriptor =
                new MergeOp.NodeDescriptor[eraInterimBandsToCopy.length+1];
        includeDescriptor[0] = new MergeOp.NodeDescriptor();
        includeDescriptor[0].setProductId("masterProduct");
        includeDescriptor[0].setNamePattern(".*");
        for (int i = 1; i < includeDescriptor.length; i++) {
            includeDescriptor[i] = new MergeOp.NodeDescriptor();
            includeDescriptor[i].setProductId("sourceProduct.1");
            includeDescriptor[i].setNamePattern(eraInterimBandsToCopy[i-1]);
        }
        mergeOp.setParameterDefaultValues();
        mergeOp.setParameter("includes", includeDescriptor);

        setTargetProduct(mergeOp.getTargetProduct());
    }

    private void validateIdepixProduct() {
        if (!idepixProduct.containsBand(TcwvConstants.IDEPIX_CLASSIF_BAND_NAME)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                                                "pixel classification flag band '" +
                                                TcwvConstants.IDEPIX_CLASSIF_BAND_NAME + "'.");
        }

        for (String bandName : sensor.getReflBandNames()) {
            if (!idepixProduct.containsBand(bandName)) {
                throw new OperatorException("Source product is not valid, as it does not contain " +
                                                    "mandatory band '" + bandName + "'.");
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeIdepixEraInterimOp.class);
        }
    }
}
