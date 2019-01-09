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

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h4.H4Datatype;
import ncsa.hdf.object.h4.H4Group;
import ncsa.hdf.object.h4.H4SDS;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
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

    private int byteSegmentSize;
    private int qaDim;

    private FileFormat h4File;

    private TreeNode h4RootNode;
    private TreeNode mod35Node;
    private H4SDS cloudMaskDS;


    Modis35ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    @Override
    protected Product readProductNodesImpl() {
        final Object inputObject = getInput();
        Product targetProduct = null;
        File mod35File = Modis35ProductReaderPlugIn.getFileInput(inputObject);

        if (mod35File != null) {
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

                final H4Group rootGroup = (H4Group) ((DefaultMutableTreeNode) h4RootNode).getUserObject();
                final List rootMetadata = rootGroup.getMetadata();
                final String[] structMetadata0String = (String[]) ((Attribute) rootMetadata.get(1)).getValue();
                setProductDimensions(structMetadata0String[0]);
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
        }

        return targetProduct;
    }

    private void setProductDimensions(String structMetadata0String) {
        productWidth = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String,
                                                               Mod35Constants.CELL_ACROSS_SWATH_1KM_DIM_NAME);
        productHeight = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String,
                                                                Mod35Constants.CELL_ALONG_SWATH_1KM_DIM_NAME);
        tpWidth = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String,
                                                          Mod35Constants.CELL_ACROSS_SWATH_5KM_DIM_NAME);
        tpHeight = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String,
                                                           Mod35Constants.CELL_ALONG_SWATH_5KM_DIM_NAME);
        byteSegmentSize = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String,
                                                                  Mod35Constants.BYTE_SEGMENT_DIM_NAME);
        qaDim = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String, Mod35Constants.QA_DIM_NAME);
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
        throw new IllegalStateException(String.format("No source to read from for band '%s'.", targetBand.getName()));
    }


//////////// private methods //////////////////

    private Product createTargetProduct(File inputFile) throws Exception {
        mod35Node = h4RootNode.getChildAt(0);

        Product product = new Product(inputFile.getName(), Mod35Constants.MOD35_l2_PRODUCT_TYPE, productWidth, productHeight);

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
                    Mod35Utils.addRootMetadataElement(product, (DefaultMutableTreeNode) fieldsNode,
                                                      Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
                    createGeolocationTpgs(product, fieldsNode);
                    break;

                case Mod35Constants.DATA_FIELDS_GROUP_NAME:
                    Mod35Utils.addRootMetadataElement(product, (DefaultMutableTreeNode) fieldsNode,
                                                      Mod35Constants.DATA_FIELDS_GROUP_NAME);
                    createCloudMaskBands(product, fieldsNode);
                    createGeometryTpgs(product, fieldsNode);
                    break;

                default:
                    break;
            }
        }

        Mod35BitMaskUtils.attachPixelClassificationFlagBand(product);

        return product;
    }

    private void createGeolocationTpgs(Product product, TreeNode fieldsNode) throws Exception {
        // 'Latitude', 'Longitude' (both float32)
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
                latGrid = new TiePointGrid("Latitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lats);
                product.addTiePointGrid(latGrid);
                TiePointGrid lonGrid = new TiePointGrid("Longitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lons);
                product.addTiePointGrid(lonGrid);
                final TiePointGeoCoding tiePointGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
                product.setSceneGeoCoding(tiePointGeoCoding);
            }
        } catch (Exception e) {
            SystemUtils.LOG.log(Level.WARNING, "Cannot attach geocoding: " + e.getMessage());
        }

    }

    private void createGeometryTpgs(Product product, TreeNode fieldsNode) throws Exception {
        // 'Solar_Zenith', 'Solar_Azimuth', 'Sensor_Zenith', 'Sensor_Azimuth' (int16)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.DATA_FIELDS_GROUP_NAME);

        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode geometryChildNode = fieldsNode.getChildAt(j);
            final String geometryChildNodeName = geometryChildNode.toString();

            H4SDS geometryDS = null;
            float[] geometryData = null;
            if (geometryChildNodeName.contains("Zenith") || geometryChildNodeName.contains("Azimuth")) {
                geometryData = new float[tpWidth * tpHeight];
                geometryDS = Mod35Utils.getH4ScalarDS(geometryChildNode);
                final short[] geometryDSData = (short[]) geometryDS.getData();
                for (int i = 0; i < geometryDSData.length; i++) {
                    geometryData[i] = (float) geometryDSData[i];
                }
            } else if (geometryChildNodeName.equals("Scan_Start_Time")) {
                geometryData = new float[tpWidth * tpHeight];
                geometryDS = Mod35Utils.getH4ScalarDS(geometryChildNode);
                final double[] geometryDSData = (double[]) geometryDS.getData();
                for (int i = 0; i < geometryDSData.length; i++) {
                    geometryData[i] = (float) geometryDSData[i];
                }
            }
            if (geometryData != null) {
                final TiePointGrid geometryTpg = new TiePointGrid(geometryChildNodeName,
                                                                  tpWidth, tpHeight,
                                                                  0, 0, 5.0, 5.0,
                                                                  geometryData);

                final List geometryDSMetadata = geometryDS.getMetadata();
                Mod35Utils.addMetadataElementWithAttributes(geometryDSMetadata,
                                                            rootMetadataElement,
                                                            geometryChildNodeName);
                final double scaleFactorAttr = Mod35Utils.getDoubleAttributeValue(geometryDSMetadata, "scale_factor");
                final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
                final double scaleOffsetAttr = Mod35Utils.getDoubleAttributeValue(geometryDSMetadata, "add_offset");
                final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
                geometryTpg.setScalingFactor(scaleFactor);
                geometryTpg.setScalingOffset(scaleOffset);
                product.addTiePointGrid(geometryTpg);
            }
        }
    }

    private void createCloudMaskBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Cloud_Mask' (int8)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.DATA_FIELDS_GROUP_NAME);

        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode dataChildNode = fieldsNode.getChildAt(j);
            final String dataChildNodeName = dataChildNode.toString();
            if (dataChildNodeName.equals(Mod35Constants.CLOUD_MASK_BAND_NAME)) {
                cloudMaskDS = Mod35Utils.getH4ScalarDSForCloudMask(dataChildNode,
                                                                   byteSegmentSize, productHeight, productWidth);
            }
        }
        final List<Attribute> cloudMaskDSMetadata = cloudMaskDS.getMetadata();

        final byte[] cloudMaskData3DArr = (byte[]) cloudMaskDS.getData();
        byte[] tmpArr = new byte[productWidth * productHeight];
        // bytes 1-5
        for (int i = 0; i < byteSegmentSize; i++) {
            ProductData productData = Mod35Utils.getDataBufferForH4DataRead(H4Datatype.CLASS_CHAR,
                                                                            productWidth, productHeight);
            final Band cloudMaskByteBand = Mod35Utils.createTargetBand(product,
                                                                       cloudMaskDSMetadata,
                                                                       Mod35Constants.CLOUD_MASK_BYTE_TARGET_BAND_NAME + i,
                                                                       productData.getType());
            Mod35Utils.setBandUnitAndDescription(cloudMaskDSMetadata, cloudMaskByteBand);
            cloudMaskByteBand.setNoDataValue(Mod35Constants.CLOUD_MASK_NO_DATA_VALUE);
            cloudMaskByteBand.setNoDataValueUsed(true);
            final int offset = i * productWidth * productHeight;
            System.arraycopy(cloudMaskData3DArr, offset, tmpArr, 0, tmpArr.length);
            productData.setElems(tmpArr);
            cloudMaskByteBand.setRasterData(productData);
            if (cloudMaskDSMetadata != null) {
                Mod35Utils.addMetadataElementWithAttributes(cloudMaskDSMetadata,
                                                            rootMetadataElement,
                                                            Mod35Constants.CLOUD_MASK_BYTE_TARGET_BAND_NAME + i);
            }
        }
    }

}

