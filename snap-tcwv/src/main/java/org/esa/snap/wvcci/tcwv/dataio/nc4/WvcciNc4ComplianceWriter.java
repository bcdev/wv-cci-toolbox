package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.apache.commons.cli.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides products in final WV-CCI NetCDF4 format, i.e.:
 *  - Adds a time dimension and sets time value to days since 1970-01-01
 *  - Sets global attributes for CF and CCI compliance
 *  - Sets variable attributes for CF and CCI compliance
 *
 * @author olafd
 */
public class WvcciNc4ComplianceWriter {

    private String sourceFilePath;
    private File targetDir;

    private final String year;
    private final String month;
    private final String day;

    /**
     * TODO
     *
     * @param sourceFilePath
     * @param targetDir
     * @param year
     * @param month
     * @param day
     */
    WvcciNc4ComplianceWriter(String sourceFilePath, File targetDir, String year, String month, String day) {
        this.sourceFilePath = sourceFilePath;
        this.targetDir = targetDir;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /**
     *
     *
     * @param srcDateString
     * @return
     * @throws java.text.ParseException
     */
    static int getDaysSince1970(String srcDateString) throws java.text.ParseException {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd");
        String refString = "1970-01-01";

        Date srcDate = myFormat.parse(srcDateString);
        Date refDate = myFormat.parse(refString);
        long diff = srcDate.getTime() - refDate.getTime();
        return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    public void execute() throws IOException, InvalidRangeException, java.text.ParseException {
        final NetcdfFile ncSourceFile = NetcdfFile.open(sourceFilePath);
        final String sourceFileName = new File(sourceFilePath).getName();
        final String targetFilePath = targetDir + File.separator + sourceFileName;
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, targetFilePath);

        // copy dimensions, add time dimension
        final List<Dimension> srcDimensions = ncSourceFile.getDimensions();
        for (Dimension srcDimension : srcDimensions) {
            writer.addDimension(null, srcDimension.getFullName(), srcDimension.getLength());
        }
        writer.addUnlimitedDimension("time");


        // add variables
        final List<Variable> variablesSrc = ncSourceFile.getVariables();
        for (Variable varSrc : variablesSrc) {
            Variable varTarget = writer.addVariable(null,
                                                    varSrc.getShortName(),
                                                    varSrc.getDataType(),
                                                    varSrc.getDimensions());
            final List<Attribute> varSrcAttributes = varSrc.getAttributes();
            for (Attribute varSrcAttribute : varSrcAttributes) {
                varTarget.addAttribute(varSrcAttribute);
            }
        }

        Variable time = writer.addVariable(null, "time", DataType.INT, "time");
        time.addAttribute(new Attribute("units", "hours since 1990-01-01"));

        // global attributes
        final List<Attribute> globalAttributes = ncSourceFile.getGlobalAttributes();
        for (Attribute globalAttribute : globalAttributes) {
            writer.addGroupAttribute(null, globalAttribute);
        }

        writer.create();

        // add variable data
        for (Variable varSrc : variablesSrc) {
            Variable varTarget = writer.findVariable(varSrc.getFullNameEscaped());
            final Array arraySrc = varSrc.read();
            writer.write(varTarget, arraySrc);
        }

        final String sourceDateString = year + "-" + month + "-" + day;
        Array timeData = Array.factory(DataType.INT, new int[]{1});
        timeData.setInt(0, getDaysSince1970(sourceDateString));
        writer.setRedefineMode(false);
        writer.write(time, timeData);
        writer.setRedefineMode(true);

        writer.close();
    }

}
