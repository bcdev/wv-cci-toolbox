__author__ = 'olafd'

# Further improves TCWV L2 nc product, i.e. adds 1D scan_time variable as requested by T. Trent, Univ of Leicester.
# Usage: nc-compliance-tcwv-l2-process.py ./${nc_infile} ${sensor} ${product version}


import os
import sys
import time
import datetime
import uuid
import numpy as np
import netCDF4
import calendar

from datetime import datetime

from netCDF4 import Dataset

###########################################################
def getNumDaysInMonth(year, month):
    return calendar.monthrange(int(year), int(month))[1]
###########################################################


########## initialize input parameters ######################
if len(sys.argv) != 4:
    print ('Usage:  python nc-compliance-tcwv-l2-process.py <nc_infile> <sensor> <product version>')
    sys.exit(-1)

nc_infile = sys.argv[1]
sensor = sys.argv[2]
version = sys.argv[3]

print ('nc_infile: ', nc_infile)
print ('sensor: ', sensor)
print ('version: ', version)

if sensor == 'meris':
    # e.g. L2_of_L2_of_MER_RR__1PRACR20110116_112159_000026023098_00296_46431_0000_era-interim.nc
    start_index = nc_infile.find("MER_RR__") + 14
    year = nc_infile[start_index:start_index+4]
    month = nc_infile[start_index+4:start_index+6]
    day = nc_infile[start_index+6:start_index+8]
    hour = nc_infile[start_index+9:start_index+11]
    min = nc_infile[start_index+11:start_index+13]
    sec = nc_infile[start_index+13:start_index+15]
    datestring = nc_infile[start_index:start_index+15]
    print ('datestring: ', datestring)

nc_outfile = 'ESACCI-WATERVAPOUR-L2-TCWV-' + sensor + '-'  + datestring + '-fv' + version + '.nc'

print ('nc_infile: ', nc_infile)
print ('nc_outfile: ', nc_outfile)
outpath = './' + nc_outfile
print ('outpath: ', outpath)

############# set global attributes to destination file #######################
with Dataset(nc_infile) as src, Dataset(outpath, 'w', format='NETCDF4') as dst:

    if sensor == 'meris':
        # e.g. L2_of_L2_of_MER_RR__1PRACR20110116_112159_000026023098_00296_46431_0000_era-interim.nc
        # start_date = "16-JAN-2011 11:21:59.126000" ;
        # stop_date = "16-JAN-2011 12:05:23.748387" ;

        source = 'MERIS RR L1B 3rd Reprocessing'

        start_date = src.getncattr('start_date')        
        stop_date = src.getncattr('stop_date')        
        starttime = datetime.strptime(start_date, "%d-%b-%Y %H:%M:%S.%f")        
        stoptime = datetime.strptime(stop_date, "%d-%b-%Y %H:%M:%S.%f")
        time1970 = datetime(1970, 1, 1)
        print ('start_date: ', start_date)
        print ('stop_date: ', stop_date)
        start_since_1970 = (starttime-time1970).total_seconds()
        stop_since_1970 = (stoptime-time1970).total_seconds()
        print ('starttime: ', start_since_1970)
        print ('stoptime: ', stop_since_1970)
    elif sensor == 'olci':
        sys.exit(0)
    elif sensor == 'modis_terra':
        source = 'MODIS MOD021KM L1b'
        sys.exit(0)
    else:
        sys.exit(0)
        
    # set subset of global attributes following CF and CCI standards:
    dst.setncattr('title', 'Water Vapour CCI Total Column of Water Vapour L2 Product')
    dst.setncattr('institution', 'Brockmann Consult GmbH')
    dst.setncattr('source', source)
    dst.setncattr('product_version', version)
    dst.setncattr('summary', 'Water Vapour CCI TCWV Version')
    dst.setncattr('id', nc_outfile)
    dst.setncattr('naming-authority', 'brockmann-consult.de')
    dst.setncattr('comment', 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ESA Climate Change Initiative Extension (CCI+) Phase 2')
    
    from datetime import datetime, timedelta
    date_created = str(datetime.utcnow())[:19] + ' UTC'
    dst.setncattr('date_created', date_created)
    dst.setncattr('creator_name', 'Brockmann Consult GmbH')
    dst.setncattr('creator_url', 'www.brockmann-consult.de')
    dst.setncattr('creator_email', 'info@brockmann-consult.de')
    dst.setncattr('project', 'WV_cci')
    dst.setncattr('time_coverage_start', start_date)
    dst.setncattr('time_coverage_end', stop_date)
    dst.setncattr('key_variables', 'tcwv')
    # metadata_profile is needed by SNAP in order to allow reading as Netcdf-BEAM and display TPGs and masks properly
    dst.setncattr('metadata_profile', 'beam')
    dst.setncattr('metadata_version', '0.5')

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    width = len(dst.dimensions['x'])
    height = len(dst.dimensions['y'])
    print('width: ', width)
    print('height: ', height)

    scantime_incr = (stop_since_1970 - start_since_1970)*1.0 / (len(dst.dimensions['y']) - 1)
    scantime_arr = np.arange(start_since_1970, stop_since_1970 + scantime_incr, scantime_incr)
    scan_time = dst.createVariable('scan_time', np.float64, ('y'), zlib=True)
    scan_time[:] = scantime_arr
    scan_time.setncattr('long_name', 'Across-track scan time')
    scan_time.setncattr('standard_name', 'scan_time')
    scan_time.setncattr('units', 'Seconds since 1970-01-01')
        
    # copy variables with attributes from source product:
    for name, variable in src.variables.iteritems():
        print ('variable.dimensions: ', name, variable.dimensions)
        #if name.find("metadata") == -1:
        dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
        for attr in variable.ncattrs():
            if attr in dstvar.ncattrs():
                dstvar.delncattr(attr)
            dstvar.setncattr(attr, getattr(variable, attr))

    # copy variable data from source product
    for variable in dst.variables:
        print ('dst variable: ', variable)
        if variable.find("scan_time") == -1:
            if variable.find("metadata") != -1:
                dst.variables[variable] = src.variables[variable]
            elif variable.find("TCWV_") != -1 or variable.find("IDEPIX_") != -1:
                dst.variables[variable][:] = src.variables[variable][:]
            else:
                dst.variables[variable][:,:] = src.variables[variable][:,:]


