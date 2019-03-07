/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.wvcci.tcwv.dataio.erainterim;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * A product reader for era-interim data in GRIB format.
 */
public class EraInterimProductReader extends AbstractProductReader {

    private NetcdfFile netcdfFile;


    EraInterimProductReader(ProductReaderPlugIn readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        CfNetCdfReaderPlugIn plugIn = new CfNetCdfReaderPlugIn();
        netcdfFile = EraInterimProductReaderPlugin.openGrib(getInput());
        if (netcdfFile == null) {
            throw new IOException("Failed to open file ");
        }

        final File fileLocation = new File(getInput().toString());
        final ProfileReadContext ctx = new ProfileReadContextImpl(netcdfFile);
        ctx.setProperty(Constants.PRODUCT_FILENAME_PROPERTY, fileLocation.getName());
        final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest == null) {
            throw new IOException("File does not contain any bands.");
        }
        ctx.setRasterDigest(rasterDigest);

        ProfileInitPartReader profileInitPart = plugIn.createInitialisationPartReader();
        List<ProfilePartReader> profileParts = new ArrayList<>();
        profileParts.add(plugIn.createMetadataPartReader());
        profileParts.add(plugIn.createBandPartReader());
        profileParts.add(plugIn.createTiePointGridPartReader());
        profileParts.add(plugIn.createFlagCodingPartReader());
        profileParts.add(plugIn.createGeoCodingPartReader());
        profileParts.add(plugIn.createImageInfoPartReader());
        profileParts.add(plugIn.createIndexCodingPartReader());
        profileParts.add(plugIn.createMaskPartReader());
        profileParts.add(plugIn.createStxPartReader());
        profileParts.add(plugIn.createTimePartReader());
        profileParts.add(plugIn.createDescriptionPartReader());
        final Product product = profileInitPart.readProductBody(ctx);
        AbstractProductReader.configurePreferredTileSize(product);
        for (ProfilePartReader profilePart : profileParts) {
            profilePart.preDecode(ctx, product);
        }
        for (ProfilePartReader profilePart : profileParts) {
            profilePart.decode(ctx, product);
        }
        String name = fileLocation.getName();
        try {
            // era_interim_yyyyMMdd.grib
            int profixLength = "era_interim_".length();
            String dateString = name.substring(profixLength, profixLength + 8);
            ProductData.UTC productUTC = ProductData.UTC.parse(dateString, "yyyyMMdd");
            product.setStartTime(productUTC);
            Calendar asCalendar = productUTC.getAsCalendar();
            asCalendar.add(Calendar.DAY_OF_MONTH, 1);
            product.setEndTime(ProductData.UTC.create(asCalendar.getTime(), 0L));
        } catch (ParseException ignore) {
            Debug.trace(ignore);
        }
        product.setFileLocation(fileLocation);
        product.setProductReader(this);
        product.setModified(false);
        return product;

    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) {
        throw new IllegalStateException("Data is provided by images");
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

    private static class ProfileReadContextImpl implements ProfileReadContext {

        private final Map<String, Object> propertyMap;
        private final NetcdfFile netcdfFile;

        private RasterDigest rasterDigest;

        ProfileReadContextImpl(NetcdfFile netcdfFile) {
            this.netcdfFile = netcdfFile;
            this.propertyMap = new HashMap<>();
        }


        @Override
        public void setRasterDigest(RasterDigest rasterDigest) {
            this.rasterDigest = rasterDigest;
        }

        @Override
        public RasterDigest getRasterDigest() {
            return rasterDigest;
        }

        @Override
        public void setProperty(String name, Object value) {
            propertyMap.put(name, value);
        }

        @Override
        public Object getProperty(String name) {
            return propertyMap.get(name);
        }

        @Override
        public NetcdfFile getNetcdfFile() {
            return netcdfFile;
        }

    }
}
