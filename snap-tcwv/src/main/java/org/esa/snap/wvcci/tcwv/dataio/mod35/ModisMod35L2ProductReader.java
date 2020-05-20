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
import org.esa.snap.raster.gpf.FlipOp;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * Product reader responsible for reading MODIS MOD35 or MYD35 L2 cloud mask HDF products.
 * Example: MOD35_L2.A2011196.1055.061.2017325012717.hdf
 * See https://modis.gsfc.nasa.gov/data/dataprod/mod35.php
 *
 * @author Olaf Danne
 */
public class ModisMod35L2ProductReader extends AbstractProductReader {

    private int productWidth;
    private int productHeight;

    private int tpWidth;
    private int tpHeight;

    private int qualityAssuranceDim;

    private FileFormat h4File;

    private TreeNode h4RootNode;
    private TreeNode mod35Node;

    /**
     * ModisMod35L2ProductReader constructor
     *
     * @param readerPlugIn - the corresponding reader plugin
     */
    ModisMod35L2ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() {
        final Object inputObject = getInput();
        Product targetProduct = null;
        File mod35File = ModisMod35L2ProductReaderPlugIn.getFileInput(inputObject);

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

    private static Band createTargetBand(Product product, List<Attribute> metadata, String bandName, int dataType) {
        final double scaleFactorAttr = ModisMod35L2Utils.getDoubleAttributeValue(metadata, "SCALE");
        final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
        final double scaleOffsetAttr = ModisMod35L2Utils.getDoubleAttributeValue(metadata, "OFFSET");
        final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(1.0 / scaleFactor);
        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);
        return band;
    }

    private Product createTargetProduct(File inputFile) throws Exception {
        mod35Node = h4RootNode.getChildAt(0);

        Product targetProduct = new Product(inputFile.getName(),
                ModisMod35L2Constants.MOD35_l2_PRODUCT_TYPE,
                productWidth, productHeight);

        final H4Group rootGroup = (H4Group) ((DefaultMutableTreeNode) h4RootNode).getUserObject();
        final List rootMetadata = rootGroup.getMetadata();

        ModisMod35L2Utils.addMetadataElementWithAttributes(rootMetadata, targetProduct.getMetadataRoot(), ModisMod35L2Constants.MPH_NAME);

        targetProduct.setDescription(ModisMod35L2Constants.MOD35_l2_PRODUCT_DESCR);
        targetProduct.setFileLocation(inputFile);

        for (int i = 0; i < mod35Node.getChildCount(); i++) {
            // we have: 'Geolocation Fields', 'Data Fields'
            final TreeNode fieldsNode = mod35Node.getChildAt(i);
            final String fieldsNodeName = fieldsNode.toString();

            switch (fieldsNodeName) {
                case ModisMod35L2Constants.GEOLOCATION_FIELDS_GROUP_NAME:
                    ModisMod35L2Utils.addRootMetadataElement(targetProduct, (DefaultMutableTreeNode) fieldsNode,
                            ModisMod35L2Constants.GEOLOCATION_FIELDS_GROUP_NAME);
                    createGeolocationTpgs(targetProduct, fieldsNode);
                    break;

                case ModisMod35L2Constants.DATA_FIELDS_GROUP_NAME:
                    ModisMod35L2Utils.addRootMetadataElement(targetProduct, (DefaultMutableTreeNode) fieldsNode,
                            ModisMod35L2Constants.DATA_FIELDS_GROUP_NAME);
                    createCloudMaskBands(targetProduct, fieldsNode);
                    createQualityAssuranceBands(targetProduct, fieldsNode);
                    createGeometryTpgs(targetProduct, fieldsNode);
                    break;

                default:
                    break;
            }
        }

        setStartStopTimes(targetProduct, inputFile.getName());

        Mod35L2CloudMaskUtils.attachPixelClassificationFlagBand(targetProduct);
        Mod35L2CloudMaskUtils.attachQualityAssuranceFlagBand(targetProduct);

        // MYD35_L2 products are flipped, but MOD35_L2 are not...
        if (inputFile.getName().startsWith("MYD")) {
            FlipOp flipOp = new FlipOp();
            flipOp.setParameterDefaultValues();
            flipOp.setSourceProduct(targetProduct);
            flipOp.setParameter("flipType", "Horizontal and Vertical");
            final Product flippedMydProduct = flipOp.getTargetProduct();
            flippedMydProduct.setName(targetProduct.getName());
            return flippedMydProduct;
        } else {
            return targetProduct;
        }
    }

    private void setProductDimensions(String structMetadata0String) {
        productWidth = ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String,
                ModisMod35L2Constants.CELL_ACROSS_SWATH_1KM_DIM_NAME);
        productHeight = ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String,
                ModisMod35L2Constants.CELL_ALONG_SWATH_1KM_DIM_NAME);
        tpWidth = ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String,
                ModisMod35L2Constants.CELL_ACROSS_SWATH_5KM_DIM_NAME);
        tpHeight = ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String,
                ModisMod35L2Constants.CELL_ALONG_SWATH_5KM_DIM_NAME);
        qualityAssuranceDim = ModisMod35L2Utils.getDimensionSizeFromMetadata(structMetadata0String, ModisMod35L2Constants.QA_DIM_NAME);
    }

    private void setStartStopTimes(Product p, String name) {
        // e.g. name = 'MOD35_L2.A2011196.1055.061.2017325012717.hdf'
        try {
            final int year = Integer.parseInt(name.substring(10, 14));
            final int doy = Integer.parseInt(name.substring(14, 17));
            final int hour = Integer.parseInt(name.substring(18, 20));
            final int min = Integer.parseInt(name.substring(20, 22));
            final int sec = 0;
            p.setStartTime(ModisMod35L2Utils.getProductDate(year, doy, hour, min, sec));
            p.setEndTime(ModisMod35L2Utils.getProductDate(year, doy, hour, min, sec));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void createGeolocationTpgs(Product product, TreeNode fieldsNode) throws Exception {
        // 'Latitude', 'Longitude' (both float32)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(ModisMod35L2Constants.GEOLOCATION_FIELDS_GROUP_NAME);

        float[] lats = null;
        float[] lons = null;
        TiePointGrid latGrid = null;
        TiePointGrid lonGrid = null;
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode geolocationChildNode = fieldsNode.getChildAt(j);
            final String geolocationChildNodeName = geolocationChildNode.toString();

            final H4SDS geolocationDS = ModisMod35L2Utils.getH4ScalarDS(geolocationChildNode);
            final List geolocationMetadata = geolocationDS.getMetadata();
            if (geolocationMetadata != null) {
                ModisMod35L2Utils.addMetadataElementWithAttributes(geolocationMetadata,
                        rootMetadataElement,
                        geolocationChildNodeName);
            }

            // add TPGs:
            if (geolocationChildNodeName.equals("Latitude")) {
                lats = (float[]) geolocationDS.getData();
                if (lats != null) {
                    latGrid = new TiePointGrid("Latitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lats);
                    ModisMod35L2Utils.setUnitAndDescription(geolocationMetadata, latGrid);
                    product.addTiePointGrid(latGrid);
                }
            } else if (geolocationChildNodeName.equals("Longitude")) {
                lons = (float[]) geolocationDS.getData();
                if (lons != null) {
                    lonGrid = new TiePointGrid("Longitude", tpWidth, tpHeight, 0, 0, 5.0, 5.0, lons);
                    ModisMod35L2Utils.setUnitAndDescription(geolocationMetadata, lonGrid);
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
                getElement(ModisMod35L2Constants.DATA_FIELDS_GROUP_NAME);

        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode geometryChildNode = fieldsNode.getChildAt(j);
            final String geometryChildNodeName = geometryChildNode.toString();

            H4SDS geometryDS = null;
            float[] geometryData = null;
            if (geometryChildNodeName.contains("Zenith") || geometryChildNodeName.contains("Azimuth")) {
                geometryData = new float[tpWidth * tpHeight];
                geometryDS = ModisMod35L2Utils.getH4ScalarDS(geometryChildNode);
                final short[] geometryDSData = (short[]) geometryDS.getData();
                for (int i = 0; i < geometryDSData.length; i++) {
                    geometryData[i] = (float) geometryDSData[i];
                }
            } else if (geometryChildNodeName.equals("Scan_Start_Time")) {
                geometryData = new float[tpWidth * tpHeight];
                geometryDS = ModisMod35L2Utils.getH4ScalarDS(geometryChildNode);
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
                ModisMod35L2Utils.addMetadataElementWithAttributes(geometryDSMetadata,
                        rootMetadataElement,
                        geometryChildNodeName);
                final double scaleFactorAttr = ModisMod35L2Utils.getDoubleAttributeValue(geometryDSMetadata, "scale_factor");
                final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
                final double scaleOffsetAttr = ModisMod35L2Utils.getDoubleAttributeValue(geometryDSMetadata, "add_offset");
                final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
                geometryTpg.setScalingFactor(scaleFactor);
                geometryTpg.setScalingOffset(scaleOffset);
                ModisMod35L2Utils.setUnitAndDescription(geometryDSMetadata, geometryTpg);
                product.addTiePointGrid(geometryTpg);
            }
        }
    }

    private void createCloudMaskBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Cloud_Mask' (int8, 6 * productHeight * productWidth)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(ModisMod35L2Constants.DATA_FIELDS_GROUP_NAME);

        H4SDS cloudMaskDS = null;
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode dataChildNode = fieldsNode.getChildAt(j);
            final String dataChildNodeName = dataChildNode.toString();
            if (dataChildNodeName.equals(ModisMod35L2Constants.CLOUD_MASK_BAND_NAME)) {
                // we only need the first of six byte segment to build the cloud flag for our purpose:
                final int requiredByteSegments = 1;
                cloudMaskDS = ModisMod35L2Utils.getH4ScalarDSForCloudMask(dataChildNode,
                        requiredByteSegments, productHeight, productWidth);
            }
        }
        final List<Attribute> cloudMaskDSMetadata = cloudMaskDS.getMetadata();

        final byte[] cloudMaskData3DArr = (byte[]) cloudMaskDS.getData();
        byte[] tmpArr = new byte[productWidth * productHeight];
        // for pixel classification we need segment 1 only:
        for (int i = 0; i < 1; i++) {
            ProductData productData = ModisMod35L2Utils.getDataBufferForH4DataRead(H4Datatype.CLASS_CHAR,
                    productWidth, productHeight);
            final String cloudMaskByteBandName = ModisMod35L2Constants.CLOUD_MASK_BYTE_TARGET_BAND_NAME + (i + 1);
            final Band cloudMaskByteBand = createTargetBand(product,
                    cloudMaskDSMetadata,
                    cloudMaskByteBandName,
                    productData.getType());
            ModisMod35L2Utils.setUnitAndDescription(cloudMaskDSMetadata, cloudMaskByteBand);
            cloudMaskByteBand.setNoDataValue(ModisMod35L2Constants.CHAR_NO_DATA_VALUE);
            cloudMaskByteBand.setNoDataValueUsed(true);
            final int offset = i * productWidth * productHeight;
            System.arraycopy(cloudMaskData3DArr, offset, tmpArr, 0, tmpArr.length);
            productData.setElems(tmpArr);
            cloudMaskByteBand.setRasterData(productData);
            if (cloudMaskDSMetadata != null) {
                ModisMod35L2Utils.addMetadataElementWithAttributes(cloudMaskDSMetadata,
                        rootMetadataElement,
                        cloudMaskByteBandName);
            }
        }
    }

    private void createQualityAssuranceBands(Product product, TreeNode fieldsNode) throws Exception {
        // 'Quality_Assurance' (int8, productHeight * productWidth * 10)
        final MetadataElement rootMetadataElement = product.getMetadataRoot().
                getElement(ModisMod35L2Constants.DATA_FIELDS_GROUP_NAME);

        H4SDS qualityAssuranceDS = null;
        for (int j = 0; j < fieldsNode.getChildCount(); j++) {
            final TreeNode dataChildNode = fieldsNode.getChildAt(j);
            final String dataChildNodeName = dataChildNode.toString();
            if (dataChildNodeName.equals(ModisMod35L2Constants.QUALITY_ASSURANCE_BAND_NAME)) {
                // we need to read all dimensions because the array nesting is different
                // from the one for 'Cloud_Mask'
                qualityAssuranceDS = ModisMod35L2Utils.getH4ScalarDSForQualityAssurance(dataChildNode,
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
            ProductData productData = ModisMod35L2Utils.getDataBufferForH4DataRead(H4Datatype.CLASS_CHAR,
                    productWidth, productHeight);
            final String qualityAssuranceQaDimBandName = ModisMod35L2Constants.QUALITY_ASSURANCE_QA_DIMENSION_BAND_NAME + (i + 1);
            final Band qualityAssuranceQaDimBand = createTargetBand(product,
                    qualityAssuranceDSMetadata,
                    qualityAssuranceQaDimBandName,
                    productData.getType());
            ModisMod35L2Utils.setUnitAndDescription(qualityAssuranceDSMetadata, qualityAssuranceQaDimBand);
            qualityAssuranceQaDimBand.setNoDataValue(ModisMod35L2Constants.CHAR_NO_DATA_VALUE);
            qualityAssuranceQaDimBand.setNoDataValueUsed(true);

            // we need to resort because the array nesting is different from the one for 'Cloud_Mask'
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
                ModisMod35L2Utils.addMetadataElementWithAttributes(qualityAssuranceDSMetadata,
                        rootMetadataElement,
                        qualityAssuranceQaDimBandName);
            }
        }
    }
}

