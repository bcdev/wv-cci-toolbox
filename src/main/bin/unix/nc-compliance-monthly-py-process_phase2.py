# -*- coding: utf-8 -*-
# !/usr/bin/env python
from __future__ import print_function

__author__ = 'olafd'

# Generates final CF- and CCI-compliant TCWV L3 MONTHLY products ready for delivery.
# Usage: python nc-compliance-monthly-py-process.py ./${nc_infile} ${sensor} ${year} ${month} ${resolution} ${version}
# Example: python nc-compliance-monthly-py-process.py l3_tcwv_olci_005deg_2018-07-01_2018-07-31.nc olci 2018 07 005 2.0
#
# OD, 20191111

import os
import sys
import uuid
import numpy as np
import calendar

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


def set_ocean_wvpa_errors(dst_var, wvpa_error_array):
    """
    Sets HOAPS water vapour error terms over ocean.
    :param dst_var:
    :param wvpa_error_array:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    dst_var_arr_0 = np.copy(dst_var_arr)[0]
    wvpa_error_array_2d = wvpa_error_array[0]

    do_use_hoaps = np.where(((~np.isnan(wvpa_error_array_2d)) & (wvpa_error_array_2d >= 0.0)))
    dst_var_arr_0[do_use_hoaps] = wvpa_error_array_2d[do_use_hoaps]
    dst_var[0, :, :] = dst_var_arr_0[:, :]


def update_errors_for_hoaps(dst, src):
    """
    Wrapper function for setting HOAPS error terms over ocean.
    :param dst:
    :param src:
    :return:
    """
    wvpa_err_arr = np.array(src.variables['wvpa_err'])
    wvpa_ran_arr = np.array(src.variables['wvpa_ran'])

    # if no wvpa errors available, set to NaN over ocean (should no longer happen for latest HOAPS L3 products)
    set_ocean_wvpa_errors(dst.variables['tcwv_err'], wvpa_err_arr)
    set_ocean_wvpa_errors(dst.variables['tcwv_ran'], wvpa_ran_arr)


def reset_ocean_cdr1(dst_var, surface_type_array, reset_value):
    """
    Resets everything to nan over ocean, seaice, coastlines in case of CDR-1 (no HOAPS, only land)
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
                       (surface_type_array == 7))] = reset_value
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


def reset_polar(dst_var, tcwv_arr, lat_arr, surface_type_array, reset_value):
    """
    Resets everything to nan everywhere except ocean for tcwv > 10 and abs(lat) > 75
    :param dst_var:
    :param surface_type_array:
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    # identify inconsistent pixel over non-land
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((surface_type_array == 3) |
                        (surface_type_array == 4) |
                        (surface_type_array == 6)))] = reset_value
    # identify inconsistent pixel over land
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((surface_type_array == 0) | (surface_type_array == 2) | (
                               surface_type_array == 5)))] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


# def reset_ocean_for_cdr1(dst, sensor):
#     """
#     Wrapper function for resetting everything to nan over ocean in case of CDR-1 (no HOAPS, only land).
#     :param dst:
#     :param sensor:
#     :return:
#     """
#     var_surface_type = dst.variables['surface_type_flag']
#     surface_type_arr = np.array(var_surface_type)
#     if is_cdr_1(sensor):
#         # set num_obs to 0:
#         reset_ocean_cdr1(dst.variables['num_obs'], surface_type_arr, 0)
#         # set tcwv, stdv, and error terms to nan:
#         reset_ocean_cdr1(dst.variables['tcwv'], surface_type_arr, np.nan)
#         reset_ocean_cdr1(dst.variables['stdv'], surface_type_arr, np.nan)
#         reset_ocean_cdr1(dst.variables['tcwv_err'], surface_type_arr, np.nan)
#         reset_ocean_cdr1(dst.variables['tcwv_ran'], surface_type_arr, np.nan)

def cleanup_inconsistencies(dst, sensor):
    """
    Final cleanup of inconsistencies caused by L3 resampling problems such as Moiree effects, distortion near poles etc.
    :param dst:
    :param sensor:
    :return:
    todo: do we need this at all in monthly final processing?
    """
    var_surface_type = dst.variables['surface_type_flag']
    surface_type_arr = np.array(var_surface_type)
    var_tcwv = dst.variables['tcwv']
    tcwv_arr = np.array(var_tcwv)
    var_lat = dst.variables['lat']
    lat_arr = np.array(var_lat)
    lat_arr_3d = np.zeros((tcwv_arr.shape))
    for i in range(len(lat_arr)):
        lat_arr_3d[0][:][i] = lat_arr[i]

    # cleanup polar regions
    # set num_obs to 0:
    reset_polar(dst.variables['num_obs'], tcwv_arr, lat_arr_3d, surface_type_arr, 0)
    # set tcwv, stdv, and error terms to nan:
    reset_polar(dst.variables['tcwv'], tcwv_arr, lat_arr_3d, surface_type_arr, np.nan)
    reset_polar(dst.variables['stdv'], tcwv_arr, lat_arr_3d, surface_type_arr, np.nan)
    reset_polar(dst.variables['tcwv_err'], tcwv_arr, lat_arr_3d, surface_type_arr, np.nan)
    reset_polar(dst.variables['tcwv_ran'], tcwv_arr, lat_arr_3d, surface_type_arr, np.nan)

    # remove all HOAPS (everything over ocean, coastal, seaice) in case of CDR-1
    if is_cdr_1(sensor):
        # set num_obs to 0:
        reset_ocean_cdr1(dst.variables['num_obs'], surface_type_arr, 0)
        # set tcwv, stdv, and error terms to nan:
        reset_ocean_cdr1(dst.variables['tcwv'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['stdv'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['tcwv_err'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['tcwv_ran'], surface_type_arr, np.nan)

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


def set_surface_type_flag(dst, src, ds_landcover):
    """
    Sets 'surface_type_flag' variable and its attributes.
    Do here: LAND OCEAN INLAND_WATER PERMANENT_WETLANDS SEA_ICE COAST PARTLY_SEA_ICE
    --> LAND: ds_landmask = 1 && ds_landcover != 0 && ds_landcover != 11
    --> OCEAN: ds_landmask = 1
    --> INLAND_WATER: ds_landmask = 1 && ds_landcover = 0
    --> PERMANENT_WETLANDS: ds_landmask = 1 && ds_landcover = 11
    --> SEAICE: no change
    --> PARTLY_SEA_ICE: no change
    :param dst:
    :param src:
    :param day:
    :param res:
    :param ds_landmask:
    :param ds_landcover:
    :param ds_seaice:
    :return:
    """

    variable = dst.variables['surface_type_flag']
    set_variable_long_name_and_unit_attributes(variable, 'Surface type flag', ' ')
    variable.setncattr('standard_name', 'status_flag ')
    min_valid = 0
    max_valid = 6
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3, 4, 5, 6], 'b'))
    variable.setncattr('flag_meanings', 'LAND OCEAN INLAND_WATER PERMANENT_WETLANDS SEA_ICE COAST PARTLY_SEA_ICE')

    # In Phase 2 we want (see PSD Vx.y):
    # LAND (0), OCEAN (1), INLAND_WATER (2), PERMANENT_WETLANDS (3), SEA_ICE (4), COAST (5), PARTLY_SEA_ICE (6)

    surface_type_flag_arr_maj = np.array(src.variables['surface_type_flag_majority'])

    tmparr = np.copy(surface_type_flag_arr_maj)

    # set to land if partly cloudy or cloudy:
    tmparr[np.where((surface_type_flag_arr_maj == 2) | (surface_type_flag_arr_maj == 6))] = 0
    # set to ocean if heavy precip over ocean:
    tmparr[np.where(surface_type_flag_arr_maj == 3)] = 1
    # shift partly seaice flag by 1:
    tmparr[np.where(surface_type_flag_arr_maj == 7)] = 6

    # MODIS landcover info (MCD12C1.A2022001.061.05deg.nc):
    # https://lpdaac.usgs.gov/documents/101/MCD12_User_Guide_V6.pdf
    modis_landcover_flag_arr_src = np.array(ds_landcover.variables['Majority_Land_Cover_Type_1'])

    # --> INLAND_WATER (2): ds_landmask = 1 && ds_landcover = 0:
    tmparr[np.where((tmparr == 0) & (modis_landcover_flag_arr_src == 0))] = 2
    # --> PERMANENT_WETLANDS (8): ds_landmask = 1 && ds_landcover = 11
    tmparr[np.where((tmparr == 0) & (modis_landcover_flag_arr_src == 11))] = 3

    variable[0, :, :] = tmparr[:, :]


def set_atmospheric_conditions_flag(dst, src, ds_hoaps, ds_landmask):
    """
    Sets 'atmospheric_conditions' variable and its attributes.
    Do here: CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN PARTLY_CLOUDY_OVER_LAND
    --> : CLOUD_OVER_LAND (1): land+cloud || inlandwater+cloud || permanentwetlands+cloud
    --> : PARTLY_CLOUDY_OVER_LAND (2): CLOUD_OVER_LAND && valid tcwv
    --> : HEAVY_PRECIP_OVER_OCEAN (3): hoaps_scat_ratio_arr > 0.2
    --> : CLEAR (0): none of those
    :param dst:
    :param src:
    :param ds_hoaps:
    :param ds_landmask:
    :return:
    """

    variable = dst.variables['atmospheric_conditions_flag']
    set_variable_long_name_and_unit_attributes(variable, 'Atmospheric conditions flag', ' ')
    variable.setncattr('standard_name', 'status_flag ')
    min_valid = 0
    max_valid = 3
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3], 'b'))
    variable.setncattr('flag_meanings', 'CLEAR PARTLY_CLOUDY_OVER_LAND CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN')

    # In Phase 2 we want (see PSD Vx.y):
    # CLEAR (0), PARTLY_CLOUDY_OVER_LAND (1), CLOUD_OVER_LAND (2), HEAVY_PRECIP_OVER_OCEAN (3)
    hoaps_surface_type_flag_arr_src = np.array(ds_landmask.variables['mask'])
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    surface_type_flag_arr_maj = np.array(src.variables['surface_type_flag_majority'])
    surface_type_flag_arr_max = np.array(src.variables['surface_type_flag_max'])

    tmparr = np.copy(surface_type_flag_arr_maj)

    # set ocean to clear (no cloud obs):
    tmparr[np.where(surface_type_flag_arr_maj == 1)] = 0
    # set to cloudy if partly cloudy but no valid TCWV:
    tmparr[np.where((surface_type_flag_arr_maj == 6) & (~np.isfinite(tcwv_arr_src)))] = 2
    # set to partly cloudy  if cloudy but valid TCWV:
    tmparr[np.where((surface_type_flag_arr_maj == 2) & (np.isfinite(tcwv_arr_src)))] = 1
    # set to partly cloudy  if at least one daily sample is partly cloudy, and not already cloudy:
    tmparr[np.where((surface_type_flag_arr_max == 6) & (tmparr != 2))] = 1

    if ds_hoaps:
        hoaps_scat_ratio_arr_src = np.array(ds_hoaps.variables['scat_ratio'])
        tmparr[np.where(hoaps_scat_ratio_arr_src[0] > 0.2)] = 3  # hoaps heavy precipitation criterion (MS, 202012)

    # set flag to CLEAR for all remaining others (no cloud over land, no cloud obs over ocean, no precip over ocean):
    tmparr[np.where((tmparr != 1) & (tmparr != 2) & (tmparr != 3))] = 0

    variable[0, :, :] = tmparr[:, :]



def copy_and_rename_variables_from_source_product(dst, src, has_latlon):
    """
    Copies variables from source product, renames to correct names, and sets attributes and data...
    for MONTHLY we just need to
            - copy 'num_obs_sum' into 'num_obs', set attributes
            - copy 'tcwv_mean' into 'tcwv', set attributes
            - copy 'stdv_mean' into 'stdv', set attributes
            - copy 'tcwv_err_mean' into 'tcwv_err', set attributes
            - copy 'tcwv_err_ran' into 'tcwv_ran', set attributes
            - copy 'surface_type_flag_majority' to 'surface_type_flag', set attributes, make cloud_over_land to
                   partly_cloudy_over_land where we have tcwv!

    :param dst:
    :param src:
    :param has_latlon:
    :return:
    """

    for name, variable in src.variables.items():

        if name == 'num_obs':
            # - todo 20201109: per grid cell, we want to have number of days which have a TCWV value:
            # -->  take num_obs = 9*numDaysinMonth (e.g. 279) and 'tcwv_ran_counts' = x*num_obs/numDaysinMonth where x
            # is the number we want. ==> x = tcwv_ran_counts/9 . This is implemented in latest l3-tcwv-monthly.xml
            dstvar = dst.createVariable('num_days_tcwv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar,
                                                       'Number of days in month with a valid TCWV value '
                                                       'in L3 grid cell',
                                                       ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar.setncattr('units', ' ')
            tcwv_ran_counts_arr = np.array(src.variables['tcwv_ran_counts'])
            num_days_tcwv_arr = tcwv_ran_counts_arr * 1.0 / 9.0
            dstvar[0, :, :] = num_days_tcwv_arr[:, :]

        if name == 'num_obs_sum':
            dstvar = dst.createVariable('num_obs', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar,
                                                       'Number of Total Column of Water Vapour retrievals contributing '
                                                       'to L3 grid cell',
                                                       ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar.setncattr('units', ' ')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_mean':
            dstvar = dst.createVariable('tcwv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Total Column of Water', 'kg/m2')
            dstvar.setncattr('standard_name', 'atmosphere_water_vapor_content ')
            dstvar.setncattr('ancillary_variables', 'tcwv_uncertainty tcwv_counts')
            tcwv_arr = np.array(variable)
            tcwv_min_valid = 0.0
            tcwv_max_valid = 70.0
            tcwv_arr[np.where(tcwv_arr < tcwv_min_valid)] = tcwv_min_valid
            tcwv_arr[np.where(tcwv_arr > tcwv_max_valid)] = tcwv_max_valid
            tcwv_min = np.nanmin(tcwv_arr)
            tcwv_max = np.nanmax(tcwv_arr)
            dstvar.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
            dstvar.setncattr('valid_range', np.array([tcwv_min_valid, tcwv_max_valid], 'f4'))
            dstvar.setncattr('ancillary_variables', 'stdv num_obs')
            dstvar[0, :, :] = tcwv_arr[:, :]

        if name == 'stdv_mean':
            dstvar = dst.createVariable('stdv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Standard deviation of Total Column of Water Vapour',
                                                       'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_err_mean':
            dstvar = dst.createVariable('tcwv_err', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Average retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_ran_mean':
            dstvar = dst.createVariable('tcwv_ran', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Random retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'crs':
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            copy_variable_attributes_from_source(variable, dstvar)
            dstvar.setncattr('long_name', 'Coordinate Reference System ')
            dstvar.setncattr('comment',
                             'A coordinate reference system (CRS) defines how the georeferenced spatial data relates '
                             'to real locations on the Earth\'s surface ')
            dstvar[:] = variable[:]

        if has_latlon:
            if name == 'lat':
                dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
                set_variable_long_name_and_unit_attributes(dstvar, 'Latitude', 'degrees_north ')
                dstvar.setncattr('standard_name', 'latitude')
                dstvar.setncattr('valid_range', np.array([LAT_MIN_VALID, LAT_MAX_VALID], 'f4'))
                dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
                dstvar.setncattr('axis', 'Y')
                dstvar.setncattr('bounds', 'lat_bnds')
                dstvar[:] = variable[:]
            if name == 'lon':
                dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
                set_variable_long_name_and_unit_attributes(dstvar, 'Longitude', 'degrees_east')
                dstvar.setncattr('standard_name', 'longitude')
                dstvar.setncattr('valid_range', np.array([LON_MIN_VALID, LON_MAX_VALID], 'f4'))
                dstvar.setncattr('reference_datum', 'geographical coordinates, WGS84 projection')
                dstvar.setncattr('axis', 'X')
                dstvar.setncattr('bounds', 'lon_bnds')
                dstvar[:] = variable[:]


def set_lat_lon_variables(dst, res, src):
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
    time_bnds[0, 0] = timeval - 14  # timeval refers to the 15th in month
    num_days_in_month = calendar.monthrange(int(year), int(month))[1]
    time_bnds[0, 1] = time_bnds[0, 0] + num_days_in_month - 1
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
    nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + sensor + '-' + res + 'deg-' + datestring + '-fv' + \
                 version + '.nc'
    outpath = os.getcwd() + os.sep + nc_outfile
    nc_compliant_ds = Dataset(outpath, 'w', format='NETCDF4')

    return nc_compliant_ds, nc_outfile


def get_ds_seaice(args):
    """
    Returns seaice mask nc4 dataset
    :param args: program arguments
    :return: ds_seaice: seaice mask nc4 dataset
    """
    seaice_mask_file = None
    if len(args) == 10:
        seaice_mask_file = args[9]
    ds_seaice = None
    if seaice_mask_file:
        try:
            ds_seaice = Dataset(seaice_mask_file)
        except OSError:
            print('Cannot read seaice mask file')
            ds_seaice = None
    return ds_seaice


def get_ds_hoaps(args):
    """
    Returns HOAPS original dataset if corresponding input file is given
    (i.e. to apply HOAPS fixes at this final stage to avoid reprocessing full L3 chain).
    :param args: program arguments
    :return: ds_hoaps: hoaps nc4 dataset
    """
    hoaps_file = None
    if len(args) == 11:
        hoaps_file = args[10]
    ds_hoaps = None
    if hoaps_file:
        try:
            ds_hoaps = Dataset(hoaps_file)
        except OSError:
            print('Cannot read original HOAPS L3 file')
            ds_hoaps = None
    return ds_hoaps


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
        # print('dimension: ' + name)
        if name == 'time':
            has_timedim = True
    if not has_timedim:
        # set new time dimension:
        dst.createDimension('time', None)

    dst.createDimension('nv', 2)


def set_global_attributes(sensor, datestring, dst, month, year, res, version, nc_infile, nc_outfile):
    """
    Sets all global attributes  in nc compliant product.
    CCI data standards v2.1 section 2.5.1. Updated to latest agreements in team, 20201015.
    :param sensor:
    :param datestring:
    :param dst:
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
    dst.setncattr('history', 'python nc-compliance-py-process.py ' + nc_infile)
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
    dst.setncattr('id', get_global_attr_id(sensor, nc_outfile))
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
    num_days_in_month = calendar.monthrange(int(year), int(month))[1]
    starttime = datestring + '01 00:00:00 UTC'
    endtime = datestring + str(num_days_in_month) + ' 23:59:59 UTC'
    dst.setncattr('time_coverage_duration', 'P1M')
    dst.setncattr('time_coverage_resolution', 'P1M')
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


def get_global_attr_id(sensor, nc_outfile):
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


def run(args):
    """
    Run the conversion to final nc and CCI compliant product.
    :param args: program arguments
    :return:
    """

    # Evaluate input parameters...
    nc_infile = args[1]
    landmask_file = args[2]
    landcover_file = args[3]
    sensor = args[4].replace('-', '_')
    year = args[5]
    month = args[6]
    res = args[7]
    version = args[8]

    # Source dataset...
    src = Dataset(nc_infile)

    # Land and seaice mask datasets...
    ds_seaice = get_ds_seaice(args)
    ds_landmask = Dataset(landmask_file)
    ds_landcover = Dataset(landcover_file)

    # Original HOAPS dataset if given (20201103: to ingest corrected wvpa_ran values, requested by MS)
    ds_hoaps = get_ds_hoaps(args)

    # Initialize nc result file and dataset...
    # we have MONTHLY products:
    datestring = year + month
    dst, nc_outfile = init_nc_compliant_product(datestring, res, sensor, version)

    # Set dimensions...
    set_dimensions(dst, src)

    # set global attributes...
    set_global_attributes(sensor, datestring, dst, month, year, res, version, nc_infile, nc_outfile)

    # Create time variables...
    # use days since 1970-01-01 as time base value, and the 15th of given month at 00:00 as reference time:
    create_time_variables(dst, '15', month, year)

    # Create lat/lon variables...
    has_latlon, height, width = set_lat_lon_variables(dst, res, src)

    # Create final flag bands:
    # no quality flag in monthlies!
    # dst.createVariable('surface_type_flag', 'b', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
    dst.createVariable('atmospheric_conditions_flag', 'u2', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)
    dst.createVariable('surface_type_flag', 'u2', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)

    copy_and_rename_variables_from_source_product(dst, src, has_latlon)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src, ds_hoaps, ds_landmask)

    # Set final surface type flag...
    set_surface_type_flag(dst, src, ds_landcover)

    # Update (fix) tcwv_err and tcwv_ran in case of existing HOAPS...
    if ds_hoaps:
        update_errors_for_hoaps(dst, ds_hoaps)

    # Cleanup inconsistencies of final arrays at this point:
    cleanup_inconsistencies(dst, sensor)
    var_tcwv_arr = np.array(dst.variables['tcwv'])
    dst.variables['tcwv'].setncattr('actual_range', np.array([np.nanmin(var_tcwv_arr), np.nanmax(var_tcwv_arr)], 'f4'))

    # Close files...

    print("Closing L3 input file...", file=sys.stderr)
    src.close()
    print("Closing landmask input file...", file=sys.stderr)
    ds_landmask.close()
    if ds_seaice:
        print("Closing seaice file...", file=sys.stderr)
        ds_seaice.close()
    if ds_hoaps:
        print("Closing HOAPS L3 file...", file=sys.stderr)
        ds_hoaps.close()

    print("FINISHED nc-compliance-monthly-py-process.py...", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING nc-compliance-monthly-py-process.py", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 10 and len(sys.argv) != 11:
        print('Usage:  python nc-compliance-monthly-py-process.py <nc_infile> <landmask_file> <landcover_file> <sensor> ' +
              '<year> <month> <resolution> < product version> <seaice_mask_file> [<hoaps_l3_file>]')
        sys.exit(-1)

    run(sys.argv)
