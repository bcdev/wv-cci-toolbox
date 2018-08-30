package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamMetadataPart;

/**
 * Modification of BeamMetadataPart for WV_cci purposes
 *
 * @author olafd
 */
public class SnapWvcciMetadataPart extends BeamMetadataPart {
    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) {
        // nothing to do here for WV_cci
    }
}
