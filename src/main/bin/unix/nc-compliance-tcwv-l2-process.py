__author__ = 'olafd'

# Further improves TCWV L2 nc product, i.e. adds 1D scan_time variable as requested by T. Trent, Univ of Leicester.
# Usage: python nc-compliance-tcwv-l2-process.py ./${nc_infile} ${sensor} ${year} ${month} ${day} ${product version}


import os
import sys
import time
import datetime
import uuid
import numpy as np
import netCDF4
import calendar

from netCDF4 import Dataset

###########################################################
def getNumDaysInMonth(year, month):
    return calendar.monthrange(int(year), int(month))[1]
###########################################################


########## initialize input parameters ######################
if len(sys.argv) != 7:
    print ('Usage:  python nc-compliance-tcwv-l2-process.py <nc_infile> <sensor> <year> <month> <day> <product version>')
    sys.exit(-1)

nc_infile = sys.argv[1]
sensor = sys.argv[2]
year = sys.argv[3]
month = sys.argv[4]
day = sys.argv[5]
version = sys.argv[6]

print 'nc_infile: ', nc_infile
print 'sensor: ', sensor
print 'year: ', year
print 'month: ', month
print 'day: ', day
print 'version: ', version

from datetime import datetime as dt
from datetime import timedelta

time1970 = dt(1970, 1, 1)
if sensor == 'meris':
    # e.g. L2_of_L2_of_MER_RR__1PRACR20110116_112159_000026023098_00296_46431_0000_era-interim.nc
    source = 'MERIS RR L1B 3rd Reprocessing'
    start_index = nc_infile.find("MER_RR__") + 14
    datestring = nc_infile[start_index:start_index+15]
    print 'datestring: ', datestring
elif sensor == 'olci':
    # e.g. L2_of_L2_of_S3A_OL_1_ERR____20181023T112231_20181023T120647_20181024T174418_2656_037_137______MAR_O_NT_002.SEN3_era-interim.nc
    source = ' OLCI RR L1b'
    start_index = nc_infile.find("S3A_OL_1_") + 16
    start_hour = nc_infile[start_index+25:start_index+27]
    start_min = nc_infile[start_index+27:start_index+29]
    start_sec = nc_infile[start_index+29:start_index+31]
    datestring = year + str(month).zfill(2) + str(day).zfill(2) + '_' + start_hour + start_min + start_sec
    #print ('startdatestring: ', datestring)
    stop_hour = nc_infile[start_index+41:start_index+43]
    stop_min = nc_infile[start_index+43:start_index+45]
    stop_sec = nc_infile[start_index+45:start_index+47]
    stopdatestring_yyMMMdd_hhmmss = year + str(month).zfill(2) + str(day).zfill(2) + '_' + stop_hour + stop_min + stop_sec
    #print ('stopdatestring: ', stopdatestring_yyMMMdd_hhmmss)
    start_date = dt.strptime(datestring, "%Y%m%d_%H%M%S")        
    start_since_1970 = (start_date-time1970).total_seconds()
    #print ('starttime: ', start_since_1970)
    start_date_string = start_date.strftime("%d-%b-%Y %H:%M:%S.%f")
    stop_date = dt.strptime(stopdatestring_yyMMMdd_hhmmss, "%Y%m%d_%H%M%S")        
    stop_since_1970 = (stop_date-time1970).total_seconds()
    #print ('stoptime: ', stop_since_1970)
    stop_date_string = stop_date.strftime("%d-%b-%Y %H:%M:%S.%f")
elif sensor == 'modis_terra':
    # e.g. MOD021KM.A2011069.1520.061.2017321122220_tcwv.nc
    source = 'MODIS MOD021KM L1b'
    start_index = nc_infile.find("MOD021KM") + 10
    hour = nc_infile[start_index+8:start_index+10]
    min = nc_infile[start_index+10:start_index+12]
    sec = '00'
    datestring = year + str(month).zfill(2) + str(day).zfill(2) + '_' + hour + min + sec
    #print ('datestring: ', datestring)
    start_date = dt.strptime(datestring, "%Y%m%d_%H%M%S")        
    start_since_1970 = (start_date-time1970).total_seconds()
    stop_since_1970 = (start_date-time1970).total_seconds() + 300
    stop_date = dt.utcfromtimestamp(stop_since_1970)
    #print ('starttime: ', start_since_1970)
    #print ('stoptime: ', stop_since_1970)
    #print ('start_date: ', start_date)
    #print ('stop_date: ', stop_date)
    start_date_string = start_date.strftime("%d-%b-%Y %H:%M:%S.%f")
    stop_date_string = stop_date.strftime("%d-%b-%Y %H:%M:%S.%f")
else:
    print 'sensor ' + sensor + ' not supported'
    sys.exit(1)    
    
nc_outfile = 'ESACCI-WATERVAPOUR-L2-TCWV-' + sensor + '-'  + datestring + '-fv' + version + '.nc'

print ('nc_infile: ', nc_infile)
print ('nc_outfile: ', nc_outfile)
outpath = './' + nc_outfile

############# set global attributes to destination file #######################
with Dataset(nc_infile) as src, Dataset(outpath, 'w', format='NETCDF4') as dst:
    if sensor == 'meris':
        # e.g. L2_of_L2_of_MER_RR__1PRACR20110116_112159_000026023098_00296_46431_0000_era-interim.nc
        # start_date = "16-JAN-2011 11:21:59.126000" ;
        # stop_date = "16-JAN-2011 12:05:23.748387" ;
        start_date_string = src.getncattr('start_date')        
        stop_date_string = src.getncattr('stop_date')        
        starttime = dt.strptime(start_date_string, "%d-%b-%Y %H:%M:%S.%f")        
        stoptime = dt.strptime(stop_date_string, "%d-%b-%Y %H:%M:%S.%f")
        start_since_1970 = (starttime-time1970).total_seconds()
        stop_since_1970 = (stoptime-time1970).total_seconds()
        #print ('starttime: ', start_since_1970)
        #print ('stoptime: ', stop_since_1970)

    print ('start_date_string: ', start_date_string)
    print ('stop_date_string: ', stop_date_string)
        
    # set subset of global attributes following CF and CCI standards:
    dst.setncattr('title', 'Water Vapour CCI Total Column of Water Vapour L2 Product')
    dst.setncattr('institution', 'Brockmann Consult GmbH')
    dst.setncattr('source', source)
    dst.setncattr('product_version', version)
    dst.setncattr('summary', 'Water Vapour CCI TCWV Version')
    dst.setncattr('id', nc_outfile)
    dst.setncattr('naming-authority', 'brockmann-consult.de')
    dst.setncattr('comment', 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ESA Climate Change Initiative Extension (CCI+) Phase 2')
    
    date_created = str(dt.utcnow())[:19] + ' UTC'
    dst.setncattr('date_created', date_created)
    dst.setncattr('creator_name', 'Brockmann Consult GmbH')
    dst.setncattr('creator_url', 'www.brockmann-consult.de')
    dst.setncattr('creator_email', 'info@brockmann-consult.de')
    dst.setncattr('project', 'WV_cci')
    dst.setncattr('time_coverage_start', start_date_string)
    dst.setncattr('time_coverage_end', stop_date_string)
    dst.setncattr('key_variables', 'tcwv')
    # metadata_profile is needed by SNAP in order to allow reading as Netcdf-BEAM and display TPGs and masks properly
    dst.setncattr('metadata_profile', 'beam')
    dst.setncattr('metadata_version', '0.5')

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    scantime_incr = (stop_since_1970 - start_since_1970)*1.0 / (len(dst.dimensions['y']) - 1)
    scantime_arr = np.arange(start_since_1970, stop_since_1970 + scantime_incr/2, scantime_incr)
    scan_time = dst.createVariable('scan_time', np.float64, ('y'), zlib=True)
    scan_time[:] = scantime_arr
    scan_time.setncattr('long_name', 'Across-track scan time')
    scan_time.setncattr('standard_name', 'scan_time')
    scan_time.setncattr('units', 'Seconds since 1970-01-01')
        
    # copy variables with attributes from source product:
    for name, variable in src.variables.iteritems():
        print 'variable.dimensions: ', name, variable.dimensions
        #if name.find("metadata") == -1:
        dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
        for attr in variable.ncattrs():
            if attr in dstvar.ncattrs():
                dstvar.delncattr(attr)
            dstvar.setncattr(attr, getattr(variable, attr))

    # copy variable data from source product
    for variable in dst.variables:
        #print ('dst variable: ', variable)
        print 'dst variable: ', variable
        if variable.find("scan_time") == -1:
            if variable.find("metadata") != -1 or variable.find("_mask") != -1:
                dst.variables[variable] = src.variables[variable]
            elif variable.find("TCWV_") != -1 or variable.find("IDEPIX_") != -1:
                dst.variables[variable][:] = src.variables[variable][:]
            else:
                dst.variables[variable][:,:] = src.variables[variable][:,:]
                
                if variable.find("surface_type_flags") != -1:
                    
                    # We lost the l1_invalid pixels (MERIS, OLCI), they are currently set to ocean in 'Dataset 2' L2 retrieval.
                    # Workaround to avoid L2 reprocessing: set them to cloud with condition below, describe in
                    # attribute as 'cloud or L1 invalid'.
                    # todo: fix in TCWV L2 retrieval, introduce a new flag value (invalid, unknown, 'not set' etc.)

                    # we modify the 'TCWV invalid' pixels (== 4) which should not be invalid as they are
                    # over land, ocean, sea ice, but not cloudy (< 8). These remaining pixels must be l1_invalid
                    # dst.variables[variable][(np.where(src.variables[variable] < 8)) and
                    #                         (np.where(src.variables["tcwv_quality_flags"] > 2))] = 8
                    dst.variables[variable].setncattr('comment',
                                                      'NOTE: L1 invalid pixels (no TCWV retrieval at all) are flagged as CLOUD in this product version.')
                    surface_type_flags_arr = src.variables["surface_type_flags"][:,:]
                    quality_flags_arr = src.variables["tcwv_quality_flags"][:,:]
                    corr_arr = np.where((src.variables["surface_type_flags"][:,:] < 8) & (src.variables["tcwv_quality_flags"][:,:] > 2),
                                      8,
                                      src.variables["surface_type_flags"])
                    
