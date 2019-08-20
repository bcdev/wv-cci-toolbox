__author__ = 'olafd'

# Generates final CF- and CCI-compliant TCWV products ready for delivery.
# Usage: python nc-compliance-py-process.py ./${nc_infile} ${sensor} ${year} ${month} ${day} ${resolution} ${version}
# Example: python nc-compliance-py-process.py L2_of_l3_tcwv_olci_005deg_2018-07-25_2018-07-25.nc olci 2018 07 25 005 2.0

import os
import sys
import time
import datetime
import uuid
import numpy as np
import netCDF4
import calendar

from calendar import monthrange, isleap
from netCDF4 import Dataset

########## initialize input parameters ######################
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
    datestring = year + month

    # use days since 1970-01-01 as time base value, and the 15th of given month at 00:00 as reference time:
    num_days_in_month = calendar.monthrange(int(year), int(month))[1]
    timeval = (datetime.datetime(int(year),int(month),15)  - datetime.datetime(1970,1,1)).days
    time_bnds_0 = timeval - 15 
    time_bnds_1 = time_bnds_0 + num_days_in_month 
else:
    # daily products:
    datestring = year + month + day

    # use days since 1970-01-01 as time value, and the given day at 12:00 as reference time:
    timeval = (datetime.datetime(int(year),int(month),int(day))  - datetime.datetime(1970,1,1)).days
    time_bnds_0 = timeval 
    time_bnds_1 = timeval + 1
    
if sensor.find("-") != -1:
    l3_suffix = 'S'
else:
    l3_suffix = 'C' 

# final product name following CCI data standards v2.1 section 2.7:
nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + sensor + '-'  + res + 'deg-' + datestring + '-fv' + version + '.nc'

print ('nc_infile: ', nc_infile)
print ('nc_outfile: ', nc_outfile)
outpath = './' + nc_outfile
print ('outpath: ', outpath)

############# set up and fill NetCDF destination file  #######################
with Dataset(nc_infile) as src, Dataset(outpath, 'w', format='NETCDF4') as dst:

    # set global attributes (CCI data standards v2.1 section 2.5.1):
    dst.setncattr('title', 'Water Vapour CCI Total Column of Water Vapour Product')
    dst.setncattr('institution', 'Brockmann Consult GmbH; EUMETSAT/CMSAF')
    dst.setncattr('source', 'MERIS RR L1B 3rd Reprocessing; MODIS MOD021KM L1B; HOAPS-S version 4.0 released by CM SAF')
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
    dst.setncattr('comment', 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ESA Climate Change Initiative Extension (CCI+) Phase 1. They include CM SAF products over the ocean.')
    
    from datetime import datetime, timedelta
    date_created = str(datetime.utcnow())[:19] + ' UTC'
    dst.setncattr('date_created', date_created)
    dst.setncattr('creator_name', 'Brockmann Consult GmbH; EUMETSAT/CMSAF')
    dst.setncattr('creator_url', 'www.brockmann-consult.de; http://www.cmsaf.eu')
    dst.setncattr('creator_email', 'info@brockmann-consult.de; contact.cmsaf@dwd.de')
    dst.setncattr('project', 'Climate Change Initiative - European Space Agency')
    dst.setncattr('geospatial_lat_min', '-90.0')
    dst.setncattr('geospatial_lat_max', '90.0')
    dst.setncattr('geospatial_lon_min', '-180.0')
    dst.setncattr('geospatial_lon_max', '180.0')
    dst.setncattr('geospatial_vertical_min', '0.0')
    dst.setncattr('geospatial_vertical_max', '0.0')
    if int(day) == 0:
        starttime = datestring + '-01 00:00:00 UTC'
        endtime = datestring + '-' + str(getNumDaysInMonth(year, month)) +  ' 23:59:59 UTC'
        dst.setncattr('time_coverage_duration', 'P1M')
        dst.setncattr('time_coverage_resolution', 'P1M')
    else:
        starttime = datestring + ' 00:00:00 UTC'
        endtime = datestring + ' 23:59:59 UTC'
        dst.setncattr('time_coverage_duration', 'P1D')
        dst.setncattr('time_coverage_resolution', 'P1D')

    dst.setncattr('time_coverage_start', starttime)
    dst.setncattr('time_coverage_end', endtime)
    dst.setncattr('standard_name_vocabulary', 'NetCDF Climate and Forecast (CF) Metadata Convention version 67')
    dst.setncattr('license', 'ESA CCI Data Policy: free and open access. Products containing CM SAF data are made available under the CM SAF data policy.')
    dst.setncattr('platform', 'Envisat, Terra, DMSP-F16, DMSP-F17, DMSP-F18')
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

    # if not present in source product, create 'time: dimension:
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
        #time.setncattr('month_lengths', np.array([31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31], 'i4'))
        #time.setncattr('leap_year', np.array(2000, 'i4'))
        time.setncattr('calendar', 'gregorian')
        time.setncattr('axis', 'T')
        time.setncattr('bounds', 'time_bnds')
        time[:] = int(timeval)
        
    # if not present in source product, create lat/lon variables as 1D:
    has_latlon = False
    for name, variable in src.variables.iteritems():
        print ('src variable: ', name)
        if name == 'lat' or name == 'lon':
            has_latlon = True

    lat_min_valid = -90.0
    lat_max_valid = 90.0
    lon_min_valid = -180.0
    lon_max_valid = 180.0

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
        lat.setncattr('valid_range', np.array([lat_min_valid, lat_max_valid], 'f4'))
        lat.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lat.setncattr('axis', 'Y')
        lat.setncattr('bounds', 'lat_bnds')
        lon.setncattr('long_name', 'Longitude')
        lon.setncattr('standard_name', 'longitude')
        lon.setncattr('units', 'degrees_east')
        lon.setncattr('valid_range', np.array([lon_min_valid, lon_max_valid], 'f4'))
        lon.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lon.setncattr('axis', 'X')
        lon.setncattr('bounds', 'lon_bnds')
        
        lat[:] = lat_arr
        lon[:] = lon_arr

    width = len(dst.dimensions['lon'])
    height = len(dst.dimensions['lat'])
    print('width: ', width)
    print('height: ', height)

    # create 'nv' dimension and '*_bnds' variables (CCI data standards v2.1 section 2.5.3):
    dst.createDimension('nv', 2)

    # create 'time_bnds' variable:
    time_bnds_arr = np.array([time_bnds_0, time_bnds_1])
    time_bnds = dst.createVariable('time_bnds', 'i4', ('time','nv'), zlib=True)
    time_bnds[:,0] = time_bnds_arr[0]
    time_bnds[:,1] = time_bnds_arr[1]
    time_bnds.setncattr('long_name', 'Time cell boundaries')
    #time_bnds.setncattr('units', 'days since 1970-01-01')
    time_bnds.setncattr('comment', 'Contains the start and end times for the time period the data represent.')
    
    # create 'lat_bnds' and 'lon_bnds' variables:
    incr = 0.05 if res == '005' else 0.5
    lat_bnds_arr_0 = np.arange(90.0, -90.0, -incr)
    lat_bnds_arr_1 = np.arange(90.0-incr, -90.0-incr, -incr)
    lon_bnds_arr_0 = np.arange(-180.0, 180.0, incr)
    lon_bnds_arr_1 = np.arange(-180.0+incr, 180.0+incr, incr)
    lat_bnds_arr = np.empty(shape=[height, 2])
    lon_bnds_arr = np.empty(shape=[width, 2])
    lat_bnds_arr[:,0] = lat_bnds_arr_0
    lat_bnds_arr[:,1] = lat_bnds_arr_1
    lon_bnds_arr[:,0] = lon_bnds_arr_0
    lon_bnds_arr[:,1] = lon_bnds_arr_1

    lat_bnds = dst.createVariable('lat_bnds', 'f4', ('lat','nv'), zlib=True)
    lon_bnds = dst.createVariable('lon_bnds', 'f4', ('lon','nv'), zlib=True)
    lat_bnds.setncattr('long_name', 'Latitude cell boundaries')
    # CF compliance checker sometimes complains about units, sometimes not... leave them out for now
    #lat_bnds.setncattr('units', 'degrees_north')
    lat_bnds.setncattr('valid_range', np.array([lat_min_valid, lat_max_valid], 'f4'))
    lat_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lat_bnds.setncattr('comment', 'Contains the northern and southern boundaries of the grid cells.')
    lon_bnds.setncattr('long_name', 'Longitude cell boundaries')
    #lon_bnds.setncattr('units', 'degrees_east')
    lon_bnds.setncattr('valid_range', np.array([lon_min_valid, lon_max_valid], 'f4'))
    lon_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lon_bnds.setncattr('comment', 'Contains the eastern and western boundaries of the grid cells.')
    lat_bnds[:,:] = lat_bnds_arr
    lon_bnds[:,:] = lon_bnds_arr
    
    # if not present in source product, create final quality flag:
    has_quality = False
    for name, variable in src.variables.iteritems():
        if name == 'tcwv_quality_flag':
            has_quality = True
    
    if not has_quality:
        tcwv_quality_flag = dst.createVariable('tcwv_quality_flag', 'b', (dst.dimensions['lat'].name,dst.dimensions['lon'].name), zlib=True)
        tcwv_quality_flag_arr = np.zeros(shape=(height, width))
        
    # copy variables with attributes from source product:
    for name, variable in src.variables.iteritems():
        if name.find("_sigma") == -1 and name.find("_sum") == -1 and name.find("_weights") == -1 and \
           (name.find("tcwv") != -1 or name.find("lat") != -1 or name.find("lon") != -1 or \
            name == 'wvpa' or name == 'numo' or name == 'stdv' or name == 'crs' or name == 'time'):
            print ('variable.dimensions: ', variable.dimensions)
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            for attr in variable.ncattrs():
                if attr in dstvar.ncattrs():
                    dstvar.delncattr(attr)
                dstvar.setncattr(attr, getattr(variable, attr))

    # copy variable data from source product
    for variable in dst.variables:
        print ('dst variable: ', variable)

        if has_timedim: 
            # CM SAF HOAPS original or other merged product after compliance step
            if variable == 'time':
                dst.variables[variable][:] = src.variables[variable][:]
            elif variable == 'crs':
                dst.variables[variable][:] = src.variables[variable][:]
            elif has_latlon and (variable == 'lat' or variable == 'lon'):
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
            elif has_latlon and (variable == 'lat' or variable == 'lon'):
                dst.variables[variable][:] = src.variables[variable][:]
            elif variable.find("tcwv") != -1 and variable.find("tcwv_quality_flag") == -1:
                dst.variables[variable][:,:] = src.variables[variable][:,:]

    # if necessary, rename variables to their final names following PSD:
    for variable in dst.variables:        
        if variable.find("_mean") != -1:
            dst.renameVariable(variable, variable.replace("_mean", ""))
        elif variable.find("wvpa") != -1:
            dst.renameVariable(variable, variable.replace("wvpa", "tcwv"))
        elif variable.find("numo") != -1:
            dst.renameVariable(variable, variable.replace("numo", "tcwv_counts"))
        elif variable.find("stdv") != -1:
            dst.renameVariable(variable, variable.replace("stdv", "tcwv_uncertainty")) 
            
    # for VIS/NIR sensors, make sure tcwv_* variables have a long_name, correct units, and tcwv as key variable has a bit more...
    # not necessary for CM SAF HOAPS as they will not be published separately
    if sensor.startswith('meris') or sensor.startswith('modis') or sensor.startswith('olci'):
        for name, variable in dst.variables.iteritems():
            if name == 'tcwv':
                if 'long_name' in variable.ncattrs():
                    variable.delncattr('long_name')
                if 'units' in variable.ncattrs():
                    variable.delncattr('units')
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
                tcwv_min_valid = 0.0
                tcwv_max_valid = 80.0
                variable.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
                variable.setncattr('valid_range', np.array([tcwv_min_valid, tcwv_max_valid], 'f4'))
                variable.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')
                
            if name == 'tcwv_uncertainty':
                if 'long_name' in variable.ncattrs():
                    variable.delncattr('long_name')
                if 'units' in variable.ncattrs():
                    variable.delncattr('units')
                variable.setncattr('long_name', 'Uncertainty associated with the mean of Total Column of Water Vapour')
                variable.setncattr('units', 'kg/m^2')

                if not has_quality:
                    # set the quality flag values here, depending on uncertainties:
                    # flag = 0 for uncert < 5.0 mm, flag = 1 for uncert < 5.0 mm, flag = 2 for NaN pixels 
                    tcwv_uncertainty_arr = np.array(variable)
                    tcwv_quality_flag_arr[np.where(np.isnan(tcwv_uncertainty_arr))] = 2
                    # the usage of 'where' is nasty if we have NaNs, so make a copy of the uncertainty array and replace the NaNs by -1.0:
                    tmparr = np.copy(tcwv_uncertainty_arr)
                    tmparr[np.where(np.isnan(tmparr))] = -1.0
                    tcwv_uncertain_thresh = 4.0  # todo: discuss
                    tcwv_quality_flag_arr[np.where(tmparr > tcwv_uncertain_thresh)] = 1 
                    dst.variables['tcwv_quality_flag'][:,:] = tcwv_quality_flag_arr 

            if name == 'tcwv_quality_flag' and not has_quality:
                if 'long_name' in variable.ncattrs():
                    variable.delncattr('long_name')
                variable.setncattr('long_name', 'Quality flag associated with the mean of Total Column of Water Vapour')
                variable.setncattr('standard_name', 'status_flag ')
                fill_value = -128
                variable.setncattr('_FillValue', np.array([fill_value], 'b'))
                quality_min_valid = 0
                quality_max_valid = 2
                variable.setncattr('valid_range', np.array([quality_min_valid, quality_max_valid], 'b'))
                variable.setncattr('flag_values', np.array([0, 1, 2], 'b'))
                variable.setncattr('flag_meanings', 'TCWV_OK TCWV_UNCERTAIN TCWV_NODATA')
                
            if name == 'tcwv_counts':
                if 'long_name' in variable.ncattrs():
                    variable.delncattr('long_name')
                variable.setncattr('long_name', 'Number of samples of Total Column of Water ')
                if 'units' in variable.ncattrs():
                    variable.delncattr('units')
                variable.setncattr('units', ' ')

            if name == 'crs':
                # this check is necessary as the merged monthly products have no long_name and comment attributes in their crs variable
                if 'long_name' in variable.ncattrs():
                    variable.delncattr('long_name')
                variable.setncattr('long_name', 'Coordinate Reference System ')
                if 'comment' in variable.ncattrs():
                    variable.delncattr('comment')
                variable.setncattr('comment', 'A coordinate reference system (CRS) defines how the georeferenced spatial data relates to real locations on the Earth\'s surface ')

