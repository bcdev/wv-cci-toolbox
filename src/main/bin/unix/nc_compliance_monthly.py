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

import numpy as np
from netCDF4 import Dataset

from py_utils import nc_compliance_utils as ncu

LAT_MIN_VALID = -90.0
LAT_MAX_VALID = 90.0
LON_MIN_VALID = -180.0
LON_MAX_VALID = 180.0


#############################################################################


def set_surface_type_flag(dst, src):
    """
    Sets 'surface_type_flag' variable and its attributes.:
    Just copy surface_type_flag from raw monthly L3.
    :param dst:
    :param src:
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

    surface_type_flag_arr_src = np.array(src.variables['surface_type_flag_majority'])
    variable[0, :, :] = surface_type_flag_arr_src[:, :]


def set_atmospheric_conditions_flag_OLD(dst, src):
    """
    Sets 'atmospheric_conditions_flag' variable and its attributes.:
    Just copy atmospheric_conditions_flag from raw monthly L3.
    :param dst:
    :param src:
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

    ac_flag_arr_src = np.array(src.variables['atmospheric_conditions_flag_majority'])

    # a) CLOUDY_OVER_LAND (2) in daily products: the pixels identified purely as CLOUD (all L2 samples), no valid TCWV
    # b) PARTLY_CLOUDY_OVER_LAND (5) in daily products: the pixels identified in majority as CLOUD,
    #    but have a valid TCWV
    # c) CLOUDY_OVER_LAND (2) in monthly products: all daily aggregates are CLOUDY_OVER_LAND,
    #    no valid TCWV (usually very few pixels)
    # d) PARTLY_CLOUDY_OVER_LAND (5) in monthly products: at least 1 , but not all daily aggregates are
    #    CLOUDY_OVER_LAND, valid TCWV
    # -->
    # 1. make cloud over land (2) if partly cloudy and no tcwv (refers to c)
    # 2. then make cloud_over_land to partly_cloudy_over_land where we have tcwv

    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tmparr = np.copy(ac_flag_arr_src)
    # set to cloudy if partly cloudy but no valid TCWV:
    tmparr[np.where((ac_flag_arr_src == 1) & (~np.isfinite(tcwv_arr_src)))] = 2
    # set to partly cloudy  if cloudy but valid TCWV:
    tmparr[np.where((ac_flag_arr_src == 2) & (np.isfinite(tcwv_arr_src)))] = 1

    variable[0, :, :] = tmparr[:, :]

def set_atmospheric_conditions_flag(dst, src):
    """
    Sets 'atmospheric_conditions_flag' variable and its attributes.:
    Just copy atmospheric_conditions_flag from raw monthly L3.
    :param dst:
    :param src:
    :return:
    """
    variable = dst.variables['atmospheric_conditions_flag']
    ncu.set_variable_long_name_and_unit_attributes(variable, 'Atmospheric conditions flag', ' ')
    variable.setncattr('standard_name', 'status_flag ')
    min_valid = 0
    max_valid = 4
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3, 4], 'b'))
    variable.setncattr('flag_meanings', 'NO_OBSERVATION CLEAR PARTLY_CLOUDY_OVER_LAND CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN')

    ac_flag_arr_maj = np.array(src.variables['atmospheric_conditions_flag_majority'])
    ac_flag_arr_max = np.array(src.variables['atmospheric_conditions_flag_max'])

    # a) CLOUDY_OVER_LAND (2) in daily products: the pixels identified purely as CLOUD (all L2 samples), no valid TCWV
    # b) PARTLY_CLOUDY_OVER_LAND (5) in daily products: the pixels identified in majority as CLOUD,
    #    but have a valid TCWV
    # c) CLOUDY_OVER_LAND (2) in monthly products: all daily aggregates are CLOUDY_OVER_LAND,
    #    no valid TCWV (usually very few pixels)
    # d) PARTLY_CLOUDY_OVER_LAND (5) in monthly products: at least 1 , but not all daily aggregates are
    #    CLOUDY_OVER_LAND, valid TCWV
    # -->
    # 1. make cloud over land (2) if partly cloudy and no tcwv (refers to c)
    # 2. then make cloud_over_land to partly_cloudy_over_land where we have tcwv

    tcwv_arr_src = np.array(src.variables['tcwv_mean'])
    tmparr = np.copy(ac_flag_arr_maj)
    # set to 3 (cloudy) if partly cloudy but no valid TCWV:
    tmparr[np.where((ac_flag_arr_maj == 2) & (~np.isfinite(tcwv_arr_src)))] = 3
    # set to 2 (partly cloudy)  if cloudy but valid TCWV:
    tmparr[np.where((ac_flag_arr_maj == 3) & (np.isfinite(tcwv_arr_src)))] = 2
    # set to 1 (clear) if not cloudy or partly cloudy and valid TCWV:
    tmparr[np.where((ac_flag_arr_maj < 2) & (np.isfinite(tcwv_arr_src)))] = 1
    # set to 0 (no obs) if not cloudy or partly cloudy and no valid TCWV:
    tmparr[np.where((ac_flag_arr_maj < 2) & (~np.isfinite(tcwv_arr_src)))] = 0

    # we need to activate this to reproduce Phase 1 results!
    # set to partly cloudy  if at least one daily sample is partly cloudy, and not already cloudy:
    tmparr[np.where((ac_flag_arr_max == 2) & (ac_flag_arr_maj > 0) & (ac_flag_arr_maj < 4) & (tmparr != 3))] = 2

    variable[0, :, :] = tmparr[:, :]



def copy_and_rename_variables_from_source_product(dst, src, has_latlon, single_sensors_list):
    """
    Copies variables from source product, renames to correct names, and sets attributes and data...
    for MONTHLY we just need to
            - copy 'num_obs_sum' into 'num_obs', set attributes
            # --> CHANGE in Phase 2: 'num_obs_sum' now 'num_obs'
            # copy 'num_obs_<SINGLESENSOR>_sum' into 'num_obs_<SINGLESENSOR>', set attributes
            # the sum of all 'num_obs_<SINGLESENSOR>' is the old 'num_obs_sum'
            - copy 'tcwv_mean' into 'tcwv', set attributes
            - copy 'stdv_mean' into 'stdv', set attributes
            - copy 'tcwv_err_mean' into 'tcwv_err', set attributes
            - copy 'tcwv_err_ran' into 'tcwv_ran', set attributes
            - copy 'surface_type_flag_majority' to 'surface_type_flag', set attributes
            - copy 'atmospheric_conditions_flag_majority' to 'atmospheric_conditions_flag', set attributes,
                    make cloud_over_land to partly_cloudy_over_land where we have tcwv!

    :param dst:
    :param src:
    :param single_sensors_list:
    :param has_latlon:
    :return:
    """

    for name, variable in src.variables.items():

        if name == 'num_obs':
            # - todo 20201109: per grid cell, we want to have number of days which have a TCWV value:
            # -->  take num_obs = 9*numDaysinMonth (e.g. 279) and 'tcwv_ran_counts' = x*num_obs/numDaysinMonth where x
            # is the number we want. ==> x = tcwv_ran_counts/9 . The '9' comes from 3*3 (superSampling)
            # This is implemented in latest tcwv-l3-monthly-template.json
            dstvar = dst.createVariable('num_days_tcwv', np.int32, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of days in month with a valid TCWV value '
                                                           'in L3 grid cell',
                                                           ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar.setncattr('units', ' ')
            tcwv_ran_counts_arr = np.array(src.variables['tcwv_ran_counts'])
            super_sampling = 3.0
            num_days_tcwv_arr = tcwv_ran_counts_arr * 1.0 / (super_sampling * super_sampling)
            dstvar[0, :, :] = num_days_tcwv_arr[:, :]

        for single_sensor in single_sensors_list:
            if name == 'num_obs_' + single_sensor + '_sum':
                dstvar = dst.createVariable('num_obs_' + single_sensor, np.int32, ('time', 'lat', 'lon'),
                                            zlib=True,
                                            fill_value=getattr(variable, '_FillValue'))
                ncu.copy_variable_attributes_from_source(variable, dstvar)
                ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                               'Number of Total Column of Water Vapour retrievals '
                                                               'contributing '
                                                               'to L3 grid cell',
                                                               ' ')
                dstvar.setncattr('coordinates', 'lat lon')
                dstvar.setncattr('units', ' ')

                num_obs_arr = np.array(variable)
                surface_type_flag_arr = np.array(src.variables['surface_type_flag_majority'])
                # for NIR sensors, set num_obs to 0 over ocean:
                if single_sensor != 'CMSAF_HOAPS':
                    num_obs_arr[np.where(surface_type_flag_arr == 1)] = 0
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
            dstvar.setncattr('ancillary_variables', 'stdv num_obs')
            dstvar[0, :, :] = tcwv_arr[:, :]

        if name == 'stdv_mean':
            dstvar = dst.createVariable('stdv', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Standard deviation of Total Column of Water Vapour',
                                                           'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_err_mean':
            dstvar = dst.createVariable('tcwv_err', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Average retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
        if name == 'tcwv_ran_mean':
            dstvar = dst.createVariable('tcwv_ran', variable.datatype, ('time', 'lat', 'lon'), zlib=True,
                                        fill_value=getattr(variable, '_FillValue'))
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar, 'Random retrieval uncertainty', 'kg/m2')
            dstvar[0, :, :] = variable[:, :]
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
    nc_outfile = 'ESACCI-WATERVAPOUR-L3' + l3_suffix + '-TCWV-' + _sensor + '-' + res + 'deg-' + roi + '-' + \
                 datestring + '-fv' + version + '.nc'
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
    sensor = args[2]
    year = args[3]
    month = args[4]
    res = args[5]
    version = args[6]

    # Maximum contributing sensors depending on observation date
    # (list contains sensors which SHOULD contribute, even if missing for particular day) :
    maximum_single_sensors_list = ncu.get_maximum_single_sensors_list(year, month)
    if ncu.is_cdr_2(sensor):
        maximum_single_sensors_list.append('CMSAF_HOAPS')

    # Source dataset...
    src = Dataset(nc_infile)

    # Initialize nc result file and dataset...
    # we have MONTHLY products:
    datestring = year + month
    dst, nc_outfile = ncu.init_nc_compliant_product(datestring, res, sensor, version)

    # Set dimensions...
    ncu.set_dimensions(dst, src)

    # set global attributes...
    ncu.set_global_attributes(sensor, datestring, dst, 0, month, year, res, version, nc_infile, nc_outfile)

    # Create time variables...
    # use days since 1970-01-01 as time base value, and the 15th of given month at 00:00 as reference time:
    ncu.create_time_variables(dst, '15', month, year)

    # Create lat/lon variables...
    has_latlon, height, width = ncu.set_lat_lon_variables_global(dst, res, src)

    # Create final flag bands:
    # no quality flag in monthlies!
    dst.createVariable('atmospheric_conditions_flag', 'u2',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)
    dst.createVariable('surface_type_flag', 'u2', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)

    copy_and_rename_variables_from_source_product(dst, src, has_latlon, maximum_single_sensors_list)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src)
    # set_atmospheric_conditions_flag_OLD(dst, src)

    # Set final surface type flag...
    set_surface_type_flag(dst, src)

    var_tcwv_arr = np.array(dst.variables['tcwv'])
    dst.variables['tcwv'].setncattr('actual_range', np.array([np.nanmin(var_tcwv_arr), np.nanmax(var_tcwv_arr)], 'f4'))

    # Close files...
    print("Closing L3 input file...", file=sys.stderr)
    src.close()

    print("FINISHED nc_compliance_monthly.py...", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING nc-compliance-monthly-py-process.py", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 7:
        print(
            'Usage:  python nc-compliance-monthly-py-process.py <nc_infile> <sensor> ' +
            '<year> <month> <resolution> < product version>')
        sys.exit(-1)

    run(sys.argv)
