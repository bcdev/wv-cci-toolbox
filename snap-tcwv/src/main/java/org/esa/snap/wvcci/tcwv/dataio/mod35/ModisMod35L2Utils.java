package org.esa.snap.wvcci.tcwv.dataio.mod35;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.h4.H4Datatype;
import ncsa.hdf.object.h4.H4Group;
import ncsa.hdf.object.h4.H4SDS;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * MODIS MOD35 utility methods
 *
 * @author olafd
 */
class ModisMod35L2Utils {

    /**
     * Returns the value of an HDF double attribute with given name
     *
     * @param metadata      - the metadata containing the attributes
     * @param attributeName - the attribute name
     * @return the value as double
     */
    static double getDoubleAttributeValue(List<Attribute> metadata, String attributeName) {
        double doubleAttr = Double.NaN;
        for (int i = 0; i < metadata.size(); i++) {
            Attribute attribute = metadata.get(i);
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
     * Provides a HDF4 scalar dataset corresponding to given HDF product node
     *
     * @param node - the data node
     * @return - the data set (H4SDS)
     */
    static H4SDS getH4ScalarDS(TreeNode node) {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) node).getUserObject();
        scalarDS.open();
        scalarDS.init();
        return scalarDS;
    }

    /**
     * Provides a HDF4 scalar dataset corresponding to 'Cloud_Mask' node
     *
     * @param node - the data node
     * @param byteSegmentSize - number of byte segments needed (max. 6)
     * @param productHeight - productHeight
     * @param productWidth - productWidth
     *
     * @return - the data set (H4SDS)
     * @throws HDFException -
     */
    static H4SDS getH4ScalarDSForCloudMask(TreeNode node,
                                                  int byteSegmentSize,
                                                  int productHeight, int productWidth) throws HDFException {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) node).getUserObject();
        scalarDS.open();
        scalarDS.init();
        long[] selectedDims = scalarDS.getSelectedDims();
        selectedDims[0] = byteSegmentSize;
        selectedDims[1] = productHeight;
        selectedDims[2] = productWidth;
        scalarDS.read();
        return scalarDS;
    }

    /**
     * Provides a HDF4 scalar dataset corresponding to 'Quality_Assurance' node
     *
     * @param node - the data node
     * @param qualityAssuranceDim - number of quality assurance dimensions needed (usually 10)
     * @param productHeight - productHeight
     * @param productWidth - productWidth
     *
     * @return - the data set (H4SDS)
     * @throws HDFException -
     */
    static H4SDS getH4ScalarDSForQualityAssurance(TreeNode node,
                                                  int qualityAssuranceDim,
                                                  int productHeight, int productWidth) throws HDFException {
        H4SDS scalarDS = (H4SDS) ((DefaultMutableTreeNode) node).getUserObject();
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
    static void addMetadataElementWithAttributes(List<Attribute> metadataAttributes,
                                                        final MetadataElement parentElement,
                                                        String metadataElementName) {
        final MetadataElement metadataElement = new MetadataElement(metadataElementName);
        for (Attribute attribute : metadataAttributes) {
            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
                    ProductData.createInstance(ModisMod35L2Utils.getAttributeValue(attribute)), true));
        }
        parentElement.addElement(metadataElement);
    }

    /**
     * Adds a metadata element with attributes to root node
     *
     * @param product     - the product
     * @param parentNode  - the HDF parent node
     * @param elementName - the metadata element name
     * @throws HDFException -
     */
    static void addRootMetadataElement(Product product, DefaultMutableTreeNode parentNode, String elementName)
            throws HDFException {
        final H4Group parentGeometryGroup = (H4Group) parentNode.getUserObject();
        final List parentGeometryMetadata = parentGeometryGroup.getMetadata();
        ModisMod35L2Utils.addMetadataElementWithAttributes(parentGeometryMetadata, product.getMetadataRoot(), elementName);
    }

    /**
     * Extracts unit and description from HDF metadata and adds to given node
     *
     * @param metadata - HDF metadata
     * @param node     - the node
     */
    static void setUnitAndDescription(List<Attribute> metadata, RasterDataNode node) {
        node.setDescription(ModisMod35L2Utils.getStringAttributeValue(metadata, "long_name"));
        node.setUnit(ModisMod35L2Utils.getStringAttributeValue(metadata, "units"));
    }

    /**
     * Provides a ProductData instance according to given HDF5 data type
     *
     * @param datatypeClass - the HDF4 data type
     * @param width         - buffer width
     * @param height        - buffer height
     * @return the data buffer
     */
    static ProductData getDataBufferForH4DataRead(int datatypeClass, int width, int height) {
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

    /**
     * Provides the size of a HDF4 dimension from the StructMetadata.0 element
     *
     * @param structMetadata0String - the whole StructMetadata.0 as a string
     * @param dimensionName - name of the dimension
     *
     * @return the dimension size (int)
     */
    static int getDimensionSizeFromMetadata(String structMetadata0String, String dimensionName) {
        String[] lines = structMetadata0String.split("\\r?\\n");

        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].trim().equals("DimensionName=\"" + dimensionName + "\"")) { // e.g. "Cell_Across_Swath_1km"
                return Integer.parseInt(lines[i+1].trim().substring(5));             // e.g. 'Size=1354'
            }
        }
        return -1;
    }

    /**
     * Provides the product date from year, doy, hour, min, sec
     *
     * @param year - year
     * @param doy - day of year
     * @param hour - hour
     * @param min - minute
     * @param sec - seconds
     *
     * @return product date as {@link ProductData.UTC}
     */
    static ProductData.UTC getProductDate(int year, int doy, int hour, int min, int sec) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, doy);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);

        final String pattern = "dd-MM-yyyy HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
        String stopFormatted = sdf.format(cal.getTime());
        try {
            return ProductData.UTC.parse(stopFormatted, pattern);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    ////// private methods //////////////////////

    private static String getStringAttributeValue(List<Attribute> metadata, String attributeName) {
        for (int i = 0; i < metadata.size(); i++) {
            Attribute attribute = metadata.get(i);
            if (attribute.getName().equals(attributeName)) {
                try {
                    return getAttributeValue(attribute);
                } catch (NumberFormatException e) {
                    SystemUtils.LOG.log(Level.WARNING, "Cannot parse string attribute: " +
                            e.getMessage());
                }
            }
        }

        return "N/A";
    }

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
            case Datatype.CLASS_CHAR:
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
}
