package org.esa.snap.wvcci.tcwv.dataio.mod35;

import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h4.H4Datatype;
import ncsa.hdf.object.h4.H4Group;
import ncsa.hdf.object.h4.H4SDS;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;

/**
 * Proba-V utility methods
 *
 * @author olafd
 */
public class Mod35Utils {

    /**
     * Returns the value of a given HDF attribute
     *
     * @param attribute - input attribute
     * @return the value as string
     */
    private static String getAttributeValue(Attribute attribute) {
        String result = "";
        switch (attribute.getType().getDatatypeClass()) {
            case Datatype.CLASS_INTEGER:
                if (attribute.getValue().getClass() == long[].class) {
                    long[] ivals = (long[]) attribute.getValue();
                    for (long ival : ivals) {
                        result = result.concat(Long.toString(ival) + " ");
                    }
                }
                if (attribute.getValue().getClass() == int[].class) {
                    int[] ivals = (int[]) attribute.getValue();
                    for (int ival : ivals) {
                        result = result.concat(Integer.toString(ival) + " ");
                    }
                }
                if (attribute.getValue().getClass() == short[].class) {
                    short[] ivals = (short[]) attribute.getValue();
                    for (short ival : ivals) {
                        result = result.concat(Short.toString(ival) + " ");
                    }
                }
                break;
            case Datatype.CLASS_FLOAT:
                if (attribute.getValue().getClass() == float[].class) {
                    float[] fvals = (float[]) attribute.getValue();
                    for (float fval : fvals) {
                        result = result.concat(Float.toString(fval) + " ");
                    }
                }
                if (attribute.getValue().getClass() == double[].class) {
                    double[] dvals = (double[]) attribute.getValue();
                    for (double dval : dvals) {
                        result = result.concat(Double.toString(dval) + " ");
                    }
                }
                break;
            case Datatype.CLASS_STRING:
                String[] svals = (String[]) attribute.getValue();
                for (String sval : svals) {
                    result = result.concat(sval + " ");
                }
                break;
            default:
                break;
        }

        return result.trim();
    }

    /**
     * Returns the value of an HDF string attribute with given name
     *
     * @param metadata      - the metadata containing the attributes
     * @param attributeName - the attribute name
     * @return the value as string
     */
    private static String getStringAttributeValue(List<Attribute> metadata, String attributeName) {
        String stringAttr = null;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(attributeName)) {
                try {
                    stringAttr = getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse string attribute: " +
                            e.getMessage());
                }
            }
        }
        return stringAttr;
    }

    /**
     * Returns the value of an HDF double attribute with given name
     *
     * @param metadata      - the metadata containing the attributes
     * @param attributeName - the attribute name
     * @return the value as double
     */
    public static double getDoubleAttributeValue(List<Attribute> metadata, String attributeName) {
        double doubleAttr = Double.NaN;
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(attributeName)) {
                try {
                    doubleAttr = Double.parseDouble(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse float attribute: " + e.getMessage());
                }
            }
        }
        return doubleAttr;
    }

    /**
     * Checks if tree child note corresponds to viewing angle group
     *
     * @param geometryChildNodeName - the tree child note
     * @return boolean
     */
    public static boolean isMod35ViewAngleGroupNode(String geometryChildNodeName) {
        return geometryChildNodeName.equals("SWIR") || geometryChildNodeName.equals("VNIR");
    }

    /**
     * Checks if tree child note corresponds to sun angle group
     *
     * @param geometryChildNodeName - the tree child note
     * @return boolean
     */
    public static boolean isMod35SunAngleDataNode(String geometryChildNodeName) {
        return geometryChildNodeName.equals("SAA") || geometryChildNodeName.equals("SZA");
    }

    /**
     * Creates a target band matching given metadata information
     *
     * @param product  - the target product
     * @param metadata - the HDF metadata attributes
     * @param bandName - band name
     * @param dataType - data type
     * @return the target band
     * @throws Exception
     */
    public static Band createTargetBand(Product product, List<Attribute> metadata, String bandName, int dataType) throws Exception {
        final double scaleFactorAttr = Mod35Utils.getDoubleAttributeValue(metadata, "SCALE");
        final double scaleFactor = Double.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
        final double scaleOffsetAttr = Mod35Utils.getDoubleAttributeValue(metadata, "OFFSET");
        final double scaleOffset = Double.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(1.0 / scaleFactor);
        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);

        return band;
    }

    /**
     * Provides a HDF4 scalar dataset corresponding to given HDF product node
     *
     * @param level3BandsChildNode - the data node
     * @return - the data set (H5ScalarDS)
     */
    public static H4SDS getH4ScalarDS(TreeNode level3BandsChildNode) {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
        scalarDS.open();
        scalarDS.init();
        return scalarDS;
    }

    public static H4SDS getH4ScalarDSForCloudMask(TreeNode level3BandsChildNode,
                                                  int byteSegmentSize,
                                                  int productHeight, int productWidth) throws HDFException {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
        scalarDS.open();
        scalarDS.init();
        long[] selectedDims = scalarDS.getSelectedDims();
        selectedDims[0] = byteSegmentSize;
        selectedDims[1] = productHeight;
        selectedDims[2] = productWidth;
        scalarDS.read();
        return scalarDS;
    }

    public static H4SDS getH4ScalarDSForQualityAssurance(TreeNode level3BandsChildNode,
                                                  int qualityAssuranceDim,
                                                  int productHeight, int productWidth) throws HDFException {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) level3BandsChildNode).getUserObject();
        scalarDS.open();
        scalarDS.init();
        long[] selectedDims = scalarDS.getSelectedDims();
        selectedDims[0] = productHeight;
        selectedDims[1] = productWidth;
        selectedDims[2] = qualityAssuranceDim;
        scalarDS.read();
        return scalarDS;
    }


    /**
     * Extracts a HDF metadata element and adds accordingly to given product
     *
     * @param metadataAttributes  - the HDF metadata attributes
     * @param parentElement       - the parent metadata element
     * @param metadataElementName - the element name
     */
    public static void addMetadataElementWithAttributes(List<Attribute> metadataAttributes,
                                                        final MetadataElement parentElement,
                                                        String metadataElementName) {
        final MetadataElement metadataElement = new MetadataElement(metadataElementName);
        for (Attribute attribute : metadataAttributes) {
            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
                    ProductData.createInstance(Mod35Utils.getAttributeValue(attribute)), true));
        }
        parentElement.addElement(metadataElement);
    }

    /**
     * Adds HDF metadata attributes to a given metadata element
     *
     * @param metadataAttributes - the HDF metadata attributes
     * @param parentElement      - the parent metadata element
     */
    public static void addMetadataAttributes(List<Attribute> metadataAttributes,
                                             final MetadataElement parentElement) {
        for (Attribute attribute : metadataAttributes) {
            parentElement.addAttribute(new MetadataAttribute(attribute.getName(),
                    ProductData.createInstance(Mod35Utils.getAttributeValue(attribute)), true));
        }
    }

    /**
     * Extracs start/stop times from HDF metadata and adds to given product
     *
     * @param product  - the product
     * @param timeNode - the HDF node containing the time information
     * @throws HDF5Exception
     * @throws ParseException
     */
    public static void addStartStopTimes(Product product, DefaultMutableTreeNode timeNode) throws HDF5Exception, ParseException {
        // todo
        //        final H5Group timeGroup = (H5Group) timeNode.getUserObject();
//        final List timeMetadata = timeGroup.getMetadata();
//        String[] startEndTime = Mod35Utils.getStartEndTimeFromAttributes(timeMetadata);
//        if (startEndTime != null) {
//            product.setStartTime(ProductData.UTC.parse(startEndTime[0],
//                                                       Mod35Constants.PROBAV_DATE_FORMAT_PATTERN));
//            product.setEndTime(ProductData.UTC.parse(startEndTime[1],
//                                                     Mod35Constants.PROBAV_DATE_FORMAT_PATTERN));
//        }
    }

    /**
     * Adds a metadata element with attributes to root node
     *
     * @param product     - the product
     * @param parentNode  - the HDF parent node
     * @param elementName - the metadata element name
     * @throws HDF5Exception
     */
    public static void addRootMetadataElement(Product product, DefaultMutableTreeNode parentNode, String elementName)
            throws HDFException {
        final H4Group parentGeometryGroup = (H4Group) parentNode.getUserObject();
        final List parentGeometryMetadata = parentGeometryGroup.getMetadata();
        Mod35Utils.addMetadataElementWithAttributes(parentGeometryMetadata, product.getMetadataRoot(), elementName);
    }

    /**
     * Extracts unit and description from HDF metadata and adds to given band
     *
     * @param metadata - HDF metadata
     * @param band     - the band
     * @throws HDF5Exception
     */
    public static void setBandUnitAndDescription(List<Attribute> metadata, Band band) throws HDF5Exception {
        band.setDescription(Mod35Utils.getStringAttributeValue(metadata, "long_name"));
        band.setUnit(Mod35Utils.getStringAttributeValue(metadata, "units"));
    }

    /**
     * Provides a ProductData instance according to given HDF5 data type
     *
     * @param datatypeClass - the HDF4 data type
     * @param width         - buffer width
     * @param height        - buffer height
     * @return the data buffer
     */
    public static ProductData getDataBufferForH4DataRead(int datatypeClass, int width, int height) {
        switch (datatypeClass) {
            case H4Datatype.CLASS_CHAR:
                return ProductData.createInstance(new byte[width * height]);
            case H4Datatype.CLASS_FLOAT:
                return ProductData.createInstance(new float[width * height]);
            case H4Datatype.CLASS_INTEGER:
                return ProductData.createInstance(new short[width * height]);
            default:
                break;
        }
        return null;
    }

    public static int getDimensionSizeFromMetadata(String structMetadata0String, String dimensionName) {
        String[] lines = structMetadata0String.split("\\r?\\n");

        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].trim().equals("DimensionName=\"" + dimensionName + "\"")) { // e.g. "Cell_Across_Swath_1km"
                return Integer.parseInt(lines[i+1].trim().substring(5));             // e.g. 'Size=1354'
            }
        }
        return -1;
    }

    //// private methods ////

}
