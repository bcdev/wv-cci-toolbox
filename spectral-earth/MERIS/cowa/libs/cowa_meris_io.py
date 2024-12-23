import sys, os
from pathlib import Path
from lxml import etree as ET
from netCDF4 import Dataset
import numpy as np
import scipy.interpolate as interp
from scipy import signal 
from pathlib import Path
import datetime
import calendar
import time


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

def masked2filled(m,fv=np.nan):
    try:
        return m.filled(fv)
    except AttributeError:
        return m

def azi2azid(sa,va):
    return acosd(cosd(sa)*cosd(va)+sind(sa)*sind(va))

def lon2lon180(lon):
    '''
    make longitudes consistent 
    between -180,180
    '''
    re=sind(lon)
    im=cosd(lon)
    return np.arctan2(re,im)*180./np.pi


def height2press(hh,p0=1013.25):
        return p0*(1.-(hh*0.0065/288.15))**5.2555


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
    # file format is buggy:
    with Dataset('%s/geo_coordinates.nc'%ff) as ds:
        rows,columns = ds['latitude'].shape
    return rows,columns 
    
    #xfu = get_manifest(ff)
    #return (int(xfu.find('.//sentinel3:rows',xfu.nsmap).text) ,
            #int(xfu.find('.//sentinel3:columns',xfu.nsmap).text))

def get_orbitNumber(ff):
    xfu = get_manifest(ff)
    inf= xfu.find('.//sentinel-safe:orbitNumber',xfu.nsmap).text
    return int(inf)

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

def interpolate_tie__(inn,al,ac):
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

def interpolate_tie_(inn,al,ac):
    npa=np.arange
    npl=np.linspace
    sh_in=inn.shape
    sh_ou=((sh_in[0]-1)*al+1,(sh_in[1]-1)*ac+1)
    itp=interp.RectBivariateSpline(npl(0.,sh_ou[0],sh_in[0]), 
                                   npl(0.,sh_ou[1],sh_in[1]), 
                                   inn, kx=1, ky=1) 
    ou=itp(npa(sh_ou[0]), npa(sh_ou[1]))
    return ou

def interpolate_tie(inn,al,ac, profile=False):
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
    
    xlid = np.floor(xflt).astype(np.int)  # x lower index
    ylid = np.floor(yflt).astype(np.int)  # y lower index

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
    return interpolate_tie(inn,al,ac,profile=True)




def get_pressure(ff,st=(1,1)):
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        lat=ds.variables['sea_level_pressure'][:]*1.
        ac=ds.ac_subsampling_factor
        al=ds.al_subsampling_factor
    return masked2filled(interpolate_tie(lat,al,ac)[::st[0],::st[1]])

def get_temperature(ff,st=(1,1)):
    # this is just the tenmperatur at the lowest 
    # pressure profile, wich could be below
    # the surface. Use get_surf_temperature
    # instead
    with Dataset('%s/tie_meteo.nc'%ff) as ds:
        tmp=ds.variables['atmospheric_temperature_profile'][:,:,-1].squeeze()*1.
        #tmp=ds.variables['atmospheric_temperature_profile'][:,:,0].squeeze()*1.
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
    bit = flagname_to_flagbit(ff, datatype='l1')['saturated@M%02i'%bn]
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
    variable='M%02i_radiance'%bn
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

def add_cloud_margin(cm, b=2):
    '''
    '''
    kk = np.ones((2*b+1,2*b+1))
    cs = signal.convolve2d(cm+0,kk+0)
    out = ((cs[b:-b,b:-b]>0) | (cm))
    return out

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
    #below 1000 hPa: 
    xdx = uindx==len(plv)
    stm[xdx] =tmp[:,:,-1][xdx]
    # residual is nan
    stm[prs<0] = np.nan
    stm[prs>1050] = np.nan
    stm[~np.isfinite(prs)] = np.nan
    try:
        stm[prs.mask] = np.nan
    except AttributeError:
        pass 
    return stm


def get_l2_cloudmask(ff,st=(1,1)):
    fname = 'common_flags'
    variable = 'CC'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CIRRUS')
    return out

def get_l2_aot(ff,st=(1,1)):
    variable='T865'
    fname='w_aer'
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        out=(ds.variables[variable][:,:])*1.
    return out[::st[0],::st[1]]

def get_l2_sea_ice(ff,st=(1,1)):
    fname = 'wqsf'
    variable = 'WP_QS'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        sea_ice = tb('SEA_ICE')
    return sea_ice

def test_bit(f,b):
    return (f & 2**b ) == 2**b

def get_l2_ocean_aot_fail(ff,st=(1,1)):
    fname = 'wqsf'
    variable = 'WP_QS'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        wpqs = tb('MEGLINT') | tb('HIGHGLINT') | tb('WHITECAPS') |tb('SEA_ICE')
    fname = 'wqsf'
    variable = 'WP_PC'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        wppc = tb('T865_FAIL') 
    fname = 'common_flags'
    variable = 'CC'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        cla = tb('CLOUD') | tb('CLOUD_AMBIGUOUS')| tb('CIRRUS')
    variable = 'CO'
    flagbits = flagname_to_flagbit(ff, datatype='l2',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        comm = tb('INVALID') |  tb('SUSPECT')| tb('HISOLZEN') | (~tb('DO_WATER'))
    inv = wpqs | wppc | cla | comm
    return inv


def get_idepix_cloudmask(ff,st=(1,1)):
    fname = 'Idepix'
    variable='pixel_classif_flags'
    flagbits = flagname_to_flagbit(ff, datatype='idepix',target=fname,variable=variable)
    with Dataset('%s/%s.nc'%(ff,fname)) as ds:
        flg=ds.variables[variable][:,:][::st[0],::st[1]]
        def tb(fn):
            bit = flagbits[fn]
            return test_bit(flg,bit)
        out=tb('IDEPIX_CLOUD') | tb('IDEPIX_CLOUD_AMBIGUOUS')| tb('IDEPIX_CLOUD_BUFFER')| tb('IDEPIX_CLOUD_SURE')
    return out


def flagname_to_flagbit(ff, datatype='l2', target ='common_flags',variable='CC'):
    '''
    valid l2 targets: common_flags, wqsf,cqsf,lqsf
    '''
    if datatype == 'l1':
        variable='quality_flags'
        target='qualityFlags'
    with Dataset('%s/%s.nc'%(ff,target)) as ds:
        meanings = ds.variables[variable].flag_meanings.split()
        masks = ds.variables[variable].flag_masks
    flgs = {n: int(b).bit_length()-1 for n,b in  zip(meanings,masks)}
    return flgs

def get_corner(ff,st=(1,1)):
    ## assumes FR dataset
    ww = (0,0),(0,-1),(-1,0),(-1,-1)
    with Dataset('%s/tie_geo_coordinates.nc'%ff) as ds:
        lat=np.array([ds.variables['latitude'][i,j] for i,j in ww])
        lon=np.array([ds.variables['longitude'][i,j] for i,j in ww])
    return lat,lon

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
    """
    ###
    THIS WOUL NEED SOME WORK. POLYGONE OVERLAP; BUT DONT HAVE THE SAME SIZE...
    TRICKY
    ###
    """
    
    same_start = (get_startTimestamp(oll1) == get_startTimestamp(oll2))
    same_end = (get_stopTimestamp(oll1) == get_stopTimestamp(oll2))
    if not ( same_start and same_end):
        print('times of L1 and L2 do not fit, exiting')
        print('Starts:', get_startTimestamp(oll1),  get_startTimestamp(oll2), same_start ) 
        print('Stops:', get_stopTimestamp(oll1),  get_stopTimestamp(oll2), same_end ) 
        return False
        
    l1_shape = get_dimension(oll1)
    l2_shape = get_dimension(oll2)
    same_shape = (l2_shape == l1_shape) 
    if not same_shape:
        print('shapes of L1 and L2 do not fit, exiting')
        print('l1:', l1_shape)
        print('l2:', l2_shape)
        #sys.exit()
        return False

    l1_orbit = get_orbitNumber(oll1)
    l2_orbit = get_orbitNumber(oll2)
    if not (l1_orbit == l2_orbit):
        print('orbits of L1 and L2 do not fit, exiting')
        print('l1:', l1_orbit)
        print('l2:', l2_orbit)
        return False
    
    l1_corner = get_corner(oll1)
    l2_corner = get_corner(oll2)
    is_ok = (np.sum(np.abs(l1_corner[0] - l2_corner[0]) < 0.0001) 
             + np.sum(np.abs(l1_corner[1] - l2_corner[1]) < 0.0001))
    return is_ok == 8

    #l1_lon, l1_lat = get_envelop(oll1)
    #print(l1_lon)
    #l2_lon, l2_lat = get_envelop(oll2)
    #print(l2_lon)
    #lon_ok = (l1_lon.size == l2_lon.size) and ( np.sum(np.abs(l1_lon - l2_lon) < 0.0001) == l1_lon.size)
    #lat_ok = (l1_lat.size == l2_lat.size) and ( np.sum(np.abs(l1_lat - l2_lat) < 0.0001) == l1_lat.size)
    #if not (lon_ok or lat_ok):
        #print('Polygon envelops of L1 and L2 do not fit, exiting')
        ##sys.exit()



def get_relevant_l1l2_data(oll1, oll2, config, cmi = False,bnds=[13,14,15,]): 
    '''
    '''
    stride = config['PROCESSING']['stride']
    #l2_lon = get_longitude(oll2,stride)
    #l2_lat = get_latitude(oll2,stride)
    l1_lon = get_longitude(oll1,stride)
    l1_lat = get_latitude(oll1,stride)

    rad = {k:get_band(oll1,k,stride)/get_solar_flux(oll1,k,stride) for k in bnds}
    cwl = {k:get_cwl(oll1,k,stride) for k in  bnds}
    sat = {k:get_band_saturation(oll1,k,stride) for k in range(1,16)}
    geo = get_geometry(oll1,stride)
    lon = get_longitude(oll1,stride)
    lat = get_latitude(oll1,stride)
    prs = height2press(get_altitude(oll1,stride))
    lsm = get_lsmask(oll1,stride)
    
    cb = config['GENERAL']['coast_border']//((stride[0]+stride[1])//2)
    cb = max(1 , cb)
    #print('Coast Boarder in stride units:',cb)
    sct = simple_coast(lsm, cb)
    tem_ = get_temperature(oll1,stride)
    tem = get_surf_temperature(oll1,stride)
    #tem = get_temperature(oll1,stride)
    #tem_ = get_surf_temperature(oll1,stride)
    wsp = get_wind(oll1,stride)
    tcw = get_tcw(oll1,stride)
    if cmi:
        cld = get_idepix_cloudmask(oll1,stride)
        aot_l2 = np.zeros_like(tcw) + config['PROCESSING']['ocean_aot_fallback']
    else:
        cld = get_l2_cloudmask(oll2,stride)
        cld = add_cloud_margin(cld)
        aot_l2 = np.log10(0.1 + get_l2_aot(oll2,stride).filled(np.nan))
        aot_l2 = np.where(~ get_l2_ocean_aot_fail(oll2,stride), aot_l2, config['PROCESSING']['ocean_aot_fallback'])
    amf = 1./np.cos(geo['SZA']*np.pi/180.)+1./np.cos(geo['OZA']*np.pi/180.)
    
    # radiance ok
    mask = np.ones_like(lsm)        
    for xx in rad:
        mask = mask & np.isfinite(rad[xx])
    for xx in sat:
        mask = mask & ~sat[xx]

    #data ok for processing
    dok = np.ones_like(mask)
    dok = dok & mask
    dok = dok & ~cld.filled(True)
    dok = dok & (geo['SZA'] <= config['PROCESSING']['max_solar_zenith'])
    for b in rad:
        dok = dok & (rad[b] >= config['PROCESSING']['min_norm_rad'])


    # data for land processing
    dfl = dok & (lsm  |  (sct & (rad[14] > config['PROCESSING']['min_coast_norm_rad'])))
    # data for ocean processing
    dfo = dok & ~dfl
    
    #eventally fill the land processing with aot land background
    aot_l2[dfl] = config['PROCESSING']['land_aot_fallback']

    return {'lon': l1_lon, 'lat': l1_lat, 'geo': geo, 'amf': amf,
            'rad': rad, 'cwl': cwl, 'sat': sat, 
            'lsm': lsm, 'sct': sct, 'cld': cld,
            'prs': prs, 'wsp': wsp, 'tem': tem,'tem_': tem_, 'tcw': tcw, 'aot': aot_l2,
            'msk': mask, 'dok': dok, 'nnn': dok.size,'dfl': dfl, 'dfo': dfo,
            }


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
        if dct[key].dtype == np.bool:
            dct[key] = dct[key].astype(np.int8)

    with Dataset(outname, 'w', format='NETCDF4') as nc:
        nc.filename = outname
        nc.history = datetime.datetime.now().strftime('created on %Y-%m-%d %H:%M:%S UTC')
        nc.Conventions = "CF-1.4"
        nc.metadata_version = "0.5"
        nc.createDimension('y',yy)
        nc.createDimension('x',xx)
        for key in dct:
            nc_dum=nc.createVariable(key,dct[key].dtype,('y','x'),zlib=True)
            nc_dum[:]=dct[key]



