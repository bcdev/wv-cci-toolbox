package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.*;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.nc2.NetcdfFileWriter;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Writer plugin for NetCDF4 output adapted to SNAP WV_cci requirements
 * (i.e. keep products as small as possible)
 *
 * @author olafd
 */
public class SnapWvcciNc4WriterPlugIn extends AbstractNetCdfWriterPlugIn {

    public String[] getFormatNames() {
        return new String[]{"NetCDF4-WVCCI"};
    }

    public String[] getDefaultFileExtensions() {
        return new String[]{".nc"};
    }

    public String getDescription(Locale locale) {
        return "SNAP WV_cci NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ProfilePartWriter createTiePointGridPartWriter() {
        return new BeamTiePointGridPart();
    }

    @Override
    public ProfilePartWriter createMaskPartWriter() {
        return new BeamMaskPart();
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new BeamMetadataPart();
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new SnapWvcciGeocodingPart();  // use this instead of BeamGeocodingPart
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new SnapWvcciMainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf4");
    }

    public EncodeQualification getEncodeQualification(Product product) {
        return new EncodeQualification(EncodeQualification.Preservation.PARTIAL);
    }

    private class SnapWvcciMainPart implements ProfileInitPartWriter {

        // still TODO here or in PostProcessor or elsewhere:
        // - add lots of global attributes (see QA4ECV)
        // - check for CCI compliance
        // - remove variable attribute 'metadata'
        // - add time dimension

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        private Dimension tileSize;

        SnapWvcciMainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);

            if (isGeographicCRS(product.getSceneGeoCoding())) {
                writeDimensions(writeable, product, "lat", "lon");
            } else {
                writeDimensions(writeable, product, "y", "x");
            }

            addGlobalAttributes(writeable, product);

            for (Band b : product.getBands()) {
                final String bandName = b.getName();
                if (bandName.startsWith("tcwv") || bandName.startsWith("ctp") || bandName.endsWith("_classif_flags")) {
                    addNc4BandVariableAndAttributes(writeable, b);

                }
            }
        }

        private void writeDimensions(NFileWriteable writeable, Product p, String dimY, String dimX) throws IOException {
            writeable.addDimension(dimY, p.getSceneRasterHeight());
            writeable.addDimension(dimX, p.getSceneRasterWidth());
        }

        private boolean isGeographicCRS(final GeoCoding geoCoding) {
            return (geoCoding instanceof CrsGeoCoding) &&
                    CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }

        private void addGlobalAttributes(NFileWriteable writeable, Product product) throws IOException {
            writeable.addGlobalAttribute("Conventions", "CF-1.4");
            writeable.addGlobalAttribute("title", "WV_cci product");
            writeable.addGlobalAttribute("product_type", product.getProductType());

            if (product.getStartTime() != null) {
                writeable.addGlobalAttribute("start_date", product.getStartTime().format());
            }
            if (product.getEndTime() != null) {
                writeable.addGlobalAttribute("stop_date", product.getEndTime().format());
            }
            writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);
            // the 'metadata_profile' attribute is required to make SNAP use the BEAM NetCDF reader when
            // reading this product!!
            writeable.addGlobalAttribute("metadata_profile", "beam");
            writeable.addGlobalAttribute("metadata_version", "0.5");

            final MetadataElement metadataRoot = product.getMetadataRoot();
            final MetadataElement processingGraph = metadataRoot.getElement("Processing_Graph");
            if (processingGraph != null) {
                metadataRoot.removeElement(processingGraph);
            }
        }

        private void addNc4BandVariableAndAttributes(NFileWriteable writeable, RasterDataNode b) throws IOException {
            NVariable variable = writeable.addVariable(b.getName(),
                                                       DataTypeUtils.getNetcdfDataType(b.getDataType()),
                                                       tileSize, writeable.getDimensions());
            writeBandAttributes(b, variable);
        }

        private void writeBandAttributes(RasterDataNode rasterDataNode, NVariable variable) throws IOException {
            final String description = rasterDataNode.getDescription();
            if (description != null) {
                variable.addAttribute("long_name", description);
            } else {
                // consider TCWV L3 here
                if (rasterDataNode.getName().equals("tcwv_mean")) {
                    variable.addAttribute("long_name", "Mean of Total Column of Water Vapour");
                }
                if (rasterDataNode.getName().equals("tcwv_sigma")) {
                    variable.addAttribute("long_name", "Sigma of Total Column of Water Vapour");
                }
            }
            final String unit = rasterDataNode.getUnit();
            if (unit != null && !unit.equals("dl")) {
                variable.addAttribute("units", unit);
            } else {
                // consider TCWV L3 here
                if (rasterDataNode.getName().contains("tcwv")) {
                    variable.addAttribute("units", "mm");
                } else {
                    variable.addAttribute("units", "1");
                }
            }

            double noDataValue;
            final double scalingFactor = rasterDataNode.getScalingFactor();
            if (scalingFactor != 1.0) {
                variable.addAttribute(Constants.SCALE_FACTOR_ATT_NAME, scalingFactor);
            }
            final double scalingOffset = rasterDataNode.getScalingOffset();
            if (scalingOffset != 0.0) {
                variable.addAttribute(Constants.ADD_OFFSET_ATT_NAME, scalingOffset);
            }
            noDataValue = rasterDataNode.getNoDataValue();
            if (rasterDataNode.isNoDataValueUsed()) {
                Number fillValue = DataTypeUtils.convertTo(noDataValue, variable.getDataType());
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, fillValue);
            }

            final String validPixelExpression = rasterDataNode.getValidPixelExpression();
            if (validPixelExpression != null && validPixelExpression.trim().length() > 0) {
                variable.addAttribute("valid_pixel_expression", validPixelExpression);
            }
        }
    }
}
