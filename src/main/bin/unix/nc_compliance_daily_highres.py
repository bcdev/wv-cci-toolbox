# -*- coding: utf-8 -*-
# ! /usr/bin/env python
from __future__ import print_function

# Generates final CF- and CCI-compliant TCWV L3 daily high-resolution products ready for delivery.
#
__author__ = 'olafd'

import calendar
import os
import sys
import uuid

import numpy as np
import scipy.ndimage
from netCDF4 import Dataset

from py_utils import nc_compliance_utils as ncu

LAT_MIN_VALID = -90.0
LAT_MAX_VALID = 90.0
LON_MIN_VALID = -180.0
LON_MAX_VALID = 180.0


#############################################################################

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


def rescale_hoaps(src_arr):
    """
    Rescales 0.5 deg hoaps array to target resolution

    :param src_arr:
    :return:
    """
    # res = '001'
    if len(src_arr.shape) == 3:
        return scipy.ndimage.zoom(src_arr[0], 50, order=0)
    else:
        return scipy.ndimage.zoom(src_arr, 50, order=0)


def set_errors_for_hoaps(dst, src):
    """
    Wrapper function for setting HOAPS error terms over ocean.
    :param dst:
    :param src:
    :return:
    """
    has_wvpa_errors = False
    wvpa_err_arr = None
    wvpa_ran_arr = None
    for name, variable in ncu.get_iteritems(src.variables):
        if name == 'wvpa_err':
            has_wvpa_errors = True
            wvpa_err_arr = np.array(src.variables['wvpa_err'])
            # wvpa_err_arr_src = np.array(src.variables['wvpa_err'])
            # wvpa_err_arr = rescale_hoaps(wvpa_err_arr_src)
        if name == 'wvpa_ran':
            has_wvpa_errors = True
            wvpa_ran_arr = np.array(src.variables['wvpa_ran'])
            # wvpa_ran_arr_src = np.array(src.variables['wvpa_ran'])
            # wvpa_ran_arr = rescale_hoaps(wvpa_ran_arr_src)

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


def set_num_obs_variable(dst, src):
    """
    Sets 'num_obs' variable and its attributes.
    :param dst:
    :param src:
    :return:
    """
    var_counts = src.variables['tcwv_uncertainty_counts']
    var_counts_arr = np.array(var_counts).astype(int)
    var_tcwv = dst.variables['tcwv']
    var_tcwv_arr = np.array(var_tcwv)
    dstvar = dst.variables['num_obs']
    tmparr = np.copy(var_counts_arr)
    tmparr[:, :] = var_counts[:, :]
    tmparr[np.where(np.isnan(var_tcwv_arr[0]))] = 0
    tmparr[np.where(tmparr < 0)] = 0
    dstvar[0, :, :] = tmparr[:, :]


def set_surface_type_flag(dst, src, ds_landmask, ds_landcover):
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
    :param ds_landmask:
    :param ds_landcover:
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

    # hoaps_surface_type_flag_arr = ncu.upscale_auxdata(hoaps_surface_type_flag_arr_src, res)
    hoaps_surface_type_flag_arr = scipy.ndimage.zoom(hoaps_surface_type_flag_arr_src, 50, order=0)
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

    # MODIS landcover info (MCD12C1.A2022001.061.005deg.nc):
    # https://lpdaac.usgs.gov/documents/101/MCD12_User_Guide_V6.pdf

    # use 005 input and rescale from 005 to 001:
    modis_landcover_flag_arr = scipy.ndimage.zoom(modis_landcover_flag_arr_src, 5, order=0)

    # --> INLAND_WATER (2): ds_landmask = 1 && ds_landcover = 0:
    tmparr[np.where((hoaps_surface_type_flag_arr == 1) & (modis_landcover_flag_arr == 0))] = 4
    # --> PERMANENT_WETLANDS (8): ds_landmask = 1 && ds_landcover = 11
    tmparr[np.where((hoaps_surface_type_flag_arr == 1) & (modis_landcover_flag_arr == 11))] = 8

    surface_type_flag_arr = np.log2(tmparr)
    variable[0, :, :] = surface_type_flag_arr[:, :]


def set_atmospheric_conditions_flag(dst, src, res):
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
    :return:
    """
    variable = dst.variables['atmospheric_conditions_flag']
    ncu.set_variable_long_name_and_unit_attributes(variable, 'Atmospheric conditions flag', ' ')
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
    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tcwv_arr = np.copy(tcwv_arr_src)

    tmparr = np.copy(ac_flag_arr_src)

    # make original OCEAN (2), SEAICE (4), OCEAN+CLOUD (10), SEAICE+CLOUD (12) to 'CLEAR' :
    tmparr[np.where((tmparr == 2) | (tmparr == 4) | (tmparr == 10) | (tmparr == 12))] = 1
    # make land+cloud to CLOUD OVER LAND:
    tmparr[np.where(tmparr == 9)] = 4

    hoaps_scat_ratio_arr_src = np.array(src.variables['scat_ratio'])
    hoaps_scat_ratio_arr = ncu.upscale_auxdata(hoaps_scat_ratio_arr_src, res)
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

    # we must consider the 'best available' quality in the grid cell (lowest value --> 'tcwv_quality_flags_min')
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
           - copy 'num_obs' into 'num_obs'
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
            dstvar.setncattr('ancillary_variables', 'num_obs_<sensor>')
            dstvar[0, :, :] = tcwv_arr[:, :]

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

        if name == 'wvpa':
            dstvar = dst.createVariable('tcwv_ocean_hoaps', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=np.nan)
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Total Column of Water', 'kg/m2')
            dstvar.setncattr('standard_name', 'atmosphere_water_vapor_content ')
            tcwv_arr = np.array(variable)
            tcwv_min_valid = 0.0
            tcwv_max_valid = 70.0
            # tcwv_arr[np.where(tcwv_arr < tcwv_min_valid)] = tcwv_min_valid
            tcwv_arr[np.where(tcwv_arr < tcwv_min_valid)] = np.nan
            tcwv_arr[np.where(tcwv_arr > tcwv_max_valid)] = tcwv_max_valid
            tcwv_min = np.nanmin(tcwv_arr)
            tcwv_max = np.nanmax(tcwv_arr)
            dstvar.setncattr('actual_range', np.array([tcwv_min, tcwv_max], 'f4'))
            dstvar.setncattr('valid_range', np.array([tcwv_min_valid, tcwv_max_valid], 'f4'))
            dstvar[0, :, :] = tcwv_arr[:, :]

        if name == 'crs':
            dstvar = dst.createVariable(name, variable.datatype, variable.dimensions, zlib=True)
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            dstvar.setncattr('long_name', 'Coordinate Reference System ')
            dstvar.setncattr('comment',
                             'A coordinate reference system (CRS) defines how the georeferenced spatial data relates '
                             'to real locations on the Earth\'s surface ')
            dstvar[:] = variable[:]

        if has_latlon:
            if name == 'lat':
                ncu.create_nc_lat_variable(dst, variable)
            if name == 'lon':
                ncu.create_nc_lon_variable(dst, variable)

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


def set_lat_lon_variables(dst, lat_min, lat_max, lon_min, lon_max, src):
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
    for name, variable in ncu.get_iteritems(src.variables):
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

def init_nc_compliant_product(datestring, roi, sensor, version):
    """
    Setup nc result file and dataset for input daily product.
    :param datestring:
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
    nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + _sensor + '-' + '001deg-' + roi + '-' + datestring + '-fv' + \
                 version + '.nc'
    outpath = os.getcwd() + os.sep + nc_outfile
    nc_compliant_ds = Dataset(outpath, 'w', format='NETCDF4')

    return nc_compliant_ds, nc_outfile


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
    roi = args[4]
    # sensor = args[5].replace('-', '_')
    sensor = args[5]
    year = args[6]
    month = args[7]
    day = args[8]
    lat_min = float(args[9])
    lat_max = float(args[10])
    lon_min = float(args[11])
    lon_max = float(args[12])
    version = args[13]

    # Maximum contributing sensors depending on observation date
    # (list contains sensors which SHOULD contribute, even if missing for particular day) :
    maximum_single_sensors_list = ncu.get_maximum_single_sensors_list(year, month)

    # Source dataset...
    src = Dataset(nc_infile)

    # Land mask and land cover datasets...
    ds_landmask = Dataset(landmask_file)
    ds_landcover = Dataset(landcover_file)

    # Initialize nc result file and dataset...
    datestring = year + month + day
    dst, nc_outfile = init_nc_compliant_product(datestring, roi, sensor, version)

    # ### set up and fill NetCDF destination file: ###

    # Set dimensions...
    ncu.set_dimensions(dst, src)

    # set global attributes...
    ncu.set_global_attributes(sensor, datestring, dst, day, month, year, '001', version, nc_infile, nc_outfile)

    # Create time variables...
    ncu.create_time_variables(dst, day, month, year)

    # Create lat/lon variables...
    has_latlon, height, width = \
        set_lat_lon_variables(dst, float(lat_min), float(lat_max), float(lon_min), float(lon_max), src)

    # Create final flag variables...
    dst.createVariable('tcwv_quality_flag', 'b',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))
    dst.createVariable('surface_type_flag', 'b',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))
    dst.createVariable('atmospheric_conditions_flag', 'b',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True,
                       fill_value=np.array([-128], 'b'))

    # Copy variables from source product and rename to correct names. Set attributes and data...
    copy_and_rename_variables_from_source_product(dst, src, has_latlon, sensor, maximum_single_sensors_list)

    # Set TCWV final quality flag. Get back indices of finally invalid pixels...
    indices = set_tcwv_quality_flag(dst, src)

    # In case there are remaining 'valid' values for invalid pixels, reset to nan...
    ncu.reset_var_to_value(dst.variables['tcwv'], indices, np.nan)
    ncu.reset_var_to_value(dst.variables['tcwv_err'], indices, np.nan)
    ncu.reset_var_to_value(dst.variables['tcwv_ran'], indices, np.nan)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src, '001')

    # compute 1-pixel cloud buffer for both total and partly cloudy pixels
    ncu.apply_cloud_buffer(dst, maximum_single_sensors_list)

    # Set final surface type flag...
    set_surface_type_flag(dst, src, ds_landmask, ds_landcover)

    # Set num_obs variable...
    # set_num_obs_variable(dst, src)

    set_errors_for_hoaps(dst, src)

    var_tcwv_arr = np.array(dst.variables['tcwv'])
    dst.variables['tcwv'].setncattr('actual_range', np.array([np.nanmin(var_tcwv_arr), np.nanmax(var_tcwv_arr)], 'f4'))

    # Close files...
    print("Closing L3 input file...", file=sys.stderr)
    src.close()
    print("Closing landmask input file...", file=sys.stderr)
    ds_landmask.close()

    print("FINISHED nc-compliance-py-process_phase1.py...", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING nc_compliance_daily_highres.py", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 14:
        print(
            'Usage:  python nc_compliance_daily_highres.py <nc_infile> <landmask_file> <landcover_file> <roi> <sensor> '
            '<year> <month> <day> <lat_min> <lat_max> <lon_min> <lon_max> <product version>')
        sys.exit(-1)

    run(sys.argv)
