import os
import sys
import collections

import numpy as np
from netCDF4 import Dataset

from cowa import cowa_meris as cowa

SCRIPT_PATH = os.path.dirname(os.path.realpath(__file__))
COWA_DEMO_PROCESSOR_RULE = {
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
DEMO_CONFIG = cowa.cowa_core.read_and_analyze_ini(
    os.path.join(SCRIPT_PATH, 'ini', 'meris_processor.ini'),
    COWA_DEMO_PROCESSOR_RULE)

STRIDE = DEMO_CONFIG['PROCESSING']['stride']

SNR_land = DEMO_CONFIG['PROCESSING']['land_snr']  # , actually, its hard to believe ...
SNR_ocean = DEMO_CONFIG['PROCESSING']['ocean_snr']  # , actually, its hard to believe ...
IPE_land = DEMO_CONFIG['PROCESSING']['land_interpolation_error']
IPE_ocean = DEMO_CONFIG['PROCESSING']['ocean_interpolation_error']

cowa_land_processor = cowa.cowa_land(DEMO_CONFIG['GENERAL']['land_processor_ini'])
cowa_ocean_processor = cowa.cowa_land(DEMO_CONFIG['GENERAL']['ocean_processor_ini'])


def doit():
    if len(sys.argv) != 3:
        print('Usage: %s l1file result' % sys.argv[0])
        sys.exit()

    # 1. get sysargv
    l1_name = sys.argv[1]
    tcwv_name = sys.argv[2]

    if l1_name.lower().endswith('nc') or l1_name.lower().endswith('nc.gz'):
        print('input is NetCDF format...')
        cowa.cowa_meris_io_nc_for_cci.check_if_files_exists(l1_name)
        ds_l1 = Dataset(l1_name)
        start_date_string = ds_l1.getncattr('start_date')
        stop_date_string = ds_l1.getncattr('stop_date')
        data = cowa.cowa_meris_io_nc_for_cci.get_relevant_l1l2_data(ds_l1, DEMO_CONFIG, cmi=True)
    else:
        print('input is not NetCDF format - exiting')
        sys.exit()

    data_land = cowa_land_processor.prepare_processing(data, DEMO_CONFIG, target='land')
    data_ocean = cowa_ocean_processor.prepare_processing(data, DEMO_CONFIG, target='ocean')
    data_land['eps'] = float(cowa_land_processor.config['INTERNAL']['eps'])
    data_land['maxiter'] = int(cowa_land_processor.config['INTERNAL']['maxiter'])
    data_land['progress'] = True
    data_ocean['eps'] = float(cowa_ocean_processor.config['INTERNAL']['eps'])
    data_ocean['maxiter'] = int(cowa_ocean_processor.config['INTERNAL']['maxiter'])
    data_ocean['progress'] = True

    erg_land = None
    erg_ocean = None
    if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
        erg_land = cowa_land_processor.inverse(**data_land)
    if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
        erg_ocean = cowa_ocean_processor.inverse(**data_ocean)

    #    4. select relevant fields
    out = collections.OrderedDict()

    out['lon'] = data['lon']
    out['lat'] = data['lat']
    out['lon_tp'] = data['lon_tp']
    out['lat_tp'] = data['lat_tp']
    out['stfl'] = data['stfl']
    out['tcwv'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['cst'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    out['nit'] = np.zeros_like(data['lon'], dtype=np.int16)
    out['unc'] = np.zeros_like(data['lon'], dtype=np.float32) + np.nan
    if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
        out['tcwv'][data['dfl']] = erg_land.state[:, 0] ** 2
        out['cst'][data['dfl']] = erg_land.cost
        out['nit'][data['dfl']] = erg_land.number_of_iterations
        out['unc'][data['dfl']] = erg_land.retrieval_error_covariance[:, 0, 0]
        out['unc'] = np.sqrt(out['unc'] * 4 * out['tcwv'])
    if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
        out['tcwv'][data['dfo']] = erg_ocean.state[:, 0] ** 2
        out['cst'][data['dfo']] = erg_ocean.cost
        out['nit'][data['dfo']] = erg_ocean.number_of_iterations
        out['unc'][data['dfo']] = erg_ocean.retrieval_error_covariance[:, 0, 0]

    # below debug only
    # for k in ('wsp','prs','tem','amf','tcw','cld','lsm',):
    #     out['d_'+k] = data[k]
    # for k in data['rad']:
    #     out['d_rad_%i'%k] = data['rad'][k]
    # for i in range(3):
    #     for k in ('yy','xa'):
    #         out['d_inp_%s_%i'%(k,i)] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    #         if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
    #             out['d_inp_%s_%i'%(k,i)][data['dfl']] = data_land[k][:,i]
    #         if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
    #             out['d_inp_%s_%i'%(k,i)][data['dfo']] = data_ocean[k][:,i]
    # for i in range(6):
    #     k = 'pa'
    #     out['d_inp_%s_%i'%(k,i)] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    #     if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
    #         out['d_inp_%s_%i'%(k,i)][data['dfl']] = data_land[k][:,i]
    #     if i <3:
    #         if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
    #             out['d_inp_%s_%i'%(k,i)][data['dfo']] = data_ocean[k][:,i]
    # for i in range(3):
    #     for k in ('se','sa'):
    #         out['d_inp_%s_%i'%(k,i)] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    #         if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
    #             out['d_inp_%s_%i'%(k,i)][data['dfl']] = data_land[k][:,i,i]
    #         if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
    #             out['d_inp_%s_%i'%(k,i)][data['dfo']] = data_ocean[k][:,i,i]
    # for i in range(3):
    #     for j in range(3):
    #         out['d_jac_%i_%i'%(i,j)] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    #         if data_land['yy'].shape[0] > 0 and data_land['yy'].shape[1] > 0:
    #             out['d_jac_%i_%i'%(i,j)][data['dfl']] = erg_land.jacobian[:,i,j]
    #         if data_ocean['yy'].shape[0] > 0 and data_ocean['yy'].shape[1] > 0:
    #             out['d_jac_%i_%i'%(i,j)][data['dfo']] = erg_ocean.jacobian[:,i,j]

    #  5. finaly write result
    cowa.cowa_meris_io_nc_for_cci.write_to_ncdf_cci(tcwv_name, out, start_date_string, stop_date_string)


if __name__ == '__main__':
    doit()
