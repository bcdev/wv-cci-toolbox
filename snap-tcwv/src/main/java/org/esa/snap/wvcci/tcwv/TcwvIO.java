package org.esa.snap.wvcci.tcwv;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Input/Output related methods for TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvIO {

    public static NetcdfFile getTcwvLookupTableNcFile(String lutFileName) throws IOException {
        // todo: read all LUTs as auxdata !!!
        final URL resource = TcwvLandLut.class.getResource(lutFileName);
        if (resource == null) {
            // todo: get rid of this!
            System.out.println("WARNING: NetCDF file '" + lutFileName + "' does not exist in test resources." +
                                       " Test will be ignored.");
            System.out.println("This large file shall not be committed to GitHub repository!");
            System.out.println("Get it from CAWA and copy manually to " +
                                       "../wv-cci-toolbox/snap-tcwv/src/main/resources/org/esa/snap/wvcci/tcwv," +
                                       " but make sure not to add it to GitHub!");
        }
        return NetcdfFile.open(resource.getPath());
    }

    public static TcwvOceanLut getTcwvOceanLut(NetcdfFile lutNcFile) throws IOException {
//        final List<Attribute> globalAttributes = lutNcFile.getGlobalAttributes();
//        final List<Dimension> dimensions = lutNcFile.getDimensions();
        final List<Variable> variables = lutNcFile.getVariables();

        final Variable wvcVariable = variables.get(0);
        final Variable aotVariable = variables.get(1);
        final Variable wspVariable = variables.get(2);
        final Variable aziVariable = variables.get(3);
        final Variable vieVariable = variables.get(4);
        final Variable suzVariable = variables.get(5);
        final Variable jacoVariable = variables.get(6);
        final Variable lutVariable = variables.get(7);
        final Variable jlutVariable = variables.get(8);

        final double[] wvcArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(wvcVariable);
        final double[] aotArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(aotVariable);
        final double[] wspArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(wspVariable);
        final double[] aziArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(aziVariable);
        final double[] vieArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(vieVariable);
        final double[] suzArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(suzVariable);
        final int[] jacoArray = TcwvInterpolationUtils.getInt1DArrayFromNetcdfVariable(jacoVariable);
        // 6*6*11*11*9*9*3
        final double[][][][][][][] lutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(lutVariable);
        // 6*6*11*11*9*9*18
        final double[][][][][][][] jlutArray = TcwvInterpolationUtils.getDouble7DArrayFromNetcdfVariable(jlutVariable);

        return new TcwvOceanLut(wvcArray, aotArray, aziArray, vieArray, suzArray, jacoArray, lutArray, jlutArray, wspArray);

    }
}
