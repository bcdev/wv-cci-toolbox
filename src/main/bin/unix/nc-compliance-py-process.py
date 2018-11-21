__author__ = 'olafd'

# Adds 'time' as 3rd dimension in 2D NetCDF input raster file. On request of AL, 20170215
# ga-l3-albedo-mosaic-timedim-python.py ${sensorID} ${year} ${iDoy} ${res} ${snowMode} ${albedoSourceDir} ${albedoTimedimDir}

import os
import sys
import time
import datetime
import netCDF4
import gzip
import shutil

from netCDF4 import Dataset

if len(sys.argv) != 5:
    print ('Usage:  python wvcci-nc-compliance.py <nc_infile> <year> <month> <day>')
    sys.exit(-1)

nc_infile = sys.argv[1]
year = sys.argv[2]
month = sys.argv[3]
day = sys.argv[4]

print ('nc_infile: ', nc_infile)
print ('day: ', day)
print ('month: ', month)
print ('year: ', year)

# use days since 1970-01-01 as time value:
timeval = (datetime.datetime(int(year),int(month),int(day))  - datetime.datetime(1970,1,1)).days

#outfile = nc_outdir + '/test.nc'
nc_outfile = nc_infile.replace("L2_of_l3_", "WV-CCI_L3_")
outpath = './' + nc_outfile
#with Dataset(nc_infile) as src, Dataset(outfile, 'w', format='NETCDF4') as dst:
with Dataset(nc_infile) as src, Dataset(outpath, 'w', format='NETCDF4') as dst:
    # set new time dimension:
    time = 1
    dst.createDimension('time', None)

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    # set global attributes from src
    for attr in src.ncattrs():
        dst.setncattr(attr, getattr(src, attr))

    # set variable attributes from src
    for name, variable in src.variables.iteritems():
        if name != 'metadata':
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            for attr in variable.ncattrs():
                dstvar.setncattr(attr, getattr(variable, attr))

    # set variable data from src
    for variable in dst.variables:
        print ('variable: ', variable)
        if variable != 'crs':
            dst.variables[variable][:,:] = src.variables[variable][:,:]
        else:
            dst.variables[variable][:] = src.variables[variable][:]

    # set time data
    time = dst.createVariable('time', 'i4', ('time'), zlib=True)
    time.setncattr('long_name', 'Product dataset time given as days since 1970-01-01')
    time.setncattr('standard_name', 'time')
    time.setncattr('units', 'days since 1970-01-01')
    time[:] = int(timeval)

