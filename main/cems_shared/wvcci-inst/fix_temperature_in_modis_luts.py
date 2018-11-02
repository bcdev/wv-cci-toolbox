import sys
import time
import datetime
import netCDF4

from netCDF4 import Dataset

###########################################################################################################
# fix variable in nc4 LUTs for MODIS:
# we have: 1D one-element array [10.0] --> wrong and meaningless, also not accepted by SNAP LookupTable API
# we want: 1D 3-element array [263.13, 288.13, 313.13] as for MERIS
###########################################################################################################

infile = sys.argv[1]
outfile = sys.argv[2]

names_1dim = ['tmp']

with Dataset(infile) as src, Dataset(outfile, 'w', format='NETCDF4') as dst:

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        if name in names_1dim:
	    dst.createDimension(name, 3)
	else:
	    dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    # set global attributes from src
    for attr in src.ncattrs():
        dst.setncattr(attr, getattr(src, attr))

    # set variable data from src
    for name,srcvar in src.variables.iteritems():
        print 'name: ', name

        if name in names_1dim:
            dstvar = dst.createVariable(name, srcvar.datatype, (name), zlib=True)
            for attr in srcvar.ncattrs():
                dstvar.setncattr(attr, getattr(srcvar, attr))

            print 'srcvar: ', srcvar[:]
            dstvar[0] = 263.13
            dstvar[1] = 283.13
            dstvar[2] = 313.13
        elif name == 'lut' or name == 'jlut':
            dstvar = dst.createVariable(name, srcvar.datatype, srcvar.dimensions, zlib=True)
            for attr in srcvar.ncattrs():
                dstvar.setncattr(attr, getattr(srcvar, attr))

            print 'writing: ', name, ' // ', dstvar.dimensions
            dstvar[:,:,:,:,:,0,:,:,:] = srcvar[:,:,:,:,:,0,:,:,:]
            dstvar[:,:,:,:,:,1,:,:,:] = srcvar[:,:,:,:,:,0,:,:,:]
        else:
            dstvar = dst.createVariable(name, srcvar.datatype, srcvar.dimensions, zlib=True)
            for attr in srcvar.ncattrs():
                dstvar.setncattr(attr, getattr(srcvar, attr))

            dstvar[:] = srcvar[:]
 
print 'done'

