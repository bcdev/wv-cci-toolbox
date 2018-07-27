package org.esa.snap.wvcci.tcwv;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.wvcci.tcwv.interpolation.TcwvInterpolationUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Input/Output related methods for TCWV retrieval.
 *
 * @author olafd
 */
public class TcwvIO {

    /**
     * Installs auxiliary data (i.e. lookup tables for desmiling).
     *
     * @return - the auxdata path for O2 correction
     * @throws IOException -
     */
    static Path installAuxdata() throws IOException {
        Path auxdataDirectory = SystemUtils.getAuxDataPath().resolve("wvcci");
        final Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(TcwvOp.class).resolve("auxdata/luts");
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }


    public static TcwvOceanLut readOceanLookupTable(String auxdataPath, Sensor sensor) {
        final NetcdfFile ncFile;
        try {
            ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath, sensor.getOceanLutName());
            return TcwvIO.getTcwvOceanLut(ncFile);
        } catch (IOException e) {
            throw new OperatorException("Cannot read ocean LUT for sensor '" + sensor.getName() + "'.");
        }
    }

    public static TcwvLandLut readLandLookupTable(String auxdataPath, Sensor sensor) {
        final NetcdfFile ncFile;
        try {
            ncFile = TcwvIO.getTcwvLookupTableNcFile(auxdataPath, sensor.getLandLutName());
            return TcwvIO.getTcwvLandLut(ncFile);
        } catch (IOException e) {
            throw new OperatorException("Cannot read land LUT for sensor '" + sensor.getName() + "'.");
        }
    }


    public static NetcdfFile getTcwvLookupTableNcFile(String auxdataPath, String lutFileName) throws IOException {
        return NetcdfFile.open(auxdataPath + File.separator + lutFileName);
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


        // todo: prs array seems to be totally wrong in MERIS Land LUT. Report to RP!
        for (int i = 0; i < prsArray.length; i++) {
            // for testing, mirror into negative axis. This should be ok as it gives same fractional indices.
            prsArray[i] = -prsArray[i];
        }

        return new TcwvLandLut(wvcArray, aotArray, aziArray, vieArray, suzArray,
                               jacoArray, lutArray, jlutArray, al0Array, al1Array, prsArray, tmpArray);
    }
}
