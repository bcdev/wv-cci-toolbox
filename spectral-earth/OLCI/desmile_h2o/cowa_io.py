import sys, os
from pathlib import Path
from lxml import etree as ET
from netCDF4 import Dataset
from netCDF4 import stringtochar
import numpy as np
import scipy.interpolate as interp
from scipy import signal 
from pathlib import Path
import datetime
import calendar
import time
import argparse

NOW = time.strftime('%Y%m%dT%H%M%S',time.gmtime())
AOT_LOG_SCALE_OFFSET = 0.01

def cowa_parser():

    cp = argparse.ArgumentParser(description='COWA processor for OLCI')

    cp.add_argument('-l1','--l1_file', type=str, action="store", dest="l1", required=True,
                        help='Mandatory: Sentinel3 Level 1 file path name (ending with .SEN3)')

    cp.add_argument('-l2','--l2_file', type=str, action="store", dest="l2",
                        help='Sentinel3 Level 2 file path name (*.SEN3). Not needed if idepix is used.')

    cp.add_argument('-idp','--idepix_file', type=str, action="store", dest="idp",
                        help='Name of the idepix file. Default is /spam/egs/S3?_OL_1.....SEN3/Idepix.nc')

    cp.add_argument('-ini','--ini_file', type=str, action="store", dest="ini", default=None, 
                        help='Name of the ini file. Default is none. Calling routine must take care.')

    cp.add_argument('-t','--target_file', type=str, action="store", dest="result",
                        help='Will be a netCDF4 and should have an according suffix. If not given, name and path are constructed from l1')

    cp.add_argument('-cmi','--cloudmask_idepix', action="store_true", dest="cmi", default=False,
                        help='Use idepix cloudmask, default is to use cloudmask from level2.')

    cp.add_argument('-s','--stride', type=str, action="store", dest="stride", default=None,
                        help='Use nx,ny as stride instead of stride from ini')

    cp.add_argument('-p','--progress_bar', action="store_true", dest="progress", default=False,
                        help='Show a progress bar (unhandy for logfiles)')

    cp.add_argument('-icm','--ignore_cloudmask', action="store_true", dest="ignore_cloudmask", default=False,
                        help='Ignores the cloudmask. All pixel are processed.')

    cp.add_argument('-ims','--ignore_maxsolar', action="store_true", dest="ignore_maxsolar", default=False,
                        help='Ignores the maximum solar zenith. All pixel are processed.')

    cp.add_argument('-ail','--all_is_land', action="store_true", dest="all_is_land", default=False,
                        help='Treats every pixel as if it were land (ignores ocean).')

    cp.add_argument('-dsm','--desmile_h2o_bands', action="store_true", dest="desmile_h2o_bands", default=False,
                        help='Desmiles bands 19 and 20 before starting cowa (currently implemented for CCI only)')

    return cp.parse_args()


def check_if_files_exists(*arg):
    for ff in arg:
        if ff is None:
            continue
        if not os.path.exists(ff):
            print(ff+' not found, exiting!','red')
            sys.exit(-2)
            
def l1_to_result(l1_name, stride, add_l1_folder=True, now=NOW):
    seg = Path(l1_name).name.split('_')
    seg[2] = '2'
    seg[3] = 'COWA'+seg[3][-2:]
    seg[9] = now
    seg[-4] = 'RP1'
    seg[-1] = seg[-1].split('.')[0]
    if (stride[0],stride[1]) == (1,1):
        seg[-1] += '.nc4'
    else:
        seg[-1] += '_%ix%i.nc4'%(stride[0],stride[1])
    res = '_'.join(seg)
    if add_l1_folder:
        res = str(Path(l1_name).parent / res)
    return res

            
def cosd(inn):
    return np.cos(inn*np.pi/180.)
def sind(inn):
    return np.sin(inn*np.pi/180.)
def acosd(inn):
    return np.arccos(inn)*180./np.pi
def asind(inn):
    return np.arcsin(inn)*180./np.pi

def masked2filled(m,fv=np.nan):
    try:
        return m.filled(fv)
    except AttributeError:
        return m

def azi2azid(sa,va):
    return acosd(cosd(sa)*cosd(va)+sind(sa)*sind(va))

#def height2press(hh):
    #'''
    #This is very simple, but actually sufficient 
    #'''
    #return 1013.*(1.-(hh*0.0065/288.15))**5.2555

def height2press(hh,p0=1013.25):
        return p0*(1.-(hh*0.0065/288.15))**5.2555
    
def lon2lon180(lon):
    '''
    make longitudes consistent 
    between -180,180
    '''
    re=sind(lon)
    im=cosd(lon)
    return np.arctan2(re,im)*180./np.pi



def read_manifest(s3):
    '''
    returns text
    '''
    xfx = os.path.join(s3,'xfdumanifest.xml')
    if xfx is None:
        return None
    with open(xfx) as fp:
        txt=fp.read()
    return txt
    

def get_manifest(s3):
    '''
    returns an xml object
    '''
    xfx = os.path.join(s3,'xfdumanifest.xml')
    if xfx is None:
        return None

    with open(xfx) as fp:
        xfu=ET.parse(fp)
    return xfu.getroot()

def get_startTime(ff,tupel=False):
    xfu = get_manifest(ff)
    inf= xfu.find('.//sentinel-safe:startTime',xfu.nsmap).text.split(' ')
    if tupel is True:
        return time.strptime(inf[0],'%Y-%m-%dT%H:%M:%S.%fZ')
    else:
        return inf
def get_stopTime(ff,tupel=False):
    xfu = get_manifest(ff)
    inf= xfu.find('.//sentinel-safe:stopTime',xfu.nsmap).text.split(' ')
    if tupel is True:
        return time.strptime(inf[0],'%Y-%m-%dT%H:%M:%S.%fZ')
    else:
        return inf
def get_stopTimestamp(ff):
    return calendar.timegm(get_stopTime(ff,tupel=True))

def get_startTimestamp(ff):
    return calendar.timegm(get_startTime(ff,tupel=True))

def get_dimension(ff):
    xfu = get_manifest(ff)
    return (int(xfu.find('.//sentinel3:rows',xfu.nsmap).text) ,
            int(xfu.find('.//sentinel3:columns',xfu.nsmap).text))

def get_orbitNumber(ff):
    xfu = get_manifest(ff)
    inf= xfu.find('.//sentinel-safe:orbitNumber',xfu.nsmap).text
    return int(inf)
    
def get_olci(ff):
    xfu = get_manifest(ff)
    pfm = xfu.find('.//sentinel-safe:nssdcIdentifier',xfu.nsmap).text
    if pfm == '2016-011A':
        olc = 'A'
    elif pfm == '2018-039A':
        olc = 'B'
    else:
        #dirty
        olc = Path(oll1).name[2]
    return olc


def get_envelop(ff):
    xfu = get_manifest(ff)
    lst= xfu.find('.//gml:posList',xfu.nsmap).text.split(' ')
    lat=np.array([float(lst[2*i])   for i in range(len(lst)//2)])
    lon=np.array([float(lst[2*i+1]) for i in range(len(lst)//2)])
    return lon,lat

def get_altitude(ff,st=(1,1)):
    with Dataset('%s/geo_coordinates.nc'%ff) as ds:
        alt=ds.variables['altitude'][::st[0],::st[1]]*1.
    return masked2filled(alt)

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
    return masked2filled(ou)

def interpolate_tie_(inn,al,ac, profile=False):
    npa=np.arange
    npl=np.linspace
    sh_in=inn.shape
    if profile is True:
        sh_ou=((sh_in[0]-1)*al+1,(sh_in[1]-1)*ac+1,sh_in[2])
    else:
        sh_ou=((sh_in[0]-1)*al+1,(sh_in[1]-1)*ac+1)

    xv = npl(0.,sh_in[0]-1,sh_ou[0])
    yv = npl(0.,sh_in[1]-1,sh_ou[1])
    xflt, yflt = np.meshgrid(xv, yv, sparse=False, indexing='ij',copy=True)
    
    xlid = np.floor(xflt).astype(np.int32)  # x lower index
    ylid = np.floor(yflt).astype(np.int32)  # y lower index

    xuid = (xlid +1).clip(0,sh_in[0]-1) # x upper index
    yuid = (ylid +1).clip(0,sh_in[1]-1) # y upper index

    wx = xuid-xflt
    wy = yuid-yflt
    w00 = (wx)*(wy)      # xlo,ylo
    w10 = (1-wx)*(wy)    # xup,ylo
    w01 = (wx)*(1-wy)    # xlo,yup
    w11 = (1-wx)*(1-wy)  # xlo,yup

    if profile is True:
        out =  inn[((xlid),(ylid))]*w00[:,:,np.newaxis]
        out += inn[((xuid),(ylid))]*w10[:,:,np.newaxis]
        out += inn[((xlid),(yuid))]*w01[:,:,np.newaxis]
        out += inn[((xuid),(yuid))]*w11[:,:,np.newaxis]
    else:
        out =  inn[((xlid),(ylid))]*w00
        out += inn[((xuid),(ylid))]*w10
        out += inn[((xlid),(yuid))]*w01
        out += inn[((xuid),(yuid))]*w11
    return masked2filled(out)

def interpolate_tie_profile(inn,al,ac):
    return interpolate_tie_(inn,al,ac,profile=True)

def get_pressure(ff,st=(1,1)):
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        lat=ds.variables['sea_level_pressure'][:]*1.
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(lat,al,ac)[::st[0],::st[1]])

def get_temperature(ff,st=(1,1)):
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        tmp=ds.variables['atmospheric_temperature_profile'][:,:,0].squeeze()*1.
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(tmp,al,ac)[::st[0],::st[1]])

def get_wind(ff,st=(1,1)):
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        wsp=(ds.variables['horizontal_wind'][:,:,:]**2).sum(axis=2)**0.5
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(wsp,al,ac)[::st[0],::st[1]])

def get_tcw(ff,st=(1,1)):
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        tcw=ds.variables['total_columnar_water_vapour'][:]*1
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(tcw,al,ac)[::st[0],::st[1]])

def get_lsmask(ff,st=(1,1)):
    bit = flagname_to_flagbit(ff, datatype='l1')['land']
    with Dataset('%s/qualityFlags.nc'%ff) as ds:
        lsm=(ds.variables['quality_flags'][:,:] & 2**bit )== 2**bit
    return masked2filled(lsm[::st[0],::st[1]],False)

def get_band_saturation(ff,bn,st=(1,1)):
    bit = flagname_to_flagbit(ff, datatype='l1')['saturated@Oa%02i'%bn]
    with Dataset('%s/qualityFlags.nc'%ff) as ds:
        lsm=(ds.variables['quality_flags'][:,:] & 2**bit )== 2**bit
    return masked2filled(lsm[::st[0],::st[1]],True)


def get_latitude(ff,st=(1,1)):
    ## assumes FR dataset
    with Dataset('%s/tie_geo_coordinates.nc'%ff) as ds:
        lat=ds.variables['latitude'][:]*1.
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(lat,al,ac)[::st[0],::st[1]])

def get_longitude(ff,st=(1,1)):
    ## assumes FR dataset
    with Dataset('%s/tie_geo_coordinates.nc'%ff) as ds:
        lon=ds.variables['longitude'][:]*1.
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    re=np.sin(lon*np.pi/180.)
    im=np.cos(lon*np.pi/180.)
    re_out=interpolate_tie(re,al,ac)[::st[0],::st[1]]
    im_out=interpolate_tie(im,al,ac)[::st[0],::st[1]]
    return masked2filled(np.arctan2(re_out,im_out)*180./np.pi)

def get_geometry(ff,st=(1,1)):
    out={}
    with Dataset('%s/tie_geometries.nc'%ff) as ds:
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
        out={k: ds.variables[k][:]*1. for k in ds.variables}
    for k in out:
        out[k]=masked2filled(interpolate_tie(out[k],al,ac)[::st[0],::st[1]])
    out['ADA']=azi2azid(out['SAA'],out['OAA'])    
    return out

def get_detector_index(ff,st=(1,1)):
    with Dataset('%s/instrument_data.nc'%ff) as ds:
        di=ds.variables['detector_index'][:]*1
    return di[::st[0],::st[1]]

def get_cwl(ff,bn,st=(1,1)):
    with Dataset('%s/instrument_data.nc'%ff) as ds:
        l0=(ds.variables['lambda0'][bn-1,:]*1.).squeeze()
        di=ds.variables['detector_index'][:]*1
    return masked2filled(l0[di][::st[0],::st[1]])

def get_solar_flux(ff,bn,st=(1,1)):
    with Dataset('%s/instrument_data.nc'%ff) as ds:
        sf=(ds.variables['solar_flux'][bn-1,:]*1.).squeeze()
        di=ds.variables['detector_index'][:]*1
    return masked2filled(sf[di][::st[0],::st[1]])

def get_band(ff,bn,st=(1,1)):
    variable='Oa%02i_radiance'%bn
    with Dataset('%s/%s.nc'%(ff,variable)) as ds:
        out=ds.variables[variable][:]*1.    
    return masked2filled(out[::st[0],::st[1]])

def simple_coast(lm, b=5):
    '''
    shift in 8 directions ...
    '''
    kk = np.ones((2*b+1,2*b+1))
    cs = signal.convolve2d(lm+0,kk+0)
    out = ((cs[b:-b,b:-b]>0) & (~lm))
    return out


def get_l2_ocean_cloudmask(ff,st=(1,1)):
    flagbits = flagname_to_flagbit(ff, datatype='ocean')
    variable='WQSF'
    fname='wqsf'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CLOUD_MARGIN')
    return out

def get_l2_aot(ff,st=(1,1)):
    variable='T865'
    fname='w_aer'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        out=(ds.variables[variable][:,:])*1.
    return out[::st[0],::st[1]]

def test_bit(f,b):
    return (f & 2**b ) == 2**b

def get_l2_ocean_aot_fail(ff,st=(1,1)):
    flagbits = flagname_to_flagbit(ff, datatype='ocean')
    variable='WQSF'
    fname='wqsf'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        inv = tb('INVALID')
        lnd = tb('LAND')
        gln = tb('MEGLINT') | tb('HIGHGLINT')
        cla = tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CLOUD_MARGIN') 
        sat = tb('SATURATED')
        snw = tb('SNOW_ICE')
        wht = tb('WHITECAPS')
        spt = tb('SUSPECT') | tb('HISOLZEN')
        acf = tb('AC_FAIL')  # ac fail

    inv = inv | lnd | gln | cla | sat | snw | wht | spt | acf
    return inv

def get_l2_ocean_cloudmask(ff,st=(1,1)):
    flagbits = flagname_to_flagbit(ff, datatype='ocean')
    variable='WQSF'
    fname='wqsf'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CLOUD_MARGIN')
    return out

def get_l2_land_cloudmask(ff,st=(1,1)):
    flagbits = flagname_to_flagbit(ff, datatype='land')
    variable='LQSF'
    fname='lqsf'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]  
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CLOUD_MARGIN')
    return out

def get_idepix_cloudmask(ff,st=(1,1)):
    flagbits = flagname_to_flagbit(ff, datatype='idepix')
    variable='pixel_classif_flags'
    with Dataset(ff) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('IDEPIX_CLOUD') | tb('IDEPIX_CLOUD_AMBIGUOUS')| tb('IDEPIX_CLOUD_BUFFER')| tb('IDEPIX_CLOUD_SURE')
    return out


def flagname_to_flagbit(ff, datatype='ocean'):
    if datatype == 'ocean':
        variable='WQSF'
        fname='%s/%s.nc'%(ff,'wqsf')
    elif datatype == 'land':
        variable='LQSF'
        fname='%s/%s.nc'%(ff,'lqsf')
    elif datatype == 'l1':
        variable='quality_flags'
        fname='%s/%s.nc'%(ff,'qualityFlags')
    elif datatype == 'idepix':
        variable='pixel_classif_flags'
        fname=ff
    else:
        return None
    with Dataset(fname) as ds:
        meanings = ds.variables[variable].flag_meanings.split()
        masks = ds.variables[variable].flag_masks
    flgs = {n: int(b).bit_length()-1 for n,b in  zip(meanings,masks)}
    return flgs

def get_surf_temperature(s3,st=(1,1)):
    tmp = get_temperature_profile(s3,st)
    plv = get_pressure_profile(s3,st)
    alt = get_altitude(s3,st)
    slp = get_pressure(s3,st)
    prs = height2press(alt.clip(0),slp)
    stm = np.zeros_like(alt,dtype=np.float32)
    if plv[0] < plv[1]: # Merislike
        pass
    else:
        plv=plv[...,::-1]
        tmp=tmp[...,::-1]
    uindx = np.searchsorted(plv,prs)
    uwght = np.zeros(uindx.shape,dtype=np.float32)
    lwght = np.zeros(uindx.shape,dtype=np.float32)    
    for i in range(1,len(plv)):
        xdx = uindx==i
        lwght[xdx] = (prs[xdx]-plv[i])/(plv[i-1]-plv[i])
        uwght[xdx] = 1. - lwght[xdx]
        stm[xdx] = (tmp[:,:,i][xdx]*uwght[xdx] + tmp[:,:,i-1][xdx]*lwght[xdx])
    xdx = uindx==len(plv)
    stm[xdx] =tmp[:,:,-1][xdx]
    stm[prs<0] = np.nan
    stm[prs>1050] = np.nan
    stm[~np.isfinite(prs)] = np.nan
    try:
        stm[prs.mask] = np.nan
    except AttributeError:
        pass 
    return stm

def get_temperature_profile(ff,st=(1,1)):
    variable='atmospheric_temperature_profile'
    fname='tie_meteo'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        tie = (ds.variables[variable][:,:,:])*1. 
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    out = interpolate_tie_profile(tie,al,ac) 
    return out[::st[0],::st[1],:]

def get_pressure_profile(ff,st=(1,1)):
    variable='reference_pressure_level'
    fname='tie_meteo'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        out = (ds.variables[variable][:])*1. 
    return out




def test_agreement_and_exit_if_not(oll1, oll2):
    
    same_start = (get_startTimestamp(oll1) == get_startTimestamp(oll2))
    same_end = (get_stopTimestamp(oll1) == get_stopTimestamp(oll2))
    if not ( same_start and same_end):
        print('times of L1 and L2 do not fit, exiting')
        print('Starts:', get_startTimestamp(oll1),  get_startTimestamp(oll2), same_start ) 
        print('Stops:', get_stopTimestamp(oll1),  get_stopTimestamp(oll2), same_end ) 
        sys.exit()

    l1_shape = get_dimension(oll1)
    l2_shape = get_dimension(oll2)
    same_shape = (l2_shape == l1_shape) 
    if not same_shape:
        print('shapes of L1 and L2 do not fit, exiting')
        print('l1:', l1_shape)
        print('l2:', l2_shape)
        sys.exit()

    l1_orbit = get_orbitNumber(oll1)
    l2_orbit = get_orbitNumber(oll2)
    if not (l1_orbit == l2_orbit):
        print('orbits of L1 and L2 do not fit, exiting')
        print('l1:', l1_orbit)
        print('l2:', l2_orbit)
        sys.exit()

    l1_lon, l1_lat = get_envelop(oll1)
    l2_lon, l2_lat = get_envelop(oll2)
    lon_ok = (l1_lon.size == l2_lon.size) and ( np.sum(np.abs(l1_lon - l2_lon) < 0.0001) == l1_lon.size)
    lat_ok = (l1_lat.size == l2_lat.size) and ( np.sum(np.abs(l1_lat - l2_lat) < 0.0001) == l1_lat.size)
    if not (lon_ok or lat_ok):
        print('Polygon envelops of L1 and L2 do not fit, exiting')
        sys.exit()



def get_relevant_l1l2_data(oll1, oll2, idp, config, cmi=False, 
                           icm=False, ims=False, ail=False): 
    '''
    '''
    stride = config['PROCESSING']['stride']
    #l2_lon = get_longitude(oll2,stride)
    #l2_lat = get_latitude(oll2,stride)
    l1_lon = get_longitude(oll1,stride)
    l1_lat = get_latitude(oll1,stride)

    rad = {k:get_band(oll1,k,stride)/get_solar_flux(oll1,k,stride) for k in [17,18,19,20,21,]}
    dix = get_detector_index(oll1,stride)
    cwl = {k:get_cwl(oll1,k,stride) for k in [17,18,19,20,21,]}
    try:
        sat = {k:get_band_saturation(oll1,k,stride) for k in range(1,22)}
    except KeyError:
        sat = {k:np.zeros_like(rad[k],dtype=np.bool_) for k in rad}
   
    geo = get_geometry(oll1,stride)
    lon = get_longitude(oll1,stride)
    lat = get_latitude(oll1,stride)
    prs = height2press(get_altitude(oll1,stride)) # TODO adjust sea level pressure
    lsm = get_lsmask(oll1,stride)
    env = get_envelop(oll1)
    
    cb = config['GENERAL']['coast_border']//((stride[0]+stride[1])//2)
    cb = max(1 , cb)
    print('Coast Boarder in stride units:',cb)
    sct = simple_coast(lsm, cb)
    #todo switch tem. Be onsistent with cowa slow!!!
    tem = get_temperature(oll1,stride)
    tem_ = get_surf_temperature(oll1,stride)
    wsp = get_wind(oll1,stride)
    tcw = get_tcw(oll1,stride)
    if cmi:
        cld = get_idepix_cloudmask(idp,stride)
        aot_l2 = np.zeros_like(tcw) + config['PROCESSING']['ocean_aot_fallback']
    else:
        land_or_ocean = Path(oll2).name.split('_')[3][0]
        if land_or_ocean == 'L':
            cld = get_l2_land_cloudmask(oll2,stride)
            aot_l2 = np.zeros_like(tcw) + config['PROCESSING']['ocean_aot_fallback']
        elif land_or_ocean == 'W':
            cld = get_l2_ocean_cloudmask(oll2,stride)
            aot_l2 = np.log10(AOT_LOG_SCALE_OFFSET + get_l2_aot(oll2,stride).filled(np.nan))
            aot_l2 = np.where(~ get_l2_ocean_aot_fail(oll2,stride), aot_l2, config['PROCESSING']['ocean_aot_fallback'])
        else:
            print('Unknown L2 '+oll2,'red')
            sys.exit(2)

    amf = 1./np.cos(geo['SZA']*np.pi/180.)+1./np.cos(geo['OZA']*np.pi/180.)
    
    # radiance ok
    mask = np.ones_like(lsm)        
    for xx in rad:
        mask = mask & np.isfinite(rad[xx])
    for xx in sat:
        mask = mask & ~sat[xx]
    mask = mask & np.isfinite(tcw)
    mask = mask & (tcw>0)


    #data ok for processing
    dok = np.ones_like(mask)
    dok = dok & mask
    if not icm:
        dok = dok & ~cld.filled(True)
    if not ims:
        dok = dok & (geo['SZA'] <= config['PROCESSING']['max_solar_zenith'])
    for b in rad:
        dok = dok & (rad[b] >= config['PROCESSING']['min_norm_rad'])


    if not ail:
        # data for land processing
        dfl = dok & (lsm  |  (sct & (rad[21] > config['PROCESSING']['min_coast_norm_rad'])))
        # data for ocean processing
        dfo = dok & ~dfl
    else:
        dfl = dok & True
        dfo = dok & ~dfl

    orbit = get_orbitNumber(oll1)
    olc = get_olci(oll1)
    
    #eventally fill the land processing with aot land background
    aot_l2[dfl] = config['PROCESSING']['land_aot_fallback']
    
    #
    l1_manifest = read_manifest(oll1)
    if cmi:
        l2_manifest = None
    else:
        l2_manifest = read_manifest(oll2)

    return {'lon': l1_lon, 'lat': l1_lat, 'geo': geo, 'amf': amf,
            'rad': rad, 'cwl': cwl, 'sat': sat,'dix':dix.astype(np.int16), 
            'lsm': lsm, 'sct': sct, 'cld': cld,
            'prs': prs, 'wsp': wsp, 'tem': tem, 'tem_': tem_, 'tcw': tcw, 'aot': aot_l2,
            'msk': mask, 'dok': dok, 'nnn': dok.size,'dfl': dfl, 'dfo': dfo,
            'l1_manifest': l1_manifest, 'l2_manifest': l2_manifest, 'envelop': env,
            'orb': orbit, 'olci': olc}


def write_to_ncdf(outname, dct, gatr):
    yy = dct['lat'].shape[0]   
    xx = dct['lat'].shape[1]
    en = dct['envelop'][0].shape[0]
    for key in dct:
        if 'manifest' in key:
            continue
        if 'envelop' in key:
            continue
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
        nc.filename = Path(outname).name
        nc.history = datetime.datetime.now(datetime.timezone.utc).strftime('created on %Y-%m-%d %H:%M:%S UTC')
        nc.Conventions = "CF-1.4"
        nc.metadata_version = "0.5"
        nc.createDimension('y',yy)
        nc.createDimension('x',xx)
        nc.createDimension('envelop',en)

        for k in gatr:
            nc.__setattr__(k,gatr[k])
        for key in dct:
            if key == 'envelop':
                nc_dum=nc.createVariable('env_lon',dct[key][0].dtype,('envelop',),zlib=True)
                nc_dum[:]=dct[key][0]
                nc_dum=nc.createVariable('env_lat',dct[key][1].dtype,('envelop',),zlib=True)
                nc_dum[:]=dct[key][1]
            else:
                nc_dum=nc.createVariable(key,dct[key].dtype,('y','x'),zlib=True)
                nc_dum[:]=dct[key]
                nc_dum.__setattr__('coordinates','lat lon')
                if 'tcwv' in key:
                    kkk = key.replace('tcwv','total_columnar_water_vapour')
                    nc_dum.__setattr__('units','kg.m-2')
                    nc_dum.__setattr__('long_name',kkk)

def add_txt_to_netcdf(nc4f,name,txt):
    with Dataset(nc4f,'r+') as nc4:
        dimname = 'nchar_%s'%name
        nc4.createDimension(dimname,len(txt))
        nc4.createVariable(name,'S1',(dimname,),zlib=True)
        nc4[name][:] = stringtochar(np.array([txt],'S'))
        nc4[name].decode_with = 'ds["%s"][...].tobytes().decode()'%name


def add_idepix_to_netcdf(idpf,nc4f,stride):
    to_copy = ('pixel_classif_flags','quality_flags')
    with Dataset(idpf) as idp, Dataset(nc4f,'r+') as nc4:
        for atr in idp.ncattrs():
            nc4.__setattr__(atr,idp.getncattr(atr))
        #for d in idp.dimensions:
            #nc4.createDimension(d,len(idp.dimensions[d]))
        for v in to_copy:
            nc4.createVariable(v,idp[v].dtype,('y','x'),zlib=True)
            for atr in idp[v].ncattrs() :
                nc4[v].__setattr__(atr,idp[v].getncattr(atr))
            nc4[v][:]=idp[v][::stride[0],::stride[1]]
    return



