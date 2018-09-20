package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.MergeOp;
import org.esa.snap.wvcci.tcwv.Sensor;
import org.esa.snap.wvcci.tcwv.TcwvConstants;

import java.io.File;
import java.io.IOException;

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

    @Parameter(defaultValue = ".seq", valueSet = {".seq", ".nc", ".dim"},
            description = "Idepix file extension.")
    private String idepixFileExt;

    @Parameter(description = "EraInterim product parent directory.")
    private String eraInterimProductParentDir;

    @Parameter(description = "The list of EraInterim bands to copy to target product.",
            defaultValue = "t2m,msl,tcwv")
    private String[] eraInterimBandsToCopy = {"t2m","msl","tcwv"};

    
    @SourceProduct(description = "IdePix product")
    private Product idepixProduct;

    @SourceProduct(description = "EraInterim product", optional = true)
    private Product eraInterimProduct;


    @Override
    public void initialize() throws OperatorException {

        validateSourceProduct(idepixProduct);

        if (eraInterimProduct == null) {
            // in this case we expect the Calvalus default file/folder structure:
            // ../idepix/../L2_of_MER_RR__1PRACR20081023_085102_000026342073_00136_34759_0000.seq
            // ../erainterim/MER_RR__1PRACR20081023_085102_000026342073_00136_34759_0000_era-interim.nc
            final int idepixNameStartIndex = 6; // after 'L2_of_'
            final String eraInterimProductPath = eraInterimProductParentDir + File.separator +
                    idepixProduct.getName().substring(idepixNameStartIndex) + "_era-interim.nc";
            try {
                eraInterimProduct = ProductIO.readProduct(new File(eraInterimProductPath));
            } catch (IOException e) {
                e.printStackTrace();
                throw new OperatorException("Cannot read EraInterim product '" + eraInterimProduct.getName() +
                                                    "' - exiting.");
            }
        }

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

    private void validateSourceProduct(Product sourceProduct) {
        if (!sourceProduct.containsBand(TcwvConstants.IDEPIX_CLASSIF_BAND_NAME)) {
            throw new OperatorException("Source product is not valid, as it does not contain " +
                                                "pixel classification flag band '" +
                                                TcwvConstants.IDEPIX_CLASSIF_BAND_NAME + "'.");
        }

        for (String bandName : sensor.getReflBandNames()) {
            if (!sourceProduct.containsBand(bandName)) {
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
