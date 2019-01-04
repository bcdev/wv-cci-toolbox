/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.wvcci.tcwv.dataio.mod35;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h4.H4Group;
import ncsa.hdf.object.h4.H4SDS;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Product reader responsible for reading MODIS MOD35 cloud mask HDF product.
 *
 * @author Olaf Danne
 */
public class Modis35ProductReader extends AbstractProductReader {

    private int productWidth = 1354;   // todo: check if these are constants for all MOD35 L2!
    private int productHeight = 2030;

    private int file_id;

    private HashMap<Band, Hdf4DatasetVar> datasetVars;


    Modis35ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object inputObject = getInput();
        File mod35File = Modis35ProductReaderPlugIn.getFileInput(inputObject);

        Product targetProduct = null;

        FileFormat h4FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
        if (h4FileFormat == null) {
            System.err.println("Cannot find HDF4 FileFormat.");
            return null;
        }

        FileFormat h4File = null;
        try {
            h4File = h4FileFormat.createInstance(mod35File.getAbsolutePath(), FileFormat.READ);
            file_id = h4File.open();

            targetProduct = createTargetProduct(mod35File, h4File.getRootNode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (h4File != null) {
                try {
                    h4File.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return targetProduct;
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer,
                                          ProgressMonitor pm) throws IOException {

        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        final Hdf4DatasetVar datasetVar = datasetVars.get(targetBand);
        synchronized (datasetVar) {
            if (datasetVar.getName().equals("/mod35/Data Fields/" + Mod35Constants.CLOUD_MASK_BAND_NAME) &&
                    targetBand.getName().equals(Mod35Constants.CLOUD_FLAG_BAND_NAME)) {
                ProductData tmpBuffer =
                        Mod35Utils.getDataBufferForH5Dread(datasetVar.getType(), targetWidth, targetHeight);
                Mod35Utils.readMod35Data(file_id,
                                         targetWidth, targetHeight,
                                         targetOffsetX, targetOffsetY,
                                         datasetVar.getName(),
                                         datasetVar.getType(),
                                         tmpBuffer);
                Mod35Flags.setSmFlagBuffer(targetBuffer, tmpBuffer);
            } else {
                Mod35Utils.readMod35Data(file_id,
                                         targetWidth, targetHeight,
                                         targetOffsetX, targetOffsetY,
                                         datasetVar.getName(),
                                         datasetVar.getType(),
                                         targetBuffer);
            }
        }
    }

//////////// private methods //////////////////

    private Product createTargetProduct(File inputFile, TreeNode inputFileRootNode) throws Exception {
        Product product = null;

        if (inputFileRootNode != null) {
            final TreeNode productTypeNode = inputFileRootNode.getChildAt(0);        // 'mod35'

            // get dimensions either from GEOMETRY/SAA or for NDVI products from NDVI/NDVI
            final int productTypeNodeStartIndex = 0;
            final int rasterNodeStartIndex = 0;

//            productWidth = (int) Mod35Utils.getH4ScalarDS(productTypeNode.getChildAt(productTypeNodeStartIndex).
//                    getChildAt(rasterNodeStartIndex)).getDims()[1];   // take from SAA
//            productHeight = (int) Mod35Utils.getH4ScalarDS(productTypeNode.getChildAt(productTypeNodeStartIndex).
//                    getChildAt(rasterNodeStartIndex)).getDims()[0];
            product = new Product(inputFile.getName(), Mod35Constants.MOD35_l2_PRODUCT_TYPE, productWidth, productHeight);
//            product.setPreferredTileSize(productWidth, 16);

            final H4Group rootGroup = (H4Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final List rootMetadata = rootGroup.getMetadata();
            Mod35Utils.addMetadataElementWithAttributes(rootMetadata, product.getMetadataRoot(), Mod35Constants.MPH_NAME);
            product.setDescription(Mod35Constants.MOD35_l2_PRODUCT_DESCR);
            Mod35Utils.addStartStopTimes(product, (DefaultMutableTreeNode) inputFileRootNode);
            product.setFileLocation(inputFile);

            for (int i = 0; i < productTypeNode.getChildCount(); i++) {
                // we have: 'Geolocation Fields', 'Data Fields'
                final TreeNode productTypeChildNode = productTypeNode.getChildAt(i);
                final String productTypeChildNodeName = productTypeChildNode.toString();


                switch (productTypeChildNodeName) {
                    case Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME:
                        createGeolocationBand(inputFileRootNode, product, productTypeChildNode);
                        break;

                    case Mod35Constants.DATA_FIELDS_GROUP_NAME:
                        createCloudMaskBand(product, productTypeChildNode);
                        break;

                    default:
                        break;
                }
            }
        }

        return product;
    }

    private void createCloudMaskBand(Product product, TreeNode productTypeChildNode) {

    }

    private void createGeolocationBand(TreeNode inputFileRootNode, Product product, TreeNode productTypeChildNode) throws Exception {

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) productTypeChildNode;
        Mod35Utils.addRootMetadataElement(product, parentNode, Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);


        float[] latBounds = null;
        float[] lonBounds = null;
        float[] lats = null;
        float[] lons = null;
        for (int j = 0; j < productTypeChildNode.getChildCount(); j++) {
            final TreeNode geolocationChildNode = productTypeChildNode.getChildAt(j);
            final String geolocationChildNodeName = geolocationChildNode.toString();

            final H4SDS geolocationDS = Mod35Utils.getH4ScalarDS(geolocationChildNode);
            final Band geolocationBand = Mod35Utils.createTargetBand(product,
                                                             geolocationDS.getMetadata(),
                                                                     geolocationChildNodeName,
                                                             ProductData.TYPE_FLOAT32);
            Mod35Utils.setBandUnitAndDescription(geolocationDS.getMetadata(), geolocationBand);
            geolocationBand.setNoDataValue(Mod35Constants.GEOMETRY_NO_DATA_VALUE);
            geolocationBand.setNoDataValueUsed(true);

            final String geolocationDatasetName = "/mod35/Geolocation Fields/" + geolocationChildNodeName;
            final int geolocationDatatypeClass = geolocationDS.getDatatype().getDatatypeClass();
            datasetVars.put(geolocationBand, new Hdf4DatasetVar(geolocationDatasetName, geolocationDatatypeClass));
            final List geolocationMetadata = geolocationDS.getMetadata();
            if (geolocationMetadata != null) {
                Mod35Utils.addMetadataElementWithAttributes(geolocationMetadata,
                                                            rootMetadataElement,
                                                            geolocationChildNodeName);
            }
            if (geolocationChildNodeName.equals("latitude")) {
                lats = (float[]) geolocationDS.getData();
                latBounds = getProductBounds(geolocationDS);
            } else if (geolocationChildNodeName.equals("longitude")) {
                lons = (float[]) geolocationDS.getData();
                lonBounds = getProductBounds(geolocationDS);
            }
        }

        // add geocoding:
        Mod35Utils.setMod35GeoCoding(product, inputFileRootNode, productTypeChildNode,
                                     productWidth, productHeight, lats, lons, latBounds, lonBounds);

    }

    private float[] getProductBounds(H4SDS ds) throws Exception {
        final long[] dsDims = ds.getDims();
        final int dsWidth = (int) dsDims[0];
        final int dsHeight = (int) dsDims[1];
        final float[] dsData = (float[]) ds.getData();
        final float ul = dsData[0];
        final float ll = dsData[dsHeight - 1];
        final float ur = dsData[(dsWidth - 1) * dsHeight];
        final float lr = dsData[dsWidth * dsHeight - 1];

        return new float[]{ul, ll, ur, lr};
    }

    private static class Hdf4DatasetVar {

        final String name;
        final int type;

        public Hdf4DatasetVar(String name, int type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }
    }
}

