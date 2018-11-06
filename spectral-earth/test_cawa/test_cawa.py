from __future__ import print_function

import numpy as np
from netCDF4 import Dataset

import interpolate_mc as intmc
import interpolators_pure_python

interpolators = interpolators_pure_python

def checklut(x):
    if getattr(interpolators, 'checklut', None) is not None:
        interpolators.checklut(x)
    else:
        if not isinstance(x, np.ndarray):
            print('not numpy array', type(x))
        if (x.dtype != np.float64) and (x.dtype != np.float32):
            print('not float')


def is_monotonically_increasing(v):
    for i, e in enumerate(v[1:], 1):
        if e <= v[i - 1]:
            return False
    return True


def is_monotonically_decreasing(v):
    for i, e in enumerate(v[1:], 1):
        if e >= v[i - 1]:
            return False
    return True


def generate_interpol_to_index(xx):
    return lambda x, xx=np.array(xx, order='F'): interpolators.linint2index(x, xx)


def generate_interpol_to_index_rev(xx):
    return lambda x, xx=-np.array(xx, order='F'): interpolators.linint2index(-x, xx)


def check_luts(luts):
    for ilut, lut in enumerate(luts):
        print('checking Lut %i' % ilut)
        _ = checklut(lut)


def check_axes(axes):
    for idim, dim in enumerate(axes):
        print('checking Axes %i' % idim)
        _ = checklut(dim)


def lut2func(luts, axes, verbose=True, check_nans=False, vectorial=False):
    """
    Is actually a wrapper around an n-dimensional linear interpolation
    from R^m --> R^n

    Input:

        lut : tupel of n  m-dimensional np-arrays
              or an m+1 dimensional np-array
        axes: tupel of m 1d-numpy arrays

        check nans works only on tuple luts!

    If vectorial is set, map_cordinates is used

    Returns a function

    """

    # 1. check validity of input:
    if isinstance(luts, tuple):
        if verbose:
            check_luts(luts)
        for ilut, lut in enumerate(luts):
            if not isinstance(lut, np.ndarray):
                print(ilut, ' element is not an Numpy ndarray')
                return None
            if ilut == 0:
                lut_dtype = lut.dtype
            else:
                if lut.dtype != lut_dtype:
                    print('Lut ', ilut, ' dtype does not agree with Lut 0 dtype')
                    print(lut.dtype, '<>', lut_dtype)
                    return None

        shap = luts[0].shape
        ndim = luts[0].ndim

        for ilut, lut in enumerate(luts):
            if shap != lut.shape:
                print(ilut, ' ndarray has not the shape of ', shap)

    elif isinstance(luts, np.ndarray):
        if verbose:
            check_luts((luts,))
        lut_dtype = luts.dtype
        shap = luts.shape[:-1]
        ndim = luts.ndim - 1
    else:
        print('Input is neither an tupel of ndarrays nor an ndarray')
        return None

    if not isinstance(axes, tuple):
        print('Axes is not a tuple')
        return None

    if len(axes) != ndim:
        print('Number of elements of axes (%i)' % len(axes))
        print('is not equal the number of dimensions (%i)' % ndim)
        return None

    # 2. analyze axes
    #
    axes_imi = []
    axes_imd = []
    for idim, dim in enumerate(axes):
        if not isinstance(dim, np.ndarray):
            print('Axes %i is not an Numpy ndarray' % idim)
            return None
        if not dim.ndim == 1:
            print('Axes %i is not an 1D Numpy ndarray' % idim)
            return None
        if len(dim) != shap[idim]:
            print('Axes %i does not agree with the shape of LUT' % idim)
            print('Axes %i has %i elements but LUT needs %i.' % (idim, len(dim), shap[idim]))
            return None
        axes_imi.append(is_monotonically_increasing(dim))
        axes_imd.append(is_monotonically_decreasing(dim))
        if not (axes_imi[-1] or axes_imd[-1]):
            print('Axes %i is neither monotonically increasing nor decreasing' % idim)
            print(dim)
            print('Re-organize your data!')
            return None

    # 3. generate interpolating function
    if vectorial is True:
        if check_nans is True:
            print("WARNING: Don't know if map_cordinates like NaNs")

        if isinstance(luts, tuple):
            itps = [intmc.interpolate_n(_lut, axes) for _lut in luts]
        elif isinstance(luts, np.ndarray):
            sss = luts.shape
            itps = [intmc.interpolate_n(luts.T[i].T, axes) for i in range(sss[-1])]
        function = lambda x: np.array([_.recall(x) for _ in itps]).T

    else:
        if isinstance(luts, tuple):
            if check_nans is True:
                itps = [interpolators.generate_nan_itp(lut) for lut in luts]
            else:
                itps = [interpolators.generate_itp(lut) for lut in luts]
        elif isinstance(luts, np.ndarray):
            if check_nans is True:
                print("WARNING: Check_nans works only on tuple luts")
                print('Re-organize your data!')
                return None
            itps = interpolators.generate_itp_pn(luts)

        # 3. generate axes 1d interpolators
        axes_max = []
        axes_min = []
        axes_int = []
        for idim, dim in enumerate(axes):

            if not axes_imi[idim]:
                # print 'inverting dimension'
                axes_int.append(generate_interpol_to_index_rev(dim.astype(lut_dtype)))
                axes_max.append(dim[0])
                axes_min.append(dim[-1])
            else:
                axes_int.append(generate_interpol_to_index(dim.astype(lut_dtype)))
                axes_max.append(dim[-1])
                axes_min.append(dim[0])

        if isinstance(itps, list):
            def function(wo):
                woo = np.array([axe(w) for axe, w in zip(axes_int, wo)], order='F', dtype=lut_dtype)
                out = np.array([itp(woo) for itp in itps], order='F')
                return out
        else:
            def function(wo):
                woo = np.array([axe(w) for axe, w in zip(axes_int, wo)], order='F', dtype=lut_dtype)
                out = itps(woo)
                return out

    return function


def jlut2func(dum):
    func = lut2func(dum['lut'], dum['axes'])

    def jfunc(woo):
        # return func(woo).reshape(dum['ny'],dum['nx'],order='F')
        return np.array(func(woo).reshape(dum['ny'], dum['nx']), order='F')

    return jfunc


# def test_cawa_meris_ocean():
#
#     with Dataset('ocean_core_meris.nc4', 'r') as ncds:
#         # get the full lut
#         lut = np.array(ncds.variables['lut'][:], order='F')
#         # print('self.lut[0][0][0][0][0][0][0]: ', lut[0][0][0][0][0][0][0])
#         # print('self.lut[3][3][8][0][8][1][2]: ', lut[3][3][8][0][8][1][2])
#         # print('self.lut[3][3][8][0][8][1][2]: ', lut[3][3][8][0][8][1][2])
#         # print('self.lut[2][4][6][3][2][7][1]: ', lut[2][4][6][3][2][7][1])
#         # print('self.lut[4][2][5][4][1][7][2]: ', lut[4][2][5][4][1][7][2])
#         # print('self.lut[1][5][4][5][3][6][0]: ', lut[1][5][4][5][3][6][0])
#         # print('self.lut[0][5][0][8][6][3][0]: ', lut[0][5][0][8][6][3][0])
#         # print('self.lut[2][4][1][8][8][1][2]: ', lut[2][4][1][8][8][1][2])
#         # print('self.lut[5][1][2][7][0][8][2]: ', lut[5][1][2][7][0][8][2])
#         # print('self.lut[3][0][3][6][7][2][1]: ', lut[3][0][3][6][7][2][1])
#         # print('self.lut[5][5][10][10][8][8][2]: ', lut[5][5][10][10][8][8][2])
#         jlut = np.array(ncds.variables['jlut'][:], order='F')
#         # print('self.jlut[0][0][0][0][0][0][0]: ', jlut[0][0][0][0][0][0][0])
#         # print('self.jlut[3][3][8][0][8][1][2]: ', jlut[3][3][8][0][8][1][2])
#         # print('self.jlut[3][3][8][0][8][1][2]: ', jlut[3][3][8][0][8][1][2])
#         # print('self.jlut[2][4][6][3][2][7][1]: ', jlut[2][4][6][3][2][7][1])
#         # print('self.jlut[4][2][5][4][1][7][2]: ', jlut[4][2][5][4][1][7][2])
#         # print('self.jlut[1][5][4][5][3][6][0]: ', jlut[1][5][4][5][3][6][0])
#         # print('self.jlut[0][5][0][8][6][3][0]: ', jlut[0][5][0][8][6][3][0])
#         # print('self.jlut[2][4][1][8][8][1][2]: ', jlut[2][4][1][8][8][1][2])
#         # print('self.jlut[5][1][2][7][0][8][2]: ', jlut[5][1][2][7][0][8][2])
#         # print('self.jlut[3][0][3][6][7][2][1]: ', jlut[3][0][3][6][7][2][1])
#         # print('self.jlut[5][5][10][10][8][8][2]: ', jlut[5][5][10][10][8][8][2])
#         axes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['lut'].dimensions[:-1]])
#         # print('self.axes: ', str(axes))
#         # print('self.axes[0]: ', str(axes[0]))
#         # print('self.axes[1]: ', str(axes[1]))
#         # print('self.axes[2]: ', str(axes[2]))
#         # print('self.axes[3]: ', str(axes[3]))
#         # print('self.axes[4]: ', str(axes[4]))
#         # print('self.axes[5]: ', str(axes[5]))
#         jaxes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['jlut'].dimensions[:-1]])
#         print('self.jaxes: ', str(jaxes))
#         # print('self.jaxes[0]: ', str(jaxes[0]))
#         # print('self.jaxes[1]: ', str(jaxes[1]))
#         # print('self.jaxes[2]: ', str(jaxes[2]))
#         # print('self.jaxes[3]: ', str(jaxes[3]))
#         # print('self.jaxes[4]: ', str(jaxes[4]))
#         # print('self.jaxes[5]: ', str(jaxes[5]))
#         ny_nx = np.array(ncds.variables['jaco'][:])
#         # print('self.ny_nx]: ', str(ny_nx))
#         wb = ncds.getncattr('win_bnd').split(',')
#         ab = ncds.getncattr('abs_bnd').split(',')
#         # print('self.wb]: ', str(wb))
#         # print('self.ab]: ', str(ab))
#
#     # generic forward
#     _forward = lut2func(lut, axes)
#     # generic jacobian
#     # import lut2jacobian_lut
#     _jacobian = jlut2func({'lut': jlut,
#                            'axes': jaxes,
#                            'ny': ny_nx[0],
#                            'nx': ny_nx[1]})
#
#     # global predefinition of input for speed reasons
#     xaa = np.zeros(3)
#     par = np.zeros(3)
#     mes = np.zeros(len(wb) + len(ab))
#
#     # local predefine inp for speed
#     inp = np.zeros(6)
#
#     def forward(woo, geo):
#         """
#         Input:
#             woo: state (wvc aot wsp)
#             geo: azi vie suz
#             aot is aot at winband [0]
#             wvc is sqrt of wvc
#
#         Output:
#             normalized radiances at winbands
#             -np.log(effective_transmission)/sqrt(amf) at absbands
#
#             effective_transmission= L_toa/L_0
#             with L_0 is normalized radiance without water vapor
#
#         """
#         inp[:3], inp[3:] = woo, geo
#         print('forward input: ', str(inp))
#         print('forward woo: ', str(woo))
#         print('forward geo: ', str(geo))
#         print('_forward(inp): ', str(_forward(inp)))
#         return _forward(inp)
#
#     self_forward = forward
#
#     def jforward(woo, geo):
#         """
#         as forward, but returns jacobian
#         output must be limited to the first three elements (the state and not the geometry)
#         """
#         inp[:3], inp[3:] = woo, geo
#         print('Jacobian input: ', str(inp))
#         print('Jacobian woo: ', str(woo))
#         print('Jacobian geo: ', str(geo))
#         print('_jacobian(inp)[:, :3]: ', str(_jacobian(inp)[:, :3]))
#         return _jacobian(inp)[:, :3]
#
#     self_jforward = jforward
#
#     # min_state
#     a = np.array([axes[i].min() for i in range(3)])
#     print('a: ', str(a))
#     # max_state
#     b = np.array([axes[i].max() for i in range(3)])
#     print('b: ', str(b))
#     import optimal_estimation_cawa as oe
#     inverter = oe.my_inverter(self_forward, a, b, jaco=self_jforward)
#
#     # finaly preset SE
#     sew = [0.0001 for i in wb]
#     sea = [0.001 for i in ab]
#     self_SE = np.diag(sew + sea)
#     print('sew: ', str(sew))
#     print('sea: ', str(sea))
#     print('sew + sea: ', str(sew + sea))
#     print('self.SE: ', str(self_SE))
#
#     sa = np.zeros(shape=(3, 3))
#     sa[0][0] = 8.
#     sa[1][1] = 0.1
#     sa[2][2] = 25.
#     print('sa: ', str(sa))
#
#     mes = [0.19034228, 0.18969933, 0.21104884]
#     par = [135.61277771, 28.43509483, 61.43579102]
#     xaa = [5.47722558, 0.15, 7.5]
#
#     res = inverter(mes, fparams=par, jparams=par, se=self_SE, sa=sa, xa=xaa, method=2,
#                    full='fast', maxiter=3)
#     print('res: ', str(res))
#
#     print(' data[res]:', str(res))
#     print(' data[tcwv]:', str(res.x[0] ** 2))
#     print(' data[aot]:', str(res.x[1]))
#     print(' data[wsp]:', str(res.x[2]))

def test_cawa_land_meris():

    with Dataset('land_core_meris.nc4', 'r') as ncds:
        # get the full lut
        lut = np.array(ncds.variables['lut'][:], order='F')
        jlut = np.array(ncds.variables['jlut'][:], order='F')
        axes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['lut'].dimensions[:-1]])
        jaxes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['jlut'].dimensions[:-1]])
        ny_nx = np.array(ncds.variables['jaco'][:])
        wb = ncds.getncattr('win_bnd').split(',')
        ab = ncds.getncattr('abs_bnd').split(',')

    # generic forward
    # just to compare with Java: invert prs axis to ascending (prs is completely wrong in Meris Land LUT!)
    # TODO: inform RP !!
    # axes[4][0] = 1./axes[4][0]
    # axes[4][1] = 1./axes[4][1]
    # axes[4][2] = 1./axes[4][2]
    # jaxes[4][0] = 1./jaxes[4][0]
    # jaxes[4][1] = 1./jaxes[4][1]
    # jaxes[4][2] = 1./jaxes[4][2]
    axes[4][0] = -axes[4][0]
    axes[4][1] = -axes[4][1]
    axes[4][2] = -axes[4][2]
    jaxes[4][0] = -jaxes[4][0]
    jaxes[4][1] = -jaxes[4][1]
    jaxes[4][2] = -jaxes[4][2]

    _forward = lut2func(lut, axes)
    # generic jacobian
    # import lut2jacobian_lut
    _jacobian = jlut2func({'lut': jlut,
                           'axes': jaxes,
                           'ny': ny_nx[0],
                           'nx': ny_nx[1]})

    # local predefine inp for speed
    inp = np.zeros(9)

    def forward(woo, geo):
        """
        Input:
            woo: state (wvc aot wsp)
            geo: azi vie suz
            aot is aot at winband [0]
            wvc is sqrt of wvc

        Output:
            normalized radiances at winbands
            -np.log(effective_transmission)/sqrt(amf) at absbands

            effective_transmission= L_toa/L_0
            with L_0 is normalized radiance without water vapor

        """
        inp[:3], inp[3:] = woo, geo
        # print('forward input: ', str(inp))
        # print('forward woo: ', str(woo))
        # print('forward geo: ', str(geo))
        # print('_forward(inp): ', str(_forward(inp)))
        return _forward(inp)

    self_forward = forward

    def jforward(woo, geo):
        """
        as forward, but returns jacobian
        output must be limited to the first three elements (the state and not the geometry)
        """
        inp[:3], inp[3:] = woo, geo
        print('Jacobian input: ', str(inp))
        print('Jacobian woo: ', str(woo))
        print('Jacobian geo: ', str(geo))
        print('_jacobian(inp)[:, :3]: ', str(_jacobian(inp)[:, :3]))
        return _jacobian(inp)[:, :3]

    self_jforward = jforward

    # min_state
    a = np.array([axes[i].min() for i in range(3)])
    print('a: ', str(a))
    # max_state
    b = np.array([axes[i].max() for i in range(3)])
    print('b: ', str(b))
    import optimal_estimation_cawa as oe
    inverter = oe.my_inverter(self_forward, a, b, jaco=self_jforward)

    # finaly preset SE
    sew = [0.0001 for i in wb]
    sea = [0.001 for i in ab]
    self_SE = np.diag(sew + sea)
    print('sew: ', str(sew))
    print('sea: ', str(sea))
    print('sew + sea: ', str(sew + sea))
    print('self.SE: ', str(self_SE))

    sa = np.zeros(shape=(3, 3))
    sa[0][0] = 20.
    sa[1][1] = 1.
    sa[2][2] = 1.
    print('sa: ', str(sa))

    mes = [0.19290966,  0.19140355,  0.14358414]
    par = [1.00000000e-01,   -1.01325000e+01,   3.03000000e+02,
           4.48835754e+01, 2.70720062e+01,   5.29114494e+01]
    xaa = [5.47722558,  0.13,        0.13]

    res = inverter(mes, fparams=par, jparams=par, se=self_SE, sa=sa, xa=xaa, method=2,
                   full='fast', maxiter=3)
    print('res: ', str(res))

    print(' data[res]:', str(res))
    print(' data[tcwv]:', str(res.x[0] ** 2))
    print(' data[aot]:', str(res.x[1]))

# def test_cawa_modis_ocean():
#
#     with Dataset('ocean_core_modis_aqua.nc4', 'r') as ncds:
#         # get the full lut
#         lut = np.array(ncds.variables['lut'][:], order='F')
#         # print('self.lut[0][0][0][0][0][0][0]: ', lut[0][0][0][0][0][0][0])
#         # print('self.lut[3][3][8][0][8][1][2]: ', lut[3][3][8][0][8][1][2])
#         # print('self.lut[3][3][8][0][8][1][2]: ', lut[3][3][8][0][8][1][2])
#         # print('self.lut[2][4][6][3][2][7][1]: ', lut[2][4][6][3][2][7][1])
#         # print('self.lut[4][2][5][4][1][7][2]: ', lut[4][2][5][4][1][7][2])
#         # print('self.lut[1][5][4][5][3][6][0]: ', lut[1][5][4][5][3][6][0])
#         # print('self.lut[0][5][0][8][6][3][0]: ', lut[0][5][0][8][6][3][0])
#         # print('self.lut[2][4][1][8][8][1][2]: ', lut[2][4][1][8][8][1][2])
#         # print('self.lut[5][1][2][7][0][8][2]: ', lut[5][1][2][7][0][8][2])
#         # print('self.lut[3][0][3][6][7][2][1]: ', lut[3][0][3][6][7][2][1])
#         # print('self.lut[5][5][10][10][8][8][2]: ', lut[5][5][10][10][8][8][2])
#         jlut = np.array(ncds.variables['jlut'][:], order='F')
#         # print('self.jlut[0][0][0][0][0][0][0]: ', jlut[0][0][0][0][0][0][0])
#         # print('self.jlut[3][3][8][0][8][1][2]: ', jlut[3][3][8][0][8][1][2])
#         # print('self.jlut[3][3][8][0][8][1][2]: ', jlut[3][3][8][0][8][1][2])
#         # print('self.jlut[2][4][6][3][2][7][1]: ', jlut[2][4][6][3][2][7][1])
#         # print('self.jlut[4][2][5][4][1][7][2]: ', jlut[4][2][5][4][1][7][2])
#         # print('self.jlut[1][5][4][5][3][6][0]: ', jlut[1][5][4][5][3][6][0])
#         # print('self.jlut[0][5][0][8][6][3][0]: ', jlut[0][5][0][8][6][3][0])
#         # print('self.jlut[2][4][1][8][8][1][2]: ', jlut[2][4][1][8][8][1][2])
#         # print('self.jlut[5][1][2][7][0][8][2]: ', jlut[5][1][2][7][0][8][2])
#         # print('self.jlut[3][0][3][6][7][2][1]: ', jlut[3][0][3][6][7][2][1])
#         # print('self.jlut[5][5][10][10][8][8][2]: ', jlut[5][5][10][10][8][8][2])
#         axes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['lut'].dimensions[:-1]])
#         # print('self.axes: ', str(axes))
#         # print('self.axes[0]: ', str(axes[0]))
#         # print('self.axes[1]: ', str(axes[1]))
#         # print('self.axes[2]: ', str(axes[2]))
#         # print('self.axes[3]: ', str(axes[3]))
#         # print('self.axes[4]: ', str(axes[4]))
#         # print('self.axes[5]: ', str(axes[5]))
#         jaxes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['jlut'].dimensions[:-1]])
#         print('self.jaxes: ', str(jaxes))
#         # print('self.jaxes[0]: ', str(jaxes[0]))
#         # print('self.jaxes[1]: ', str(jaxes[1]))
#         # print('self.jaxes[2]: ', str(jaxes[2]))
#         # print('self.jaxes[3]: ', str(jaxes[3]))
#         # print('self.jaxes[4]: ', str(jaxes[4]))
#         # print('self.jaxes[5]: ', str(jaxes[5]))
#         ny_nx = np.array(ncds.variables['jaco'][:])
#         # print('self.ny_nx]: ', str(ny_nx))
#         wb = ncds.getncattr('win_bnd').split(',')
#         ab = ncds.getncattr('abs_bnd').split(',')
#         # print('self.wb]: ', str(wb))
#         # print('self.ab]: ', str(ab))
#
#     # generic forward
#     _forward = lut2func(lut, axes)
#     # generic jacobian
#     # import lut2jacobian_lut
#     _jacobian = jlut2func({'lut': jlut,
#                            'axes': jaxes,
#                            'ny': ny_nx[0],
#                            'nx': ny_nx[1]})
#
#     # global predefinition of input for speed reasons
#     xaa = np.zeros(3)
#     par = np.zeros(3)
#     mes = np.zeros(len(wb) + len(ab))
#
#     # local predefine inp for speed
#     inp = np.zeros(6)
#
#     def forward(woo, geo):
#         """
#         Input:
#             woo: state (wvc aot wsp)
#             geo: azi vie suz
#             aot is aot at winband [0]
#             wvc is sqrt of wvc
#
#         Output:
#             normalized radiances at winbands
#             -np.log(effective_transmission)/sqrt(amf) at absbands
#
#             effective_transmission= L_toa/L_0
#             with L_0 is normalized radiance without water vapor
#
#         """
#         inp[:3], inp[3:] = woo, geo
#         print('forward input: ', str(inp))
#         print('forward woo: ', str(woo))
#         print('forward geo: ', str(geo))
#         print('_forward(inp): ', str(_forward(inp)))
#         return _forward(inp)
#
#     self_forward = forward
#
#     def jforward(woo, geo):
#         """
#         as forward, but returns jacobian
#         output must be limited to the first three elements (the state and not the geometry)
#         """
#         inp[:3], inp[3:] = woo, geo
#         print('Jacobian input: ', str(inp))
#         print('Jacobian woo: ', str(woo))
#         print('Jacobian geo: ', str(geo))
#         print('_jacobian(inp)[:, :3]: ', str(_jacobian(inp)[:, :3]))
#         return _jacobian(inp)[:, :3]
#
#     self_jforward = jforward
#
#     # min_state
#     a = np.array([axes[i].min() for i in range(3)])
#     print('a: ', str(a))
#     # max_state
#     b = np.array([axes[i].max() for i in range(3)])
#     print('b: ', str(b))
#     import optimal_estimation_cawa as oe
#     inverter = oe.my_inverter(self_forward, a, b, jaco=self_jforward)
#
#     # finaly preset SE
#     sew = [0.0001 for i in wb]
#     sea = [0.001 for i in ab]
#     self_SE = np.diag(sew + sea)
#     print('sew: ', str(sew))
#     print('sea: ', str(sea))
#     print('sew + sea: ', str(sew + sea))
#     print('self.SE: ', str(self_SE))
#
#     sa = np.zeros(shape=(3, 3))
#     sa[0][0] = 8.
#     sa[1][1] = 0.1
#     sa[2][2] = 25.
#     print('sa: ', str(sa))
#
#     mes = [0.00320285,  0.15861148,  0.35012841,  0.27699689]
#     par = [118.03159332,   11.34500027,   61.64859772]
#     xaa = [5.47722558, 0.15, 7.5]
#
#     print(' mes:', str(mes))
#     print(' par:', str(par))
#     print(' xaa:', str(xaa))
#
#     res = inverter(mes, fparams=par, jparams=par, se=self_SE, sa=sa, xa=xaa, method=2,
#                    full='fast', maxiter=3)
#     print('res: ', str(res))
#
#     print(' data[res]:', str(res))
#     print(' data[tcwv]:', str(res.x[0] ** 2))
#     print(' data[aot]:', str(res.x[1]))
#     print(' data[wsp]:', str(res.x[2]))


def test_cawa_land_modis_terra():

    with Dataset('land_core_modis_aqua.nc4', 'r') as ncds:
        # get the full lut
        lut = np.array(ncds.variables['lut'][:], order='F')
        jlut = np.array(ncds.variables['jlut'][:], order='F')
        axes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['lut'].dimensions[:-1]])
        jaxes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['jlut'].dimensions[:-1]])
        ny_nx = np.array(ncds.variables['jaco'][:])
        # print('self.ny_nx]: ', str(ny_nx))
        wb = ncds.getncattr('win_bnd').split(',')
        ab = ncds.getncattr('abs_bnd').split(',')
        # print('self.wb]: ', str(wb))
        # print('self.ab]: ', str(ab))

    # generic forward
    _forward = lut2func(lut, axes)
    # generic jacobian
    # import lut2jacobian_lut
    _jacobian = jlut2func({'lut': jlut,
                           'axes': jaxes,
                           'ny': ny_nx[0],
                           'nx': ny_nx[1]})

    # global predefinition of input for speed reasons
    xaa = np.zeros(3)
    par = np.zeros(6)
    mes = np.zeros(len(wb) + len(ab))

    # local predefine inp for speed
    inp = np.zeros(9)

    def forward(woo, geo):
        """
        Input:
            woo: state (wvc aot wsp)
            geo: azi vie suz
            aot is aot at winband [0]
            wvc is sqrt of wvc

        Output:
            normalized radiances at winbands
            -np.log(effective_transmission)/sqrt(amf) at absbands

            effective_transmission= L_toa/L_0
            with L_0 is normalized radiance without water vapor

        """
        inp[:3], inp[3:] = woo, geo
        print('forward input: ', str(inp))
        print('forward woo: ', str(woo))
        print('forward geo: ', str(geo))
        print('_forward(inp): ', str(_forward(inp)))
        return _forward(inp)

    self_forward = forward

    def jforward(woo, geo):
        """
        as forward, but returns jacobian
        output must be limited to the first three elements (the state and not the geometry)
        """
        inp[:3], inp[3:] = woo, geo
        print('Jacobian input: ', str(inp))
        print('Jacobian woo: ', str(woo))
        print('Jacobian geo: ', str(geo))
        print('_jacobian(inp)[:, :3]: ', str(_jacobian(inp)[:, :3]))
        return _jacobian(inp)[:, :3]

    self_jforward = jforward

    # min_state
    a = np.array([axes[i].min() for i in range(3)])
    print('a: ', str(a))
    # max_state
    b = np.array([axes[i].max() for i in range(3)])
    print('b: ', str(b))
    import optimal_estimation_cawa as oe
    inverter = oe.my_inverter(self_forward, a, b, jaco=self_jforward)

    # finaly preset SE
    sew = [0.0001 for i in wb]
    sea = [0.001 for i in ab]
    self_SE = np.diag(sew + sea)
    print('sew: ', str(sew))
    print('sea: ', str(sea))
    print('sew + sea: ', str(sew + sea))
    print('self.SE: ', str(self_SE))

    sa = np.zeros(shape=(3, 3))
    sa[0][0] = 20.
    sa[1][1] = 1.
    sa[2][2] = 1.
    print('sa: ', str(sa))

    mes = [0.05588217,  0.06197434,  0.10987211,  0.33038937,  0.22174702]
    par = [1.00000000e-01, 1.00300000e+01, 3.03000000e+02, 1.18127800e+02, 2.59459991e+01, 6.23843994e+01]
    xaa = [5.47722558, 0.13, 0.13]

    print(' mes:', str(mes))
    print(' par:', str(par))
    print(' xaa:', str(xaa))

    res = inverter(mes, fparams=par, jparams=par, se=self_SE, sa=sa, xa=xaa, method=2,
                   full='fast', maxiter=3)
    print('res: ', str(res))

    print(' data[res]:', str(res))
    print(' data[tcwv]:', str(res.x[0] ** 2))
    print(' data[aot]:', str(res.x[1]))
    print(' data[wsp]:', str(res.x[2]))


if __name__ == '__main__':
    test_cawa_land_meris()
    test_cawa_land_modis_terra()
    # test_cawa_meris_ocean()
    # test_cawa_modis_ocean()
    pass
