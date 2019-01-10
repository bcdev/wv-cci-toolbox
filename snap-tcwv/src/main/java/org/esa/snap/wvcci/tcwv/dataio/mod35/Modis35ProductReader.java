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
    private int qualityAssuranceDim;

    private FileFormat h4File;

    private TreeNode h4RootNode;
    private TreeNode mod35Node;


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
        qualityAssuranceDim = Mod35Utils.getDimensionSizeFromMetadata(structMetadata0String, Mod35Constants.QA_DIM_NAME);
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

        Product targetProduct = new Product(inputFile.getName(),
                                            Mod35Constants.MOD35_l2_PRODUCT_TYPE,
                                            productWidth, productHeight);

        final H4Group rootGroup = (H4Group) ((DefaultMutableTreeNode) h4RootNode).getUserObject();
        final List rootMetadata = rootGroup.getMetadata();

        Mod35Utils.addMetadataElementWithAttributes(rootMetadata, targetProduct.getMetadataRoot(), Mod35Constants.MPH_NAME);

        targetProduct.setDescription(Mod35Constants.MOD35_l2_PRODUCT_DESCR);
        Mod35Utils.addStartStopTimes(targetProduct, (DefaultMutableTreeNode) h4RootNode);
        targetProduct.setFileLocation(inputFile);

        for (int i = 0; i < mod35Node.getChildCount(); i++) {
            // we have: 'Geolocation Fields', 'Data Fields'
            final TreeNode fieldsNode = mod35Node.getChildAt(i);
            final String fieldsNodeName = fieldsNode.toString();

            switch (fieldsNodeName) {
                case Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME:
                    Mod35Utils.addRootMetadataElement(targetProduct, (DefaultMutableTreeNode) fieldsNode,
                                                      Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);
                    createGeolocationTpgs(targetProduct, fieldsNode);
                    break;

                case Mod35Constants.DATA_FIELDS_GROUP_NAME:
                    Mod35Utils.addRootMetadataElement(targetProduct, (DefaultMutableTreeNode) fieldsNode,
                                                      Mod35Constants.DATA_FIELDS_GROUP_NAME);
                    createCloudMaskBands(targetProduct, fieldsNode);
                    createQualityAssuranceBands(targetProduct, fieldsNode);
                    createGeometryTpgs(targetProduct, fieldsNode);
                    break;

                default:
                    break;
            }
        }

        setStartStopTimes(targetProduct, inputFile.getName());

        Mod35BitMaskUtils.attachPixelClassificationFlagBand(targetProduct);
        Mod35BitMaskUtils.attachQualityAssuranceFlagBand(targetProduct);

        return targetProduct;
    }

    private void setStartStopTimes(Product p, String name) {
        // e.g. name = 'MOD35_L2.A2011196.1055.061.2017325012717.hdf'
        final int year = Integer.parseInt(name.substring(10, 14));
        final int doy = Integer.parseInt(name.substring(14, 17));
        final int hour = Integer.parseInt(name.substring(18, 20));
        final int min = Integer.parseInt(name.substring(20, 22));
        final int sec = 0;
        p.setStartTime(Mod35Utils.getProductDate(year, doy, hour, min, sec));
        p.setEndTime(Mod35Utils.getProductDate(year, doy, hour, min, sec));
    }

    private void createGeolocationTpgs(Product product, TreeNode fieldsNode) throws Exception {
        // 'Latitude', 'Longitude' (both float32)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.GEOLOCATION_FIELDS_GROUP_NAME);

        float[] lats = null;
        float[] lons = null;
        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;
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

            // add TPGs:
            if (geolocationChildNodeName.equals("Latitude")) {
                lats = (float[]) geolocationDS.getData();
                if (lats != null) {
                    latGrid = new TiePointGrid("Latitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lats);
                    Mod35Utils.setUnitAndDescription(geolocationMetadata, latGrid);
                    product.addTiePointGrid(latGrid);
                }
            } else if (geolocationChildNodeName.equals("Longitude")) {
                lons = (float[]) geolocationDS.getData();
                if (lons != null) {
                    lonGrid = new TiePointGrid("Longitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lons);
                    Mod35Utils.setUnitAndDescription(geolocationMetadata, lonGrid);
                    product.addTiePointGrid(lonGrid);
                }
            }
        }

        // add geocoding:
        try {
            if (lats != null && lons != null) {
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
                final long startTimeMillis = (long) (geometryData[0] * 1000.0f);
                final long stopTimeMillis = (long) (geometryData[geometryData.length-1] * 1000.0f);
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
                Mod35Utils.setUnitAndDescription(geometryDSMetadata, geometryTpg);
                product.addTiePointGrid(geometryTpg);
            }
        }
    }

    private void createCloudMaskBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Cloud_Mask' (int8, 6 * productHeight * productWidth)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.DATA_FIELDS_GROUP_NAME);

        H4SDS cloudMaskDS = null;
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
        // for pixel classification we need segment 1 only:
        for (int i = 0; i < 1; i++) {
            ProductData productData = Mod35Utils.getDataBufferForH4DataRead(H4Datatype.CLASS_CHAR,
                                                                            productWidth, productHeight);
            final String cloudMaskByteBandName = Mod35Constants.CLOUD_MASK_BYTE_TARGET_BAND_NAME + (i + 1);
            final Band cloudMaskByteBand = Mod35Utils.createTargetBand(product,
                                                                       cloudMaskDSMetadata,
                                                                       cloudMaskByteBandName,
                                                                       productData.getType());
            Mod35Utils.setUnitAndDescription(cloudMaskDSMetadata, cloudMaskByteBand);
            cloudMaskByteBand.setNoDataValue(Mod35Constants.CHAR_NO_DATA_VALUE);
            cloudMaskByteBand.setNoDataValueUsed(true);
            final int offset = i * productWidth * productHeight;
            System.arraycopy(cloudMaskData3DArr, offset, tmpArr, 0, tmpArr.length);
            productData.setElems(tmpArr);
            cloudMaskByteBand.setRasterData(productData);
            if (cloudMaskDSMetadata != null) {
                Mod35Utils.addMetadataElementWithAttributes(cloudMaskDSMetadata,
                                                            rootMetadataElement,
                                                            cloudMaskByteBandName);
            }
        }
    }

    private void createQualityAssuranceBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Quality_Assurance' (int8, productHeight * productWidth * 10)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(Mod35Constants.DATA_FIELDS_GROUP_NAME);

        H4SDS qualityAssuranceDS = null;
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode dataChildNode = fieldsNode.getChildAt(j);
            final String dataChildNodeName = dataChildNode.toString();
            if (dataChildNodeName.equals(Mod35Constants.QUALITY_ASSURANCE_BAND_NAME)) {
                qualityAssuranceDS = Mod35Utils.getH4ScalarDSForQualityAssurance(dataChildNode,
                                                                                 qualityAssuranceDim,
                                                                                 productHeight,
                                                                                 productWidth);
            }
        }
        final List<Attribute> qualityAssuranceDSMetadata = qualityAssuranceDS.getMetadata();

        final byte[] qualityAssuranceData3DArr = (byte[]) qualityAssuranceDS.getData();
        byte[] tmpArr = new byte[productWidth * productHeight];
        // for confidence levels we need dimension 1 only:
        for (int i = 0; i < 1; i++) {
            ProductData productData = Mod35Utils.getDataBufferForH4DataRead(H4Datatype.CLASS_CHAR,
                                                                            productWidth, productHeight);
            final String qualityAssuranceQaDimBandName = Mod35Constants.QUALITY_ASSURANCE_QA_DIMENSION_BAND_NAME + (i + 1);
            final Band qualityAssuranceQaDimBand = Mod35Utils.createTargetBand(product,
                                                                               qualityAssuranceDSMetadata,
                                                                               qualityAssuranceQaDimBandName,
                                                                               productData.getType());
            Mod35Utils.setUnitAndDescription(qualityAssuranceDSMetadata, qualityAssuranceQaDimBand);
            qualityAssuranceQaDimBand.setNoDataValue(Mod35Constants.CHAR_NO_DATA_VALUE);
            qualityAssuranceQaDimBand.setNoDataValueUsed(true);

            int tmpArrIndex = 0;
            int qa3DArrIndex = i;
            for (int j = 0; j < productHeight; j++) {
                for (int k = 0; k < productWidth; k++) {
                    tmpArr[tmpArrIndex] = qualityAssuranceData3DArr[qa3DArrIndex];
                    tmpArrIndex++;
                    qa3DArrIndex += qualityAssuranceDim;
                }
            }

            productData.setElems(tmpArr);
            qualityAssuranceQaDimBand.setRasterData(productData);
            if (qualityAssuranceDSMetadata != null) {
                Mod35Utils.addMetadataElementWithAttributes(qualityAssuranceDSMetadata,
                                                            rootMetadataElement,
                                                            qualityAssuranceQaDimBandName);
            }
        }
    }


}

