import os
from collections import OrderedDict as OD
from pathlib import Path

import numpy as np
from netCDF4 import Dataset

from cowa import cowa
from desmile_h2o import apply_desmile_h2o as adh

# ###
# Slightly modified version of olci_processor4cci.py.
# I.e., provides advanded NetCDF output comaptible with input specs for Calvalus L3 processing chain.
#
# Run with:
#       olci_l2_processor_for_calvalus.py
#           -l1 <OLCI L1 product> -idp <Idepix NetCDF product>  -ini <ini file> -t <TCWV result NetCDF L2 product>
#
# OD, 20230702
# ###

SCRIPT_PATH = os.path.dirname(os.path.realpath(__file__))

COWA_PROCESSOR_RULE = {
    'GENERAL':
        {
            'land_processor_ini': 's',
            'ocean_processor_ini': 's',
            'coast_border': 'i',
        },
    'PROCESSING':
        {
            'land_apriori_error_covariance_diagonals': 'lf',
            'ocean_apriori_error_covariance_diagonals': 'lf',
            'land_snr': 'lf',
            'ocean_snr': 'lf',
            'land_interpolation_error': 'lf',
            'ocean_interpolation_error': 'lf',
            'temperature_uncertainty': 'f',
            'pressure_uncertainty': 'f',
            'land_aot_fallback': 'f',
            'ocean_aot_fallback': 'f',
            'aot_fallback_uncertainty': 'f',
            'spectral_uncertainty': 'f',
            'relative_model_uncertainty': 'f',
            'min_norm_rad': 'f',
            'min_coast_norm_rad': 'f',
            'max_solar_zenith': 'f',
            'stride': 'li',
            'debug': 'b',
        },
}


def doit():
    """

    :return:
    """
    cowa_args = cowa.cowa_olci_io_for_cci.cowa_parser()
    print(cowa_args)
    if cowa_args.ini is None:
        cowa_config = cowa.cowa_core.read_and_analyze_ini(
            os.path.join(SCRIPT_PATH, 'ini', 'olci_processor.ini'),
            COWA_PROCESSOR_RULE)
    else:
        cowa_config = cowa.cowa_core.read_and_analyze_ini(
            cowa_args.ini,
            COWA_PROCESSOR_RULE)

    stride = cowa_config['PROCESSING']['stride']

    # cowa_land_processor = cowa.cowa_land(cowa_config['GENERAL']['land_processor_ini'])
    # cowa_ocean_processor = cowa.cowa_ocean(cowa_config['GENERAL']['ocean_processor_ini'])
    land_processor_ini = cowa_args.lpi
    ocean_processor_ini = cowa_args.opi
    land_lut = cowa_args.ll
    ocean_lut = cowa_args.ol
    cowa_land_processor = cowa.cowa_land(land_processor_ini, land_lut)
    cowa_ocean_processor = cowa.cowa_ocean(ocean_processor_ini, ocean_lut)

    l1_name = cowa_args.l1
    idp_name = cowa_args.idp
    if not idp_name:
        idp_name = str(Path(l1_name) / 'Idepix.nc')
    tcwv_name = cowa_args.result
    cowa.cowa_olci_io_for_cci.check_if_files_exists(l1_name, idp_name)

    ds_idp = Dataset(idp_name)
    start_date_string = ds_idp.getncattr('start_date')
    stop_date_string = ds_idp.getncattr('stop_date')

    if cowa_args.stride is None:
        pass
    else:
        stride = [int(i) for i in cowa_args.stride.split(',')]
        cowa_config['PROCESSING']['stride'] = stride

    if not tcwv_name:
        tcwv_name = cowa.cowa_olci_io_for_cci.l1_to_result(l1_name, stride)

    # get and prepare  all necessary data
    data = cowa.cowa_olci_io_for_cci.get_relevant_l1l2_data(l1_name, idp_name, cowa_config)
    if cowa_args.desmile_h2o_bands:
        olci_ = data['olci']
        if olci_ is None:
            index_of_olci = l1_name.find("S3")
            olci_ = l1_name[index_of_olci + 2: index_of_olci + 3]

        cwvl, fwhm = adh.get_spectral_charact(data['orb'], adh.SPEC_CHAR_LUT[olci_])
        data.update(adh.prepare_homog(data, cwvl, fwhm))
        cor = adh.perform_correction(data)
        data['rad'][19] *= cor['19_corr']
        data['rad'][20] *= cor['20_corr']

    data_land = cowa_land_processor.prepare_processing(data, cowa_config, target='land')
    data_ocean = cowa_ocean_processor.prepare_processing(data, cowa_config, target='ocean')
    data_land['eps'] = float(cowa_land_processor.config['INTERNAL']['eps'])
    data_land['maxiter'] = int(cowa_land_processor.config['INTERNAL']['maxiter'])
    data_ocean['eps'] = float(cowa_ocean_processor.config['INTERNAL']['eps'])
    data_ocean['maxiter'] = int(cowa_ocean_processor.config['INTERNAL']['maxiter'])

    out = OD()
    out['lon'] = data['lon']
    out['lat'] = data['lat']
    out['lon_tp'] = data['lon_tp']
    out['lat_tp'] = data['lat_tp']
    out['stfl'] = data['stfl']
    out['tcwv'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['tcwv_prior'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['cnv'] = np.zeros_like(data['lon'], dtype=bool)
    out['avk'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['dof'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['cst'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['unc'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    # out['nit'] = np.zeros_like(data['lon'],dtype=np.int16)
    # out['envelop'] = data['envelop']

    if data_land['yy'].shape[0] > 0:
        erg_land = cowa_land_processor.inverse(**data_land)
        out['tcwv'][data['dfl']] = erg_land.state[:, 0] ** 2
        out['tcwv_prior'][data['dfl']] = data['tcw'][data['dfl']]
        out['cnv'][data['dfl']] = erg_land.convergence
        out['avk'][data['dfl']] = erg_land.averaging_kernel[:, 0, 0]
        out['dof'][data['dfl']] = np.trace(erg_land.averaging_kernel, 0, 1, 2)
        out['cst'][data['dfl']] = erg_land.cost
        out['unc'][data['dfl']] = erg_land.retrieval_error_covariance[:, 0, 0]
        # out['nit'][data['dfl']] = erg_land.number_of_iterations
    if data_ocean['yy'].shape[0] > 0:
        erg_ocean = cowa_ocean_processor.inverse(**data_ocean)
        out['tcwv'][data['dfo']] = erg_ocean.state[:, 0] ** 2
        out['tcwv_prior'][data['dfo']] = data['tcw'][data['dfo']]
        out['cnv'][data['dfo']] = erg_ocean.convergence
        out['avk'][data['dfo']] = erg_ocean.averaging_kernel[:, 0, 0]
        out['dof'][data['dfo']] = np.trace(erg_ocean.averaging_kernel, 0, 1, 2)
        out['cst'][data['dfo']] = erg_ocean.cost
        out['unc'][data['dfo']] = erg_ocean.retrieval_error_covariance[:, 0, 0]
        # out['nit'][data['dfo']] = erg_ocean.number_of_iterations
    out['unc'] = np.sqrt(out['unc'] * 4 * out['tcwv'])

    #  5. finaly write result
    cowa.cowa_olci_io_for_cci.write_to_ncdf_cci(tcwv_name, out, start_date_string, stop_date_string)


if __name__ == '__main__':
    doit()
