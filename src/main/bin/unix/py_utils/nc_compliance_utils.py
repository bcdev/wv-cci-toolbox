# -*- coding: utf-8 -*-
# ! /usr/bin/env python
from __future__ import print_function

# Generates final CF- and CCI-compliant TCWV L3 daily products ready for Phase 2 delivery.
#
__author__ = 'olafd'

import os
import calendar
import datetime
import sys
import uuid

import numpy as np
import scipy.ndimage
from netCDF4 import Dataset

LAT_MIN_VALID = -90.0
LAT_MAX_VALID = 90.0
LON_MIN_VALID = -180.0
LON_MAX_VALID = 180.0


#############################################################################
def is_py3():
    return sys.version_info.major == 3


def is_cdr_1(sensor):
    return not is_cdr_2(sensor)


def is_cdr_2(sensor):
    return sensor.find("hoaps") != -1


def is_tcwv_l3_unmerged_product(sensor):
    return sensor.find("-") == -1


def get_iteritems(iterable_obj):
    """
    Wraps Python2/3 difference for iterable objects
    :param iterable_obj:
    :return:
    """
    if is_py3():
        return iterable_obj.items()
    else:
        return iterable_obj.iteritems()


def copy_variable_attributes_from_source(srcvar, dst_var):
    """
    Copies variable attributes from source to destination nc4.
    :param srcvar:
    :param dst_var:
    :return:
    """
    for attr in srcvar.ncattrs():
        if getattr(srcvar, attr) and attr != '_FillValue':
            if attr in dst_var.ncattrs():
                dst_var.delncattr(attr)
            dst_var.setncattr(attr, getattr(srcvar, attr))


def set_variable_long_name_and_unit_attributes(dst_var, long_name_string, unit_string):
    """
    Sets variable long name and unit attributes in destination nc4.
    :param dst_var:
    :param long_name_string:
    :param unit_string:
    :return:
    """
    if 'long_name' in dst_var.ncattrs():
        dst_var.delncattr('long_name')
    if 'units' in dst_var.ncattrs():
        dst_var.delncattr('units')
    dst_var.setncattr('long_name', long_name_string)
    dst_var.setncattr('units', unit_string)


def reset_var_to_value(dst_var, dst_indices, value):
    """
    Resets array values to value at given indices for given variable in destination nc4.
    :param dst_var:
    :param dst_indices:
    :param value: the new value
    :return:
    """
    dst_var_arr_0 = np.array(dst_var)[0]
    dst_var_arr_0[dst_indices] = value
    dst_var[0, :, :] = dst_var_arr_0[:, :]


def reset_ocean_num_obs_nir(sensor, dst_var, surface_type_array, reset_value):
    """
    Resets everything to nan over ocean, seaice, coastlines, partly seaice in case of CDR-1 (no HOAPS, only land)
    :param sensor:
    :param dst_var:
    :param surface_type_array:
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    if is_cdr_1(sensor):
        tmp_array[np.where((surface_type_array == 1) |
                           (surface_type_array == 4) |
                           (surface_type_array == 5) |
                           (surface_type_array == 6))] = reset_value
    else:
        tmp_array[np.where(surface_type_array == 1)] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


def reset_ocean_cdr1(dst_var, surface_type_array, reset_value):
    """
    Resets everything to nan over ocean, seaice, coastlines, partly seaice in case of CDR-1 (no HOAPS, only land)
    :param dst_var:
    :param surface_type_array:
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    tmp_array[np.where((surface_type_array == 1) |
                       (surface_type_array == 4) |
                       (surface_type_array == 5) |
                       (surface_type_array == 6))] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


def reset_ocean_cdr2(dst_var, wvpa_hoaps_array, surface_type_array, reset_value):
    """
    Resets (cleans) everything to nan over ocean, seaice where we have no HOAPS (wvpa) over water in case of CDR-2
    :param dst_var:
    :param wvpa_hoaps_array:
    :param surface_type_array:
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    tmp_array[np.where((surface_type_array == 1) & (wvpa_hoaps_array < 0.0))] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


def reset_polar(dst_var, tcwv_arr, lat_arr, surface_type_array, atm_cond_arr, reset_value):
    """
    Resets everything to nan everywhere except ocean for tcwv > 10 and abs(lat) > 75
    :param dst_var:
    :param tcwv_arr:
    :param lat_arr:
    :param surface_type_array:
    :param atm_cond_arr:
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    # identify inconsistent pixel over non-land
    # heavy precip, partly cloudy, seaice :
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((atm_cond_arr == 3) |
                        (atm_cond_arr == 1) |
                        (surface_type_array == 4)))] = reset_value
    # identify inconsistent pixel over land
    # land, coast, cloudy:
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((surface_type_array == 0) | (surface_type_array == 5) | (
                               atm_cond_arr == 2)))] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


def upscale_auxdata(src_arr, target_res):
    """
    Rescales 0.5deg auxdata array (e.g. landmask) to 0.05deg target resolution

    :param src_arr:
    :param target_res:
    :return:
    """
    if target_res == '005':
        if len(src_arr.shape) == 3:
            return scipy.ndimage.zoom(src_arr[0], 10, order=0)
        else:
            return scipy.ndimage.zoom(src_arr, 10, order=0)
    else:
        # no rescaling, target resolution already 0.5deg
        if len(src_arr.shape) == 3:
            return src_arr[0]
        else:
            return src_arr


def downscale_auxdata(src_arr, target_res):
    """
    Rescales 0.05deg auxdata array (e.g. landcover) to 0.5deg target resolution

    :param src_arr:
    :param target_res:
    :return:
    """
    if target_res == '05':
        if len(src_arr.shape) == 3:
            return scipy.ndimage.zoom(src_arr[0], 0.1, order=0)
        else:
            return scipy.ndimage.zoom(src_arr, 0.1, order=0)
    else:
        # no rescaling, target resolution already 0.05deg
        if len(src_arr.shape) == 3:
            return src_arr[0]
        else:
            return src_arr


def set_lat_lon_variables_global(dst, res, src):
    """
    Sets latitude and longitude variables and their attributes.
    :param dst:
    :param res:
    :param src:
    :return:
    """
    # if not present in source product, create lat/lon variables as 1D:
    has_latlon = False
    for name, variable in get_iteritems(src.variables):
        # print('src variable: ' + name)
        if name == 'lat' or name == 'lon':
            has_latlon = True
    # print('has_latlon: ' + str(has_latlon))
    if not has_latlon:
        incr = 0.05 if res == '005' else 0.5
        lat_arr = np.arange(90.0, -90.0, -incr) - incr / 2.0
        lon_arr = np.arange(-180.0, 180.0, incr) + incr / 2.0
        # set new lat/lon variables:
        lat = dst.createVariable('lat', 'f4', 'lat', zlib=True)
        lon = dst.createVariable('lon', 'f4', 'lon', zlib=True)
        lat.setncattr('long_name', 'Latitude')
        lat.setncattr('standard_name', 'latitude')
        lat.setncattr('units', 'degrees_north')
        lat.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
        lat.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lat.setncattr('axis', 'Y')
        lat.setncattr('bounds', 'lat_bnds')
        lon.setncattr('long_name', 'Longitude')
        lon.setncattr('standard_name', 'longitude')
        lon.setncattr('units', 'degrees_east')
        lon.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
        lon.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lon.setncattr('axis', 'X')
        lon.setncattr('bounds', 'lon_bnds')

        lat[:] = lat_arr
        lon[:] = lon_arr

    width = len(dst.dimensions['lon'])
    height = len(dst.dimensions['lat'])

    # create 'lat_bnds' and 'lon_bnds' variables:
    incr = 0.05 if res == '005' else 0.5
    lat_bnds_arr_0 = np.arange(90.0, -90.0, -incr)
    lat_bnds_arr_1 = np.arange(90.0 - incr, -90.0 - incr, -incr)
    lon_bnds_arr_0 = np.arange(-180.0, 180.0, incr)
    lon_bnds_arr_1 = np.arange(-180.0 + incr, 180.0 + incr, incr)
    lat_bnds_arr = np.empty(shape=[height, 2])
    lon_bnds_arr = np.empty(shape=[width, 2])
    lat_bnds_arr[:, 0] = lat_bnds_arr_0
    lat_bnds_arr[:, 1] = lat_bnds_arr_1
    lon_bnds_arr[:, 0] = lon_bnds_arr_0
    lon_bnds_arr[:, 1] = lon_bnds_arr_1
    lat_bnds = dst.createVariable('lat_bnds', 'f4', ('lat', 'nv'), zlib=True)
    lon_bnds = dst.createVariable('lon_bnds', 'f4', ('lon', 'nv'), zlib=True)
    lat_bnds.setncattr('long_name', 'Latitude cell boundaries')
    # CF compliance checker sometimes complains about units, sometimes not... leave them out for now
    # lat_bnds.setncattr('units', 'degrees_north')
    lat_bnds.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
    lat_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lat_bnds.setncattr('comment', 'Contains the northern and southern boundaries of the grid cells.')
    lon_bnds.setncattr('long_name', 'Longitude cell boundaries')
    # lon_bnds.setncattr('units', 'degrees_east')
    lon_bnds.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
    lon_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lon_bnds.setncattr('comment', 'Contains the eastern and western boundaries of the grid cells.')
    lat_bnds[:, :] = lat_bnds_arr
    lon_bnds[:, :] = lon_bnds_arr

    return has_latlon, height, width


def set_lat_lon_variables_regional(dst, lat_min, lat_max, lon_min, lon_max, src):
    """
    Sets latitude and longitude variables and their attributes.
    :param dst:
    :param lat_min:
    :param lat_max:
    :param lon_min:
    :param lon_max:
    :param src:
    :return:
    """
    # if not present in source product, create lat/lon variables as 1D:
    has_latlon = False
    for name, variable in get_iteritems(src.variables):
        # print('src variable: ' + name)
        if name == 'lat' or name == 'lon':
            has_latlon = True
    # print('has_latlon: ' + str(has_latlon))
    if not has_latlon:
        incr = 0.01
        lat_arr = np.arange(lat_max, lat_min, -incr) - incr / 2.0
        lon_arr = np.arange(lon_min, lon_max, incr) + incr / 2.0
        # set new lat/lon variables:
        lat = dst.createVariable('lat', 'f4', 'lat', zlib=True)
        lon = dst.createVariable('lon', 'f4', 'lon', zlib=True)
        lat.setncattr('long_name', 'Latitude')
        lat.setncattr('standard_name', 'latitude')
        lat.setncattr('units', 'degrees_north')
        lat.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
        lat.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lat.setncattr('axis', 'Y')
        lat.setncattr('bounds', 'lat_bnds')
        lon.setncattr('long_name', 'Longitude')
        lon.setncattr('standard_name', 'longitude')
        lon.setncattr('units', 'degrees_east')
        lon.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
        lon.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
        lon.setncattr('axis', 'X')
        lon.setncattr('bounds', 'lon_bnds')

        lat[:] = lat_arr
        lon[:] = lon_arr

    width = len(dst.dimensions['lon'])
    height = len(dst.dimensions['lat'])

    # create 'lat_bnds' and 'lon_bnds' variables:
    incr = 0.01
    lat_bnds_arr_0 = np.arange(lat_max, lat_min, -incr)
    # lat_bnds_arr_1 = np.arange(lat_max - incr, lat_min - incr, -incr) # why this? I don't remember (OD, 29230712)
    lon_bnds_arr_0 = np.arange(lon_min, lon_max, incr)
    # lon_bnds_arr_1 = np.arange(lon_min + incr, lon_max + incr, incr)  # s.a.
    lat_bnds_arr = np.empty(shape=[height, 2])
    lon_bnds_arr = np.empty(shape=[width, 2])
    lat_bnds_arr[:, 0] = lat_bnds_arr_0
    # lat_bnds_arr[:, 1] = lat_bnds_arr_1
    lat_bnds_arr[:, 1] = lat_bnds_arr_0
    lon_bnds_arr[:, 0] = lon_bnds_arr_0
    # lon_bnds_arr[:, 1] = lon_bnds_arr_1
    lon_bnds_arr[:, 1] = lon_bnds_arr_0
    lat_bnds = dst.createVariable('lat_bnds', 'f4', ('lat', 'nv'), zlib=True)
    lon_bnds = dst.createVariable('lon_bnds', 'f4', ('lon', 'nv'), zlib=True)
    lat_bnds.setncattr('long_name', 'Latitude cell boundaries')
    # CF compliance checker sometimes complains about units, sometimes not... leave them out for now
    # lat_bnds.setncattr('units', 'degrees_north')
    lat_bnds.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
    lat_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lat_bnds.setncattr('comment', 'Contains the northern and southern boundaries of the grid cells.')
    lon_bnds.setncattr('long_name', 'Longitude cell boundaries')
    # lon_bnds.setncattr('units', 'degrees_east')
    lon_bnds.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
    lon_bnds.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    lon_bnds.setncattr('comment', 'Contains the eastern and western boundaries of the grid cells.')
    lat_bnds[:, :] = lat_bnds_arr
    lon_bnds[:, :] = lon_bnds_arr

    return has_latlon, height, width


def create_nc_lat_variable(dst, variable):
    """
    Creates lat variable in NetCDF destination dataset
    :param dst: NetCDF destination dataset
    :param variable: nc destination variable
    :return:
    """
    dstvar = dst.createVariable('lat', variable.datatype, variable.dimensions, zlib=True)
    set_variable_long_name_and_unit_attributes(dstvar, 'Latitude', 'degrees_north ')
    dstvar.setncattr('standard_name', 'latitude')
    dstvar.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
    dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    dstvar.setncattr('axis', 'Y')
    dstvar.setncattr('bounds', 'lat_bnds')
    dstvar[:] = variable[:]


def create_nc_lon_variable(dst, variable):
    """
    Creates lon variable in NetCDF destination dataset
    :param dst: NetCDF destination dataset
    :param variable: nc destination variable
    :return:
    """
    dstvar = dst.createVariable('lon', variable.datatype, variable.dimensions, zlib=True)
    set_variable_long_name_and_unit_attributes(dstvar, 'Longitude', 'degrees_east')
    dstvar.setncattr('standard_name', 'longitude')
    dstvar.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
    dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
    dstvar.setncattr('axis', 'X')
    dstvar.setncattr('bounds', 'lon_bnds')
    dstvar[:] = variable[:]


def create_time_variables(dst, day, month, year):
    """
    Creates time related variables. Also sets the actual timevalue for this product (days since 19700101)
    :param dst:
    :param day:
    :param month:
    :param year:
    :return:
    """
    import datetime

    # use days since 1970-01-01 as time value, and the given day at 12:00 as reference time...
    timeval = (datetime.datetime(int(year), int(month), int(day)) - datetime.datetime(1970, 1, 1)).days
    # create 'time_bnds' variable:
    time_bnds = dst.createVariable('time_bnds', 'i4', ('time', 'nv'), zlib=True)
    time_bnds[0, 0] = timeval
    time_bnds[0, 1] = timeval + 1
    time_bnds.setncattr('long_name', 'Time cell boundaries')
    time_bnds.setncattr('comment', 'Contains the start and end times for the time period the data represent.')

    # create time variable and set time data
    time = dst.createVariable('time', 'i4', 'time', zlib=True)
    time.setncattr('long_name', 'Product dataset time given as days since 1970-01-01')
    time.setncattr('standard_name', 'time')
    time.setncattr('units', 'days since 1970-01-01')
    time.setncattr('calendar', 'gregorian')
    time.setncattr('axis', 'T')
    time.setncattr('bounds', 'time_bnds')
    time[:] = int(timeval)


def init_nc_compliant_product(datestring, res, sensor, version):
    """
    Setup nc result file and dataset for input daily product.
    :param datestring:
    :param res:
    :param sensor:
    :param version:

    :return: nc_compliant_ds: nc4 dataset in target product; nc_outfile: target nc4 file
    """
    if sensor.find("-") != -1:
        l3_suffix = 'S'
    else:
        l3_suffix = 'C'
    # final product name following CCI data standards v2.1 section 2.7:
    _sensor = sensor.replace('-', '_')
    nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + _sensor + '-' + res + 'deg-' + datestring + \
                 '-fv' + version + '.nc'
    outpath = os.getcwd() + os.sep + nc_outfile
    nc_compliant_ds = Dataset(outpath, 'w', format='NETCDF4')

    return nc_compliant_ds, nc_outfile


def set_dimensions(dst, src):
    """
    Sets all dimensions in nc compliant product.
    :param dst:
    :param src:
    :return:
    """
    # set dimensions from src:
    for name, dimension in get_iteritems(src.dimensions):
        dst.createDimension(name, len(dimension) if not dimension.isunlimited() else None)
    # if not present in source product, create 'time: dimension:
    has_timedim = False
    for name, dimension in get_iteritems(src.dimensions):
        if name == 'time':
            has_timedim = True
    if not has_timedim:
        # set new time dimension:
        dst.createDimension('time', None)

    dst.createDimension('nv', 2)


def set_global_attributes(sensor, datestring, dst, day, month, year, res, version, nc_infile, nc_outfile):
    """
    Sets all global attributes  in nc compliant product.
    CCI data standards v2.1 section 2.5.1. Updated to the latest agreements in team, 20201015.
    :param sensor:
    :param datestring:
    :param dst:
    :param day:
    :param month:
    :param year:
    :param res:
    :param version:
    :param nc_infile:
    :param nc_outfile:
    :return:
    """

    dst.setncattr('title', get_global_attr_title(sensor))
    dst.setncattr('institution', get_global_attr_institution(sensor))
    dst.setncattr('publisher_name', get_global_attr_publisher_name(sensor))
    dst.setncattr('publisher_email', get_global_attr_publisher_email(sensor))
    dst.setncattr('publisher_url', get_global_attr_publisher_url(sensor))
    dst.setncattr('source', get_global_attr_source(sensor))
    dst.setncattr('history', 'python nc_compliance_daily.py ' + nc_infile)
    dst.setncattr('references',
                  'WV_cci D2.2: ATBD Part 1 - MERIS-MODIS-OLCI L2 Products, Issue 2.1, 21 January 2021; WV_cci D4.2: '
                  'CRDP Issue 3.0, 11 August 2021 ')
    dst.setncattr('tracking_id', str(uuid.uuid1()))
    dst.setncattr('Conventions', 'CF-1.7')
    dst.setncattr('product_version', version)
    dst.setncattr('format_version', 'CCI Data Standards v2.0')
    dst.setncattr('summary', get_global_attr_summary(sensor))
    dst.setncattr('keywords',
                  'EARTH SCIENCE > ATMOSPHERE > ATMOSPHERIC WATER VAPOR > WATER VAPOR,EARTH SCIENCE > ATMOSPHERE > '
                  'ATMOSPHERIC WATER VAPOR > PRECIPITABLE WATER')
    dst.setncattr('id', get_global_attr_id(sensor))
    dst.setncattr('naming-authority', get_global_attr_naming_authority(sensor))
    dst.setncattr('filename', nc_outfile)
    dst.setncattr('keywords-vocabulary', 'GCMD Science Keywords, Version 8.1')
    dst.setncattr('cdm_data_type', 'grid')
    dst.setncattr('comment', get_global_attr_comment(sensor))
    from datetime import datetime
    date_created = str(datetime.utcnow())[:19] + ' UTC'
    dst.setncattr('date_created', date_created)
    dst.setncattr('creator_name', 'ESA Water_Vapour_cci; Brockmann Consult; DWD; EUMETSAT/CM SAF; Spectral Earth')
    dst.setncattr('creator_url', 'http://cci.esa.int/watervapour')
    dst.setncattr('creator_email', get_global_attr_creator_email(sensor))
    dst.setncattr('project', get_global_attr_project(sensor))
    dst.setncattr('acknowledgement', get_global_attr_acknowledgement(sensor))
    dst.setncattr('geospatial_lat_min', '-90.0')
    dst.setncattr('geospatial_lat_max', '90.0')
    dst.setncattr('geospatial_lon_min', '-180.0')
    dst.setncattr('geospatial_lon_max', '180.0')
    dst.setncattr('geospatial_vertical_min', '0.0')
    dst.setncattr('geospatial_vertical_max', '0.0')
    if int(day) == 0:
        num_days_in_month = calendar.monthrange(int(year), int(month))[1]
        starttime = datestring + '-01 00:00:00 UTC'
        endtime = datestring + '-' + str(num_days_in_month) + ' 23:59:59 UTC'
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
    dst.setncattr('license', get_global_attr_license(sensor))
    dst.setncattr('platform', get_global_attr_platform(sensor))
    dst.setncattr('sensor', 'Medium Resolution Imaging Spectrometer; Moderate-Resolution Imaging Spectroradiometer; '
                            'Ocean and Land Colour Instrument; Special Sensor Microwave Imager/Sounder')
    spatial_resolution = '5.6km at Equator' if res == '005' else '56km at Equator'
    dst.setncattr('spatial_resolution', spatial_resolution)
    dst.setncattr('geospatial_lat_units', 'degrees_north')
    dst.setncattr('geospatial_lon_units', 'degrees_east')
    geospatial_resolution = '0.05' if res == '005' else '0.5'
    dst.setncattr('geospatial_lat_resolution', geospatial_resolution)
    dst.setncattr('geospatial_lon_resolution', geospatial_resolution)
    dst.setncattr('key_variables', 'tcwv')


def get_global_attr_title(sensor):
    if is_cdr_1(sensor):
        return 'Global Total Column of Water Vapour Product from Near Infrared Imagers'
    else:
        return 'Global Total Column of Water Vapour Product from Microwave and Near Infrared Imagers'


def get_global_attr_institution(sensor):
    if is_cdr_1(sensor):
        return 'ESACCI'
    else:
        return 'EUMETSAT/CM SAF'


def get_global_attr_publisher_name(sensor):
    if is_cdr_1(sensor):
        return 'ESACCI'
    else:
        return 'EUMETSAT/CM SAF'


def get_global_attr_publisher_email(sensor):
    if is_cdr_1(sensor):
        return 'climate.office@esa.int'
    else:
        return 'contact.cmsaf@dwd.de'


def get_global_attr_publisher_url(sensor):
    if is_cdr_1(sensor):
        return 'https://climate.esa.int/en/esa-climate/esa-cci/'
    else:
        return 'http://cmsaf.eu'


def get_global_attr_source(sensor):
    if is_cdr_1(sensor):
        return 'Near-infrared Level 3 data over land from Brockmann Consult and Spectral Earth'
    else:
        return 'Near-infrared Level 3 data (land, sea-ice and coast) from Brockmann Consult and ' + \
               'Spectral Earth : microwave imager Level 3 data (ice-free ocean) from EUMETSAT CM SAF : ' + \
               'combined NIR and MW data from Brockmann Consult and Spectral Earth : the combined product ' + \
               'was funded by and generated within the ESA Water_Vapour_cci project'


def get_global_attr_summary(sensor):
    if is_cdr_1(sensor):
        return 'This global TCWV data record was generated from MERIS, MODIS and OLCI ' + \
               'observations over land. The product covers the ' + \
               'period 2002-2017 with daily and montlhy as well as 0.05° and 0.5° temporal and spatial ' + \
               'resolutions, respectively.'
    else:
        return 'This global TCWV data record makes use of the complementary spatial coverage of near ' + \
               'infrared (NIR) and microwave imager (MW) observations: SSM/I observations were used to ' + \
               'generate TCWV data over the global ice-free ocean while MERIS, MODIS and OLCI ' + \
               'observations were used over land, coastal areas and sea-ice. The product covers the ' + \
               'period 2002-2017 with daily and montlhy as well as 0.05° and 0.5° temporal and spatial ' + \
               'resolutions, respectively.'


def get_global_attr_id(sensor):
    if is_cdr_1(sensor):
        # return '10.5285/a5c833831e26474bb1100ad3aa58bdf9'
        return '10.5285/4a85c0ef880e4f668cd4ec8e846855ef'
    else:
        return '10.5676/EUM_SAF_CM/COMBI/V001'


def get_global_attr_naming_authority(sensor):
    if is_cdr_1(sensor):
        return 'ESACCI'
    else:
        return 'EUMETSAT/CM SAF'


def get_global_attr_comment(sensor):
    if is_cdr_1(sensor):
        return 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ' + \
               'ESA Climate Change Initiative Extension (CCI+) Phase 1.'
    else:
        return 'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ' + \
               'ESA Climate Change Initiative Extension (CCI+) Phase 1. ' + \
               'They include CM SAF products over the ocean.'


def get_global_attr_creator_email(sensor):
    if is_cdr_1(sensor):
        return 'climate.office@esa.int'
    else:
        return 'contact.cmsaf@dwd.de'


def get_global_attr_project(sensor):
    if is_cdr_1(sensor):
        return 'Climate Change Initiative - European Space Agency'
    else:
        return 'CM SAF'


def get_global_attr_acknowledgement(sensor):
    if is_cdr_1(sensor):
        return 'The combined MW and NIR product was initiated and funded by the ESA Water_Vapour_cci ' + \
               'project. The NIR retrieval was developed by Spectral Earth. The NIR data was processed ' + \
               'by Brockmann Consult. NIR data is owned by Brockmann Consult and Spectral Earth.'
    else:
        return 'The combined MW and NIR product was initiated and funded by the ESA Water_Vapour_cci ' + \
               'project. The NIR retrieval was developed by Spectral Earth. The NIR data was processed ' + \
               'and combined with the MW data by Brockmann Consult. NIR data is owned by ' + \
               'Brockmann Consult and Spectral Earth.'


def get_global_attr_license(sensor):
    if is_cdr_1(sensor):
        return 'ESA CCI Data Policy: free and open access'
    else:
        return 'The CM SAF data are owned by EUMETSAT and are available to all users free of charge and with no ' + \
               'conditions to use. If you wish to use these products, EUMETSATs copyright credit must be shown ' + \
               'by displaying the words "Copyright (c) ([release-year]) EUMETSAT" under/in each of these SAF ' + \
               'Products used in a project or shown in a publication or website. Please follow the citation ' + \
               'guidelines given at [DOI landing-page] and also register as a user at http://cm-saf.eumetsat.int/ ' + \
               'to receive latest information on CM SAF services and to get access to the CM SAF User Help Desk.'


def get_global_attr_platform(sensor):
    if is_cdr_1(sensor):
        return 'Envisat, Terra, Sentinel-3'
    else:
        return 'Environmental Satellite; Earth Observing System, Terra (AM-1); ' + \
               'Defense Meteorological Satellite Program-F16; ' + \
               'Defense Meteorological Satellite Program-F17; ' + \
               'Defense Meteorological Satellite Program-F18'


def get_maximum_single_sensors_list(year, month):
    # Sensor availability:
    # 03/2000 - 06/2002: MODIS_TERRA
    # 07/2002 - 03/2012: MODIS_TERRA, MODIS_AQUA, MERIS
    # 04/2012 - 03/2016: MODIS_TERRA, MODIS_AQUA
    # 04/2016 - 12/2018: MODIS_TERRA, MODIS_AQUA, OLCI_A
    # 01/2019 - 12/2023: MODIS_TERRA, MODIS_AQUA, OLCI_A, OLCI_B

    if int(year) < 2002:
        return ['MODIS_TERRA']
    if int(year) == 2002:
        if int(month) < 6:
            return ['MODIS_TERRA']
        else:
            return ['MODIS_TERRA', 'MODIS_AQUA', 'MERIS']
    if 2002 < int(year) < 2012:
        return ['MODIS_TERRA', 'MODIS_AQUA', 'MERIS']
    if int(year) == 2012:
        if int(month) < 4:
            return ['MODIS_TERRA', 'MODIS_AQUA', 'MERIS']
        else:
            return ['MODIS_TERRA', 'MODIS_AQUA']
    if 2012 < int(year) < 2016:
        return ['MODIS_TERRA', 'MODIS_AQUA']
    if int(year) == 2016:
        if int(month) < 4:
            return ['MODIS_TERRA', 'MODIS_AQUA']
        else:
            return ['MODIS_TERRA', 'MODIS_AQUA', 'OLCI_A']
    if 2016 < int(year) < 2019:
        return ['MODIS_TERRA', 'MODIS_AQUA', 'OLCI_A']
    if int(year) >= 2019:
        return ['MODIS_TERRA', 'MODIS_AQUA', 'OLCI_A', 'OLCI_B']

def get_cloud_buffer(cld_arr, cld_val=1, buf=3):
    # cld_buf = np.copy(cld_arr)
    cld_buf = np.zeros(cld_arr.shape, dtype=int)

    ind_x, ind_y = np.where(cld_arr == cld_val)

    for p in range(len(ind_x)):
        left_border = np.maximum(ind_x[p] - buf, 0)
        right_border = np.minimum(ind_x[p] + buf, cld_arr.shape[0] - 1)
        top_border = np.maximum(ind_y[p] - buf, 0)
        bottom_border = np.minimum(ind_y[p] + buf, cld_arr.shape[1] - 1)

        for i in range(left_border, right_border + 1):
            for j in range(top_border, bottom_border + 1):
                cld_buf[i, j] = 1

    indices = np.where(cld_buf == 1)
    return cld_buf, indices


def apply_cloud_buffer(dst, maximum_single_sensors_list):
    # partly_cldbuf, partly_indices = \
    #     get_cloud_buffer(np.array(dst.variables['atmospheric_conditions_flag'])[0], cld_val=1)
    total_cldbuf, total_indices = \
        get_cloud_buffer(np.array(dst.variables['atmospheric_conditions_flag'])[0], cld_val=2)
    # reset_var_to_value(dst.variables['tcwv'], partly_indices, np.nan)
    reset_var_to_value(dst.variables['tcwv'], total_indices, np.nan)
    # reset_var_to_value(dst.variables['tcwv_quality_flag'], partly_indices, 3)
    reset_var_to_value(dst.variables['tcwv_quality_flag'], total_indices, 3)
    for name, variable in get_iteritems(dst.variables):
        for single_sensor in maximum_single_sensors_list:
            if name == 'num_obs_' + single_sensor:
                # reset_var_to_value(dst.variables[name], partly_indices, 0)
                reset_var_to_value(dst.variables[name], total_indices, 0)

# /**
# * Computes solar zenith angle at local noon as function of Geoposition and DoY
# *
# * @param geoPos - geoposition
# * @param doy    - day of year
# * @return sza - in degrees!!
# */
# public static double computeSza(GeoPos geoPos, int doy) {
#
#     final double latitude = geoPos.getLat() * MathUtils.DTOR;
#
# // # To emulate MODIS products, set fixed LST = 12.00
# final double LST = 12.0;
# // # Now we can calculate the Sun Zenith Angle (SZArad):
# final double h = (12.0 - (LST)) / 12.0 * Math.PI;
# final double delta = -23.45 * (Math.PI / 180.0) * Math.cos(2 * Math.PI / 365.0 * (doy + 10));
# double SZArad = Math.acos(Math.sin(latitude) * Math.sin(delta) + Math.cos(latitude) * Math.cos(delta) * Math.cos(h));
#
# return SZArad * MathUtils.RTOD;
# }

def get_sza_from_doy(doy, lat):
    """
    Computes solar zenith angle at local noon as function of latitude and DoY
    :param doy: day of year
    :param lat: latitude in degrees
    :return: sza in degrees
    """
    DTOR = 0.017453292519943295
    RTOD = 57.29577951308232

    latitude = lat * DTOR

    # To emulate MODIS products, set fixed LST = 12.00:
    LST = 12.0

    # Now we can calculate the Sun Zenith Angle (SZArad):
    h = (12.0 - (LST)) / 12.0 * np.pi
    delta = -23.45 * (np.pi / 180.0) * np.cos(2 * np.pi / 365.0 * (doy + 10))
    SZArad = np.acos(np.sin(latitude) * np.sin(delta) + np.cos(latitude) * np.cos(delta) * np.cos(h))

    return SZArad * RTOD


def get_sza_from_date(yyyy, mm, dd, lat):
    """
    Computes solar zenith angle at local noon as function of latitude and DoY
    :param yyyy: year
    :param mm: month
    :param day: day
    :param lat: latitude in degrees (can be a numpy array)
    :return: sza in degrees (can be a numpy array)
    """
    DTOR = 0.017453292519943295
    RTOD = 57.29577951308232

    latitude = lat * DTOR

    # To emulate MODIS products, set fixed LST = 12.00:
    LST = 12.0

    doy = (datetime.datetime(int(yyyy), int(mm), int(dd)) - datetime.datetime(yyyy, 1, 1)).days + 1

    # Now we can calculate the Sun Zenith Angle (SZArad):
    h = (12.0 - (LST)) / 12.0 * np.pi
    delta = -23.45 * (np.pi / 180.0) * np.cos(2 * np.pi / 365.0 * (doy + 10))
    SZArad = np.acos(np.sin(latitude) * np.sin(delta) + np.cos(latitude) * np.cos(delta) * np.cos(h))

    return SZArad * RTOD

