__author__ = 'olafd'

# Generates final CF- and CCI-compliant TCWV products ready for delivery.
# Usage: nc-compliance-py-process.py ./${nc_infile} ${sensor} ${year} ${month} ${day} ${resolution}


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


if len(sys.argv) != 8:
    print ('Usage:  python nc-compliance-py-process.py <nc_infile> <sensor> <year> <month> <day> <resolution> < product version>')
    sys.exit(-1)

nc_infile = sys.argv[1]
sensor = sys.argv[2]
year = sys.argv[3]
month = sys.argv[4]
day = sys.argv[5]
res = sys.argv[6]
version = sys.argv[7]

print ('nc_infile: ', nc_infile)
print ('sensor: ', sensor)
print ('year: ', year)
print ('month: ', month)
print ('day: ', day)
print ('res: ', res)
print ('version: ', version)

nc_infile_root_index = nc_infile.find(year)

if int(day) == 0:
    # monthly products:
    # --> final output name e.g.: WV_CCI_L3_tcwv_meris_05deg_2011-01.nc
    # datestring = year + '-' + month
    datestring = year + month

    # use days since 1970-01-01 as time value:
    timeval = (datetime.datetime(int(year),int(month),1)  - datetime.datetime(1970,1,1)).days
else:
    # daily products:
    # --> final output name e.g.: WV_CCI_L3_tcwv_meris_05deg_2011-01-16.nc
    #datestring = year + '-' + month + '-' + day
    datestring = year + month + day

    # use days since 1970-01-01 as time value:
    timeval = (datetime.datetime(int(year),int(month),int(day))  - datetime.datetime(1970,1,1)).days

#nc_outfile = 'WV_CCI_L3_tcwv_' + sensor + '_' + res + 'deg_' + datestring + '.nc'    
if sensor.find("-") != -1:
    l3_suffix = 'S'
else:
    l3_suffix = 'C' 

sensor = sensor.replace('ssmi', 'cmsaf_hoaps')

nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + sensor + '-'  + datestring + '-' + res + 'deg-fv' + version + '.nc'

print ('nc_outfile: ', nc_outfile)
outpath = './' + nc_outfile
print ('nc_infile: ', nc_infile)
print ('outpath: ', outpath)
with Dataset(nc_infile) as src, Dataset(outpath, 'w', format='NETCDF4') as dst:

    # set global attributes following CF and CCI standards:
    dst.setncattr('title', 'Water Vapour CCI Total Column of Water Vapour Product')
    dst.setncattr('institution', 'Brockmann Consult GmbH; EUMETSAT/CMSAF')
    dst.setncattr('source', 'MERIS RR L1B 3rd Reprocessing; MODIS MOD021KM L1B; HOAPS-S version 4.0')
    dst.setncattr('history', 'python nc-compliance-py-process.py ' + nc_infile)
    dst.setncattr('references', 'WV_cci D2.2: ATBD Part 1 - MERIS-MODIS-OLCI L2 Products, Issue 1.1, 3 April 2019; WV_cci D4.2: CRDP Issue 1.0, 13 June 2019 ')
    dst.setncattr('tracking_id', str(uuid.uuid1()))
    dst.setncattr('Conventions', 'CF-1.7')
    dst.setncattr('product_version', version)
    dst.setncattr('format_version', 'CCI Data Standards v2.0')
    dst.setncattr('summary', 'Water Vapour CCI TCWV Dataset 1 (2010-2012)')
    dst.setncattr('keywords', 'EARTH SCIENCE > ATMOSPHERE > ATMOSPHERIC WATER VAPOR > WATER VAPOR,EARTH SCIENCE > ATMOSPHERE > ATMOSPHERIC WATER VAPOR > PRECIPITABLE WATER')
    dst.setncattr('id', nc_outfile)
    dst.setncattr('naming-authority', 'brockmann-consult.de')
    dst.setncattr('keywords-vocabulary', 'GCMD Science Keywords, Version 8.1')
    dst.setncattr('cdm_data_type', 'grid')
    dst.setncattr('comment', 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ESA Climate Change Initiative Extension (CCI+) Phase 1')
    
    from datetime import datetime, timedelta
    date_created = str(datetime.utcnow())[:19] + ' UTC'
    dst.setncattr('date_created', date_created)
    dst.setncattr('creator_name', 'Brockmann Consult GmbH; EUMETSAT/CMSAF')
    dst.setncattr('creator_url', 'www.brockmann-consult.de; http://www.cmsaf.eu')
    dst.setncattr('creator_email', 'info@brockmann-consult.de; contact.cmsaf@dwd.de')
    dst.setncattr('project', 'WV_cci')
    dst.setncattr('geospatial_lat_min', '-90.0')
    dst.setncattr('geospatial_lat_max', '90.0')
    dst.setncattr('geospatial_lon_min', '-180.0')
    dst.setncattr('geospatial_lon_max', '180.0')
    dst.setncattr('geospatial_vertical_min', '0.0')
    dst.setncattr('geospatial_vertical_max', '0.0')
    if int(day) == 0:
        starttime = datestring + '-01 00:00:00 UTC'
        endtime = datestring + '-' + str(getNumDaysInMonth(year, month)) +  ' 23:59:59 UTC'
    else:
        starttime = datestring + ' 00:00:00 UTC'
        endtime = datestring + ' 23:59:59 UTC'
    dst.setncattr('time_coverage_start', starttime)
    dst.setncattr('time_coverage_end', endtime)
    dst.setncattr('time_coverage_duration', 'P1D')
    dst.setncattr('time_coverage_resolution', 'P1D')
    dst.setncattr('standard_name_vocabulary', 'NetCDF Climate and Forecast (CF) Metadata Convention version 18')
    dst.setncattr('license', 'ESA CCI Data Policy: free and open access')
    dst.setncattr('platform', 'Envisat, Terra, DMSP 5D-3/F16, DMSP 5D-3/F17, DMSP 5D-3/F18')
    dst.setncattr('sensor', 'MERIS, MODIS, SSMIS')
    spatial_resolution = '5.6km at Equator' if res == '005' else '56km at Equator'
    dst.setncattr('spatial_resolution', spatial_resolution)
    dst.setncattr('geospatial_lat_units', 'degrees_north')
    dst.setncattr('geospatial_lon_units', 'degrees_east')
    geospatial_resolution = '0.05' if res == '005' else '0.5'
    dst.setncattr('geospatial_lat_resolution', geospatial_resolution)
    dst.setncattr('geospatial_lon_resolution', geospatial_resolution)
    dst.setncattr('key_variables', 'tcwv')

    # set dimensions from src:
    for name, dimension in src.dimensions.iteritems():
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)

    # check if source product contains 'time: dimension:
    has_timedim = False    
    for name, dimension in src.dimensions.iteritems():
        print ('dimension: ', name)
        if name == 'time':
            has_timedim = True

    if not has_timedim:
        # set new time dimension:
        time = 1
        dst.createDimension('time', None)
        # create time variable and set time data
        time = dst.createVariable('time', 'i4', ('time'), zlib=True)
        time.setncattr('long_name', 'Product dataset time given as days since 1970-01-01')
        time.setncattr('standard_name', 'time')
        time.setncattr('units', 'days since 1970-01-01')
        time.setncattr('month_lengths', np.array([31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31], 'i4'))
        time.setncattr('leap_year', np.array(2000, 'i4'))
        time[:] = int(timeval)

    # if not present,set lat/lon variables:
    has_latlon = False
    for name, variable in src.variables.iteritems():
        print ('src variable: ', name)
        if name == 'lat' or name == 'lon':
            has_latlon = True

    if not has_latlon:
        incr = 0.05 if res == '005' else 0.5
        lat_arr = np.arange(90.0, -90.0, -incr) - incr/2.0
        lon_arr = np.arange(-180.0, 180.0, incr) + incr/2.0
        # set new lat/lon variables:
        lat = dst.createVariable('lat', 'f4', ('lat'), zlib=True)
        lon = dst.createVariable('lon', 'f4', ('lon'), zlib=True)
        lat.setncattr('long_name', 'Latitude')
        lat.setncattr('standard_name', 'latitude')
        lat.setncattr('units', 'degrees_north')
        lon.setncattr('long_name', 'Longitude')
        lon.setncattr('standard_name', 'longitude')
        lon.setncattr('units', 'degrees_east')

        lat[:] = lat_arr
        lon[:] = lon_arr

    # set variable attributes from src
    for name, variable in src.variables.iteritems():
        if name.find("_sigma") == -1 and name.find("_sum") == -1 and name.find("_weights") == -1 and \
           (name.find("tcwv") != -1 or name == 'wvpa' or name == 'numo' or name == 'stdv' or name == 'crs' or name == 'time'):
            print ('variable.dimensions: ', variable.dimensions)
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            for attr in variable.ncattrs():
                dstvar.setncattr(attr, getattr(variable, attr))

            if name.find("counts") != -1:
                dstvar.setncattr('units', ' ')

    #print ('variable keys: ', dst.variables.keys())

    # set variable data from src
    for variable in dst.variables:
        print ('dst variable: ', variable)

        if has_timedim: 
            # SSMI original or other merged product after compliance step
            if variable == 'time':
                dst.variables[variable][:] = src.variables[variable][:]
            elif variable == 'crs':
                dst.variables[variable][:] = src.variables[variable][:]
            elif variable.find("tcwv") != -1:
                # tcwv* are still 2D (20190612)
                dst.variables[variable][:,:] = src.variables[variable][:,:]
            elif variable == 'wvpa' or variable == 'numo' or variable == 'stdv':
                dst.variables[variable][:,:,:] = src.variables[variable][:,:,:]
        else:
            # MERIS or MODIS non-merged
            if variable == 'crs':
                dst.variables[variable][:] = src.variables[variable][:]
            elif variable.find("tcwv") != -1:
                dst.variables[variable][:,:] = src.variables[variable][:,:]            

    for variable in dst.variables:        
        if variable.find("_mean") != -1:
            dst.renameVariable(variable, variable.replace("_mean", ""))
        elif variable.find("wvpa") != -1:
            dst.renameVariable(variable, variable.replace("wvpa", "tcwv"))
        elif variable.find("numo") != -1:
            dst.renameVariable(variable, variable.replace("numo", "tcwv_counts"))
        elif variable.find("stdv") != -1:
            dst.renameVariable(variable, variable.replace("stdv", "tcwv_uncertainty")) 

    # make sure tcwv_* variables have a long_name, correct units, and tcwv as key variable has a bit more...:
    for name, variable in dst.variables.iteritems():
        if name == 'tcwv':
            if int(day) == 0:
                variable.setncattr('long_name', 'Mean of Total Column of Water (Level-3 global monthly aggregation) ')
            else:
                variable.setncattr('long_name', 'Mean of Total Column of Water (Level-3 global daily aggregation) ')
            variable.setncattr('standard_name', 'atmosphere_water_vapor_content ')
            variable.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')
            variable.setncattr('units', 'kg/m^2')
            tcwv_arr = np.array(variable)
            tcwv_min = np.nanmin(tcwv_arr)
            tcwv_max = np.nanmax(tcwv_arr)
            variable.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
            variable.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')

        if name == 'tcwv_uncertainty':
            variable.setncattr('long_name', 'Uncertainty associated with the mean  of Total Column of Water ')
            variable.setncattr('units', 'kg/m^2')

        if name == 'tcwv_counts':
            variable.setncattr('long_name', 'Number of samples of Total Column of Water ')

