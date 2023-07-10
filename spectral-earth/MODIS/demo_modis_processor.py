import sys, os
import time, datetime, calendar, tqdm
from lxml import etree as ET
from netCDF4 import Dataset
from  pyhdf import SD
import numpy as np
import scipy.interpolate as interp
import re
#import hdf_sd
import modis_l1b_alt as modis_l1b
from scipy import signal  

script_path = os.path.dirname(os.path.realpath(sys.argv[0]))
sys.path.append(os.path.realpath(os.path.join(script_path,'..')))
from cowa import cowa



DEMO_PROCESSOR_RULE = {
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
            'land_aot_fallback': 'f',
            'ocean_aot_fallback': 'f',
            'temperature_uncertainty': 'f',
            'pressure_uncertainty': 'f',
            'aot_fallback_uncertainty': 'f',
            'spectral_uncertainty': 'f',
            'relative_model_uncertainty': 'f',
            'min_norm_rad': 'f',
            'max_solar_zenith': 'f',
            'debug': 'b', 
        },
}

DEMO_CONFIG = cowa.cowa_core.read_and_analyze_ini(
    os.path.join(script_path,'demo_modis_processor.ini'),
    DEMO_PROCESSOR_RULE)

SNR_land = DEMO_CONFIG['PROCESSING']['land_snr'] #, actually, its hard to believe ...
SNR_ocean = DEMO_CONFIG['PROCESSING']['ocean_snr'] #, actually, its hard to believe ...

# pseudo_absoprtion_measurement uncertainty,
# needed for SE
PABUNCL = lambda itp_unc, amf: cowa.cowa_core.snr_to_pseudo_absoprtion_measurement_variance(SNR_land, itp_unc, amf)
PABUNCO = lambda itp_unc, amf: cowa.cowa_core.snr_to_pseudo_absoprtion_measurement_variance(SNR_ocean, itp_unc, amf)


def check_if_files_exists(*arg):
    for ff in arg:
        if ff is None:
            continue
        if not os.path.exists(ff):
            print(ff,' not found, exiting!')
            sys.exit()

def cosd(inn):
    return np.cos(inn*np.pi/180.)
def sind(inn):
    return np.sin(inn*np.pi/180.)
def acosd(inn):
    return np.arccos(inn)*180./np.pi
def asind(inn):
    return np.arcsin(inn)*180./np.pi

def azi2azid(sa,va):
    return acosd(cosd(sa)*cosd(va)+sind(sa)*sind(va))

def mask2nan(inn,buul=False):
    #return inn
    if isinstance(inn,np.ma.MaskedArray):
        if buul is False:
            return inn.filled(fill_value=np.nan)
        else:
            return inn.filled(fill_value=False)
    else:
        return inn

def height2press(hh):
    '''
    This is very simple, but actually sufficient 
    '''
    return 1013.*(1.-(hh*0.0065/288.15))**5.2555

def interpolate_tie(inn,al,ac):
    npa=np.arange
    npl=np.linspace
    sh_in=inn.shape
    sh_ou=((sh_in[0]-1)*al+1,(sh_in[1]-1)*ac+1)
    ou=interp.RectBivariateSpline(
          npl(0.,sh_ou[0],sh_in[0])
        , npl(0.,sh_ou[1],sh_in[1])
        , inn, kx=1, ky=1)(npa(sh_ou[0])
                         , npa(sh_ou[1]))
    return ou

def get_metadata(mod):
    fh = SD.SD(mod,SD.SDC.READ)
    meta =fh.attributes()['CoreMetadata.0']
    fh.end()
    return meta

def getTimestamp(mod):
    met = get_metadata(mod)
    end_date = re.findall('\d{4}-\d{2}-\d{2}', re.findall('RANGEENDINGDATE[\s\S]+RANGEENDINGDATE',met)[0])[0]
    start_date = re.findall('\d{4}-\d{2}-\d{2}', re.findall('RANGEBEGINNINGDATE[\s\S]+RANGEBEGINNINGDATE',met)[0])[0]
    end_time = re.findall('\d{2}:\d{2}:\d{2}', re.findall('RANGEENDINGTIME[\s\S]+RANGEENDINGTIME',met)[0])[0]
    start_time = re.findall('\d{2}:\d{2}:\d{2}', re.findall('RANGEBEGINNINGTIME[\s\S]+RANGEBEGINNINGTIME',met)[0])[0]
    
    start_ts = calendar.timegm(time.strptime(start_date+' '+start_time,'%Y-%m-%d %H:%M:%S'))
    end_ts = calendar.timegm(time.strptime(end_date+' '+end_time,'%Y-%m-%d %H:%M:%S'))
    
    return start_ts, end_ts

def test_agreement_and_exit_if_not(mod1, mod2):
    
    if not ( getTimestamp(mod1) == getTimestamp(mod2)):
        print('times of L1 and L2 do not fit, exiting')
        print('L1:', getTimestamp(mod1)) 
        print('L2:', getTimestamp(mod2)) 
        sys.exit()

def height2press(hh):
    '''
    This is very simple, but actually sufficient 
    '''
    return 1013.*(1.-(hh*0.0065/288.15))**5.2555

def get_altitude(mer,st):
    alt, ncols, nlins, ac, al = get_tie_data(mer,'dem_alt')
    return interpolate_tie(alt,al,ac)[::st[0],::st[1]]

def get_atm_press(mer,st):
    prs, ncols, nlins, ac, al = get_tie_data(mer,'atm_press')
    return interpolate_tie(prs,al,ac)[::st[0],::st[1]]

def get_l2flags(dim,st):
    et = ET.parse(dim)
    ncols = int(et.find('Raster_Dimensions/NCOLS').text)
    nlins = int(et.find('Raster_Dimensions/NROWS').text)
    info = et.xpath('Image_Interpretation/Spectral_Band_Info/BAND_NAME[text()="l2_flags"]')[0].getparent()
    dtype = info.find("DATA_TYPE").text
    base = os.path.join('.'.join((os.path.splitext(dim)[0],'data')),'l2_flags')
    img = np.fromfile(base+'.img',count=-1,dtype=dtype)
    img = img.byteswap(True)
    return img.reshape(nlins,ncols)[::st[0],::st[1]]+0

def simple_coast(lm, b=5):
    '''
    shift in 8 directions ...
    '''
    kk = np.ones((2*b+1,2*b+1))
    cs = signal.convolve2d(lm+0,kk+0)
    out = ((cs[b:-b,b:-b]>0) & (~lm))
    return out

def simple_cloudmargin(cm, b=5):
    kk = np.ones((2*b+1,2*b+1))
    cs = signal.convolve2d(cm+0,kk+0)
    out = ((cs[b:-b,b:-b]>0) | cm)
    return out
    

def get_relevant_l1_and_aux(mod1, era): 
    '''
    '''
    # this is fix, and fits to the prepared 
    # aux and mask data
    stride=[5,5] 
    offset=[0,2]
    count=[2030,1350]
    # this is fix, and fits to the prepared 
    # aux and mask data
   
    mo=modis_l1b.modis_l1b(mod1)
    m2n = mask2nan
    rad = {k:m2n(mo.get_band(k,reflectance=True,stride=stride,offset=offset,count=count)) 
                     / np.pi for k in [2,5,17,18,19,]}
    suz=m2n(mo.get_solar_zenith(stride=stride,offset=offset,count=count))
    vie=m2n(mo.get_sensor_zenith(stride=stride,offset=offset,count=count))
    saa=m2n(mo.get_solar_azimuth(stride=stride,offset=offset,count=count))
    vaa=m2n(mo.get_sensor_azimuth(stride=stride,offset=offset,count=count))
    lon=m2n(mo.get_longitude(stride=stride,offset=offset,count=count))
    lat=m2n(mo.get_latitude(stride=stride,offset=offset,count=count))        
    alt=m2n(mo.get_height(stride=stride,offset=offset,count=count))

    ada=azi2azid(saa,vaa)
    amf = 1./cosd(suz)+1./cosd(vie)
 
    out =  {'lon': lon, 'lat': lat, 'vie': vie, 'suz': suz, 'saa': saa, 'vaa': vaa, 'ada': ada,
            'amf': amf, 'rad': rad, 'alt': alt, 'prs': height2press(alt)}
    with Dataset(era) as ds:
        for k in ds.variables:
            out[k] = ds[k][:]+0
            if k != 'mask':
                out[k] = m2n(out[k])
    out['cfree'] = m2n(((out['mask'] & 2**1 ) == 2**1 ) & ((out['mask'] & 2**2 ) == 2**2 ),True)
    out['water'] = m2n(((out['mask'] & 2**6 ) == 0    ) & ((out['mask'] & 2**7 ) == 0    ),True)
    out['coast'] = simple_coast(~out['water'], b=3)
    out['cld'] = simple_cloudmargin(~out['cfree'], b=1)
    out['aot'] = np.zeros_like(alt) +np.nan
    out['nnn'] = alt.size
    #for k in out: 
        #if isinstance(out[k],np.ndarray):
            #print(out[k].shape)
    return out

def pixel_not_ok(data,i):
    #if data['msk'].flat[i]:
    #    return True
    ok = True
    ok = ok and not data['cld'].flat[i]
    ok = ok and not (data['suz'].flat[i] > DEMO_CONFIG['PROCESSING']['max_solar_zenith'])
    for b in (2,18,19):
        ok = ok and not (data['rad'][b].flat[i] < DEMO_CONFIG['PROCESSING']['min_norm_rad'])
    return not ok



def write_to_ncdf(outname,dct):
    yy= dct['lat'].shape[0]   
    xx= dct['lat'].shape[1]   
    for key in dct:
        if dct[key].ndim != 2:
            print( key,'is not 2 dimensional')
            return None
        if dct[key].shape[0] != yy:
            print( "Dimension 0 of ",key ,'does not agree.')
            print( "is:", dct[key].shape[0],"should be ",yy,"(like lat)")
            return None
        if dct[key].shape[1] != xx:
            print( "Dimension 1 of ",key ,'does not agree.')
            print( "is:", dct[key].shape[1],"should be ",xx,"(like lat)")
            return None
        if dct[key].dtype == np.bool_:
            dct[key] = dct[key].astype(np.int8)

    with Dataset(outname, 'w', format='NETCDF4') as nc:
        nc.filename = outname
        nc.history = datetime.datetime.now().strftime('created on %Y-%m-%d %H:%M:%S UTC')
        nc.Conventions = "CF-1.4"
        nc.metadata_version = "0.5"
        nc.createDimension('y',yy)
        nc.createDimension('x',xx)
        for key in dct:
            nc_dum=nc.createVariable(key,dct[key].dtype,('y','x'))
            nc_dum[:]=dct[key]

def ternary(cond, a, b):
    if bool(cond):
        return a
    else:
        return b


def doit():
    if len(sys.argv) != 3:
        print('Usage: %s l1file resultfile'%sys.argv[0])
        sys.exit()

    # 1. get sysargv
    l1_name = sys.argv[1]
    tcwv_name = sys.argv[2]
    mod_base = os.path.basename(l1_name).split('.')
    aux_name = os.path.join(os.path.dirname(l1_name),'.'.join(('ERA',mod_base[1],mod_base[2],'nc4')))
  
    # 2. check l1 l2 
    check_if_files_exists(l1_name,aux_name)
    #test_agreement_and_exit_if_not(l1_name,l2_name)
    #sys.exit()
    
    # 3. get all necessary data
    data = get_relevant_l1_and_aux(l1_name, aux_name)
    #print(data['prs'])
    #import pylab as pl
    #pl.imshow((data['cld']& data['cfree'])+0)
    ##pl.imshow(data['rad'][2])
    #pl.colorbar()
    #pl.show()
    #sys.exit()
    
    # 4. initialize cowa
    cowa_land_processor = cowa.cowa_land(DEMO_CONFIG['GENERAL']['land_processor_ini'])
    cowa_ocean_processor = cowa.cowa_ocean(DEMO_CONFIG['GENERAL']['ocean_processor_ini'])
    SA_ocean = np.diag([e for e in DEMO_CONFIG['PROCESSING']['ocean_apriori_error_covariance_diagonals']])
    SA_land = np.diag([e for e in DEMO_CONFIG['PROCESSING']['land_apriori_error_covariance_diagonals']])

    # Measurement error corariance
    SE_ocean = np.zeros((4,4))
    SE_land = np.zeros((5,5))
    # First the window bands
    SE_ocean[0,0] = 1./DEMO_CONFIG['PROCESSING']['ocean_snr']**2
    SE_ocean[1,1] = 100.  # switching off 17 over ocean (too much noise)
    SE_land[0,0] = 1./DEMO_CONFIG['PROCESSING']['land_snr']**2
    SE_land[1,1] = 1./DEMO_CONFIG['PROCESSING']['land_snr']**2
   # The pseudo_absoprtion_measurement uncertainty depends on amf und thus comes later

    # 5. initialize arrays for results
    sha=data['rad'][2].shape
    out = {}
    for k in ('al0', 'al1', 'tcwv', 'aot', 'wsp', 'dof', 'avk', 'unc', 'cst'):
        out[k] = np.zeros(sha,dtype=np.float32)*np.nan 
    out['cnv'] = np.zeros(sha,dtype=np.bool_)
    out['nit'] = np.zeros(sha,dtype=np.int8)
    out['lon'] = data['lon']
    out['lat'] = data['lat']
    out['coast'] = data['coast']
    out['land'] = ~data['water']
    out['l2wv'] = data['l2wv']*10.
    
    # 6. iterate over all pixel
    for i in tqdm.tqdm(range(data['nnn']),ascii=True):
        if pixel_not_ok(data,i):
            continue
        inp = {'suz':   data['suz'].flat[i]
            ,'vie':   data['vie'].flat[i]
            ,'azi':   180.-data['ada'].flat[i]
            #,'azi':   data['ada'].flat[i]
            ,'press': data['prs'].flat[i] 
            ,'tmp':   data['t2m'].flat[i]
            #,'wvl':   data['lam19'].flat[i]
            ,'press_unc': DEMO_CONFIG['PROCESSING']['pressure_uncertainty']
            ,'aot_unc':   DEMO_CONFIG['PROCESSING']['aot_fallback_uncertainty']
            ,'tmp_unc':   DEMO_CONFIG['PROCESSING']['temperature_uncertainty']
            ,'wvl_unc':   DEMO_CONFIG['PROCESSING']['spectral_uncertainty']
            ,'rtoa':{'2':data['rad'][2].flat[i]
                    ,'5':data['rad'][5].flat[i]
                    ,'17':data['rad'][17].flat[i]
                    ,'18':data['rad'][18].flat[i]
                    ,'19':data['rad'][19].flat[i]
                    },
            'prior_iwv': data['tcwv'].flat[i]
            }
        # land or coast
        if ~data['water'].flat[i] or (data['coast'].flat[i] and (data['rad'][2].flat[i] > 0.011)):
            SE_land[2,2] = PABUNCL(DEMO_CONFIG['PROCESSING']['land_interpolation_error'][0], data['amf'].flat[i])
            SE_land[3,3] = PABUNCL(DEMO_CONFIG['PROCESSING']['land_interpolation_error'][1], data['amf'].flat[i])
            SE_land[4,4] = PABUNCL(DEMO_CONFIG['PROCESSING']['land_interpolation_error'][2], data['amf'].flat[i])
            inp['aot'] = ternary(np.isfinite(data['aot'].flat[i]), data['aot'].flat[i],
                                             DEMO_CONFIG['PROCESSING']['land_aot_fallback'])
            inp['prior_al0'] = data['rad'][2].flat[i]*np.pi
            inp['prior_al1'] = data['rad'][5].flat[i]*np.pi
            inp['se'] = SE_land + 0
            inp['sa'] = SA_land + 0 
            # decrease uncertainty of prior for coast
            if data['coast'].flat[i]:
                inp['sa'][0,0] =  SA_ocean[0,0] + 0
            erg = cowa_land_processor.estimator(inp)
            out['al0'].flat[i] = erg['result'].state[1]
            out['al1'].flat[i] = erg['result'].state[2]
            #print('land',~data['water'].flat[i], 'se:',inp['se'])
            #sys.exit()

        #ocean
        else:
            SE_ocean[2,2] = PABUNCO(DEMO_CONFIG['PROCESSING']['ocean_interpolation_error'][1], data['amf'].flat[i])
            SE_ocean[3,3] = PABUNCO(DEMO_CONFIG['PROCESSING']['ocean_interpolation_error'][2], data['amf'].flat[i])
            #print('SE_ocean',SE_ocean)
            inp['prior_wsp'] = data['wsp'].flat[i]
            inp['prior_aot'] = ternary(np.isfinite(data['aot'].flat[i]), data['aot'].flat[i],
                                             DEMO_CONFIG['PROCESSING']['ocean_aot_fallback'])
            inp['se'] = SE_ocean + 0
            inp['sa'] = SA_ocean + 0
            erg = cowa_ocean_processor.estimator(inp)
            out['wsp'].flat[i] = erg['result'].state[2]
            out['aot'].flat[i] = erg['result'].state[1]
            #print(erg['result'].state,inp)
            #print('se:',inp['se'])
            #print('ocean',data['water'].flat[i], 'se:',inp['se'],'sa:',inp['sa'])
            #sys.exit()

        out['tcwv'].flat[i] = erg['iwv']
        out['cnv'].flat[i] = erg['result'].convergence        
        out['dof'].flat[i] = erg['result'].averaging_kernel.trace()
        out['avk'].flat[i] = erg['result'].averaging_kernel[0,0]
        out['unc'].flat[i] = erg['result'].retrieval_error_covariance[0,0]
        out['cst'].flat[i] = erg['result'].cost
        out['nit'].flat[i] = erg['result'].number_of_iterations

    out['unc'] = np.sqrt(out['unc'])
    out['prior_tcwv'] = data['tcwv']+0
    # 7. write to ncdf
    write_to_ncdf(tcwv_name,out)


 
if __name__ == '__main__': 
    doit()
    pass
