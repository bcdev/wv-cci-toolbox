__author__ = 'olafd'

# Generates final CF- and CCI-compliant TCWV L3 daily non-merged products ready for delivery.
# Usage: python nc-compliance-py-process.py ./${nc_infile} ${sensor} ${year} ${month} ${day} ${resolution} ${version}
# Example: python nc-compliance-py-daily-non-merged-process.py l3_tcwv_olci_005deg_2018-07-25_2018-07-25.nc olci 2018 07 25 005 2.0

# NOTE: now we directly use the originally generated L3, rename bands where necessary, and provide the required uncertainty terms here.
# This will save the L3 --> L3 uncertainties step
#
# TODO: provide similar versions for NIR non-merged products, NIR monthly merged/non-merged products.
# TODO: In merging, use HOAPS original (adjust MergeOp) to save also HOAPS compliance step (HOAPS unmerged is not published)

# OD, 20190906

import os
import sys
import time
import datetime
import uuid
import numpy as np
import scipy.ndimage
import netCDF4
import calendar

from calendar import monthrange, isleap
from netCDF4 import Dataset


#############################################################################
def copyVariableAttributesFromSource(srcvar, dstvar):
    for attr in srcvar.ncattrs():
        if attr in dstvar.ncattrs():
            dstvar.delncattr(attr)
        dstvar.setncattr(attr, getattr(srcvar, attr))    

def setVariableLongNameAndUnitAttributes(variable, long_name_string, unit_string):
    if 'long_name' in variable.ncattrs():
        variable.delncattr('long_name')
    if 'units' in variable.ncattrs():
        variable.delncattr('units')
    variable.setncattr('long_name', long_name_string)
    variable.setncattr('units', unit_string)

    
#############################################################################
            
########## initialize input parameters ######################
if len(sys.argv) != 9 and len(sys.argv) != 10:
    print ('Usage:  python nc-compliance-py-process.py <nc_infile> <landmask_file> <sensor> <year> <month> <day> <resolution> < product version> [<seaice_mask_file>] ')
    sys.exit(-1)

nc_infile = sys.argv[1]
landmask_file = sys.argv[2]
sensor = sys.argv[3]
year = sys.argv[4]
month = sys.argv[5]
day = sys.argv[6]
res = sys.argv[7]
version = sys.argv[8]

seaice_available = False
if len(sys.argv) == 10:
    seaice_mask_file = sys.argv[9]
    seaice_available = True

print ('nc_infile: ', nc_infile)
print ('landmask_file: ', landmask_file)
if seaice_available:
    print ('seaice_mask_file: ', seaice_mask_file)
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

if seaice_available:
    try:
        ds_seaice = Dataset(seaice_mask_file)
    except:
        print 'Cannot read seaice mask file'
        ds_seaice = None
        seaice_available = False    

############# set up and fill NetCDF destination file  #######################
with Dataset(nc_infile) as src, Dataset(landmask_file) as ds_landmask, Dataset(outpath, 'w', format='NETCDF4') as dst:

    for name, variable in ds_landmask.variables.iteritems():
        print ('ds landmask variable: ', name)
    if seaice_available:
        for name, variable in ds_seaice.variables.iteritems():
            print ('ds seaice variable: ', name)

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
        num_days_in_month = calendar.monthrange(int(year), int(month))[1]
        starttime = datestring + '-01 00:00:00 UTC'
        endtime = datestring + '-' + str(num_days_in_month) +  ' 23:59:59 UTC'
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

    # TODO: if we have lat/lon, replace attributes units, long_name, standard_name, valid_min, valid_max
        
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

    # Create final flag bands:
    tcwv_quality_flag = dst.createVariable('tcwv_quality_flag', 'b', (dst.dimensions['lat'].name,dst.dimensions['lon'].name), zlib=True)
    tcwv_quality_flag_arr = np.zeros(shape=(height, width))

    surface_type_flag = dst.createVariable('surface_type_flag', 'b', (dst.dimensions['lat'].name,dst.dimensions['lon'].name), zlib=True)
    surface_type_flag_arr = np.zeros(shape=(height, width))
        
    # TODO: for daily non-merged we need to
    #       - copy 'tcwv_mean' into 'tcwv'
    #       - copy 'num_obs' into 'num_obs'
    #       - copy 'tcwv_sigma' into 'stdv'
    #       - copy 'tcwv_uncertainty_mean' into 'tcwv_ran'
    #       - compute and write 'tcwv_err' from 'tcwv_uncertainty_sums_sum_sq'
    #       - compute and write 'tcwv_quality_flags' from 'tcwv_quality_flags_majority'
    #       - compute and write 'surface_type_flags_majority' from 'surface_type_flags_majority'
    # TODO: for daily merged we need to do the same, but no renaming
    # TODO: for cmsaf we need to do the same, but specific renaming
    # TODO: monthly: separate script
        
    # copy variables with attributes from source product:
    for name, variable in src.variables.iteritems():
        if name == 'num_obs': 
            dstvar = dst.createVariable('num_obs', variable.datatype, variable.dimensions, zlib=True)
            copyVariableAttributesFromSource(variable, dstvar)
            dstvar[:,:] = variable[:,:]
        if name == 'tcwv_mean': 
            dstvar = dst.createVariable('tcwv', variable.datatype, variable.dimensions, zlib=True)
            copyVariableAttributesFromSource(variable, dstvar)
            dstvar[:,:] = variable[:,:]
        if name == 'tcwv_sigma': 
            dstvar = dst.createVariable('stdv', variable.datatype, variable.dimensions, zlib=True)
            copyVariableAttributesFromSource(variable, dstvar)
            dstvar[:,:] = variable[:,:]
        if name == 'tcwv_uncertainty_mean': 
            dstvar = dst.createVariable('tcwv_ran', variable.datatype, variable.dimensions, zlib=True)
            copyVariableAttributesFromSource(variable, dstvar)
            dstvar[:,:] = variable[:,:]
        if name == 'tcwv_uncertainty_sums_sum_sq': 
            dstvar = dst.createVariable('tcwv_err', variable.datatype, variable.dimensions, zlib=True)
            # todo: compute tcwv_err
            copyVariableAttributesFromSource(variable, dstvar)            

        if name == 'crs': 
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            copyVariableAttributesFromSource(variable, dstvar)
            dstvar[:] = variable[:]
            
        if has_latlon:
            if name == 'lat' or name == 'lon': 
                dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
                dstvar[:] = variable[:]

            
    # make sure tcwv_* variables have a long_name, correct units, and tcwv as key variable has a bit more...
    for name, variable in dst.variables.iteritems():
        print ('dst variable: ', name)
        if name == 'tcwv':
            setVariableLongNameAndUnitAttributes(variable, 'Total Column of Water', 'kg/m2')

            variable.setncattr('standard_name', 'atmosphere_water_vapor_content ')
            variable.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')
            tcwv_arr = np.array(variable)
            tcwv_min = np.nanmin(tcwv_arr)
            tcwv_max = np.nanmax(tcwv_arr)
            tcwv_min_valid = 0.0
            tcwv_max_valid = 80.0
            variable.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
            variable.setncattr('valid_range', np.array([tcwv_min_valid, tcwv_max_valid], 'f4'))
            variable.setncattr('ancillary_variables', 'stdv num_obs')
                
        if name == 'stdv':
            setVariableLongNameAndUnitAttributes(variable, 'Standard deviation of Total Column of Water Vapour', 'kg/m2')

        if name == 'tcwv_ran':
            setVariableLongNameAndUnitAttributes(variable, 'Average retrieval uncertainty', 'kg/m2')
                
        if name == 'tcwv_err':
            setVariableLongNameAndUnitAttributes(variable, 'Propagated retrieval uncertainty', 'kg/m2')

            #// Stengel et al., eq. (3):
            #  final float tcwvUncertaintySumSq = sourceSamples[SRC_TCWV_UNCERTAINTY_SUM_SQ].getFloat();   // for eq. (3)
            #  final float sigmaSqrMean = tcwvUncertaintySumSq / numObs;
            #// DWD wants this term stored instead, as this is what is written into HOAPS products (MS, 20190826):
            #  final float sigmaSqrMeanHoaps = (1.0f / numObs) * sigmaSqrMean; // = (1.0/(numObs*numObs)) * tcwvUncertaintySumSq
            #  targetSamples[TRG_TCWV_PROPAGATED_RETRIEVAL_UNCERTAINTY].set(sigmaSqrMeanHoaps);

            uncert_sum_sqr_arr = np.array(src.variables['tcwv_uncertainty_sums_sum_sq'])
            num_obs_arr = np.array(src.variables['num_obs'])
            sigma_sqr_mean_hoaps_arr = np.zeros(shape=(height, width))
            sigma_sqr_mean_hoaps_arr = uncert_sum_sqr_arr / (num_obs_arr*num_obs_arr)
            variable[:,:] = sigma_sqr_mean_hoaps_arr[:,:]

        if name == 'tcwv_quality_flag':
            setVariableLongNameAndUnitAttributes(variable, 'Quality flag of Total Column of Water Vapour', ' ')
            variable.setncattr('standard_name', 'status_flag ')
            fill_value = -128
            variable.setncattr('_FillValue', np.array([fill_value], 'b'))
            min_valid = 0
            max_valid = 2
            variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
            variable.setncattr('flag_values', np.array([0, 1, 2], 'b'))
            variable.setncattr('flag_meanings', 'TCWV_OK HIGH_COST_FUNCTION TCWV_INVALID')

            # set the quality flag values here:
            # flag = 0 for TCWV_OK, flag = 1 for TCWV_HIGH_COST_FUNCTION, flag = 2 for TCWV_INVALID (all NaN pixels)
            #tcwv_quality_flag_arr_src = np.array(src.variables['tcwv_quality_flags_majority'])
            #tmparr = np.copy(tcwv_quality_flag_arr_src)
            #tmparr[np.where(np.isnan(tmparr))] = 4
            #tcwv_quality_flag_arr = np.log2(tmparr)
            #variable[:,:] = tcwv_quality_flag_arr[:,:]

            tcwv_quality_flag_maj_arr_src = np.array(src.variables['tcwv_quality_flags_majority'])
            tcwv_quality_flag_min_arr_src = np.array(src.variables['tcwv_quality_flags_min'])
            tcwv_quality_flag_maj_arr = np.copy(tcwv_quality_flag_maj_arr_src)
            tcwv_quality_flag_min_arr = np.copy(tcwv_quality_flag_min_arr_src)
            tcwv_quality_flag_maj_arr[np.where(np.isnan(tcwv_quality_flag_maj_arr))] = 4
            tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_quality_flag_min_arr))] = 4
            tmparr = np.copy(tcwv_quality_flag_maj_arr)
            # if the majority is INVALID, we need to take the minimum of the existing samples
            # (otherwise we may have an INVALID flag together with a valid TCWV value)
            indices = np.where(tcwv_quality_flag_maj_arr > 2)
            tmparr[indices] = tcwv_quality_flag_min_arr_src[indices]
            tmparr[np.where(np.isnan(tmparr))] = 4
            tcwv_quality_flag_arr = np.log2(tmparr)
            variable[:,:] = tcwv_quality_flag_arr[:,:]
                
        if name == 'surface_type_flag':
            setVariableLongNameAndUnitAttributes(variable, 'Surface type flag', ' ')
            variable.setncattr('standard_name', 'status_flag ')
            fill_value = -128
            variable.setncattr('_FillValue', np.array([fill_value], 'b'))
            min_valid = 0
            max_valid = 6
            variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
            variable.setncattr('flag_values', np.array([0, 1, 2, 3, 4, 5, 6], 'b'))
            variable.setncattr('flag_meanings', 'LAND OCEAN CLOUD_OVER_LAND SEA_ICE COAST PARTLY_CLOUDY_OVER_LAND PARTLY_SEA_ICE')

            # we can have LAND (1), OCEAN (2), SEAICE (4), LAND+CLOUD (9), OCEAN+CLOUD (10), SEAICE+CLOUD (12):
            # but we want LAND (0), OCEAN (1), CLOUD_OVER_LAND (2), SEA_ICE (3), COAST (4), PARTLY_CLOUDY_OVER_LAND (5), PARTLY_SEA_ICE (6)
            # (invalid is i.e. outside any swaths in daily L3)
            
            surface_type_flag_arr_src = np.array(src.variables['surface_type_flags_majority'])
            hoaps_surface_type_flag_arr_src = np.array(ds_landmask.variables['mask'])
            #tcwv_quality_flag_maj_arr_src = np.array(src.variables['tcwv_quality_flags_majority'])
            tcwv_quality_flag_min_arr_src = np.array(src.variables['tcwv_quality_flags_min'])
            tcwv_arr_src = np.array(src.variables['tcwv_mean'])
            
            tmparr = np.copy(surface_type_flag_arr_src)
            tmparr[np.where(np.isnan(tmparr))] = -128  # make NaN to INVALID
            
            tcwv_arr = np.copy(tcwv_arr_src)
            
            #tcwv_quality_flag_maj_arr = np.copy(tcwv_quality_flag_maj_arr_src)
            tcwv_quality_flag_min_arr = np.copy(tcwv_quality_flag_min_arr_src)
            #tcwv_quality_flag_maj_arr[np.where(np.isnan(tcwv_quality_flag_maj_arr))] = -128  # make NaN to INVALID
            tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_quality_flag_min_arr))] = -128  # make NaN to INVALID
            
            hoaps_surface_type_flag_arr = np.copy(hoaps_surface_type_flag_arr_src[0])
            if res == '005':
                hoaps_surface_type_flag_arr = scipy.ndimage.zoom(hoaps_surface_type_flag_arr, 10, order=0)
            hoaps_surface_type_flag_arr[np.where(np.isnan(hoaps_surface_type_flag_arr))] = 0  # make NaN to water
                
            # make ocean + tcwv_quality_flag.TCWV_INVALID + tcwv = NaN to INVALID (fix of originally L2 bug)):
            tmparr[np.where((tmparr > 1) & (tmparr < 3) & (tcwv_quality_flag_min_arr > 2) & (np.isnan(tcwv_arr)))] = -128
            tmparr[np.where(tmparr == 9)] = 4  # make land+cloud to CLOUD OVER LAND
            tmparr[np.where(tmparr == 10)] = 2 # make ocean + cloud to OCEAN
            tmparr[np.where(tmparr == 12)] = 4 # make seaice+cloud to SEA_ICE
            print 'tmparr shape                     : ' , tmparr.shape
            print 'hoaps_surface_type_flag_arr shape: ' , hoaps_surface_type_flag_arr.shape
            tmparr[np.where(hoaps_surface_type_flag_arr < 1)] = 2  # make hoaps water to OCEAN
            tmparr[np.where(hoaps_surface_type_flag_arr > 1)] = 16 # make hoaps coast to COAST
            tmparr[np.where((hoaps_surface_type_flag_arr == 1) & (tmparr != 4))] = 1 # make hoaps land to LAND if not cloudy
            if seaice_available:
                seaice_arr_src = np.array(ds_seaice.variables['mask'])
                seaice_frac_arr_src = np.array(ds_seaice.variables['icec'])
                day_index = int(day.zfill(1))
                seaice_arr_src_day = seaice_arr_src[day_index]
                seaice_frac_arr_src_day = seaice_frac_arr_src[day_index]
                if res == '005':
                    seaice_arr_src_day = scipy.ndimage.zoom(seaice_arr_src_day, 10, order=0)
                    seaice_frac_arr_src_day = scipy.ndimage.zoom(seaice_frac_arr_src_day, 10, order=0)
                seaice_frac_arr_src_day[np.where(np.isnan(seaice_frac_arr_src_day))] = 0  # make NaN to 0
                tmparr[np.where(seaice_arr_src_day == 11)] = 8 # make hoaps seaice to SEA_ICE
                #tmparr[np.where(seaice_arr_src_day == 12)] = 64 # make hoaps seaice edge to PARTLY_SEA_ICE
                # maybe as suggestion: make hoaps seaice < 90% to PARTLY_SEA_ICE, TODO: discuss value of 90%:
                tmparr[np.where( (seaice_arr_src_day >= 11) & (seaice_frac_arr_src_day > 0) & (seaice_frac_arr_src_day < 90) )] = 64 

            # TODO: get PARTLY_CLOUDY_OVER_LAND from MIN_MAX aggregator
            
            #print 'src.variables[surface_type_flags_majority][0,0]: ' , src.variables['surface_type_flags_majority'][0,0]
            #print('tmparr[396,396]: ' , tmparr[396,396]) # 9
            surface_type_flag_arr = np.log2(tmparr)
            variable[:,:] = surface_type_flag_arr[:,:]
            
        if name == 'num_obs':
            setVariableLongNameAndUnitAttributes(variable, 'Number of Total Column of Water Vapour retrievals contributing to L3 grid cell', ' ')
            variable.setncattr('coordinates', 'lat lon')

        if name == 'lat':
            setVariableLongNameAndUnitAttributes(variable, 'Latitude', 'degrees_north ')
            variable.setncattr('standard_name', 'latitude')
            variable.setncattr('valid_range', np.array([lat_min_valid, lat_max_valid], 'f4'))
            variable.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
            variable.setncattr('axis', 'Y')
            variable.setncattr('bounds', 'lat_bnds')

        if name == 'lon':
            setVariableLongNameAndUnitAttributes(variable, 'Longitude', 'degrees_east')
            variable.setncattr('standard_name', 'longitude')
            variable.setncattr('valid_range', np.array([lon_min_valid, lon_max_valid], 'f4'))
            variable.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
            variable.setncattr('axis', 'X')
            variable.setncattr('bounds', 'lon_bnds')

        if name == 'crs':
            variable.setncattr('long_name', 'Coordinate Reference System ')
            variable.setncattr('comment', 'A coordinate reference system (CRS) defines how the georeferenced spatial data relates to real locations on the Earth\'s surface ')
