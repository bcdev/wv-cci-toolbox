package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamFlagCodingPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;

import java.io.IOException;

/**
 * Modification of BeamFlagCodingPart for WV_cci purposes
 *
 * @author olafd
 */
public class SnapWvcciFlagCodingPart extends BeamFlagCodingPart  {

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final Band[] bands = p.getBands();
        for (Band band : bands) {
            if (ncFile.findVariable(band.getName()) != null) {
                CfFlagCodingPart.writeFlagCoding(band, ncFile);
                writeFlagCoding(band, ncFile);
            }
        }
    }
}
