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
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Product reader responsible for reading MODIS MOD35 cloud mask HDF product.
 *
 * @author Olaf Danne
 */
public class Modis35ProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private int tpWidth;
    private int tpHeight;

    private FileFormat h4File;

    private HashMap<Band, Hdf4DatasetVar> datasetVars;
    private TreeNode h4RootNode;
    private TreeNode mod35Node;
    private H4SDS cloudMaskDS;


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

        try {
            h4File = h4FileFormat.open(mod35File.getAbsolutePath(), FileFormat.READ);
            h4File.open();
            h4RootNode = h4File.getRootNode();         // 'MOD35_L2...'
            mod35Node = h4RootNode.getChildAt(0);      // 'mod35'
            setProductHeight5km(mod35Node.getChildAt(0));
            setProductHeight1km(mod35Node.getChildAt(1));
            targetProduct = createTargetProduct(mod35File);
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
                                          ProgressMonitor pm) {

        Assert.state(sourceOffsetX == targetOffsetX, "sourceOffsetX != targetOffsetX");
        Assert.state(sourceOffsetY == targetOffsetY, "sourceOffsetY != targetOffsetY");
        Assert.state(sourceStepX == 1, "sourceStepX != 1");
        Assert.state(sourceStepY == 1, "sourceStepY != 1");
        Assert.state(sourceWidth == targetWidth, "sourceWidth != targetWidth");
        Assert.state(sourceHeight == targetHeight, "sourceHeight != targetHeight");

        final Hdf4DatasetVar datasetVar = datasetVars.get(targetBand);
        synchronized (datasetVar) {
            if (!datasetVar.getName().equals("Latitude") || datasetVar.getName().equals("Longitude")) {
                final TreeNode geolocationFieldsNode = mod35Node.getChildAt(0);
                Mod35Utils.readMod35Data(geolocationFieldsNode,
                                         targetWidth, targetHeight,
                                         targetOffsetX, targetOffsetY,
                                         datasetVar.getName(),
                                         datasetVar.getType(),
                                         datasetVar.getDataset(),
                                         targetBuffer);
            } else {
                // todo
//                final TreeNode dataFieldsNode = mod35Node.getChildAt(1);
//                ProductData tmpBuffer =
//                        Mod35Utils.getDataBufferForH5Dread(datasetVar.getType(), targetWidth, targetHeight);
//                Mod35Utils.readMod35Data(dataFieldsNode,
//                                         targetWidth, targetHeight,
//                                         targetOffsetX, targetOffsetY,
//                                         datasetVar.getName(),
//                                         datasetVar.getType(),
//                                         tmpBuffer);
//                Mod35Flags.setSmFlagBuffer(targetBuffer, tmpBuffer);
            }
        }
    }

//////////// private methods //////////////////

    private Product createTargetProduct(File inputFile) throws Exception {
        Product product = null;

        mod35Node = h4RootNode.getChildAt(0);
        datasetVars = new HashMap<>(32);

        product = new Product(inputFile.getName(), Mod35Constants.MOD35_l2_PRODUCT_TYPE, productWidth, productHeight);

        final H4Group rootGroup = (H4Group) ((DefaultMutableTreeNode) h4RootNode).getUserObject();
        final List rootMetadata = rootGroup.getMetadata();
        Mod35Utils.addMetadataElementWithAttributes(rootMetadata, product.getMetadataRoot(), Mod35Constants.MPH_NAME);
        product.setDescription(Mod35Constants.MOD35_l2_PRODUCT_DESCR);
        Mod35Utils.addStartStopTimes(product, (DefaultMutableTreeNode) h4RootNode);
        product.setFileLocation(inputFile);

        for (int i = 0; i < mod35Node.getChildCount(); i++) {
            // we have: 'Geolocation Fields', 'Data Fields'
            final TreeNode fieldsNode = mod35Node.getChildAt(i);
            final String fieldsNodeName = fieldsNode.toString();

            switch (fieldsNodeName) {
                case Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME:
                    createGeolocationTpgs(product, fieldsNode);
                    break;

                case Mod35Constants.DATA_FIELDS_GROUP_NAME:
                    createCloudMaskBand(product, fieldsNode);
                    createGeometryBands(product, fieldsNode);
                    break;

                default:
                    break;
            }
        }

        return product;
    }

    private void setProductHeight1km(TreeNode fieldsNode) {
        // get it from 'Cloud_Mask' dataset (should be 1354x2030)
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode dataChildNode = fieldsNode.getChildAt(j);
            final String dataChildNodeName = dataChildNode.toString();
            if (dataChildNodeName.equals(Mod35Constants.CLOUD_MASK_BAND_NAME)) {
                cloudMaskDS = Mod35Utils.getH4ScalarDS(dataChildNode);
                final long[] dsDims = cloudMaskDS.getDims();
                productHeight = (int) dsDims[1];
                productWidth = (int) dsDims[2];
            }
        }
    }

    private void setProductHeight5km(TreeNode fieldsNode) {
        // get it from 'Latitude' dataset (should be 270x406)
        final TreeNode geolocationChildNode = fieldsNode.getChildAt(0);
        final H4SDS geolocationDS = Mod35Utils.getH4ScalarDS(geolocationChildNode);
        final long[] dsDims = geolocationDS.getDims();
        tpHeight = (int) dsDims[0];
        tpWidth = (int) dsDims[1];
    }

    private void createGeolocationTpgs(Product product, TreeNode fieldsNode) throws Exception {
        // 'Latitude', 'Longitude' (both float32)
        Mod35Utils.addRootMetadataElement(product, (DefaultMutableTreeNode) fieldsNode,
                                          Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);

        float[] lats = null;
        float[] lons = null;
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode geolocationChildNode = fieldsNode.getChildAt(j);
            final String geolocationChildNodeName = geolocationChildNode.toString();

            final H4SDS geolocationDS = Mod35Utils.getH4ScalarDS(geolocationChildNode);
            final List geolocationMetadata = geolocationDS.getMetadata();
            if (geolocationMetadata != null) {
                Mod35Utils.addMetadataElementWithAttributes(geolocationMetadata,
                                                            rootMetadataElement,
                                                            geolocationChildNodeName);
            }

            if (geolocationChildNodeName.equals("Latitude")) {
                lats = (float[]) geolocationDS.getData();
            } else if (geolocationChildNodeName.equals("Longitude")) {
                lons = (float[]) geolocationDS.getData();
            }
        }

        // add TPGs and geocoding:
        try {
            TiePointGrid latGrid;
            if (lats != null && lons != null) {
                latGrid = new TiePointGrid("lat", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lats);
                product.addTiePointGrid(latGrid);
                TiePointGrid lonGrid = new TiePointGrid("lon", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lons);
                product.addTiePointGrid(lonGrid);
                final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                product.setSceneGeoCoding(tiePointGeoCoding);
            }
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
        }

    }

    private void createCloudMaskBand(Product product, TreeNode fieldsNode) throws Exception {
        // 'Cloud_Mask' (int8)
        Mod35Utils.addRootMetadataElement(product, (DefaultMutableTreeNode) fieldsNode,
                                          Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);


        final List cloudMaskDSMetadata = cloudMaskDS.getMetadata();
        final int cloudMaskDatatypeClass = cloudMaskDS.getDatatype().getDatatypeClass();
        final Band cloudMaskBand = Mod35Utils.createTargetBand(product,
                                                               cloudMaskDSMetadata,
                                                               Mod35Constants.CLOUD_MASK_BAND_NAME,
                                                               ProductData.TYPE_INT16);
        Mod35Utils.setBandUnitAndDescription(cloudMaskDSMetadata, cloudMaskBand);
        cloudMaskBand.setNoDataValue(Mod35Constants.GEOMETRY_NO_DATA_VALUE);
        cloudMaskBand.setNoDataValueUsed(true);

        datasetVars.put(cloudMaskBand,
                        new Hdf4DatasetVar(Mod35Constants.CLOUD_MASK_BAND_NAME,
                                           cloudMaskDatatypeClass,
                                           cloudMaskDS));
        if (cloudMaskDSMetadata != null) {
            Mod35Utils.addMetadataElementWithAttributes(cloudMaskDSMetadata,
                                                        rootMetadataElement,
                                                        Mod35Constants.CLOUD_MASK_BAND_NAME);
        }
    }

    private void createGeometryBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Solar_Zenith', 'Solar_Azimuth', 'Sensor_Zenith', 'Sensor_Azimuth' (int16)
        Mod35Utils.addRootMetadataElement(product, (DefaultMutableTreeNode) fieldsNode,
                                          Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);

        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode geometryChildNode = fieldsNode.getChildAt(j);
            final String geometryChildNodeName = geometryChildNode.toString();

            if (geometryChildNodeName.contains("Zenith") || geometryChildNodeName.contains("Azimuth")) {
                final H4SDS geometryDS = Mod35Utils.getH4ScalarDS(geometryChildNode);
                final List geometryDSMetadata = geometryDS.getMetadata();
                final int geometryDatatypeClass = geometryDS.getDatatype().getDatatypeClass();
                final Band geometryBand = Mod35Utils.createTargetBand(product,
                                                                      geometryDSMetadata,
                                                                      geometryChildNodeName,
                                                                      ProductData.TYPE_INT16);
                Mod35Utils.setBandUnitAndDescription(geometryDSMetadata, geometryBand);
                geometryBand.setNoDataValue(Mod35Constants.GEOMETRY_NO_DATA_VALUE);
                geometryBand.setNoDataValueUsed(true);

                datasetVars.put(geometryBand,
                                new Hdf4DatasetVar(geometryChildNodeName,
                                                   geometryDatatypeClass,
                                                   geometryDS));
                if (geometryDSMetadata != null) {
                    Mod35Utils.addMetadataElementWithAttributes(geometryDSMetadata,
                                                                rootMetadataElement,
                                                                geometryChildNodeName);
                }
            }
        }
    }



    private static class Hdf4DatasetVar {

        final String name;
        final int type;
        final H4SDS dataset;

        Hdf4DatasetVar(String name, int type, H4SDS dataset) {
            this.name = name;
            this.type = type;
            this.dataset = dataset;
        }

        public String getName() {
            return name;
        }

        int getType() {
            return type;
        }

        H4SDS getDataset() {
            return dataset;
        }
    }
}

