import os
import sys
from datetime import datetime as dt

import numpy as np
import scipy.interpolate as interp
from netCDF4 import Dataset
from scipy import signal


def check_if_files_exists(*arg):
    for ff in arg:
        if ff is None:
            continue
        if not os.path.exists(ff):
            print(ff, ' not found, exiting!')
            sys.exit()


def cosd(inn):
    return np.cos(inn * np.pi / 180.)


def sind(inn):
    return np.sin(inn * np.pi / 180.)


def acosd(inn):
    return np.arccos(inn) * 180. / np.pi


def asind(inn):
    return np.arcsin(inn) * 180. / np.pi


def masked2filled(m, fv=np.nan):
    try:
        return m.filled(fv)
    except AttributeError:
        return m


def azi2azid(sa, va):
    return acosd(cosd(sa) * cosd(va) + sind(sa) * sind(va))


def lon2lon180(lon):
    """
    make longitudes consistent
    between -180,180
    """
    re = sind(lon)
    im = cosd(lon)
    return np.arctan2(re, im) * 180. / np.pi


def height2press(hh, p0=1013.25):
    return p0 * (1. - (hh * 0.0065 / 288.15)) ** 5.2555


def get_dimension(ds):
    rows, columns = ds['latitude'].shape
    return rows, columns


def get_altitude(ds, st=(1, 1)):
    alt = ds.variables['altitude'][::st[0], ::st[1]] * 1.
    return masked2filled(alt)


def interpolate_tie__(inn, al, ac):
    npa = np.arange
    npl = np.linspace
    sh_in = inn.shape
    sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1)
    ou = interp.RectBivariateSpline(
        npl(0., sh_ou[0], sh_in[0]), npl(0., sh_ou[1], sh_in[1]), inn, kx=1, ky=1)(npa(sh_ou[0]), npa(sh_ou[1]))
    return masked2filled(ou)


def interpolate_tie_(inn, al, ac):
    npa = np.arange
    npl = np.linspace
    sh_in = inn.shape
    sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1)
    itp = interp.RectBivariateSpline(npl(0., sh_ou[0], sh_in[0]),
                                     npl(0., sh_ou[1], sh_in[1]),
                                     inn, kx=1, ky=1)
    ou = itp(npa(sh_ou[0]), npa(sh_ou[1]))
    return ou


def interpolate_tie(inn, al, ac, profile=False):
    npl = np.linspace
    sh_in = inn.shape
    if profile is True:
        sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1, sh_in[2])
    else:
        sh_ou = ((sh_in[0] - 1) * al + 1, (sh_in[1] - 1) * ac + 1)

    xv = npl(0., sh_in[0] - 1, sh_ou[0])
    yv = npl(0., sh_in[1] - 1, sh_ou[1])
    xflt, yflt = np.meshgrid(xv, yv, sparse=False, indexing='ij', copy=True)

    xlid = np.floor(xflt).astype(np.int)  # x lower index
    ylid = np.floor(yflt).astype(np.int)  # y lower index

    xuid = (xlid + 1).clip(0, sh_in[0] - 1)  # x upper index
    yuid = (ylid + 1).clip(0, sh_in[1] - 1)  # y upper index

    wx = xuid - xflt
    wy = yuid - yflt
    w00 = wx * wy  # xlo,ylo
    w10 = (1 - wx) * wy  # xup,ylo
    w01 = wx * (1 - wy)  # xlo,yup
    w11 = (1 - wx) * (1 - wy)  # xlo,yup

    if profile is True:
        out = inn[(xlid, ylid)] * w00[:, :, np.newaxis]
        out += inn[(xuid, ylid)] * w10[:, :, np.newaxis]
        out += inn[(xlid, yuid)] * w01[:, :, np.newaxis]
        out += inn[(xuid, yuid)] * w11[:, :, np.newaxis]
    else:
        out = inn[(xlid, ylid)] * w00
        out += inn[(xuid, ylid)] * w10
        out += inn[(xlid, yuid)] * w01
        out += inn[(xuid, yuid)] * w11
    return masked2filled(out)


def interpolate_tie_profile(inn, al, ac):
    return interpolate_tie(inn, al, ac, profile=True)


def get_pressure(ds, ac, al, st=(1, 1)):
    prs = ds.variables['sea_level_pressure'][:] * 1.
    return masked2filled(interpolate_tie(prs, al, ac)[::st[0], ::st[1]])


def get_temperature(ds, ac, al, st=(1, 1)):
    # this is just the tenmperatur at the lowest
    # pressure profile, wich could be below
    # the surface. Use get_surf_temperature
    # instead
    # tmp = ds.variables['atmospheric_temperature_profile'][:, :, -1].squeeze() * 1.
    tmp = ds.variables['atmospheric_temperature_profile_pressure_level_20'][:, :].squeeze() * 1.
    return masked2filled(interpolate_tie(tmp, al, ac)[::st[0], ::st[1]])


def get_wind(ds, ac, al, st=(1, 1)):
    wsp_comp = np.empty((ds.dimensions['tp_y'].size, ds.dimensions['tp_x'].size, 2))
    for k in range(2):
        variable = 'horizontal_wind_vector_' + str(k + 1)
        wsp_comp[:, :, k] = (ds.variables[variable][:, :]) * 1.

    wsp = (wsp_comp[:, :, :] ** 2).sum(axis=2) ** 0.5
    return masked2filled(interpolate_tie(wsp, al, ac)[::st[0], ::st[1]])


def get_tcw(ds, ac, al, st=(1, 1)):
    tcw = ds.variables['total_columnar_water_vapour'][:] * 1
    return masked2filled(interpolate_tie(tcw, al, ac)[::st[0], ::st[1]])


def get_lsmask(ds, st=(1, 1)):
    bit = flagname_to_flagbit(ds, datatype='l1')['land']
    lsm = (ds.variables['quality_flags'][:, :] & 2 ** bit) == 2 ** bit
    return masked2filled(lsm[::st[0], ::st[1]], False)


def get_band_saturation(ds, bn, st=(1, 1)):
    bit = flagname_to_flagbit(ds, datatype='l1')['saturated_M%02i' % bn]
    lsm = (ds.variables['quality_flags'][:, :] & 2 ** bit) == 2 ** bit
    return masked2filled(lsm[::st[0], ::st[1]], True)


def get_latitude(ds, ac, al, st=(1, 1)):
    # assumes FR dataset
    lat = ds.variables['latitude'][:] * 1.
    return masked2filled(interpolate_tie(lat, al, ac)[::st[0], ::st[1]])


def get_lat_tp(ds, st=(1, 1)):
    # TP grid
    lat = ds.variables['latitude'][:] * 1.
    return masked2filled(lat[::st[0], ::st[1]])


def get_longitude(ds, ac, al, st=(1, 1)):
    # assumes FR dataset
    lon = ds.variables['longitude'][:] * 1.
    re = np.sin(lon * np.pi / 180.)
    im = np.cos(lon * np.pi / 180.)
    re_out = interpolate_tie(re, al, ac)[::st[0], ::st[1]]
    im_out = interpolate_tie(im, al, ac)[::st[0], ::st[1]]
    return masked2filled(np.arctan2(re_out, im_out) * 180. / np.pi)


def get_lon_tp(ds, st=(1, 1)):
    # TP grid
    lon = ds.variables['longitude'][:] * 1.
    re = np.sin(lon[::st[0], ::st[1]] * np.pi / 180.)
    im = np.cos(lon[::st[0], ::st[1]] * np.pi / 180.)
    return masked2filled(np.arctan2(re, im) * 180. / np.pi)


def get_geometry(ds, ac, al, st=(1, 1)):
    out = {}
    for k in ds.variables:
        if k == 'SZA' or k == 'sun_zenith':
            out['SZA'] = ds.variables[k][:] * 1.
        if k == 'SAA' or k == 'sun_azimuth':
            out['SAA'] = ds.variables[k][:] * 1.
        if k == 'OZA' or k == 'view_zenith':
            out['OZA'] = ds.variables[k][:] * 1.
        if k == 'OAA' or k == 'view_azimuth':
            out['OAA'] = ds.variables[k][:] * 1.
    for k in out:
        out[k] = masked2filled(interpolate_tie(out[k], al, ac)[::st[0], ::st[1]])
    out['ADA'] = azi2azid(out['SAA'], out['OAA'])

    return out


def get_cwl(ds, bn, st=(1, 1)):
    # l0 = (ds.variables['lambda0'][bn - 1, :] * 1.).squeeze()
    l0 = (ds.variables['lambda0_band_' + str(bn)][:] * 1.).squeeze()
    # di = ds.variables['detector_index'][:] * 1
    # return masked2filled(l0[di][::st[0], ::st[1]])
    return masked2filled(l0[::st[0], ::st[1]])


def get_solar_flux(ds, bn, st=(1, 1)):
    # sf = (ds.variables['solar_flux'][bn - 1, :] * 1.).squeeze()
    sf = (ds.variables['solar_flux_band_' + str(bn)][:] * 1.).squeeze()
    # di = ds.variables['detector_index'][:] * 1
    # return masked2filled(sf[di][::st[0], ::st[1]])
    return masked2filled(sf[::st[0], ::st[1]])


def get_band(ds, bn, st=(1, 1)):
    variable = 'M%02i_radiance' % bn
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


def add_cloud_margin(cm, b=2):
    """
    """
    kk = np.ones((2 * b + 1, 2 * b + 1))
    cs = signal.convolve2d(cm + 0, kk + 0)
    out = ((cs[b:-b, b:-b] > 0) | cm)
    return out


def get_surf_temperature(s3, ac, al, st=(1, 1)):
    tmp = get_temperature_profile(s3, ac, al, st)
    plv = get_pressure_profile()
    alt = get_altitude(s3, st)
    slp = get_pressure(s3, ac, al, st)
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
    # below 1000 hPa:
    xdx = uindx == len(plv)
    stm[xdx] = tmp[:, :, -1][xdx]
    # residual is nan
    stm[prs < 0] = np.nan
    stm[prs > 1050] = np.nan
    stm[~np.isfinite(prs)] = np.nan
    try:
        stm[prs.mask] = np.nan
    except AttributeError:
        pass
    return stm


def test_bit(f, b):
    return (f & 2 ** b) == 2 ** b


def get_l2_ocean_aot_fail(ds, st=(1, 1)):
    # variable = 'WP_QS'
    variable = 'WP_QS_lsb'
    flagbits = flagname_to_flagbit(ds, datatype='l2', target=variable)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    wpqs = tb('MEGLINT') | tb('HIGHGLINT') | tb('WHITECAPS') | tb('SEA_ICE')
    variable = 'WP_PC'
    flagbits = flagname_to_flagbit(ds, datatype='l2', target=variable)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    wppc = tb('T865_FAIL')
    variable = 'CC'
    flagbits = flagname_to_flagbit(ds, datatype='l2', target=variable)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    cla = tb('CLOUD') | tb('CLOUD_AMBIGUOUS') | tb('CIRRUS')
    variable = 'CO'
    flagbits = flagname_to_flagbit(ds, datatype='l2', target=variable)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    comm = tb('INVALID') | tb('SUSPECT') | tb('HISOLZEN') | (~tb('DO_WATER'))
    inv = wpqs | wppc | cla | comm
    return inv


def flagname_to_flagbit(ds, datatype='l2', target='common_flags'):
    """
    valid l2 targets: common_flags, wqsf,cqsf,lqsf
    """
    if datatype == 'l1':
        target = 'quality_flags'
    meanings = ds.variables[target].flag_meanings.split()
    masks = ds.variables[target].flag_masks
    flgs = {n: int(b).bit_length() - 1 for n, b in zip(meanings, masks)}
    return flgs


def get_temperature_profile(ds, ac, al, st=(1, 1)):
    tie = np.empty((ds.dimensions['tp_y'].size, ds.dimensions['tp_x'].size, 20))
    for k in range(20):
        variable = 'atmospheric_temperature_profile_pressure_level_' + str(k + 1)
        tie[:, :, k] = (ds.variables[variable][:, :]) * 1.
    # ac = ds.ac_subsampling_factor
    # al = ds.al_subsampling_factor
    out = interpolate_tie_profile(tie, al, ac)
    return out[::st[0], ::st[1], :]
    # return tie[::st[0], ::st[1], :]


def get_pressure_profile():
    return np.array([300, 350, 400, 450, 500, 550, 600, 650, 700, 750,
                     775, 800, 825, 850, 875, 900, 925, 950, 975, 1000]) * 1.0


def get_idepix_masks(ds, st=(1, 1)):
    fname = 'pixel_classif_flags'
    variable = 'pixel_classif_flags'
    flagbits = flagname_to_flagbit(ds, datatype='idepix', target=fname)
    flg = ds.variables[variable][:, :][::st[0], ::st[1]]

    def tb(fn):
        bit = flagbits[fn]
        return test_bit(flg, bit)

    mask_all = tb('IDEPIX_INVALID') | tb('IDEPIX_CLOUD') | tb('IDEPIX_CLOUD_AMBIGUOUS') | tb(
        'IDEPIX_CLOUD_BUFFER') | tb(
        'IDEPIX_CLOUD_SURE') | tb('IDEPIX_GLINT_RISK')

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


def get_relevant_l1l2_data(ds_l1, config, cmi=False):
    """
    """
    bnds = [13, 14, 15, ]
    stride = config['PROCESSING']['stride']
    # tie points subsampling factor: use value for MERIS RR (16), but
    # make it work also for very small subsets (e.g. 3x3), test purposes.
    # todo: make more flexible if needed, e.g. for FR
    # the original values are global attributes in MERIS L1b netcdf files (e.g. tie_meteo.nc):
    ac_subsampling_factor = 16
    al_subsampling_factor = 16
    if ds_l1.dimensions['x'].size < 16 or ds_l1.dimensions['y'].size < 16:
        ac_subsampling_factor = 1
        al_subsampling_factor = 1

    # lat/lon
    # todo: make configurable if we need full res, TP or both
    l1_lon = get_longitude(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    l1_lat = get_latitude(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    l1_lon_tp = get_lon_tp(ds_l1, stride)
    l1_lat_tp = get_lat_tp(ds_l1, stride)

    rad = {k: get_band(ds_l1, k, stride) / get_solar_flux(ds_l1, k, stride) for k in bnds}
    sat = {k: get_band_saturation(ds_l1, k, stride) for k in range(1, 16)}
    geo = get_geometry(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    prs = height2press(get_altitude(ds_l1, stride))
    lsm = get_lsmask(ds_l1, stride)

    cb = config['GENERAL']['coast_border'] // ((stride[0] + stride[1]) // 2)
    cb = max(1, cb)
    sct = simple_coast(lsm, cb)
    tem_ = get_temperature(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    tem = get_surf_temperature(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    wsp = get_wind(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    tcw = get_tcw(ds_l1, ac_subsampling_factor, al_subsampling_factor, stride)
    amf = 1. / np.cos(geo['SZA'] * np.pi / 180.) + 1. / np.cos(geo['OZA'] * np.pi / 180.)
    aot_l2 = np.zeros_like(tcw) + config['PROCESSING']['ocean_aot_fallback']

    # radiance ok
    mask = np.ones_like(lsm)
    for xx in rad:
        mask = mask & np.isfinite(rad[xx])
    for xx in sat:
        mask = mask & ~sat[xx]

    # data ok for processing
    dok = np.ones_like(mask)
    dok = dok & mask
    idepix_masks = {}
    if cmi:
        idepix_masks = get_idepix_masks(ds_l1, stride)
        dok = dok & ~idepix_masks['mask_all'].filled(True)

    ds_l1.close()
    dok = dok & (geo['SZA'] <= config['PROCESSING']['max_solar_zenith'])
    for b in rad:
        dok = dok & (rad[b] >= config['PROCESSING']['min_norm_rad'])

    # data for land processing
    # dfl = np.ones_like(lsm)  # TEST: assume all is land and valid
    dfl = dok & (lsm | (sct & (rad[14] > config['PROCESSING']['min_coast_norm_rad'])))

    # data for ocean processing
    dfo = dok & ~dfl

    # eventally fill the land processing with aot land background
    aot_l2[dfl] = config['PROCESSING']['land_aot_fallback']

    # set surface type flag:
    stfl = np.zeros_like(mask, dtype=np.int16)
    if cmi:
        # land
        stfl[np.where(idepix_masks['land'] == 1)] = 1
        # ocean
        stfl[np.where(idepix_masks['land'] != 1)] = 2
        # sea ice
        stfl[np.where(idepix_masks['seaice'] == 1)] = 4
        # cloud
        stfl[np.where(idepix_masks['cloud'] == 1)] = 8
        # undefined:
        stfl[np.where(idepix_masks['invalid'] == 1)] = 16
        # undefined remainders??
        stfl[np.where(stfl == 0)] = 16

    return {'lon': l1_lon, 'lat': l1_lat,
            'lon_tp': l1_lon_tp, 'lat_tp': l1_lat_tp,
            'geo': geo, 'amf': amf,
            'rad': rad, 'sat': sat,
            'lsm': lsm, 'sct': sct, 'cld': idepix_masks['mask_all'], 'stfl': stfl,
            'prs': prs, 'wsp': wsp, 'tem': tem, 'tem_': tem_, 'tcw': tcw, 'aot': aot_l2,
            'msk': mask, 'dok': dok, 'nnn': dok.size, 'dfl': dfl, 'dfo': dfo,
            }


def write_to_ncdf_cci(outname, dct, start_date_string, stop_date_string):
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
        nc_out.setncattr('source', 'MERIS RR L1B 4th Reprocessing')
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

            # if key == 'lat_tp':
            #     # Latitude TP
            #     var_lat = nc_out.createVariable('latitude', dct[key].dtype, ('tp_y', 'tp_x'), zlib=True)
            #     var_lat[:] = dct[key]
            #     var_lat.setncattr('offset_x', 0.5)
            #     var_lat.setncattr('offset_y', 0.5)
            #     var_lat.setncattr('subsampling_x', 0.5)
            #     var_lat.setncattr('subsampling_y', 0.5)
            #
            # if key == 'lon_tp':
            #     # Longitude TP
            #     var_lon = nc_out.createVariable('longitude', dct[key].dtype, ('tp_y', 'tp_x'), zlib=True)
            #     var_lon[:] = dct[key]
            #     var_lon.setncattr('offset_x', 0.5)
            #     var_lon.setncattr('offset_y', 0.5)
            #     var_lon.setncattr('subsampling_x', 0.5)
            #     var_lon.setncattr('subsampling_y', 0.5)

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
