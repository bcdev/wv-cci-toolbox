import argparse
import calendar
import os
import sys
import time
from datetime import datetime as dt
from pathlib import Path

import numpy as np
import scipy.interpolate as interp
from lxml import etree as ET
from netCDF4 import Dataset
from scipy import signal

NOW = time.strftime('%Y%m%dT%H%M%S', time.gmtime())
AOT_LOG_SCALE_OFFSET = 0.01


# ###
# I/O related functions used by olci_l2_processor_for_calvalus
# ###

def cowa_parser():
    """

    :return:
    """
    cp = argparse.ArgumentParser(description='COWA processor for OLCI')

    cp.add_argument('-l1', '--l1_file', type=str, action="store", dest="l1", required=True,
                    help='Mandatory: Sentinel3 Level 1 file path name (ending with .SEN3)')

    cp.add_argument('-idp', '--idepix_file', type=str, action="store", dest="idp",
                    help='Name of the idepix file. Default is /spam/egs/S3?_OL_1.....SEN3/Idepix.nc')

    cp.add_argument('-ini', '--ini_file', type=str, action="store", dest="ini", default=None,
                    help='Name of the main ini file. Default is none. Calling routine must take care.')

    cp.add_argument('-lpi', '--land_processor_ini', type=str, action="store", dest="lpi", default=None,
                    help='Name of the land ini file. Default is none. Calling routine must take care.')

    cp.add_argument('-opi', '--ocean_processor_ini', type=str, action="store", dest="opi", default=None,
                    help='Name of the ocean ini file. Default is none. Calling routine must take care.')

    cp.add_argument('-ll', '--land_lut', type=str, action="store", dest="ll", default=None,
                    help='Name of the land LUT file. Default is none. Calling routine must take care.')

    cp.add_argument('-ol', '--ocean_lut', type=str, action="store", dest="ol", default=None,
                    help='Name of the ocean LUT file. Default is none. Calling routine must take care.')

    cp.add_argument('-t', '--target_file', type=str, action="store", dest="result",
                    help='Will be a netCDF4 and should have an according suffix. If not given, name and path are constructed from l1')

    cp.add_argument('-s', '--stride', type=str, action="store", dest="stride", default=None,
                    help='Use nx,ny as stride instead of stride from ini')

    cp.add_argument('-dsm', '--desmile_h2o_bands', action="store_true", dest="desmile_h2o_bands", default=False,
                    help='Desmiles bands 19 and 20 before starting cowa (currently implemented for CCI only)')

    return cp.parse_args()


def check_if_files_exists(*arg):
    """

    :param arg:
    :return:
    """
    for ff in arg:
        if ff is None:
            continue
        if not os.path.exists(ff):
            print(ff + ' not found, exiting!', 'red')
            sys.exit(-2)


def l1_to_result(l1_name, stride, add_l1_folder=True, now=NOW):
    """

    :param l1_name:
    :param stride:
    :param add_l1_folder:
    :param now:
    :return:
    """
    seg = Path(l1_name).name.split('_')
    seg[2] = '2'
    seg[3] = 'COWA' + seg[3][-2:]
    seg[9] = now
    seg[-4] = 'RP1'
    seg[-1] = seg[-1].split('.')[0]
    if (stride[0], stride[1]) == (1, 1):
        seg[-1] += '.nc4'
    else:
        seg[-1] += '_%ix%i.nc4' % (stride[0], stride[1])
    res = '_'.join(seg)
    if add_l1_folder:
        res = str(Path(l1_name).parent / res)
    return res


def cosd(inn):
    return np.cos(inn * np.pi / 180.)


def sind(inn):
    return np.sin(inn * np.pi / 180.)


def acosd(inn):
    return np.arccos(inn) * 180. / np.pi


def asind(inn):
    return np.arcsin(inn) * 180. / np.pi


def masked2filled(m, fv=np.nan):
    """

    :param m:
    :param fv:
    :return:
    """
    try:
        return m.filled(fv)
    except AttributeError:
        return m


def azi2azid(sa, va):
    """

    :param sa:
    :param va:
    :return:
    """
    return acosd(cosd(sa) * cosd(va) + sind(sa) * sind(va))


def height2press(hh, p0=1013.25):
    """
    This is very simple, but actually sufficient.
    :param hh:
    :param p0:
    :return:
    """
    return p0 * (1. - (hh * 0.0065 / 288.15)) ** 5.2555


def lon2lon180(lon):
    """
    make longitudes consistent 
    between -180,180
    """
    re = sind(lon)
    im = cosd(lon)
    return np.arctan2(re, im) * 180. / np.pi


def read_manifest(s3):
    """
    returns text
    """
    xfx = os.path.join(s3, 'xfdumanifest.xml')
    if xfx is None:
        return None
    with open(xfx) as fp:
        txt = fp.read()
    return txt


def get_manifest(s3):
    """
    returns an xml object
    """
    xfx = os.path.join(s3, 'xfdumanifest.xml')
    if xfx is None:
        return None

    with open(xfx) as fp:
        xfu = ET.parse(fp)
    return xfu.getroot()


def get_start_time(ff, tupel=False):
    """

    :param ff:
    :param tupel:
    :return:
    """
    xfu = get_manifest(ff)
    inf = xfu.find('.//sentinel-safe:startTime', xfu.nsmap).text.split(' ')
    if tupel is True:
        return time.strptime(inf[0], '%Y-%m-%dT%H:%M:%S.%fZ')
    else:
        return inf


def get_stop_time(ff, tupel=False):
    """

    :param ff:
    :param tupel:
    :return:
    """
    xfu = get_manifest(ff)
    inf = xfu.find('.//sentinel-safe:stopTime', xfu.nsmap).text.split(' ')
    if tupel is True:
        return time.strptime(inf[0], '%Y-%m-%dT%H:%M:%S.%fZ')
    else:
        return inf


def get_stop_timestamp(ff):
    """

    :param ff:
    :return:
    """
    return calendar.timegm(get_stop_time(ff, tupel=True))


def get_start_timestamp(ff):
    """

    :param ff:
    :return:
    """
    return calendar.timegm(get_start_time(ff, tupel=True))


def get_dimension(ff):
    """

    :param ff:
    :return:
    """
    xfu = get_manifest(ff)
    return (int(xfu.find('.//sentinel3:rows', xfu.nsmap).text),
            int(xfu.find('.//sentinel3:columns', xfu.nsmap).text))


def get_orbit_number(ff):
    """

    :param ff:
    :return:
    """
    xfu = get_manifest(ff)
    inf = xfu.find('.//sentinel-safe:orbitNumber', xfu.nsmap).text
    return int(inf)


def get_olci(ff):
    """

    :param ff:
    :return:
    """
    xfu = get_manifest(ff)
    pfm = xfu.find('.//sentinel-safe:nssdcIdentifier', xfu.nsmap).text
    if pfm == '2016-011A':
        olc = 'A'
    elif pfm == '2018-039A':
        olc = 'B'
    else:
        olc = None
    return olc


def get_envelop(ff):
    """

    :param ff:
    :return:
    """
    xfu = get_manifest(ff)
    lst = xfu.find('.//gml:posList', xfu.nsmap).text.split(' ')
    lat = np.array([float(lst[2 * i]) for i in range(len(lst) // 2)])
    lon = np.array([float(lst[2 * i + 1]) for i in range(len(lst) // 2)])
    return lon, lat


def get_altitude(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/geo_coordinates.nc' % ff) as ds:
        alt = ds.variables['altitude'][::st[0], ::st[1]] * 1.
    return masked2filled(alt)


def interpolate_tie(inn, al, ac):
    """

    :param inn:
    :param al:
    :param ac:
    :return:
    """
    npa = np.arange
    npl = np.linspace
    sh_in = inn.shape
    sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1)
    ou = interp.RectBivariateSpline(
        npl(0., sh_ou[0], sh_in[0])
        , npl(0., sh_ou[1], sh_in[1])
        , inn, kx=1, ky=1)(npa(sh_ou[0])
                           , npa(sh_ou[1]))
    return masked2filled(ou)


def interpolate_tie_(inn, al, ac, profile=False):
    """

    :param inn:
    :param al:
    :param ac:
    :param profile:
    :return:
    """
    npl = np.linspace
    sh_in = inn.shape
    if profile is True:
        sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1, sh_in[2])
    else:
        sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1)

    xv = npl(0., sh_in[0] - 1, sh_ou[0])
    yv = npl(0., sh_in[1] - 1, sh_ou[1])
    xflt, yflt = np.meshgrid(xv, yv, sparse=False, indexing='ij', copy=True)

    xlid = np.floor(xflt).astype(np.int32)  # x lower index
    ylid = np.floor(yflt).astype(np.int32)  # y lower index

    xuid = (xlid + 1).clip(0, sh_in[0] - 1)  # x upper index
    yuid = (ylid + 1).clip(0, sh_in[1] - 1)  # y upper index

    wx = xuid - xflt
    wy = yuid - yflt
    w00 = wx * (wy)  # xlo,ylo
    w10 = (1 - wx) * (wy)  # xup,ylo
    w01 = wx * (1 - wy)  # xlo,yup
    w11 = (1 - wx) * (1 - wy)  # xlo,yup

    if profile is True:
        out = inn[((xlid), (ylid))] * w00[:, :, np.newaxis]
        out += inn[((xuid), (ylid))] * w10[:, :, np.newaxis]
        out += inn[((xlid), (yuid))] * w01[:, :, np.newaxis]
        out += inn[((xuid), (yuid))] * w11[:, :, np.newaxis]
    else:
        out = inn[((xlid), (ylid))] * w00
        out += inn[((xuid), (ylid))] * w10
        out += inn[((xlid), (yuid))] * w01
        out += inn[((xuid), (yuid))] * w11
    return masked2filled(out)


def interpolate_tie_profile(inn, al, ac):
    """

    :param inn:
    :param al:
    :param ac:
    :return:
    """
    return interpolate_tie_(inn, al, ac, profile=True)


def get_pressure(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_meteo.nc' % ff) as ds:
        lat = ds.variables['sea_level_pressure'][:] * 1.
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return masked2filled(interpolate_tie(lat, al, ac)[::st[0], ::st[1]])


def get_temperature(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_meteo.nc' % ff) as ds:
        tmp = ds.variables['atmospheric_temperature_profile'][:, :, 0].squeeze() * 1.
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return masked2filled(interpolate_tie(tmp, al, ac)[::st[0], ::st[1]])


def get_wind(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_meteo.nc' % ff) as ds:
        wsp = (ds.variables['horizontal_wind'][:, :, :] ** 2).sum(axis=2) ** 0.5
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return masked2filled(interpolate_tie(wsp, al, ac)[::st[0], ::st[1]])


def get_tcw(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_meteo.nc' % ff) as ds:
        tcw = ds.variables['total_columnar_water_vapour'][:] * 1
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return masked2filled(interpolate_tie(tcw, al, ac)[::st[0], ::st[1]])


def get_subsampling_factors(ff):
    """

    :param ff:
    :return:
    """
    with Dataset('%s/tie_meteo.nc' % ff) as ds:
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return ac, al


def get_lsmask(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    bit = flagname_to_flagbit(ff, datatype='l1')['land']
    with Dataset('%s/qualityFlags.nc' % ff) as ds:
        lsm = (ds.variables['quality_flags'][:, :] & 2 ** bit) == 2 ** bit
    return masked2filled(lsm[::st[0], ::st[1]], False)


def get_band_saturation(ff, bn, st=(1, 1)):
    """

    :param ff:
    :param bn:
    :param st:
    :return:
    """
    bit = flagname_to_flagbit(ff, datatype='l1')['saturated@Oa%02i' % bn]
    with Dataset('%s/qualityFlags.nc' % ff) as ds:
        lsm = (ds.variables['quality_flags'][:, :] & 2 ** bit) == 2 ** bit
    return masked2filled(lsm[::st[0], ::st[1]], True)


def get_latitude(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_geo_coordinates.nc' % ff) as ds:
        lat = ds.variables['latitude'][:] * 1.
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    return masked2filled(interpolate_tie(lat, al, ac)[::st[0], ::st[1]])


def get_lat_tp(ds, ac, al, st=(1, 1)):
    """

    :param ds:
    :param ac:
    :param al:
    :param st:
    :return:
    """
    # TP grid
    lat = ds.variables['TP_latitude'][:] * 1.
    re = np.sin(lat * np.pi / 180.)
    im = np.cos(lat * np.pi / 180.)
    re_out = interpolate_tie(re, al, ac)[::st[0], ::st[1]]
    im_out = interpolate_tie(im, al, ac)[::st[0], ::st[1]]
    return masked2filled(np.arctan2(re_out, im_out) * 180. / np.pi)


def get_longitude(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_geo_coordinates.nc' % ff) as ds:
        lon = ds.variables['longitude'][:] * 1.
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    re = np.sin(lon * np.pi / 180.)
    im = np.cos(lon * np.pi / 180.)
    re_out = interpolate_tie(re, al, ac)[::st[0], ::st[1]]
    im_out = interpolate_tie(im, al, ac)[::st[0], ::st[1]]
    return masked2filled(np.arctan2(re_out, im_out) * 180. / np.pi)


def get_lon_tp(ds, ac, al, st=(1, 1)):
    """

    :param ds:
    :param ac:
    :param al:
    :param st:
    :return:
    """
    # TP grid
    lon = ds.variables['TP_longitude'][:] * 1.
    re = np.sin(lon * np.pi / 180.)
    im = np.cos(lon * np.pi / 180.)
    re_out = interpolate_tie(re, al, ac)[::st[0], ::st[1]]
    im_out = interpolate_tie(im, al, ac)[::st[0], ::st[1]]
    return masked2filled(np.arctan2(re_out, im_out) * 180. / np.pi)


def get_geometry(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/tie_geometries.nc' % ff) as ds:
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
        out = {k: ds.variables[k][:] * 1. for k in ds.variables}
    for k in out:
        out[k] = masked2filled(interpolate_tie(out[k], al, ac)[::st[0], ::st[1]])
    out['ADA'] = azi2azid(out['SAA'], out['OAA'])
    return out


def get_detector_index(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    with Dataset('%s/instrument_data.nc' % ff) as ds:
        di = ds.variables['detector_index'][:] * 1
    return di[::st[0], ::st[1]]


def get_cwl(ff, bn, st=(1, 1)):
    """

    :param ff:
    :param bn:
    :param st:
    :return:
    """
    with Dataset('%s/instrument_data.nc' % ff) as ds:
        l0 = (ds.variables['lambda0'][bn - 1, :] * 1.).squeeze()
        di = ds.variables['detector_index'][:] * 1
    return masked2filled(l0[di][::st[0], ::st[1]])


def get_solar_flux(ff, bn, st=(1, 1)):
    """

    :param ff:
    :param bn:
    :param st:
    :return:
    """
    with Dataset('%s/instrument_data.nc' % ff) as ds:
        sf = (ds.variables['solar_flux'][bn - 1, :] * 1.).squeeze()
        di = ds.variables['detector_index'][:] * 1
    return masked2filled(sf[di][::st[0], ::st[1]])


def get_band(ff, bn, st=(1, 1)):
    """

    :param ff:
    :param bn:
    :param st:
    :return:
    """
    variable = 'Oa%02i_radiance' % bn
    with Dataset('%s/%s.nc' % (ff, variable)) as ds:
        out = ds.variables[variable][:] * 1.
    return masked2filled(out[::st[0], ::st[1]])


def simple_coast(lm, b=5):
    """
    shift in 8 directions ...
    """
    kk = np.ones((2 * b + 1, 2 * b + 1))
    cs = signal.convolve2d(lm + 0, kk + 0)
    out = ((cs[b:-b, b:-b] > 0) & (~lm))
    return out


def get_l2_aot(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    variable = 'T865'
    fname = 'w_aer'
    with Dataset('%s/%s.nc' % (ff, fname)) as ds:
        out = (ds.variables[variable][:, :]) * 1.
    return out[::st[0], ::st[1]]


def test_bit(f, b):
    """

    :param f:
    :param b:
    :return:
    """
    return (f & 2 ** b) == 2 ** b


def get_idepix_masks(ds, st=(1, 1)):
    """

    :param ds:
    :param st:
    :return:
    """
    variable = 'pixel_classif_flags'
    flagbits = idepix_flagname_to_flagbit(ds)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    mask_all = tb('IDEPIX_INVALID') | tb('IDEPIX_CLOUD') | tb('IDEPIX_CLOUD_AMBIGUOUS') | tb(
        'IDEPIX_CLOUD_BUFFER') | tb(
        'IDEPIX_CLOUD_SURE')

    invalid = tb('IDEPIX_INVALID')
    cloud = tb('IDEPIX_CLOUD') | tb('IDEPIX_CLOUD_AMBIGUOUS') | tb('IDEPIX_CLOUD_BUFFER') | tb('IDEPIX_CLOUD_SURE')
    seaice = tb('IDEPIX_SNOW_ICE')
    land = tb('IDEPIX_LAND')
    return {
        'mask_all': mask_all,
        'invalid': invalid,
        'cloud': cloud,
        'seaice': seaice,
        'land': land
    }


def idepix_flagname_to_flagbit(ds):
    """

    :param ds:
    :return:
    """
    meanings = ds.variables['pixel_classif_flags'].flag_meanings.split()
    masks = ds.variables['pixel_classif_flags'].flag_masks
    flgs = {n: int(b).bit_length() - 1 for n, b in zip(meanings, masks)}
    return flgs


def flagname_to_flagbit(ff, datatype='ocean'):
    """

    :param ff:
    :param datatype:
    :return:
    """
    if datatype == 'ocean':
        variable = 'WQSF'
        fname = '%s/%s.nc' % (ff, 'wqsf')
    elif datatype == 'land':
        variable = 'LQSF'
        fname = '%s/%s.nc' % (ff, 'lqsf')
    elif datatype == 'l1':
        variable = 'quality_flags'
        fname = '%s/%s.nc' % (ff, 'qualityFlags')
    elif datatype == 'idepix':
        variable = 'pixel_classif_flags'
        fname = ff
    else:
        return None
    with Dataset(fname) as ds:
        meanings = ds.variables[variable].flag_meanings.split()
        masks = ds.variables[variable].flag_masks
    flgs = {n: int(b).bit_length() - 1 for n, b in zip(meanings, masks)}
    return flgs


def get_surf_temperature(s3, st=(1, 1)):
    """

    :param s3:
    :param st:
    :return:
    """
    tmp = get_temperature_profile(s3, st)
    plv = get_pressure_profile(s3, st)
    alt = get_altitude(s3, st)
    slp = get_pressure(s3, st)
    prs = height2press(alt.clip(0), slp)
    stm = np.zeros_like(alt, dtype=np.float32)
    if plv[0] < plv[1]:  # Merislike
        pass
    else:
        plv = plv[..., ::-1]
        tmp = tmp[..., ::-1]
    uindx = np.searchsorted(plv, prs)
    uwght = np.zeros(uindx.shape, dtype=np.float32)
    lwght = np.zeros(uindx.shape, dtype=np.float32)
    for i in range(1, len(plv)):
        xdx = uindx == i
        lwght[xdx] = (prs[xdx] - plv[i]) / (plv[i - 1] - plv[i])
        uwght[xdx] = 1. - lwght[xdx]
        stm[xdx] = (tmp[:, :, i][xdx] * uwght[xdx] + tmp[:, :, i - 1][xdx] * lwght[xdx])
    xdx = uindx == len(plv)
    stm[xdx] = tmp[:, :, -1][xdx]
    stm[prs < 0] = np.nan
    stm[prs > 1050] = np.nan
    stm[~np.isfinite(prs)] = np.nan
    try:
        stm[prs.mask] = np.nan
    except AttributeError:
        pass
    return stm


def get_temperature_profile(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    variable = 'atmospheric_temperature_profile'
    fname = 'tie_meteo'
    with Dataset('%s/%s.nc' % (ff, fname)) as ds:
        tie = (ds.variables[variable][:, :, :]) * 1.
        ac = ds.ac_subsampling_factor
        al = ds.al_subsampling_factor
    out = interpolate_tie_profile(tie, al, ac)
    return out[::st[0], ::st[1], :]


def get_pressure_profile(ff, st=(1, 1)):
    """

    :param ff:
    :param st:
    :return:
    """
    variable = 'reference_pressure_level'
    fname = 'tie_meteo'
    with Dataset('%s/%s.nc' % (ff, fname)) as ds:
        out = (ds.variables[variable][:]) * 1.
    return out


def get_relevant_l1l2_data(oll1, idp, config, cmi=True):
    """

    :param oll1:
    :param idp:
    :param config:
    :param cmi:
    :return:
    """
    stride = config['PROCESSING']['stride']
    l1_lon = get_longitude(oll1, stride)
    l1_lat = get_latitude(oll1, stride)
    ac_subsampling_factor, al_subsampling_factor = get_subsampling_factors(oll1)
    ds_idp = Dataset(idp)
    l1_lon_tp = get_lon_tp(ds_idp, ac_subsampling_factor, al_subsampling_factor, stride)
    l1_lat_tp = get_lat_tp(ds_idp, ac_subsampling_factor, al_subsampling_factor, stride)

    rad = {k: get_band(oll1, k, stride) / get_solar_flux(oll1, k, stride) for k in [17, 18, 19, 20, 21, ]}
    dix = get_detector_index(oll1, stride)
    cwl = {k: get_cwl(oll1, k, stride) for k in [17, 18, 19, 20, 21, ]}
    try:
        sat = {k: get_band_saturation(oll1, k, stride) for k in range(1, 22)}
    except KeyError:
        sat = {k: np.zeros_like(rad[k], dtype=np.bool_) for k in rad}

    geo = get_geometry(oll1, stride)
    prs = height2press(get_altitude(oll1, stride))  # TODO adjust sea level pressure
    lsm = get_lsmask(oll1, stride)
    env = get_envelop(oll1)

    cb = config['GENERAL']['coast_border'] // ((stride[0] + stride[1]) // 2)
    cb = max(1, cb)
    print('Coast Boarder in stride units:', cb)
    sct = simple_coast(lsm, cb)
    # todo switch tem. Be onsistent with cowa slow!!!
    tem = get_temperature(oll1, stride)
    tem_ = get_surf_temperature(oll1, stride)
    wsp = get_wind(oll1, stride)
    tcw = get_tcw(oll1, stride)

    # idepix_masks = get_idepix_cloudmask(idp,stride)
    idepix_masks = get_idepix_masks(ds_idp, stride)
    aot_l2 = np.zeros_like(tcw) + config['PROCESSING']['ocean_aot_fallback']

    amf = 1. / np.cos(geo['SZA'] * np.pi / 180.) + 1. / np.cos(geo['OZA'] * np.pi / 180.)

    # radiance ok
    mask = np.ones_like(lsm)
    for xx in rad:
        mask = mask & np.isfinite(rad[xx])
    for xx in sat:
        mask = mask & ~sat[xx]
    mask = mask & np.isfinite(tcw)
    mask = mask & (tcw > 0)

    # data ok for processing
    dok = np.ones_like(mask)
    dok = dok & mask

    dok = dok & ~idepix_masks['mask_all'].filled(True)
    dok = dok & (geo['SZA'] <= config['PROCESSING']['max_solar_zenith'])
    for b in rad:
        dok = dok & (rad[b] >= config['PROCESSING']['min_norm_rad'])

    # data for land processing
    # dfl = dok & (lsm | (sct & (rad[21] > config['PROCESSING']['min_coast_norm_rad'])))
    # Interpret seaice pixels as land in order to use the land LUT later on (RP/OD 20230927):
    dfl = dok & (idepix_masks['seaice'] | lsm | (sct & (rad[21] > config['PROCESSING']['min_coast_norm_rad'])))
    # data for ocean processing
    dfo = dok & ~dfl

    orbit = get_orbit_number(oll1)
    olc = get_olci(oll1)

    # eventally fill the land processing with aot land background
    aot_l2[dfl] = config['PROCESSING']['land_aot_fallback']

    l1_manifest = read_manifest(oll1)
    l2_manifest = None

    stfl = np.zeros_like(mask, dtype=np.int16)
    # land
    stfl[np.where(idepix_masks['land'] == 1)] = stfl[np.where(idepix_masks['land'] == 1)] + 1
    # ocean
    stfl[np.where((idepix_masks['invalid'] != 1) & (idepix_masks['land'] != 1) & (idepix_masks['seaice'] != 1))] = \
        stfl[np.where(
            (idepix_masks['invalid'] != 1) & (idepix_masks['land'] != 1) & (idepix_masks['seaice'] != 1))] + 2
    # sea ice
    stfl[np.where(idepix_masks['seaice'] == 1)] = stfl[np.where(idepix_masks['seaice'] == 1)] + 4
    # cloud
    stfl[np.where(idepix_masks['cloud'] == 1)] = stfl[np.where(idepix_masks['cloud'] == 1)] + 8
    # undefined:
    stfl[np.where((idepix_masks['invalid'] == 1) | (geo['SZA'] > config['PROCESSING']['max_solar_zenith']))] = 16
    # undefined remainders??
    stfl[np.where(stfl == 0)] = 16

    return {'lon': l1_lon, 'lat': l1_lat, 'geo': geo, 'amf': amf,
            'lon_tp': l1_lon_tp, 'lat_tp': l1_lat_tp,
            'rad': rad, 'cwl': cwl, 'sat': sat, 'dix': dix.astype(np.int16),
            'lsm': lsm, 'sct': sct, 'cld': idepix_masks, 'stfl': stfl,
            'prs': prs, 'wsp': wsp, 'tem': tem, 'tem_': tem_, 'tcw': tcw, 'aot': aot_l2,
            'msk': mask, 'dok': dok, 'nnn': dok.size, 'dfl': dfl, 'dfo': dfo,
            'l1_manifest': l1_manifest, 'l2_manifest': l2_manifest, 'envelop': env,
            'orb': orbit, 'olci': olc}


def write_to_ncdf_cci(outname, dct, start_date_string, stop_date_string):
    """

    :param outname:
    :param dct:
    :param start_date_string:
    :param stop_date_string:
    :return:
    """
    yy = dct['lat'].shape[0]
    xx = dct['lat'].shape[1]
    yy_tp = dct['lat_tp'].shape[0]
    xx_tp = dct['lat_tp'].shape[1]
    for key in dct:
        if dct[key].ndim != 2:
            print(key, 'is not 2 dimensional')
            return None
        if dct[key].shape[0] != yy and dct[key].shape[0] != yy_tp:
            print("Dimension 0 of ", key, 'does not agree.')
            print("is:", dct[key].shape[0], "should be ", yy, "(like lat) or ", yy_tp, "(like TP lat).")
            return None
        if dct[key].shape[1] != xx and dct[key].shape[1] != xx_tp:
            print("Dimension 1 of ", key, 'does not agree.')
            print("is:", dct[key].shape[1], "should be ", xx, "(like lon) or ", xx_tp, "(like TP lon).")
            return None
        if dct[key].dtype == np.bool:
            dct[key] = dct[key].astype(np.int8)

    with Dataset(outname, 'w', format='NETCDF4') as nc_out:
        nc_out.filename = outname
        nc_out.Conventions = "CF-1.10"
        nc_out.metadata_version = "0.5"
        nc_out.createDimension('y', yy)
        nc_out.createDimension('x', xx)
        nc_out.createDimension('tp_y', yy_tp)
        nc_out.createDimension('tp_x', xx_tp)

        # set subset of global attributes following CF and CCI standards:
        nc_out.setncattr('title', 'Water Vapour CCI Total Column of Water Vapour L2 Product')
        nc_out.setncattr('institution', 'Brockmann Consult GmbH, Spectral Earth GmbH')
        nc_out.setncattr('source', 'OLCI RR L1B')
        nc_out.setncattr('product_version', '4.0')  # todo: extract as parameter
        nc_out.setncattr('summary', 'Water Vapour CCI TCWV Version')
        nc_out.setncattr('id', os.path.basename(nc_out.filename))
        nc_out.setncattr('time_coverage_start', start_date_string)
        nc_out.setncattr('time_coverage_end', stop_date_string)
        nc_out.setncattr('naming-authority', 'brockmann-consult.de')
        nc_out.setncattr('comment',
                         'These data were produced in the frame of the Water Vapour ECV (Water_Vapour_cci) of the ESA '
                         'Climate Change Initiative Extension (CCI+) Phase 2')

        date_created = str(dt.utcnow())[:19] + ' UTC'
        nc_out.setncattr('date_created', date_created)
        nc_out.setncattr('creator_name', 'Brockmann Consult GmbH')
        nc_out.setncattr('creator_url', 'www.brockmann-consult.de')
        nc_out.setncattr('creator_email', 'info@brockmann-consult.de')
        nc_out.setncattr('project', 'WV_cci')

        for key in dct:
            if key == 'tcwv':
                # TCWV
                #  float tcwv(y, x) --> ushort tcwv(y, x) with scale_factor = 0.01:
                inv_scale_factor = 100
                var_tcwv = nc_out.createVariable(key, 'u2', ('y', 'x'), fill_value=0, zlib=True)
                var_tcwv[:] = dct[key] * inv_scale_factor
                var_tcwv.setncattr('long_name', 'Total Column of Water Vapour')
                var_tcwv.setncattr('units', 'kg/m^2')
                var_tcwv.setncattr('coordinates', 'lat lon')
                var_tcwv.setncattr('scale_factor', 1. / inv_scale_factor)

            if key == 'unc':
                # TCWV uncertainty
                #  float unc(y, x) --> ushort tcwv_uncertainty(y, x) with scale_factor = 0.001:
                inv_scale_factor = 1000
                var_tcwv = nc_out.createVariable('tcwv_uncertainty', 'u2', ('y', 'x'), fill_value=0, zlib=True)
                var_tcwv[:] = dct[key] * inv_scale_factor
                var_tcwv.setncattr('long_name', 'Uncertainty of Total Column of Water Vapour')
                var_tcwv.setncattr('units', 'kg/m^2')
                var_tcwv.setncattr('coordinates', 'lat lon')
                var_tcwv.setncattr('scale_factor', 1. / inv_scale_factor)

            if key == 'cst':
                # Cost function
                # todo: make configurable as debug band
                # var_cst = nc_out.createVariable(key, dct[key].dtype, ('y', 'x'), zlib=True)
                # var_cst[:] = dct[key]
                # var_cst.setncattr('units', 'dl')
                # var_cst.setncattr('long_name', 'Cost function of TCWV retrieval')
                # var_cst.setncattr('standard_name', 'cost')
                pass

            # lat/lon
            # todo: make configurable if we need full res, TP or both.
            #  T.Trent needs full res!
            if key == 'lat':
                # Latitude full
                var_lat = nc_out.createVariable(key, 'f4', ('y', 'x'), zlib=True)
                var_lat[:] = dct[key]
                var_lat.setncattr('units', 'degrees_north')
                var_lat.setncattr('long_name', 'latitude coordinate')
                var_lat.setncattr('standard_name', 'latitude')

            if key == 'lon':
                # Longitude full
                var_lon = nc_out.createVariable(key, 'f4', ('y', 'x'), zlib=True)
                var_lon[:] = dct[key]
                var_lon.setncattr('units', 'degrees_east')
                var_lon.setncattr('long_name', 'longitude coordinate')
                var_lon.setncattr('standard_name', 'longitude')

        for key in dct:
            if key == 'stfl':
                # Surface type flags
                surface_type_flags_var = nc_out.createVariable('surface_type_flags', 'i1', ('y', 'x'), zlib=True)
                surface_type_flags_var[:] = dct[key]
                surface_type_flags_var.setncattr('long_name', 'Surface type flags')
                surface_type_flags_var.setncattr('standard_name', 'Surface type flags')
                surface_type_flags_var.setncattr('coordinates', 'lat lon')
                surface_type_flags_var.setncattr('flag_meanings', 'LAND OCEAN SEA_ICE CLOUD UNDEFINED')
                surface_type_flags_var.setncattr('flag_masks', np.array([1, 2, 4, 8, 16], 'b'))
                surface_type_flags_var.setncattr('flag_coding_name', 'surface_type_flags')
                surface_type_flags_var.setncattr('flag_descriptions', 'Land\tOcean\tSea ice\tCloud\tUndefined')

            if key == 'cst':
                # TCWV quality flags
                create_tcwv_quality_flags_variable(nc_out, dct['cst'])

        # scan time (on request T.Trent):
        # todo: make this a user option
        create_scan_time_variable(nc_out, start_date_string, stop_date_string)


def create_scan_time_variable(nc_out, start_date_string, stop_date_string):
    """

    :param nc_out:
    :param start_date_string:
    :param stop_date_string:
    :return:
    """
    time1970 = dt(1970, 1, 1)
    starttime = dt.strptime(start_date_string, "%d-%b-%Y %H:%M:%S.%f")
    stoptime = dt.strptime(stop_date_string, "%d-%b-%Y %H:%M:%S.%f")
    start_since_1970 = (starttime - time1970).total_seconds()
    stop_since_1970 = (stoptime - time1970).total_seconds()

    scantime_incr = (stop_since_1970 - start_since_1970) * 1.0 / (len(nc_out.dimensions['y']) - 1)
    scantime_arr = np.arange(start_since_1970, stop_since_1970 + scantime_incr / 2, scantime_incr)
    scan_time_var = nc_out.createVariable('scan_time', np.float64, 'y', zlib=True)
    scan_time_var[:] = scantime_arr
    scan_time_var.setncattr('long_name', 'Across-track scan time')
    scan_time_var.setncattr('standard_name', 'scan_time')
    scan_time_var.setncattr('units', 'Seconds since 1970-01-01')


def create_tcwv_quality_flags_variable(nc_out, cst_arr):
    """

    :param nc_out:
    :param cst_arr:
    :return:
    """
    tcwv_quality_flags_var = nc_out.createVariable('tcwv_quality_flags', 'i1', ('y', 'x'), zlib=True)
    tcwv_quality_flags_var.setncattr('long_name', 'TCWV quality flags')
    tcwv_quality_flags_var.setncattr('standard_name', 'TCWV quality flags')
    tcwv_quality_flags_var.setncattr('coordinates', 'lat lon')
    tcwv_quality_flags_var.setncattr('flag_meanings', 'TCWV_OK TCWV_COST_FUNCTION_1 TCWV_COST_FUNCTION_2 TCWV_INVALID')
    tcwv_quality_flags_var.setncattr('flag_masks', np.array([1, 2, 4, 8], 'b'))
    tcwv_quality_flags_var.setncattr('flag_coding_name', 'tcwv_quality_flags')
    tcwv_quality_flags_var.setncattr('flag_descriptions',
                                     'TCWV retrieval has no known issues\tHigh cost function 1 in TCWV '
                                     'retrieval\tHigh cost function 2 in TCWV '
                                     'retrieval\tInvalid pixel (no TCWV retrieval)')

    tcwv_quality_flags_arr = np.array(tcwv_quality_flags_var)
    tcwv_quality_flags_arr[np.where((cst_arr >= 0.0) & (cst_arr < 1.0))] = 1.0
    tcwv_quality_flags_arr[np.where((cst_arr >= 1.0) & (cst_arr < 2.0))] = 2.0
    tcwv_quality_flags_arr[np.where(cst_arr > 2.0)] = 4.0
    tcwv_quality_flags_arr[np.where(np.isnan(cst_arr))] = 8.0
    tcwv_quality_flags_var[:, :] = tcwv_quality_flags_arr[:, :]
