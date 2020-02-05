__author__ = 'olafd'

# Generates final CF- and CCI-compliant TCWV L3 MONTHLY products ready for delivery.
# Usage: python nc-compliance-monthly-py-process.py ./${nc_infile} ${sensor} ${year} ${month} ${resolution} ${version}
# Example: python nc-compliance-monthly-py-process.py l3_tcwv_olci_005deg_2018-07-01_2018-07-31.nc olci 2018 07 005 2.0
#
# OD, 20191111

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

def resetOceanCdr1(variable, surface_type_array, reset_value):    
    var_array = np.array(variable)
    tmp_array = np.copy(var_array)
    tmp_array[np.where(surface_type_array == 1)] = reset_value
    variable[:,:] = tmp_array[:,:]

def setOceanWvpaErrors(variable, surface_type_array, tcwv_array, wvpa_err_array):
    var_array = np.array(variable)
    tmp_array = np.copy(var_array)
    if wvpa_err_array:
        use_hoaps = np.where(((surface_type_array == 1) | (surface_type_array == 4) | (surface_type_array == 6)) & (~np.isnan(tcwv_array)) & (~np.isnan(wvpa_err_array)))
        tmp_array[use_hoaps] = wvpa_err_array[use_hoaps]
    else:
        ocean_or_ice = np.where(((surface_type_array == 1) | (surface_type_array == 4) | (surface_type_array == 6)))
        tmp_array[ocean_or_ice] = np.nan

    tmp_array[np.where(np.isnan(var_tcwv_arr))] = np.nan
    variable[:,:] = tmp_array[:,:]

                       
    
#############################################################################
            
########## initialize input parameters ######################

print >> sys.stderr, "STARTING nc-compliance-monthlypy-process.py"

if len(sys.argv) != 8 and len(sys.argv) != 9:
    print ('Usage:  python nc-compliance-py-process.py <nc_infile> <landmask_file> <sensor> <year> <month> <resolution> < product version> [<seaice_mask_file>] ')
    sys.exit(-1)

nc_infile = sys.argv[1]
landmask_file = sys.argv[2]
sensor = sys.argv[3]
year = sys.argv[4]
month = sys.argv[5]
res = sys.argv[6]
version = sys.argv[7]

seaice_available = False
if len(sys.argv) == 9:
    seaice_mask_file = sys.argv[8]
    seaice_available = True

print 'nc_infile: ', nc_infile
print 'landmask_file: ', landmask_file
if seaice_available:
    print 'seaice_mask_file: ', seaice_mask_file
print 'sensor: ', sensor
print 'year: ', year
print 'month: ', month
print 'res: ', res
print 'version: ', version

nc_infile_root_index = nc_infile.find(year)

# we have MONTHLY products:
datestring = year + month

# use days since 1970-01-01 as time base value, and the 15th of given month at 00:00 as reference time:
num_days_in_month = calendar.monthrange(int(year), int(month))[1]
timeval = (datetime.datetime(int(year),int(month),15)  - datetime.datetime(1970,1,1)).days
time_bnds_0 = timeval - 15 
time_bnds_1 = time_bnds_0 + num_days_in_month 
    
if sensor.find("-") != -1:
    l3_suffix = 'S'
else:
    l3_suffix = 'C' 

# final product name following CCI data standards v2.1 section 2.7:
nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + sensor + '-'  + res + 'deg-' + datestring + '-fv' + version + '.nc'

print 'nc_infile: ', nc_infile
print 'nc_outfile: ', nc_outfile
outpath = './' + nc_outfile
print 'outpath: ', outpath

if seaice_available:
    try:
        ds_seaice = Dataset(seaice_mask_file)
    except:
        print 'Cannot read seaice mask file'
        ds_seaice = None
        seaice_available = False    

dst = Dataset(outpath, 'w', format='NETCDF4')
ds_landmask =  Dataset(landmask_file)
src = Dataset(nc_infile)

############# set up and fill NetCDF destination file  #######################

for name, variable in ds_landmask.variables.iteritems():
    print 'ds landmask variable: ', name
if seaice_available:
    for name, variable in ds_seaice.variables.iteritems():
        print 'ds seaice variable: ', name

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

num_days_in_month = calendar.monthrange(int(year), int(month))[1]
starttime = datestring + '-01 00:00:00 UTC'
endtime = datestring + '-' + str(num_days_in_month) +  ' 23:59:59 UTC'
dst.setncattr('time_coverage_duration', 'P1M')
dst.setncattr('time_coverage_resolution', 'P1M')

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
    print 'dimension: ', name
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
    time.setncattr('calendar', 'gregorian')
    time.setncattr('axis', 'T')
    time.setncattr('bounds', 'time_bnds')
    time[:] = int(timeval)
        
# if not present in source product, create lat/lon variables as 1D:
has_latlon = False
for name, variable in src.variables.iteritems():
    print 'src variable: ', name
    if name == 'lat' or name == 'lon':
        has_latlon = True
print 'has_latlon: ', has_latlon

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
print'width: ', width
print'height: ', height

# create 'nv' dimension and '*_bnds' variables (CCI data standards v2.1 section 2.5.3):
dst.createDimension('nv', 2)

# create 'time_bnds' variable:
time_bnds_arr = np.array([time_bnds_0, time_bnds_1])
time_bnds = dst.createVariable('time_bnds', 'i4', ('time','nv'), zlib=True)
time_bnds[:,0] = time_bnds_arr[0]
time_bnds[:,1] = time_bnds_arr[1]
time_bnds.setncattr('long_name', 'Time cell boundaries')
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
# no quality flag in monthlies, see below
#tcwv_quality_flag = dst.createVariable('tcwv_quality_flag', 'b', (dst.dimensions['lat'].name,dst.dimensions['lon'].name), zlib=True)
#tcwv_quality_flag_arr = np.zeros(shape=(height, width))

surface_type_flag = dst.createVariable('surface_type_flag', 'b', (dst.dimensions['lat'].name,dst.dimensions['lon'].name), zlib=True)
surface_type_flag_arr = np.zeros(shape=(height, width))
        
    # for MONTHLY we just need to
    #       - copy 'num_obs_sum' into 'num_obs', set attributes
    #       - copy 'tcwv_mean' into 'tcwv', set attributes
    #       - copy 'stdv_mean' into 'stdv', set attributes
    #       - copy 'tcwv_err_mean' into 'tcwv_err', set attributes
    #       - copy 'tcwv_err_ran' into 'tcwv_ran', set attributes
    #       - copy 'tcwv_quality_flag_majority' to 'tcwv_quality_flag', set attributes
    #       - copy 'surface_type_flag_majority' to 'surface_type_flag', set attributes, make cloud_over_land to partly_cloudy_over_land where we have tcwv!
    
#### Copy variables from source product, set attributes and data:   ###

for name, variable in src.variables.iteritems():

    if name == 'num_obs_sum':
        dstvar = dst.createVariable('num_obs', 'i4', variable.dimensions, zlib=True)
        setVariableLongNameAndUnitAttributes(dstvar, 'Number of Total Column of Water Vapour retrievals contributing to L3 grid cell', ' ')
        fill_value = -1
        dstvar.setncattr('_FillValue',  np.array([fill_value], 'i4'))
        dstvar.setncattr('coordinates', 'lat lon')
        dstvar.setncattr('units', ' ')
        dstvar[:,:] = variable[:,:]
    if name == 'tcwv_mean': 
        dstvar = dst.createVariable('tcwv', variable.datatype, variable.dimensions, zlib=True)
        copyVariableAttributesFromSource(variable, dstvar)
        setVariableLongNameAndUnitAttributes(dstvar, 'Total Column of Water', 'kg/m2')
        dstvar.setncattr('standard_name', 'atmosphere_water_vapor_content ')
        dstvar.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')
        tcwv_arr = np.array(variable)
        tcwv_min = np.nanmin(tcwv_arr)
        tcwv_max = np.nanmax(tcwv_arr)
        tcwv_min_valid = 0.0
        tcwv_max_valid = 80.0
        dstvar.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
        dstvar.setncattr('valid_range', np.array([tcwv_min_valid, tcwv_max_valid], 'f4'))
        dstvar.setncattr('ancillary_variables', 'stdv num_obs')
        dstvar[:,:] = variable[:,:]
    if name == 'stdv_mean': 
        dstvar = dst.createVariable('stdv', variable.datatype, variable.dimensions, zlib=True)
        copyVariableAttributesFromSource(variable, dstvar)
        setVariableLongNameAndUnitAttributes(dstvar, 'Standard deviation of Total Column of Water Vapour', 'kg/m2')
        dstvar[:,:] = variable[:,:]
    if name == 'tcwv_err_mean': 
        dstvar = dst.createVariable('tcwv_err', variable.datatype, variable.dimensions, zlib=True)
        copyVariableAttributesFromSource(variable, dstvar)
        setVariableLongNameAndUnitAttributes(dstvar, 'Average retrieval uncertainty', 'kg/m2')
        dstvar[:,:] = variable[:,:]
    if name == 'tcwv_ran_mean':
        dstvar = dst.createVariable('tcwv_ran', variable.datatype, variable.dimensions, zlib=True)
        copyVariableAttributesFromSource(variable, dstvar)
        setVariableLongNameAndUnitAttributes(dstvar, 'Random retrieval uncertainty', 'kg/m2')
        dstvar[:,:] = variable[:,:]
    if name == 'crs': 
        dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
        copyVariableAttributesFromSource(variable, dstvar)
        dstvar.setncattr('long_name', 'Coordinate Reference System ')
        dstvar.setncattr('comment', 'A coordinate reference system (CRS) defines how the georeferenced spatial data relates to real locations on the Earth\'s surface ')
        dstvar[:] = variable[:]
            
    if has_latlon:
        if name == 'lat': 
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            setVariableLongNameAndUnitAttributes(dstvar, 'Latitude', 'degrees_north ')
            dstvar.setncattr('standard_name', 'latitude')
            dstvar.setncattr('valid_range', np.array([lat_min_valid, lat_max_valid], 'f4'))
            dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
            dstvar.setncattr('axis', 'Y')
            dstvar.setncattr('bounds', 'lat_bnds')
            dstvar[:] = variable[:]
        if name == 'lon': 
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            setVariableLongNameAndUnitAttributes(dstvar, 'Longitude', 'degrees_east')
            dstvar.setncattr('standard_name', 'longitude')
            dstvar.setncattr('valid_range', np.array([lon_min_valid, lon_max_valid], 'f4'))
            dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
            dstvar.setncattr('axis', 'X')
            dstvar.setncattr('bounds', 'lon_bnds')
            dstvar[:] = variable[:]

            
### Set TCWV quality flag... ###
# --> DO NOT include in monthly product! (MS, 20190926)
            
#variable = dst.variables['tcwv_quality_flag']
#setVariableLongNameAndUnitAttributes(variable, 'Quality flag of Total Column of Water Vapour', ' ')
#variable.setncattr('standard_name', 'status_flag ')
#fill_value = -128
#variable.setncattr('_FillValue', np.array([fill_value], 'b'))
#min_valid = 0
#max_valid = 2 # TODO: adapt to 0..3 after 'Dataset 2' (second flag for cost function value intervals)
#variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
#variable.setncattr('flag_values', np.array([0, 1, 2], 'b'))
#variable.setncattr('flag_meanings', 'TCWV_OK HIGH_COST_FUNCTION TCWV_INVALID')
#variable[:,:] = src.variables['tcwv_quality_flag_majority'][:,:]

### Set surface type flag.... ###

variable = dst.variables['surface_type_flag']
setVariableLongNameAndUnitAttributes(variable, 'Surface type flag', ' ')
variable.setncattr('standard_name', 'status_flag ')
fill_value = -128
variable.setncattr('_FillValue', np.array([fill_value], 'b'))
min_valid = 0
max_valid = 6
variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
variable.setncattr('flag_values', np.array([0, 1, 2, 3, 4, 5, 6], 'b'))
variable.setncattr('flag_meanings', 'LAND OCEAN CLOUD_OVER_LAND SEA_ICE COAST PARTLY_CLOUDY_OVER_LAND PARTLY_SEA_ICE')

# a) CLOUDY_OVER_LAND (2) in daily products: the pixels identified purely as CLOUD (all L2 samples), no valid TCWV
# b) PARTLY_CLOUDY_OVER_LAND (5) in daily products: the pixels identified in majority as CLOUD, but have a valid TCWV
# c) CLOUDY_OVER_LAND (2) in monthly products: all daily aggregates are CLOUDY_OVER_LAND, no valid TCWV (usually very few pixels)
# d) PARTLY_CLOUDY_OVER_LAND (5) in monthly products: at least 1 , but not all daily aggregates are CLOUDY_OVER_LAND, valid TCWV
# -->
# 1. make cloud_over_land to partly_cloudy_over_land (2 to 5) where we have tcwv (refers to b))
# (we only have cloud_over_land (2, no tcwv) if surface_type = 2 for MIN, MAX and MAJORITY)
# 2. make cloud over land (2) if surface_type = 2 for MIN, MAX and MAJORITY, and no tcwv (refers to c))
# 3. make cloud over land (2) if surface_type = 0 or 5 (clear or partly cloudy), and no tcwv (refers to c))
# 4. make partly_cloudy_over_land (5) if surface_type_MAX = 2  but surface_type_MIN < 2 (cloudy on at least one day but not all days, refers to b))

tcwv_arr_src = np.array(src.variables['tcwv_mean'])
surface_type_flag_arr_maj = np.array(src.variables['surface_type_flag_majority'])
surface_type_flag_arr_min = np.array(src.variables['surface_type_flag_min'])
surface_type_flag_arr_max = np.array(src.variables['surface_type_flag_max'])
tmparr = np.copy(surface_type_flag_arr_maj)

# set to cloudy if partly cloudy but no valid TCWV:
tmparr[np.where((surface_type_flag_arr_maj == 5) & (~np.isfinite(tcwv_arr_src)))] = 2
# set to partly cloudy  if cloudy but valid TCWV: 
tmparr[np.where((surface_type_flag_arr_maj == 2) & (np.isfinite(tcwv_arr_src)))] = 5
# set to partly cloudy  if at least one daily sample is partly cloudy, and not already cloudy: 
tmparr[np.where((surface_type_flag_arr_max == 5) & (tmparr != 2))] = 5


#tmparr[np.where((tmparr == 2) & (np.isfinite(tcwv_arr_src)))] = 5  # (b)
#tmparr[np.where((surface_type_flag_arr_max == 2) & (surface_type_flag_arr_min < 2))] = 5 # (b)
#tmparr[np.where((surface_type_flag_arr_max == 2) & (surface_type_flag_arr_min == 2) & (tmparr == 2))] = 2 # (c)
#tmparr[np.where(((tmparr == 0) | (tmparr == 5)) & (surface_type_flag_arr_maj != 3)  & (surface_type_flag_arr_maj != 6) & (~np.isfinite(tcwv_arr_src)))] = 2 # (c)
variable[:,:] = tmparr[:,:]

### Close files... ###

print >> sys.stderr, "Closing L3 input file..."
src.close()
print >> sys.stderr, "Closing landmask input file..."
ds_landmask.close()
if seaice_available:
    print >> sys.stderr, "Closing seaice file..."
    ds_seaice.close()

#dst.close()
print >> sys.stderr, "FINISHED nc-compliance-py-process.py..."

