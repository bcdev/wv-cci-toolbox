# -*- coding: utf-8 -*-
# ! /usr/bin/env python
from __future__ import print_function

# Generates final CF- and CCI-compliant TCWV L3 daily products ready for Phase 2 delivery.
#
__author__ = 'olafd'

import os
import calendar
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


def reset_var_to_nan(dst_var, dst_indices):
    """
    Resets array values to nan at given indices for given variable in destination nc4.
    :param dst_var:
    :param dst_indices:
    :return:
    """
    dst_var_arr_0 = np.array(dst_var)[0]
    dst_var_arr_0[dst_indices] = np.nan
    dst_var[0, :, :] = dst_var_arr_0[:, :]


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
    :param reset_value:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    tmp_array = np.copy(dst_var_arr)
    # identify inconsistent pixel over non-land
    # heavy precip, partly cloudy, seaice :
    # todo: pass new atmos cond flag to adapt this
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((atm_cond_arr == 3) |
                        (atm_cond_arr == 1) |
                        (surface_type_array == 4)))] = reset_value
    # identify inconsistent pixel over land
    # land, coast, cloudy:
    # todo: pass new atmos cond flag to adapt this
    tmp_array[np.where((tcwv_arr > 20.0) & (np.abs(lat_arr) > 70.0) &
                       ((surface_type_array == 0) | (surface_type_array == 5) | (
                               atm_cond_arr == 2)))] = reset_value
    dst_var[0, :, :] = tmp_array[0, :, :]


def cleanup_inconsistencies(dst, src_hoaps, sensor, res, single_sensors_list):
    """
    Final cleanup of inconsistencies caused by L3 resampling problems such as Moiree effects, distortion near poles etc.
    :param dst:
    :param src_hoaps:
    :param sensor:
    :param res:
    :param single_sensors_list:
    :return:
    """
    var_surface_type = dst.variables['surface_type_flag']
    surface_type_arr = np.array(var_surface_type)
    var_atm_cond = dst.variables['atmospheric_conditions_flag']
    atm_cond_arr = np.array(var_surface_type)
    var_tcwv = dst.variables['tcwv']
    tcwv_arr = np.array(var_tcwv)
    var_lat = dst.variables['lat']
    lat_arr = np.array(var_lat)
    lat_arr_3d = np.zeros(tcwv_arr.shape)
    for i in range(len(lat_arr)):
        lat_arr_3d[0][:][i] = lat_arr[i]

    # cleanup polar regions
    # set num_obs to 0:
    # reset_polar(dst.variables['num_obs'], tcwv_arr, tcwv_quality_arr, lat_arr_3d, surface_type_arr, 0)
    for single_sensor in single_sensors_list:
        reset_polar(dst.variables['num_obs_' + single_sensor], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, 0)

    # set tcwv, stdv, and error terms to nan:
    reset_polar(dst.variables['tcwv'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    reset_polar(dst.variables['stdv'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    reset_polar(dst.variables['tcwv_err'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    reset_polar(dst.variables['tcwv_ran'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    # set tcwv_quality_flag to 3:
    reset_polar(dst.variables['tcwv_quality_flag'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, 3)

    # remove all HOAPS (everything over ocean, coastal, seaice) in case of CDR-1
    # clean everything remaining over ocean where we have no HOAPS (wvpa) over water in case of CDR-2
    if is_cdr_1(sensor):
        # set num_obs to 0:
        for single_sensor in single_sensors_list:
            reset_ocean_cdr1(dst.variables['num_obs_' + single_sensor], surface_type_arr, 0)
        # set tcwv, stdv, and error terms to nan:
        reset_ocean_cdr1(dst.variables['tcwv'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['stdv'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['tcwv_err'], surface_type_arr, np.nan)
        reset_ocean_cdr1(dst.variables['tcwv_ran'], surface_type_arr, np.nan)
        # set tcwv_quality_flag to 3:
        reset_ocean_cdr1(dst.variables['tcwv_quality_flag'], surface_type_arr, 3)
    else:
        wvpa_arr_src = np.array(src_hoaps.variables['wvpa'])
        wvpa_arr = rescale_auxdata(wvpa_arr_src, res)
        # set num_obs to 0:
        for single_sensor in single_sensors_list:
            reset_ocean_cdr2(dst.variables['num_obs_' + single_sensor], wvpa_arr, surface_type_arr, 0)
        # set tcwv, stdv, and error terms to nan:
        reset_ocean_cdr2(dst.variables['tcwv'], wvpa_arr, surface_type_arr, np.nan)
        reset_ocean_cdr2(dst.variables['stdv'], wvpa_arr, surface_type_arr, np.nan)
        reset_ocean_cdr2(dst.variables['tcwv_err'], wvpa_arr, surface_type_arr, np.nan)
        reset_ocean_cdr2(dst.variables['tcwv_ran'], wvpa_arr, surface_type_arr, np.nan)
        # set tcwv_quality_flag to 3:
        reset_ocean_cdr2(dst.variables['tcwv_quality_flag'], wvpa_arr, surface_type_arr, 3)


def update_tcwv_quality_flag_for_hoaps(dst, src_hoaps, sensor, res):
    """
    Updates TCWV quality flag over ocean in presence of HOAPS data.
    :param dst:
    :param src_hoaps:
    :param sensor:
    :param res:
    :return:
    """
    dstvar = dst.variables['tcwv_quality_flag']
    tcwv_qual_arr_hoaps_src = np.array(src_hoaps.variables['tcwv_quality_flag'])
    tcwv_qual_arr_hoaps = rescale_auxdata(tcwv_qual_arr_hoaps_src, res)
    tcwv_qual_arr_dst = np.array(dstvar).astype(int)

    var_surface_type = dst.variables['surface_type_flag']
    surface_type_arr = np.array(var_surface_type)
    tmparr = np.copy(tcwv_qual_arr_dst)
    if is_cdr_2(sensor):
        is_ocean = ((surface_type_arr == 1) | (surface_type_arr == 3))
        for i in range(4):
            tmparr[np.where(is_ocean & (tcwv_qual_arr_hoaps == i))] = i
        dstvar[0, :, :] = tmparr[0, :, :]


def set_ocean_wvpa_errors(dst_var, surface_type_array, tcwv_array, wvpa_error_array, has_errors):
    """
    Sets HOAPS water vapour error terms over ocean.
    :param dst_var:
    :param surface_type_array:
    :param tcwv_array:
    :param wvpa_error_array:
    :param has_errors:
    :return:
    """
    dst_var_arr = np.array(dst_var)
    dst_var_arr_0 = np.copy(dst_var_arr)[0]
    if has_errors:
        if len(wvpa_error_array.shape) == 3:
            wvpa_error_array_2d = wvpa_error_array[0]
        else:
            wvpa_error_array_2d = wvpa_error_array

        do_use_hoaps = np.where(
            ((surface_type_array[0] == 1) | (surface_type_array[0] == 3) | (surface_type_array[0] == 4) | (
                    surface_type_array[0] == 7)) & (
                ~np.isnan(tcwv_array[0])) & (~np.isnan(wvpa_error_array_2d)) & (wvpa_error_array_2d >= 0.0))
        dst_var_arr_0[do_use_hoaps] = wvpa_error_array_2d[do_use_hoaps]
    else:
        ocean_or_ice = np.where(
            ((surface_type_array[0] == 1) | (surface_type_array[0] == 3) | (surface_type_array[0] == 4) | (
                    surface_type_array[0] == 7)))
        dst_var_arr_0[ocean_or_ice] = np.nan

    dst_var_arr_0[np.where(np.isnan(tcwv_array[0]))] = np.nan
    dst_var[0, :, :] = dst_var_arr_0[:, :]


def rescale_auxdata(src_arr, res):
    """
    Rescales 0.5 deg auxdata array (landmask, landcover) to target resolution

    :param src_arr:
    :param res:
    :return:
    """
    if res == '005':
        if len(src_arr.shape) == 3:
            return scipy.ndimage.zoom(src_arr[0], 10, order=0)
        else:
            return scipy.ndimage.zoom(src_arr, 10, order=0)
    else:
        if len(src_arr.shape) == 3:
            return src_arr[0]
        else:
            return src_arr


def set_errors_for_hoaps(dst, src, res):
    """
    Wrapper function for setting HOAPS error terms over ocean.

    :param dst:
    :param src:
    :param res:
    :return:
    """
    has_wvpa_errors = False
    wvpa_err_arr = None
    wvpa_ran_arr = None
    for name, variable in get_iteritems(src.variables):
        if name == 'wvpa_err':
            has_wvpa_errors = True
            wvpa_err_arr_src = np.array(src.variables['wvpa_err'])
            wvpa_err_arr = rescale_auxdata(wvpa_err_arr_src, res)
        if name == 'wvpa_ran':
            has_wvpa_errors = True
            wvpa_ran_arr_src = np.array(src.variables['wvpa_ran'])
            wvpa_ran_arr = rescale_auxdata(wvpa_ran_arr_src, res)

    # if no wvpa errors available, set to NaN over ocean (should no longer happen for latest HOAPS L3 products)
    set_ocean_wvpa_errors(dst.variables['tcwv_err'],
                          np.array(dst.variables['surface_type_flag']),
                          np.array(dst.variables['tcwv']),
                          wvpa_err_arr,
                          has_wvpa_errors)
    set_ocean_wvpa_errors(dst.variables['tcwv_ran'],
                          np.array(dst.variables['surface_type_flag']),
                          np.array(dst.variables['tcwv']),
                          wvpa_ran_arr,
                          has_wvpa_errors)


def update_num_hours_tcwv_for_hoaps(dst, src_hoaps, sensor, res):
    """
    Updates 'num_hours_tcwv' variable in presence of HOAPS data.
    :param dst:
    :param src_hoaps:
    :param sensor:
    :param res:
    :return:
    """
    if is_cdr_2(sensor):
        num_hours_tcwv_arr_src = np.array(src_hoaps.variables['numh'])
        num_hours_tcwv_arr = rescale_auxdata(num_hours_tcwv_arr_src, res)
        num_hours_tcwv_arr[np.where(num_hours_tcwv_arr <= 0.0)] = -1
        dst_var = dst.variables['num_hours_tcwv']
        dst_var[0, :, :] = num_hours_tcwv_arr[:, :]


def set_surface_type_flag(dst, src, day, res, ds_landmask, ds_landcover, ds_seaice):
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

    # in original L3 we can have LAND (1), OCEAN (2), SEAICE (4), LAND+CLOUD (9), OCEAN+CLOUD (10), SEAICE+CLOUD (12):
    # but we want (see PSD v2.1):
    # LAND (0), OCEAN (1), INLAND_WATER (2), PERMANENT_WETLANDS (3), SEA_ICE (4), COAST (5), PARTLY_SEA_ICE (6)
    # (invalid is i.e. outside any swaths in daily L3)
    surface_type_flag_arr_src = np.array(src.variables['surface_type_flags_majority'])
    hoaps_surface_type_flag_arr_src = np.array(ds_landmask.variables['mask'])
    modis_landcover_flag_arr_src = np.array(ds_landcover.variables['Majority_Land_Cover_Type_1'])
    tcwv_quality_flag_min_arr_src = np.array(src.variables['tcwv_quality_flags_min'])
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tcwv_arr = np.copy(tcwv_arr_src)
    tcwv_quality_flag_min_arr = np.copy(tcwv_quality_flag_min_arr_src)
    tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_quality_flag_min_arr))] = -128  # make NaN to INVALID

    hoaps_surface_type_flag_arr = rescale_auxdata(hoaps_surface_type_flag_arr_src, res)
    hoaps_surface_type_flag_arr[np.where(np.isnan(hoaps_surface_type_flag_arr))] = 0  # make NaN to water

    # make ocean + tcwv_quality_flag.TCWV_INVALID + tcwv = NaN to INVALID (fix of originally L2 bug)):
    tmparr = np.copy(surface_type_flag_arr_src)
    tmparr[np.where(np.isnan(tmparr))] = -128  # make NaN to INVALID
    tmparr[np.where((tmparr > 1) & (tmparr < 3) & (tcwv_quality_flag_min_arr > 2) & (np.isnan(tcwv_arr)))] = -128
    tmparr[np.where(tmparr == 10)] = 2  # make ocean + cloud to OCEAN
    tmparr[np.where(tmparr == 12)] = 16  # make seaice+cloud to SEA_ICE
    tmparr[np.where(hoaps_surface_type_flag_arr < 1)] = 2  # make hoaps water to OCEAN
    tmparr[np.where(hoaps_surface_type_flag_arr > 1)] = 32  # make hoaps coast to COAST
    tmparr[np.where(hoaps_surface_type_flag_arr == 1)] = 1  # make hoaps land to LAND

    # MODIS landcover info (MCD12C1.A2022001.061.05deg.nc):
    # https://lpdaac.usgs.gov/documents/101/MCD12_User_Guide_V6.pdf

    # --> INLAND_WATER (2): ds_landmask = 1 && ds_landcover = 0:
    tmparr[np.where((hoaps_surface_type_flag_arr == 1) & (modis_landcover_flag_arr_src == 0))] = 4
    # --> PERMANENT_WETLANDS (8): ds_landmask = 1 && ds_landcover = 11
    tmparr[np.where((hoaps_surface_type_flag_arr == 1) & (modis_landcover_flag_arr_src == 11))] = 8

    if ds_seaice:
        seaice_arr_src = np.array(ds_seaice.variables['mask'])
        seaice_frac_arr_src = np.array(ds_seaice.variables['icec'])
        day_index = int(day.zfill(1)) - 1

        seaice_arr_src_day = rescale_auxdata(seaice_arr_src[day_index], res)
        seaice_frac_arr_src_day = rescale_auxdata(seaice_frac_arr_src[day_index], res)

        seaice_frac_arr_src_day[np.where(np.isnan(seaice_frac_arr_src_day))] = 0  # make NaN to 0
        tmparr[np.where(seaice_arr_src_day == 11)] = 16  # make hoaps seaice to SEA_ICE
        # requested by DWD instead: make hoaps seaice > 0% and < 100% to PARTLY_SEA_ICE
        tmparr[
            np.where(
                (seaice_arr_src_day >= 11) & (seaice_frac_arr_src_day > 0) & (seaice_frac_arr_src_day < 100))] = 64

    surface_type_flag_arr = np.log2(tmparr)
    variable[0, :, :] = surface_type_flag_arr[:, :]


def set_atmospheric_conditions_flag(dst, src, res, ds_hoaps, ds_landmask):
    """
    Sets 'atmospheric_conditions' variable and its attributes.
    Do here: CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN PARTLY_CLOUDY_OVER_LAND
    --> : CLOUD_OVER_LAND (1): land+cloud || inlandwater+cloud || permanentwetlands+cloud
    --> : PARTLY_CLOUDY_OVER_LAND (2): CLOUD_OVER_LAND && valid tcwv
    --> : HEAVY_PRECIP_OVER_OCEAN (3): hoaps_scat_ratio_arr > 0.2
    --> : CLEAR (0): none of those
    :param dst:
    :param src:
    :param res:
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

    # in original L3 we can have LAND (1), OCEAN (2), SEAICE (4), LAND+CLOUD (9), OCEAN+CLOUD (10), SEAICE+CLOUD (12):
    # but we want (see PSD Vx.y):
    # CLEAR (0), PARTLY_CLOUDY_OVER_LAND (1), CLOUD_OVER_LAND (2), HEAVY_PRECIP_OVER_OCEAN (3)
    ac_flag_arr_src = np.array(src.variables['surface_type_flags_majority'])
    hoaps_surface_type_flag_arr_src = np.array(ds_landmask.variables['mask'])
    tcwv_quality_flag_min_arr_src = np.array(src.variables['tcwv_quality_flags_min'])
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tcwv_arr = np.copy(tcwv_arr_src)
    tcwv_quality_flag_min_arr = np.copy(tcwv_quality_flag_min_arr_src)
    tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_quality_flag_min_arr))] = -128  # make NaN to INVALID

    hoaps_surface_type_flag_arr = rescale_auxdata(hoaps_surface_type_flag_arr_src, res)
    hoaps_surface_type_flag_arr[np.where(np.isnan(hoaps_surface_type_flag_arr))] = 0  # make NaN to water

    tmparr = np.copy(ac_flag_arr_src)

    # make original OCEAN (2), SEAICE (4), OCEAN+CLOUD (10), SEAICE+CLOUD (12) to 'CLEAR' :
    tmparr[np.where((tmparr == 2) | (tmparr == 4) | (tmparr == 10) | (tmparr == 12))] = 1
    # make land+cloud to CLOUD OVER LAND:
    tmparr[np.where(tmparr == 9)] = 4

    if ds_hoaps:
        hoaps_scat_ratio_arr_src = np.array(ds_hoaps.variables['scat_ratio'])
        hoaps_scat_ratio_arr = rescale_auxdata(hoaps_scat_ratio_arr_src, res)
        tmparr[np.where(hoaps_scat_ratio_arr > 0.2)] = 8  # hoaps heavy precipitation criterion (MS, 202012)

    # set PARTLY_CLOUDY_OVER_LAND: must be the pixels identified in majority as CLOUD, but have a valid TCWV:
    tmparr[np.where((np.isfinite(tcwv_arr)) & (tmparr == 4))] = 2

    # set flag to CLEAR for all remaining others (no cloud over land, no cloud obs over ocean, no precip over ocean):
    tmparr[np.where((tmparr != 2) & (tmparr != 4) & (tmparr != 8))] = 1

    atm_cond_flag_arr = np.log2(tmparr)
    variable[0, :, :] = atm_cond_flag_arr[:, :]


def set_tcwv_quality_flag(dst, src):
    """
    Sets 'tcwv_quality_flag' variable and its attributes.
    :param dst:
    :param src:
    :return:
    """
    variable = dst.variables['tcwv_quality_flag']
    set_variable_long_name_and_unit_attributes(variable, 'Quality flag of Total Column of Water Vapour', ' ')
    variable.setncattr('standard_name', 'status_flag ')
    min_valid = 0
    max_valid = 3
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3], 'b'))
    variable.setncattr('flag_meanings', 'TCWV_OK HIGH_COST_FUNCTION_1 HIGH_COST_FUNCTION_2 TCWV_INVALID')

    # set the quality flag values here:
    # flag = 0 for TCWV_OK, flag = 1 for TCWV_HIGH_COST_FUNCTION, flag = 2 for TCWV_INVALID (all NaN pixels)
    # NEW: flag = 0 for TCWV_OK, flag = 1 for TCWV_HIGH_COST_FUNCTION_1, flag = 2 for TCWV_HIGH_COST_FUNCTION_2,
    #      flag = 3 for TCWV_INVALID (all NaN pixels)

    # we must consider the 'best available' quality in the grid cell (the lowest value --> 'tcwv_quality_flags_min')
    tcwv_quality_flag_min_arr_src = np.array(src.variables['tcwv_quality_flags_min'])
    # harmonize final flag with NaNs already in raw L3 tcwv
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tcwv_quality_flag_min_arr = np.copy(tcwv_quality_flag_min_arr_src)
    tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_quality_flag_min_arr_src))] = 8
    tcwv_quality_flag_min_arr[np.where(np.isnan(tcwv_arr_src))] = 8

    # if everything is INVALID, reset everything to invalid/NaN (identify indices)
    indices = np.where(tcwv_quality_flag_min_arr > 4)
    variable[0, :, :] = np.log2(tcwv_quality_flag_min_arr[:, :])

    return indices


def copy_and_rename_variables_from_source_product(dst, src, has_latlon, sensor, single_sensors_list):
    """
    Copies variables from source product, renames to correct names, and sets attributes and data...
    For daily non-merged we need to
           - copy 'tcwv_mean' into 'tcwv'
           - for all contributing sensors copy 'num_obs_<sensor>' into 'num_obs_<sensor>'
           - copy 'tcwv_sigma' into 'stdv'
           - copy 'tcwv_uncertainty_mean' into 'tcwv_ran'
           - compute and write 'tcwv_err' from 'tcwv_uncertainty_sums_sum_sq'
           - compute and write 'tcwv_quality_flags' from 'tcwv_quality_flags_majority'
           - compute and write 'surface_type_flags_majority' from 'surface_type_flags_majority'
     For daily merged we need to do the same, but no renaming.
     For cmsaf we need to do the same, but specific renaming.

    :param dst:
    :param src:
    :param has_latlon:
    :param sensor:
    :param single_sensors_list:
    :return:
    """
    for name, variable in get_iteritems(src.variables):

        for single_sensor in single_sensors_list:
            if name == 'num_obs_' + single_sensor:
                # fill_val=getattr(variable, '_FillValue')
                fill_val = -1
                dstvar = dst.createVariable('num_obs_' + single_sensor, variable.datatype, ('time', 'lat', 'lon'),
                                            zlib=True, fill_value=fill_val)
                copy_variable_attributes_from_source(variable, dstvar)
                set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of Total Column of Water Vapour retrievals '
                                                           'from sensor ' + single_sensor + ' contributing '
                                                                                            'to L3 grid cell',
                                                           ' ')
                dstvar.setncattr('coordinates', 'lat lon')
                dstvar[0, :, :] = variable[:, :]

        if name == 'tcwv_mean':
            # in case of CDR-2, add variable 'num_hours_tcwv' ('numh' in new HOAPS products, set to -1 over land)
            if is_cdr_2(sensor):
                dstvar = dst.createVariable('num_hours_tcwv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                            fill_value=getattr(variable, '_FillValue'))
                set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of hours in day with a valid TCWV value '
                                                           'in L3 grid cell',
                                                           ' ')
                dstvar.setncattr('coordinates', 'lat lon')
                dstvar.setncattr('units', ' ')
                dstvar[0, :, :] = -1

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

        if name == 'tcwv_sigma':
            dstvar = dst.createVariable('stdv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Standard deviation of Total Column of Water Vapour',
                                                       'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_uncertainty_mean':
            # Stengel et al., eq. (2):
            dstvar = dst.createVariable('tcwv_err', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Average retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_uncertainty_sums_sum_sq':
            # Stengel et al., eq. (3):
            dstvar = dst.createVariable('tcwv_ran', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            # just set attributes, computation of tcwv_ran below
            copy_variable_attributes_from_source(variable, dstvar)
            set_variable_long_name_and_unit_attributes(dstvar, 'Propagated retrieval uncertainty', 'kg/m2')

            uncert_sum_sqr_arr = np.array(src.variables['tcwv_uncertainty_sums_sum_sq'])
            num_obs_arr = np.array(src.variables['num_obs'])
            uncert_sum_sqr_arr_norm = uncert_sum_sqr_arr / num_obs_arr  # this is eq. (3) !
            # now sqrt, see PSD v2.0 section 3.1.4:
            uncert_sum_sqr_arr_psd = np.sqrt(uncert_sum_sqr_arr_norm)  # PSD v2.0 section 3.1.4
            dstvar[0, :, :] = uncert_sum_sqr_arr_psd[:, :]
            # NOTE: with this computation, tcwv_err and tcwv_ran are nearly identical over land,
            # whereas tcwv_err/tcwv_ran ~ 5 for HOAPS over water

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
    ds_seaice = None
    seaice_mask_file = args[10]
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
    if len(args) == 12:
        hoaps_file = args[11]
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
    dst.setncattr('history', 'python nc-compliance-py-process_phase1.py ' + nc_infile)
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
    single_sensors_list = args[4].upper().split("-")
    year = args[5]
    month = args[6]
    day = args[7]
    res = args[8]
    version = args[9]

    # Source dataset...
    src = Dataset(nc_infile)

    # Land and seaice mask datasets...
    ds_seaice = get_ds_seaice(args)
    ds_landmask = Dataset(landmask_file)
    ds_landcover = Dataset(landcover_file)

    # Original HOAPS dataset if given (20201103: to ingest corrected wvpa_ran values, requested by MS)
    ds_hoaps = get_ds_hoaps(args)

    # Initialize nc result file and dataset...
    datestring = year + month + day
    dst, nc_outfile = init_nc_compliant_product(datestring, res, sensor, version)

    # ### set up and fill NetCDF destination file: ###

    # Set dimensions...
    set_dimensions(dst, src)

    # set global attributes...
    set_global_attributes(sensor, datestring, dst, day, month, year, res, version, nc_infile, nc_outfile)

    # Create time variables...
    create_time_variables(dst, day, month, year)

    # Create lat/lon variables...
    has_latlon, height, width = set_lat_lon_variables(dst, res, src)

    # Create final flag variables...
    dst.createVariable('tcwv_quality_flag', 'b', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))
    dst.createVariable('surface_type_flag', 'b', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))
    dst.createVariable('atmospheric_conditions_flag', 'b',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))

    # Copy variables from source product and rename to correct names. Set attributes and data...
    copy_and_rename_variables_from_source_product(dst, src, has_latlon, sensor, single_sensors_list)

    # Set TCWV final quality flag. Get back indices of finally invalid pixels...
    indices = set_tcwv_quality_flag(dst, src)

    # In case there are remaining 'valid' values for invalid pixels, reset to nan...
    reset_var_to_nan(dst.variables['tcwv'], indices)
    reset_var_to_nan(dst.variables['stdv'], indices)
    reset_var_to_nan(dst.variables['tcwv_err'], indices)
    reset_var_to_nan(dst.variables['tcwv_ran'], indices)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src, res, ds_hoaps, ds_landmask)

    # Set final surface type flag...
    set_surface_type_flag(dst, src, day, res, ds_landmask, ds_landcover, ds_seaice)

    if ds_hoaps:
        # Set tcwv_err and tcwv_ran in case of existing HOAPS...
        set_errors_for_hoaps(dst, ds_hoaps, res)
        # Update tcwv_quality_flag in case of existing HOAPS...
        update_tcwv_quality_flag_for_hoaps(dst, ds_hoaps, sensor, res)
        # Update 'num_hours_tcwv' in case of existing HOAPS...
        update_num_hours_tcwv_for_hoaps(dst, ds_hoaps, sensor, res)
    else:
        set_errors_for_hoaps(dst, src, res)

    # Cleanup inconsistencies of final arrays at this point:
    cleanup_inconsistencies(dst, ds_hoaps, sensor, res, single_sensors_list)
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

    print("FINISHED nc-compliance-py-process_phase1.py...", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING nc-compliance-py-process_phase1.py", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 11 and len(sys.argv) != 12:
        print(
            'Usage:  python nc-compliance-py-process_phase1.py <nc_infile> <landmask_file> <landcover_file> <sensor> <year> '
            '<month> <day> ' +
            '<resolution> <product version> <seaice_mask_file> [<hoaps_l3_file>]')
        sys.exit(-1)

    run(sys.argv)
