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
    max_valid = 3
    variable.setncattr('valid_range', np.array([min_valid, max_valid], 'b'))
    variable.setncattr('flag_values', np.array([0, 1, 2, 3], 'b'))
    variable.setncattr('flag_meanings', 'CLEAR PARTLY_CLOUDY_OVER_LAND CLOUD_OVER_LAND HEAVY_PRECIP_OVER_OCEAN')

    ac_flag_arr_src = np.array(src.variables['atmospheric_conditions_flag_majority'])
    variable[0, :, :] = ac_flag_arr_src[:, :]


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
            ncu.copy_variable_attributes_from_source(variable, dstvar)
            ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                           'Number of days in month with a valid TCWV value '
                                                           'in L3 grid cell',
                                                           ' ')
            dstvar.setncattr('coordinates', 'lat lon')
            dstvar.setncattr('units', ' ')
            tcwv_ran_counts_arr = np.array(src.variables['tcwv_ran_counts'])
            num_days_tcwv_arr = tcwv_ran_counts_arr * 1.0 / 9.0
            dstvar[0, :, :] = num_days_tcwv_arr[:, :]

        for single_sensor in single_sensors_list:
            if name == 'num_obs_' + single_sensor + '_sum':
                dstvar = dst.createVariable('num_obs_' + single_sensor, variable.datatype, ('time', 'lat', 'lon'),
                                            zlib=True,
                                            fill_value=getattr(variable, '_FillValue'))
                ncu.copy_variable_attributes_from_source(variable, dstvar)
                ncu.set_variable_long_name_and_unit_attributes(dstvar,
                                                               'Number of Total Column of Water Vapour retrievals contributing '
                                                               'to L3 grid cell',
                                                               ' ')
                dstvar.setncattr('coordinates', 'lat lon')
                dstvar.setncattr('units', ' ')

                num_obs_arr = np.array(variable)
                surface_type_flag_arr = np.array(src.variables['surface_type_flag_majority'])
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
    # sensor = args[4].replace('-', '_')
    sensor = args[4]
    year = args[5]
    month = args[6]
    res = args[7]
    version = args[8]

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
    has_latlon, height, width = ncu.set_lat_lon_variables(dst, res, src)

    # Create final flag bands:
    # no quality flag in monthlies!
    # dst.createVariable('surface_type_flag', 'b', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
    dst.createVariable('atmospheric_conditions_flag', 'u2',
                       ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)
    dst.createVariable('surface_type_flag', 'u2', ('time', dst.dimensions['lat'].name, dst.dimensions['lon'].name),
                       zlib=True)

    copy_and_rename_variables_from_source_product(dst, src, has_latlon, maximum_single_sensors_list)

    # Set atmospheric conditions flag...
    set_atmospheric_conditions_flag(dst, src)

    # Set final surface type flag...
    set_surface_type_flag(dst, src)

    # Update (fix) tcwv_err and tcwv_ran in case of existing HOAPS...
    if ds_hoaps:
        update_errors_for_hoaps(dst, ds_hoaps)

    # Cleanup inconsistencies of final arrays at this point:
    # todo: check if needed in monthly compliance
    # cleanup_inconsistencies(dst, sensor)
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
        print(
            'Usage:  python nc-compliance-monthly-py-process.py <nc_infile> <landmask_file> <landcover_file> <sensor> ' +
            '<year> <month> <resolution> < product version> <seaice_mask_file> [<hoaps_l3_file>]')
        sys.exit(-1)

    run(sys.argv)
