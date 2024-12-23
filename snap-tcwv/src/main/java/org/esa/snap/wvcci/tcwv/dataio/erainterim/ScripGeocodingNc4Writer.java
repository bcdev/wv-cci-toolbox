package org.esa.snap.wvcci.tcwv.dataio.erainterim;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.*;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

/**
 * Generates SCRIP conformant NetCDF file with the pixel geocoding of the product
 * (now NetCDF4, 20220902).
 *
 * <pre>
     netcdf ENV_ME_1_RRG____20110504T083152_20110504T091545_________________2633_102_122______DSI_R_NT____.SEN3-scrip {
     dimensions:
            grid_size = 16771281 ;
            grid_ny = 14961 ;
            grid_nx = 1121 ;
            grid_corners = 4 ;
            grid_rank = 2 ;
     variables:
            int grid_dims(grid_rank) ;
            float grid_center_lat(grid_ny, grid_nx) ;
            string grid_center_lat:units = "degrees" ;
            float grid_center_lon(grid_ny, grid_nx) ;
            string grid_center_lon:units = "degrees" ;
            int grid_imask(grid_ny, grid_nx) ;
            float grid_corner_lat(grid_ny, grid_nx, grid_corners) ;
            float grid_corner_lon(grid_ny, grid_nx, grid_corners) ;

     // global attributes:
            string :title = "geo-location in SCRIP format" ;
     }
 * </pre>
 *
 * @author Martin Boettcher, Olaf Danne
 */
public class ScripGeocodingNc4Writer extends AbstractProductWriter {

    ScripGeocodingNc4Writer(ProductWriterPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        String outputPath;
        if (getOutput() instanceof String) {
            outputPath = (String) getOutput();
        } else if (getOutput() instanceof File) {
            outputPath = ((File) getOutput()).getPath();
        } else {
            throw new IllegalArgumentException("output " + getOutput() + " neither String nor File");
        }

        // now NetCDF4:
        NetcdfFileWriter geoFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4_classic, outputPath);

        int height = getSourceProduct().getSceneRasterHeight();
        int width = getSourceProduct().getSceneRasterWidth();
        geoFile.addDimension(null, "grid_size", height * width);
        geoFile.addDimension(null, "grid_ny", height);
        geoFile.addDimension(null, "grid_nx", width);
        geoFile.addDimension(null, "grid_corners", 4);
        geoFile.addDimension(null, "grid_rank", 2);

        final Variable gridDims = geoFile.addVariable(null, "grid_dims", DataType.INT, "grid_rank");
        final Variable gridCenterLat = geoFile.addVariable(null, "grid_center_lat", DataType.FLOAT, "grid_ny grid_nx");
        final Variable gridCenterLon = geoFile.addVariable(null, "grid_center_lon", DataType.FLOAT, "grid_ny grid_nx");
        final Variable gridMask = geoFile.addVariable(null, "grid_imask", DataType.INT, "grid_ny grid_nx");
        final Variable gridCornerLat = geoFile.addVariable(null, "grid_corner_lat", DataType.FLOAT, "grid_ny grid_nx grid_corners");
        final Variable gridCornerLon = geoFile.addVariable(null, "grid_corner_lon", DataType.FLOAT, "grid_ny grid_nx grid_corners");
        gridCenterLat.addAttribute(new Attribute("units", "degrees"));
        gridCenterLon.addAttribute(new Attribute("units", "degrees"));
//        geoFile.addGroupAttribute(null, new Attribute("title", "geo-location in SCRIP format"));

        geoFile.create();
        try {
            // grid_dims

            // this does no longer work with netcdfAll_5.3.1 (OD, 20220902):
            //geoFile.write(gridDims, Array.factory(new int[]{width, height}));

            // use this instead:
            ArrayInt ai = new ArrayInt.D1(2, false);
            ai.setInt(ai.getIndex().set(0), width);
            ai.setInt(ai.getIndex().set(1), height);
            geoFile.write(gridDims, ai);

            final int[] targetStart = {0, 0};
            final int[] targetStart2 = {0, 0, 0};
            final int[] targetShape = {height, width};
            final int[] targetShape2 = {height, width, 4};
            final Array maskData = Array.factory(DataType.INT, targetShape);
            final Array centreLat = Array.factory(DataType.FLOAT, targetShape);
            final Array centreLon = Array.factory(DataType.FLOAT, targetShape);
            final Array cornerLat = Array.factory(DataType.FLOAT, targetShape2);
            final Array cornerLon = Array.factory(DataType.FLOAT, targetShape2);

            GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
            GeoPos geoPos = new GeoPos();
            Index2D index = (Index2D) centreLat.getIndex();
            Index3D[] cornerIndex = new Index3D[] { (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex() };
            GeoPos[] cornerPos = new GeoPos[] { new GeoPos(), new GeoPos(), new GeoPos(), new GeoPos() };
            for (int y=0; y<height; ++y) {
                for (int x=0; x<width; ++x) {
                    geoCoding.getGeoPos(new PixelPos(x+0.5f, y+0.5f), geoPos);
                    geoCoding.getGeoPos(new PixelPos((float) x, (float) y), cornerPos[0]);
                    geoCoding.getGeoPos(new PixelPos((float) x+1.0f, (float) y), cornerPos[1]);
                    geoCoding.getGeoPos(new PixelPos((float) x-1.0f, (float) y+1.0f), cornerPos[2]);
                    geoCoding.getGeoPos(new PixelPos((float) x, (float) y+1.0f), cornerPos[3]);
                    index.set(y, x);
                    if (isValid(geoPos)) {
                        centreLat.setFloat(index, (float) geoPos.getLat());
                        centreLon.setFloat(index, (float) geoPos.getLon());
                        maskData.setInt(index, 1);
                    } else {
                        centreLat.setFloat(index, Float.NaN);
                        centreLon.setFloat(index, Float.NaN);
                        maskData.setInt(index, 0);
                    }
                    for (int corner = 0; corner < 4; ++corner) {
                        cornerIndex[corner].set(y, x, corner);
                        if (isValid(cornerPos[corner])) {
                            cornerLat.setFloat(cornerIndex[corner], (float) cornerPos[corner].getLat());
                            cornerLon.setFloat(cornerIndex[corner], (float) cornerPos[corner].getLat());
                        } else {
                            cornerLat.setFloat(cornerIndex[corner], Float.NaN);
                            cornerLon.setFloat(cornerIndex[corner], Float.NaN);
                        }
                    }
                }
            }
            geoFile.write(gridCenterLat, targetStart, centreLat);
            geoFile.write(gridCenterLon, targetStart, centreLon);
            geoFile.write(gridMask, targetStart, maskData);
            geoFile.write(gridCornerLat, targetStart2, cornerLat);
            geoFile.write(gridCornerLon, targetStart2, cornerLon);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                geoFile.close();
            } catch (IOException ignored) {
            }
        }


    }

    private boolean isValid(GeoPos geoPos) {
        return geoPos.getLat() >= -90.0 && geoPos.getLat() <= 90.0 &&
            geoPos.getLon() >= -180.0 && geoPos.getLon() <= 180.0;
    }

    @Override
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) {

    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void deleteOutput() {
        File outputFile;
        if (getOutput() instanceof String) {
            outputFile = new File((String) getOutput());
        } else if (getOutput() instanceof File) {
            outputFile = (File) getOutput();
        } else {
            throw new IllegalArgumentException("output " + getOutput() + " neither String nor File");
        }
        outputFile.delete();
    }
}
