import os,sys
from pathlib import Path
import numpy as np
from netCDF4 import Dataset 
import xarray as xr
#from matplotlib import pyplot as pl

try:
    import sentinel_data as sd
except ModuleNotFoundError:
    from . import cowa_io as sd

try:
    import lut2oe
except ModuleNotFoundError:
    from . import lut2oe

DEBUG = False
SCRIPT_PATH = Path(__file__).resolve().parent
DESMILE_LUT = {19:SCRIPT_PATH/'./trans19_smile_corr.nc4',
               20:SCRIPT_PATH/'./trans20_smile_corr.nc4'}

SPEC_CHAR_LUT = {'A': SCRIPT_PATH/'S3A_OL_MPC_CHAR_spectral_characterisation_and_evolution.01112021.nc4',
                 'B': SCRIPT_PATH/'S3B_OL_MPC_CHAR_spectral_characterisation_and_evolution.01112021.nc4'} 

# I colud also use desmile_luts to mean A and B characteristics
# instead of nominal ...
# I HAVE TO BE CONSISTENT WITH COWA CALIBRATION
# the OLCI B calibrated file are made for OLC-B means!!!!!

DESMILE_LUT_AB = {'A':{19:SCRIPT_PATH/'./trans19_smile_corr_A.nc4',
                       20:SCRIPT_PATH/'./trans20_smile_corr_A.nc4'},
                  'B':{19:SCRIPT_PATH/'./trans19_smile_corr_B.nc4',
                       20:SCRIPT_PATH/'./trans20_smile_corr_B.nc4'},
                  'N':{19:SCRIPT_PATH/'./trans19_smile_corr.nc4',
                       20:SCRIPT_PATH/'./trans20_smile_corr.nc4'}}
NOMI = {o: {b: xr.load_dataset(DESMILE_LUT_AB[o][b]).attrs for b in (19,20)} for o in ('A','B')}
NOMI['N'] = {19: {'nomi_cwl_19': 900., 'nomi_bwd_19': 10.}, 20: {'nomi_cwl_20': 940., 'nomi_bwd_20': 20.}} 



def gen_trans2tcwv(bn=19,olci='A'):
    ds = xr.load_dataset(DESMILE_LUT_AB[olci][bn])
    #print(ds.attrs)
    #sys.exit()
    axes = (np.array(ds.amf),np.array(ds.cwl),np.array(ds.bwd),)
    axes_names = ('amf','cwl','bwd','three')
    coeffT = np.array(ds['coeff'].transpose(*axes_names))
    #print(axes)
    fnc_coef = lut2oe.lut2func(coeffT, axes=axes,axes_names=axes_names[0:3],parallel=True)
    def trans2tcwv(tra, amf, cwl, bwd):
        # assume right shape ...
        wo = np.zeros((len(amf),3))
        wo[:,0] = amf
        wo[:,1] = cwl
        wo[:,2] = bwd
        c = fnc_coef(wo,jaco=False)
        lra = np.zeros_like(tra)+np.nan
        idx = tra > 0.0001
        lra[idx] = np.log(tra[idx])
        tcw = (c[:,0]*lra**2 + c[:,1]*lra +c[:,2])**2
        return tcw
    return trans2tcwv

def gen_tcwv2corr(bn=19,olci='A'):
    ds = xr.load_dataset(DESMILE_LUT_AB[olci][bn])
    fnc_corr = lut2oe.xarray2func(ds,arrayname='corr',nd=False,parallel=True)
    #print(fnc_corr.__doc__)
    def tcwv2corr(tcw, amf, cwl, bwd):
        wo = np.zeros((len(amf),4))
        wo[:,0] = tcw
        wo[:,1] = amf
        wo[:,2] = cwl
        wo[:,3] = bwd
        return fnc_corr(wo,jaco=False)
    return tcwv2corr

def gen_tra2corr(bn=19,olci='A'):
    trans2tcwv = gen_trans2tcwv(bn,olci)
    tcwv2corr = gen_tcwv2corr(bn,olci)
    def trans2corr(tra, amf, cwl, bwd):
        tcw = trans2tcwv(tra, amf, cwl, bwd)
        cor = tcwv2corr(tcw, amf, cwl, bwd)
        return cor,tcw
    return trans2corr

TRA2CORR = {o: {b:gen_tra2corr(b) for b in (19,20)} for o in ('A','B')}

def read_all_data(oll1,stride = (4,4)):
    xfu1 = sd.get_manifest(oll1)
    geo = {k:v.astype(np.float32) for k,v in  sd.get_geometry(oll1,stride).items()}
    sfl = {k:sd.get_solar_flux(oll1,k,stride).astype(np.float32) 
           for k in [17,18,19,20,21]}
    rad = {k:(sd.get_band(oll1,k,stride)/sfl[k]).astype(np.float32) 
           for k in [17,18,19,20,21]}
    dix = sd.get_detector_index(oll1,stride).filled()
    cam = din // 740 +1
    amf = 1./np.cos(geo['SZA']*np.pi/180.)+1./np.cos(geo['OZA']*np.pi/180.)
    orb = int(sd.get_orbitNumber(xfu1))
    doy = int(sd.get_doy(xfu1))
    lon = sd.get_longitude(oll1,stride).filled()
    lat = sd.get_latitude(oll1,stride).filled()    
    mask = rad[18].mask
    for xx in rad:
        mask = mask | rad[xx].mask
    pfm = sd.get_platform(xfu1)
    if pfm == '2016-011A':
        olc = 'A'
    elif pfm == '2018-039A':
        olc = 'B'
    else:
        #dirty
        olc = Path(oll1).name[2]
    #auch nicht schoen
    rad = {b:rad[b].filled() for b in rad}
    sfl = {b:sfl[b].filled() for b in sfl}
    geo = {w:geo[w].filled() for w in geo}
    return {'lon': lon, 'lat': lat, 'geo': geo, 'amf': amf.astype(np.float32),
            'rad': rad, 'sfl': sfl,
            'msk': mask,'nnn': mask.size,
            'dix': dix.astype(np.int16), 'cam': cam.astype(np.int8),
            'orb': orb, 'doy': doy, 'pfm': pfm, 'olci': olc}

def get_spectral_charact(orbit, cwl_db):
    '''
    Based on temporal spectral OLCI model
    orbit: orbit number
    cwl_db:  netcdf file with temporal data 
    '''
    lo = np.log(orbit)
    with Dataset(cwl_db) as ds:
        cwvl_coef = ds['/cwvl_coef'][:]+0
        fwhm_coef = ds['/fwhm_coef'][:]+0
    cwvl = np.array([coef*lo**i for i,coef in enumerate(cwvl_coef)]).sum(axis=0)    
    fwhm = np.array([coef*lo**i for i,coef in enumerate(fwhm_coef)]).sum(axis=0)    
    cwvl = np.transpose(cwvl,(1,0,2))[...,::-1].reshape((21,-1))+0.5
    fwhm = np.transpose(fwhm,(1,0,2))[...,::-1].reshape((21,-1))
    return cwvl,fwhm

def prepare_homog(data,cwvl,fwhm):
    out={}
    for b in [17,18,19,20,21]:
        l0 = cwvl[b-1].squeeze()+0
        f0 = fwhm[b-1].squeeze()+0
        out['%i_cwl'%b] = l0[data['dix']]
        out['%i_fwhm'%b] = f0[data['dix']]
    dlam = out['18_cwl']-out['17_cwl']
    drad = data['rad'][18]-data['rad'][17]
    grad = np.zeros_like(drad)+np.nan
    indx = dlam > 0.0001
    grad[indx] = drad[indx]/dlam[indx]
    #out['grad'] = grad
    for b in (19,20):
        out['%i_r_abs_free'%b] = np.where(np.isfinite(grad), data['rad'][18] + grad*(out['%i_cwl'%b]-out['18_cwl']), np.nan)
    for b in (19,20):
        with np.errstate(invalid='ignore', divide='ignore'):
            out['%i_trans'%b] = data['rad'][b]/out['%i_r_abs_free'%b]
    return out

def perform_correction(data):
    out = {}
    amf = data['amf'].flat[:].copy()
    for b in (19,20):
        tra = data['%i_trans'%b].flat[:].copy()
        cwl = data['%i_cwl'%b].flat[:].copy()
        bwd = data['%i_fwhm'%b].flat[:].copy()
        cor, tcw = TRA2CORR[data['olci']][b](tra,amf,cwl,bwd)
        out['%i_corr'%b],out['%i_tcwv'%b] = cor.astype(np.float32), tcw.astype(np.float32)
    for k in out:
        out[k].shape=data['amf'].shape        
    for b in (19,20):
        out['trans_corr_%i'%b] = data['%i_trans'%b]*out['%i_corr'%b]
    return out

    
def doit(oll1, stride=(1,1)):
    '''
    if corfilename is none result is integrated into oll1
    '''
    data = read_all_data(oll1,stride=stride)
    cwvl,fwhm = get_spectral_charact(data['orb'], SPEC_CHAR_LUT[data['olci']])
    data.update(prepare_homog(data,cwvl,fwhm))
    data.update(perform_correction(data))
    return data

def write_ncdf(fn,data):
    with Dataset(fn,'w') as ds:
        ds.createDimension('rows',data['amf'].shape[0])
        ds.createDimension('columns',data['amf'].shape[1])
        if DEBUG:
            for v in data:
                print(v,type(data[v]))
                if v in ('orb', 'doy', 'pfm', 'olci','nnn'):
                    continue
                elif v in ('geo','rad','sfl'):
                    for vv in data[v]:
                        svv ='%s_%s'%(v,str(vv)) 
                        print(vv,type(data[v][vv]))
                        ds.createVariable(svv,data[v][vv].dtype,('rows','columns'),zlib=True)
                        ds[svv][:] = data[v][vv]
                        print(ds[svv].dtype)
                elif v in ('msk',):
                    ds.createVariable(str(v),'u1',('rows','columns'),zlib=True)
                    ds[str(v)][:] = data[v]
                    print(ds[str(v)].dtype)
                else:
                    ds.createVariable(str(v),data[v].dtype,('rows','columns'),zlib=True)
                    ds[str(v)][:] = data[v]
                    print(ds[str(v)].dtype)                
        else:
            for v in ('trans_corr_19', 'trans_corr_20'):
                ds.createVariable(v,data[v].dtype,('rows','columns'),zlib=True)
                ds[v][:] = data[v]
                #print(ds[v].dtype)
        for b in (19,20):
            for k in NOMI[data['olci']][b]:
                ds.setncattr(k,NOMI[data['olci']][b][k])


def test():
    print('give input file name')

if __name__ == '__main__':
    if len(sys.argv) == 1:
        test()
    elif len(sys.argv) == 2: 
        corf = Path(sys.argv[1])/'homogenized_h2o_bands.nc'
        write_ncdf(corf,doit(sys.argv[1]))#,stride=(8,8)))
    elif len(sys.argv) == 3: 
        write_ncdf(sys.argv[2],doit(sys.argv[1]))
    else:
        test()
    
    
