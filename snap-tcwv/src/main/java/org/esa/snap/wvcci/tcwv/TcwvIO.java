package org.esa.snap.wvcci.tcwv;

import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolationUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
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

    public static TcwvLandLut getTcwvLandLut(NetcdfFile lutNcFile) throws IOException {
        final List<Variable> variables = lutNcFile.getVariables();

        final double[] wvcArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(0));
        final double[] al0Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(1));
        final double[] al1Array = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(2));
        final double[] aotArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(3));
        final double[] prsArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(4));
        final double[] tmpArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(5));
        final double[] aziArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(6));
        final double[] vieArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(7));
        final double[] suzArray = TcwvInterpolationUtils.getDouble1DArrayFromNetcdfVariable(variables.get(8));
        final int[] jacoArray = TcwvInterpolationUtils.getInt1DArrayFromNetcdfVariable(variables.get(9));
        // 5 * 5 * 5 * 5 * 3 * 3 * 6 * 5 * 5 * 3
        final double[][][][][][][][][][] lutArray =
                TcwvInterpolationUtils.getDouble10DArrayFromNetcdfVariable(variables.get(10));
        // 5 * 5 * 5 * 5 * 3 * 3 * 6 * 5 * 5 * 27
        final double[][][][][][][][][][] jlutArray =
                TcwvInterpolationUtils.getDouble10DArrayFromNetcdfVariable(variables.get(11));


        Arrays.sort(prsArray);   // todo: prs array seems to be totally wrong in MERIS Land LUT. Report to RP!
//        for (int i = 0; i < prsArray.length; i++) {
//            prsArray[i] = 1./prsArray[i];   // for testing, invert to make ascending
//        }
        // obviously we have a fractional index of 0.0 anyway, so it does not matter how we make the axis ascending,
        // as long as we are out of range anyway.
        // if we are inside range, we need to map descending axis to ascending axis, with keeping the fractional
        // index!

        return new TcwvLandLut(wvcArray, aotArray, aziArray, vieArray, suzArray,
                               jacoArray, lutArray, jlutArray, al0Array, al1Array, prsArray, tmpArray);
    }
}
