package org.esa.snap.wvcci.tcwv.util;

import org.esa.snap.core.datamodel.Band;

/**
 * Utility class for TCWV retrievals
 *
 * @author olafd
 */
public class TcwvUtils {

    public static void setBandProperties(Band b, Band sourceBand) {
        b.setNoDataValue(sourceBand.getNoDataValue());
        b.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
        b.setScalingFactor(sourceBand.getScalingFactor());
        b.setScalingOffset(sourceBand.getScalingOffset());
        b.setUnit(sourceBand.getUnit());
        b.setDescription(sourceBand.getDescription());
    }

}
