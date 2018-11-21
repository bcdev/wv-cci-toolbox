package org.esa.snap.wvcci.tcwv.dataio.nc4;

import org.apache.commons.cli.*;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;

/**
 * Provides products in final WV-CCI NetCDF4 format, i.e.:
 *  - Adds a time dimension and sets time value to days since 1970-01-01
 *  - Sets global attributes for CF and CCI compliance
 *  - Sets variable attributes for CF and CCI compliance
 *
 *  TODO: THIS DOES NOT YET WORK ON CALVALUS (Netcdf/HDF problem). Using Python version (nc-compliance-py-*) instead.
 *
 * @author olafd
 */
public class WvcciNc4ComplianceWriterMain {

    private static final String TOOL_NAME = "wvcci_add_timedim_nc";

    private static final File DEFAULT_OUTPUT_DIR = new File(".");
    private static final Option OPT_OUTPUT_DIR = OptionBuilder
            .hasArg()
            .withArgName("outputDir")
            .withLongOpt("output-dir")
            .withDescription("The output directory path (default is current directory).")
            .create("o");
    private static final Option OPT_HELP = OptionBuilder
            .withLongOpt("help")
            .withDescription("Prints out this usage help.")
            .create();

    private Options options;

    private String sourceFilePath;
    private File targetDir;

    private final String year;
    private final String month;
    private final String day;

    /**
     * TODO
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            new WvcciNc4ComplianceWriterMain(args);
        } catch (java.text.ParseException | InvalidRangeException | IOException e) {
            e.printStackTrace();
        }
    }

    private WvcciNc4ComplianceWriterMain(String[] args) throws java.text.ParseException, InvalidRangeException, IOException {
        CommandLine commandLine = null;

        options = createCommandLineOptions();

        try {
            commandLine = parseCommandLine(args);
        } catch (ParseException e) {
            System.out.println("ERROR: " + e.getMessage() + " (use option '-h' for help)");
            System.exit(-1);
        }

        if (commandLine.hasOption("help")) {
            printHelp();
            System.exit(0);
        }

        extractCommandLineInput(commandLine);
        sourceFilePath = commandLine.getArgs()[0];
        year = commandLine.getArgs()[1];
        month = commandLine.getArgs()[2];
        day = commandLine.getArgs()[3];

        validateSourceProduct(sourceFilePath);

        WvcciNc4ComplianceWriter complianceWriter =
                new WvcciNc4ComplianceWriter(sourceFilePath, targetDir, year, month, day);
        complianceWriter.execute();
    }

    private void validateSourceProduct(String sourceFilePath) {
        if (!sourceFilePath.endsWith(".nc") && !sourceFilePath.endsWith(".nc4")) {
            System.out.println("Input file '" + sourceFilePath + "' does not seem to be a NetCDF file - exiting.");
            System.exit(1);
        }
        // todo: more checks?
    }

    @SuppressWarnings({"AccessStaticViaInstance"})
    private static Options createCommandLineOptions() {
        Options options = new Options();

        // argument options
        options.addOption(OPT_HELP);
        options.addOption(OPT_OUTPUT_DIR);

        return options;
    }

    private CommandLine parseCommandLine(String... args) throws ParseException {
        Parser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private void extractCommandLineInput(CommandLine cl) {
        setTargetFilePath(cl);
    }

    private void setTargetFilePath(CommandLine cl) {
        targetDir = DEFAULT_OUTPUT_DIR;
        if (cl.hasOption(OPT_OUTPUT_DIR.getOpt())) {
            targetDir = new File(cl.getOptionValue(OPT_OUTPUT_DIR.getOpt()));
        }
        if (!targetDir.isDirectory()) {
            System.out.println("ERROR: The given output directory '" + targetDir.getPath() + "' is not a directory.");
            System.exit(1);
        }
    }

    private void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String argString = TOOL_NAME + " ncSrcFilePath year month day";
        helpFormatter.printHelp(argString, options, true);
    }

}
