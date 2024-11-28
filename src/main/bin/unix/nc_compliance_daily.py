# -*- coding: utf-8 -*-
# ! /usr/bin/env python
from __future__ import print_function

# Generates final CF- and CCI-compliant TCWV L3 daily products ready for Phase 2 delivery.
# Currently a copy of nc-compliance-py-process.py
#
__author__ = 'olafd'

import os
import sys

import numpy as np
from netCDF4 import Dataset

# from src.main.bin.unix.py_utils import nc_compliance_utils as ncu
from py_utils import nc_compliance_utils as ncu

LAT_MIN_VALID = -90.0
LAT_MAX_VALID = 90.0
LON_MIN_VALID = -180.0
LON_MAX_VALID = 180.0


#############################################################################


def cleanup_inconsistencies(dst, src_hoaps, sensor, res, year, month, day, single_sensors_list):
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
        if 'num_obs_' + single_sensor in dst.variables:
            ncu.reset_polar(dst.variables['num_obs_' + single_sensor], tcwv_arr, lat_arr_3d, surface_type_arr,
                            atm_cond_arr, 0)

    # set tcwv, stdv, and error terms to nan:
    ncu.reset_polar(dst.variables['tcwv'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    ncu.reset_polar(dst.variables['stdv'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    ncu.reset_polar(dst.variables['tcwv_err'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    ncu.reset_polar(dst.variables['tcwv_ran'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, np.nan)
    # set tcwv_quality_flag to 3:
    ncu.reset_polar(dst.variables['tcwv_quality_flag'], tcwv_arr, lat_arr_3d, surface_type_arr, atm_cond_arr, 3)

    # set num_obs to 0 over ocean for NIR sensors:
    for single_sensor in single_sensors_list:
        if 'num_obs_' + single_sensor in dst.variables and single_sensor != 'CMSAF_HOAPS':
            ncu.reset_ocean_num_obs_nir(sensor, dst.variables['num_obs_' + single_sensor], surface_type_arr, 0)

    # remove all HOAPS (everything over ocean, coastal, seaice) in case of CDR-1
    # clean everything remaining over ocean where we have no HOAPS (wvpa) over water in case of CDR-2
    if ncu.is_cdr_1(sensor):
        # set tcwv, stdv, and error terms to nan:
        ncu.reset_ocean_cdr1(dst.variables['tcwv'], surface_type_arr, np.nan)
        ncu.reset_ocean_cdr1(dst.variables['stdv'], surface_type_arr, np.nan)
        ncu.reset_ocean_cdr1(dst.variables['tcwv_err'], surface_type_arr, np.nan)
        ncu.reset_ocean_cdr1(dst.variables['tcwv_ran'], surface_type_arr, np.nan)
        # set tcwv_quality_flag to 3:
        ncu.reset_ocean_cdr1(dst.variables['tcwv_quality_flag'], surface_type_arr, 3)
        # set atmospheric_conditions_flag to 0:
        ncu.reset_ocean_cdr1(dst.variables['atmospheric_conditions_flag'], surface_type_arr, 0)
    else:
        wvpa_arr_src = np.array(src_hoaps.variables['wvpa'])
        wvpa_arr = ncu.upscale_auxdata(wvpa_arr_src, res)
        # set num_obs to 0:
        # ncu.reset_ocean_cdr2(dst.variables['num_obs_CMSAF_HOAPS'], wvpa_arr, surface_type_arr, 0)
        # set tcwv, stdv, and error terms to nan:
        ncu.reset_ocean_cdr2(dst.variables['tcwv'], wvpa_arr, surface_type_arr, np.nan)
        ncu.reset_ocean_cdr2(dst.variables['stdv'], wvpa_arr, surface_type_arr, np.nan)
        ncu.reset_ocean_cdr2(dst.variables['tcwv_err'], wvpa_arr, surface_type_arr, np.nan)
        ncu.reset_ocean_cdr2(dst.variables['tcwv_ran'], wvpa_arr, surface_type_arr, np.nan)
        # set tcwv_quality_flag to 3:
        ncu.reset_ocean_cdr2(dst.variables['tcwv_quality_flag'], wvpa_arr, surface_type_arr, 3)
        # set atmospheric_conditions_flag to 0:
        ncu.reset_ocean_cdr2(dst.variables['atmospheric_conditions_flag'], wvpa_arr, surface_type_arr, 0)

    # todo: reset tcwv, stdv, tcwv_err, tcwv_ran, num_obs_*, tcwv_quality_flag over land for SZA > 75.0
    sza_arr = ncu.get_sza_from_date(year, month, day, lat_arr_3d)
    tcwv_ran_arr = np.array(dst.variables['tcwv_ran'])

    # set tcwv, stdv, and error terms to nan:
    ncu.clean_known_artefacts(dst.variables['tcwv'], surface_type_arr, sza_arr, tcwv_ran_arr, np.nan, tcwv_ran_min=0.0)
    ncu.clean_known_artefacts(dst.variables['stdv'], surface_type_arr, sza_arr, tcwv_ran_arr, np.nan, tcwv_ran_min=0.0)
    ncu.clean_known_artefacts(dst.variables['tcwv_err'], surface_type_arr, sza_arr, tcwv_ran_arr, np.nan, tcwv_ran_min=0.0)
    # set atmospheric_conditions_flag to 0:
    ncu.clean_known_artefacts(dst.variables['atmospheric_conditions_flag'], surface_type_arr, sza_arr, tcwv_ran_arr, 0, tcwv_ran_min=0.0)
    # set tcwv_quality_flag to 3:
    ncu.clean_known_artefacts(dst.variables['tcwv_quality_flag'], surface_type_arr, sza_arr, tcwv_ran_arr, 3, tcwv_ran_min=0.0)
    # set num_obs to 0 over ocean for NIR sensors:
    for single_sensor in single_sensors_list:
        if 'num_obs_' + single_sensor in dst.variables and single_sensor != 'CMSAF_HOAPS':
            ncu.clean_known_artefacts(dst.variables['num_obs_' + single_sensor], surface_type_arr, sza_arr, tcwv_ran_arr, 0, tcwv_ran_min=0.0)

    ncu.clean_known_artefacts(dst.variables['tcwv_ran'], surface_type_arr, sza_arr, tcwv_ran_arr, np.nan, tcwv_ran_min=0.0)

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
    tcwv_qual_arr_hoaps = ncu.upscale_auxdata(tcwv_qual_arr_hoaps_src, res)
    tcwv_qual_arr_dst = np.array(dstvar).astype(int)

    var_surface_type = dst.variables['surface_type_flag']
    surface_type_arr = np.array(var_surface_type)
    tmparr = np.copy(tcwv_qual_arr_dst)
    if ncu.is_cdr_2(sensor):
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
    for name, variable in ncu.get_iteritems(src.variables):
        if name == 'wvpa_err':
            has_wvpa_errors = True
            wvpa_err_arr_src = np.array(src.variables['wvpa_err'])
            wvpa_err_arr = ncu.upscale_auxdata(wvpa_err_arr_src, res)
        if name == 'wvpa_ran':
            has_wvpa_errors = True
            wvpa_ran_arr_src = np.array(src.variables['wvpa_ran'])
            wvpa_ran_arr = ncu.upscale_auxdata(wvpa_ran_arr_src, res)

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
    if ncu.is_cdr_2(sensor):
        num_hours_tcwv_arr_src = np.array(src_hoaps.variables['numh'])
        num_hours_tcwv_arr = ncu.upscale_auxdata(num_hours_tcwv_arr_src, res)
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
    ncu.set_variable_long_name_and_unit_attributes(variable, 'Surface type flag', ' ')
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

    hoaps_surface_type_flag_arr = ncu.upscale_auxdata(hoaps_surface_type_flag_arr_src, res)
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

        seaice_arr_src_day = ncu.upscale_auxdata(seaice_arr_src[day_index], res)
        seaice_frac_arr_src_day = ncu.upscale_auxdata(seaice_frac_arr_src[day_index], res)

        seaice_frac_arr_src_day[np.where(np.isnan(seaice_frac_arr_src_day))] = 0  # make NaN to 0
        tmparr[np.where(seaice_arr_src_day == 11)] = 16  # make hoaps seaice to SEA_ICE
        # requested by DWD instead: make hoaps seaice > 0% and < 100% to PARTLY_SEA_ICE
        tmparr[
            np.where(
                (seaice_arr_src_day >= 11) & (seaice_frac_arr_src_day > 0) & (seaice_frac_arr_src_day < 100))] = 64

    surface_type_flag_arr = np.log2(tmparr)
    variable[0, :, :] = surface_type_flag_arr[:, :]


def set_atmospheric_conditions_flag(dst, src, res, ds_hoaps):
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
    :return:
    """
    variable = dst.variables['atmospheric_conditions_flag']
    ncu.set_variable_long_name_and_unit_attributes(variable, 'Atmospheric conditions flag', ' ')
    variable.setncattr('standard_name', 'status_flag ')
    min_valid = 0
    max_valid = 4
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3, 4], 'b'))
    variable.setncattr('flag_meanings', 'NOT_CLASSIFIED CLEAR_OVER_LAND PARTLY_CLOUDY_OVER_LAND CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN')

    # in original L3 we can have LAND (1), OCEAN (2), SEAICE (4), LAND+CLOUD (9), OCEAN+CLOUD (10), SEAICE+CLOUD (12):
    # but we want (see PSD Vx.y):
    # NOT_CLASSIFIED (0), CLEAR_OVER_LAND (1), PARTLY_CLOUDY_OVER_LAND (2), CLOUD_OVER_LAND (3), HEAVY_PRECIP_OVER_OCEAN (4)
    ac_flag_arr_src = np.array(src.variables['surface_type_flags_majority'])
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tcwv_arr = np.copy(tcwv_arr_src)

    ac_flag_arr = np.copy(ac_flag_arr_src)
    tmparr = np.copy(ac_flag_arr_src)
    tmparr[:, :] = 2  # init with 'CLEAR_OVER_LAND'

    # make original unclassified (0), OCEAN (2), OCEAN+CLOUD (10) to 'NOT_CLASSIFIED' :
    tmparr[np.where((ac_flag_arr == 0) | (ac_flag_arr == 2) | (ac_flag_arr == 10))] = 1
    # also set to NOT_CLASSIFIED if no valid TCWV:
    tmparr[np.where(~np.isfinite(tcwv_arr_src))] = 1
    # make original SEAICE (4) to 'CLEAR_OVER_LAND' :
    # tmparr[np.where(ac_flag_arr == 4)] = 2
    # make original LAND+CLOUD (9), SEAICE+CLOUD (12) to 'CLOUD OVER LAND' :
    tmparr[np.where((ac_flag_arr == 9) | (ac_flag_arr == 12))] = 8

    if ds_hoaps:
        hoaps_scat_ratio_arr_src = np.array(ds_hoaps.variables['scat_ratio'])
        hoaps_scat_ratio_arr = ncu.upscale_auxdata(hoaps_scat_ratio_arr_src, res)
        tmparr[np.where(hoaps_scat_ratio_arr > 0.2)] = 16  # hoaps heavy precipitation criterion (MS, 202012)

    # set PARTLY_CLOUDY_OVER_LAND: must be the pixels identified in majority as CLOUD, but have a valid TCWV:
    tmparr[np.where((np.isfinite(tcwv_arr)) & (tmparr == 8))] = 4

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
    ncu.set_variable_long_name_and_unit_attributes(variable, 'Quality flag of Total Column of Water Vapour', ' ')
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


def copy_and_rename_variables_from_source_product(dst, src, sensor, single_sensors_list):
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

    for name, variable in ncu.get_iteritems(src.variables):

        # If source product is unmegred, the num_obs we want is the 'tcwv_uncertainty_counts' from original L3
        if name == 'tcwv_uncertainty_counts' and ncu.is_tcwv_l3_unmerged_product(sensor):
            fill_val = -1
            dstvar = dst.createVariable('num_obs_' + sensor.upper(), np.int32, ('time', 'lat', 'lon'),
                                        zlib=True, fill_value=fill_val)
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of Total Column of Water Vapour retrievals '
                                                           'from sensor ' + sensor.upper() + ' contributing '
                                                                                             'to L3 grid cell',
                                                           ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar[0, :, :] = variable[:, :]

        # copy the num_obs_* variables present in the source product:
        for single_sensor in single_sensors_list:
            if name == 'num_obs_' + single_sensor:
                fill_val = -1
                dstvar = dst.createVariable('num_obs_' + single_sensor, np.int32, ('time', 'lat', 'lon'),
                                            zlib=True, fill_value=fill_val)
                ncu.copy_variable_attributes_from_source(variable, dstvar)
                ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                               'Number of Total Column of Water Vapour retrievals '
                                                               'from sensor ' + single_sensor + ' contributing '
                                                                                                'to L3 grid cell',
                                                               ' ')
                dstvar.setncattr('coordinates', 'lat lon')

                num_obs_arr = np.array(variable)
                surface_type_flag_arr = np.array(src.variables['surface_type_flags_majority'])
                # for NIR sensors, set num_obs to 0 over ocean:
                if single_sensor != 'CMSAF_HOAPS':
                    num_obs_arr[np.where(surface_type_flag_arr == 2)] = 0
                dstvar[0, :, :] = num_obs_arr[:, :]

        if name == 'tcwv_mean':
            # in case of CDR-2, add variable 'num_hours_tcwv' ('numh' in new HOAPS products, set to -1 over land)
            if ncu.is_cdr_2(sensor):
                dstvar = dst.createVariable('num_hours_tcwv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                            fill_value=getattr(variable, '_FillValue'))
                ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                               'Number of hours in day with a valid TCWV value '
                                                               'in L3 grid cell',
                                                               ' ')
                dstvar.setncattr('coordinates', 'lat lon')
                dstvar.setncattr('units', ' ')
                dstvar[0, :, :] = -1

            dstvar = dst.createVariable('tcwv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Total Column of Water', 'kg/m2')
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
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Standard deviation of Total Column of Water Vapour',
                                                           'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_uncertainty_mean':
            # Stengel et al., eq. (2):
            dstvar = dst.createVariable('tcwv_err', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Average retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_uncertainty_sums_sum_sq':
            # Stengel et al., eq. (3):
            dstvar = dst.createVariable('tcwv_ran', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            # just set attributes, computation of tcwv_ran below
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Propagated retrieval uncertainty', 'kg/m2')

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
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            dstvar.setncattr('long_name', 'Coordinate Reference System ')
            dstvar.setncattr('comment',
                             'A coordinate reference system (CRS) defines how the georeferenced spatial data relates '
                             'to real locations on the Earth\'s surface ')
            dstvar[:] = variable[:]

    # Finally, add the num_obs_* variables which should be in the source product according to observation date,
    # but maybe are not because single sensor(s) are missing.
    # num_obs_* is set to 0 for these missing sensor(s)
    # This is done to keep product content a bit more consistent.
    for single_sensor in single_sensors_list:
        if not 'num_obs_' + single_sensor in src.variables and not 'num_obs_' + single_sensor in dst.variables:
            fill_val = -1
            dstvar = dst.createVariable('num_obs_' + single_sensor, np.int32, ('time', 'lat', 'lon'),
                                        zlib=True, fill_value=fill_val)
            ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of Total Column of Water Vapour retrievals '
                                                           'from sensor ' + single_sensor + ' contributing '
                                                                                            'to L3 grid cell',
                                                           ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar[0, :, :] = 0


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
    sensor = args[4]
    year = args[5]
    month = args[6]
    day = args[7]
    res = args[8]
    version = args[9]

    # Maximum contributing sensors depending on observation date
    # (list contains sensors which SHOULD contribute, even if missing for particular day) :
    maximum_single_sensors_list = ncu.get_maximum_single_sensors_list(year, month)
    if ncu.is_cdr_2(sensor):
        maximum_single_sensors_list.append('CMSAF_HOAPS')

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
    dst, nc_outfile = ncu.init_nc_compliant_product(datestring, res, sensor, version)

    # ### set up and fill NetCDF destination file: ###

    # Set dimensions...
    ncu.set_dimensions(dst, src)

    # set global attributes...
    ncu.set_global_attributes(sensor, datestring, dst, day, month, year, res, version, nc_infile, nc_outfile)

    # Create time variables...
    ncu.create_time_variables(dst, day, month, year)

    # Create lat/lon variables...
    has_latlon, height, width = ncu.set_lat_lon_variables_global(dst, res, src)

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
    copy_and_rename_variables_from_source_product(dst, src, sensor, maximum_single_sensors_list)

    # Set TCWV final quality flag. Get back indices of finally invalid pixels...
    indices = set_tcwv_quality_flag(dst, src)

    # In case there are remaining 'valid' values for invalid pixels, reset to nan...
    ncu.reset_var_to_value(dst.variables['tcwv'], indices, np.nan)
    ncu.reset_var_to_value(dst.variables['stdv'], indices, np.nan)
    ncu.reset_var_to_value(dst.variables['tcwv_err'], indices, np.nan)
    ncu.reset_var_to_value(dst.variables['tcwv_ran'], indices, np.nan)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src, res, ds_hoaps)

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
    cleanup_inconsistencies(dst, ds_hoaps, sensor, res, year, month, day, maximum_single_sensors_list)
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

    print("FINISHED nc-compliance-daily.py...", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING nc_compliance_daily.py", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 11 and len(sys.argv) != 12:
        print(
            'Usage:  python nc_compliance_daily.py <nc_infile> <landmask_file> <landcover_file> <sensor> <year> '
            '<month> <day> ' +
            '<resolution> <product version> <seaice_mask_file> [<hoaps_l3_file>]')
        sys.exit(-1)

    run(sys.argv)
