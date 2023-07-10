import sys
import os
import numpy as np
from pathlib import Path
from desmile_h2o import apply_desmile_h2o as adh

from collections import OrderedDict as OD
from cowa import cowa


#VERBOSE = True

#def myprint(*args,**kwargs):
    #if VERBOSE:
        #print(*args,**kwargs)




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

#def check_if_files_exists(*arg):
    #for ff in arg:
        #if ff is None:
            #continue
        #if not os.path.exists(ff):
            #print(ff,' not found, exiting!')
            #sys.exit()


def doit():

    cowa_args=cowa.cowa_olci_io.cowa_parser()
    print(cowa_args)
    if cowa_args.ini is None:
        cowa_config = cowa.cowa_core.read_and_analyze_ini(
            os.path.join(SCRIPT_PATH,'ini','olci_processor.ini'),
            COWA_PROCESSOR_RULE)
    else:
        cowa_config = cowa.cowa_core.read_and_analyze_ini(
            cowa_args.ini,
            COWA_PROCESSOR_RULE)

    #sys.exit()
    #SNR_land = cowa_config['PROCESSING']['land_snr'] 
    #SNR_ocean = cowa_config['PROCESSING']['ocean_snr'] 
    #IPE_land = cowa_config['PROCESSING']['land_interpolation_error']
    #IPE_ocean = cowa_config['PROCESSING']['ocean_interpolation_error']
    stride = cowa_config['PROCESSING']['stride']

    cowa_land_processor = cowa.cowa_land(cowa_config['GENERAL']['land_processor_ini'])
    cowa_ocean_processor = cowa.cowa_ocean(cowa_config['GENERAL']['ocean_processor_ini'])


    l1_name = cowa_args.l1
    l2_name = cowa_args.l2
    idp_name = cowa_args.idp
    tcwv_name = cowa_args.result
    cowa.cowa_olci_io.check_if_files_exists(l1_name,)
    if cowa_args.stride is None:
        pass
        #stride = stride
    else:
        stride = [int(i) for i in cowa_args.stride.split(',')]
        cowa_config['PROCESSING']['stride'] = stride
        
    if not cowa_args.cmi:
        if idp_name:
            print('"Use cloud mask from idepix" not set but idepix file name given. I ignore idepix and choose L2.','green')
        if l2_name: 
            cowa.cowa_olci_io.check_if_files_exists(l2_name,)
            cowa.cowa_olci_io.test_agreement_and_exit_if_not(l1_name,l2_name)
        else:
            print('L2 not given, exiting!','red')
            sys.exit(2)
    else:
        if l2_name: 
            print('"Use cloud mask from idepix" set and l2 given. I choose idepix and ignore L2.','green')
        if not idp_name:
            idp_name = str(Path(l1_name)/'Idepix.nc')
        cowa.cowa_olci_io.check_if_files_exists(idp_name,)

    if not tcwv_name:
        tcwv_name = cowa.cowa_olci_io.l1_to_result(l1_name, stride)
    
    #print(l1_name,l2_name,idp_name,tcwv_name)

    # get and prepare  all necessary data
    data = cowa.cowa_olci_io.get_relevant_l1l2_data(l1_name, l2_name, idp_name, cowa_config, 
            cmi=cowa_args.cmi, icm=cowa_args.ignore_cloudmask, ims=cowa_args.ignore_maxsolar,
            ail=cowa_args.all_is_land)
    
    if cowa_args.desmile_h2o_bands:
        cwvl,fwhm = adh.get_spectral_charact(data['orb'], adh.SPEC_CHAR_LUT[data['olci']])
        #print(cwvl.shape)
        data.update(adh.prepare_homog(data,cwvl,fwhm))
        #print(data.keys())
        cor = adh.perform_correction(data)
        data['rad'][19] *=cor['19_corr']
        data['rad'][20] *=cor['20_corr']
        #sys.exit()

    
    #sys.exit()
    data_land = cowa_land_processor.prepare_processing(data,cowa_config,target = 'land')  
    data_ocean = cowa_ocean_processor.prepare_processing(data,cowa_config,target = 'ocean')  
    data_land['eps'] = float(cowa_land_processor.config['INTERNAL']['eps'])
    data_land['maxiter'] = int(cowa_land_processor.config['INTERNAL']['maxiter'])
    data_land['progress'] = cowa_args.progress
    data_ocean['eps'] = float(cowa_ocean_processor.config['INTERNAL']['eps'])
    data_ocean['maxiter'] = int(cowa_ocean_processor.config['INTERNAL']['maxiter'])
    data_ocean['progress'] = cowa_args.progress

    out = OD()
    out['lon'] = data['lon']
    out['lat'] = data['lat']
    out['tcwv'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    out['tcwv_prior'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    out['cnv'] = np.zeros_like(data['lon'],dtype=bool)
    out['avk'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    out['dof'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    out['cst'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    out['unc'] = np.zeros_like(data['lon'],dtype=np.float32)+np.nan
    #out['nit'] = np.zeros_like(data['lon'],dtype=np.int16)
    out['envelop'] = data['envelop']

    if data_land['yy'].shape[0] > 0:
        erg_land  = cowa_land_processor.inverse(**data_land)   
        out['tcwv'][data['dfl']] = erg_land.state[:,0]**2 
        out['tcwv_prior'][data['dfl']] = data['tcw'][data['dfl']]
        out['cnv'][data['dfl']] = erg_land.convergence        
        out['avk'][data['dfl']] = erg_land.averaging_kernel[:,0,0]        
        out['dof'][data['dfl']] = np.trace(erg_land.averaging_kernel,0,1,2)        
        out['cst'][data['dfl']] = erg_land.cost        
        out['unc'][data['dfl']] = erg_land.retrieval_error_covariance[:,0,0]        
        #out['nit'][data['dfl']] = erg_land.number_of_iterations        
    if data_ocean['yy'].shape[0] > 0:
        erg_ocean  = cowa_ocean_processor.inverse(**data_ocean)
        out['tcwv'][data['dfo']] = erg_ocean.state[:,0]**2 
        out['tcwv_prior'][data['dfo']] = data['tcw'][data['dfo']]
        out['cnv'][data['dfo']] = erg_ocean.convergence        
        out['avk'][data['dfo']] = erg_ocean.averaging_kernel[:,0,0]
        out['dof'][data['dfo']] = np.trace(erg_ocean.averaging_kernel,0,1,2)        
        out['cst'][data['dfo']] = erg_ocean.cost        
        out['unc'][data['dfo']] = erg_ocean.retrieval_error_covariance[:,0,0]
        #out['nit'][data['dfo']] = erg_ocean.number_of_iterations        
    out['unc'] = np.sqrt(out['unc']*4*out['tcwv'])
 
#  5. finaly write result
    gatr={
        'l1_file': Path(l1_name).name,
        'l2_file': Path(str(l2_name)).name,         # trick to handle None
        'idepix_file': str(idp_name),         # trick to handle None
        'use_idepix': str(cowa_args.cmi),
        'cmdline': ' '.join(sys.argv),
        }
    cowa.cowa_olci_io.write_to_ncdf(tcwv_name,out,gatr)
    cowa.cowa_olci_io.add_txt_to_netcdf(tcwv_name, 'l1_manifest', data['l1_manifest'])
    cowa.cowa_olci_io.add_txt_to_netcdf(tcwv_name, 'l2_manifest', str(data['l2_manifest']))
    if cowa_args.cmi:
        cowa.cowa_olci_io.add_idepix_to_netcdf(idp_name,tcwv_name,stride)
    
    

if __name__  == '__main__':
    doit()
#olci_processor4cci.py -l1 ../examples/S3A_OL_1_EFR____20180818T095755_20180818T100055_20180819T145855_0179_034_350_2160_MAR_O_NT_002.SEN3 -l2 ../examples/S3A_OL_2_WFR____20180818T095755_20180818T100055_20180819T161250_0179_034_350_2160_MAR_O_NT_002.SEN3 -t /tmp/bla.nc4 -s 20,20 -ini ini/olci_processor4cci.ini
