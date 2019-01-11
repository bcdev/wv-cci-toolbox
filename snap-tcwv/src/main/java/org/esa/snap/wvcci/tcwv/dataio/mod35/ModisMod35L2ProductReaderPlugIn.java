package org.esa.snap.wvcci.tcwv.dataio.mod35;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * PlugIn class which provides a MODIS MOD35 L2 cloud mask HDF product reader to the framework.
 *
 * @author Olaf Danne
 */
public class ModisMod35L2ProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String FORMAT_NAME_MODIS35 = "MOD35-L2";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "MODIS35 Format";
    private static final String FILE_EXTENSION = ".hdf";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_MODIS35};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ModisMod35L2ProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], FILE_EXTENSION, DESCRIPTION);
    }

    /**
     * Returns the input object as file
     *
     * @param input - the input
     * @return file
     */
    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputHdfFileNameValid(inputFile.getName());
    }

    private boolean isInputHdfFileNameValid(String fileName) {
        // e.g.
//        MOD35_L2.A2005180.1455.006.2012278173052.hdf
//        MOD35_L2.A2014157.0910.005.2014157192917.hdf
        return (fileName.matches("MOD35_L2.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}.(?i)(hdf)"));
    }

}
