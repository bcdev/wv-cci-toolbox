from __future__ import print_function

import interpolate_mc as intmc
import interpolators_pure_python
import numpy as np

try:
    import interpolators_fortran
except (ImportError, OSError):
    interpolators_fortran = None
try:
    import interpolators_numba_python
except (ImportError, OSError):
    interpolators_numba_python = None

# precedence (according to speed...) 
if interpolators_fortran is not None:
    interpolators = interpolators_fortran
elif interpolators_numba_python is not None:
    interpolators = interpolators_numba_python
else:
    interpolators = interpolators_pure_python
    print('Could neither import numba nor fortran libs. Sequential interpolation wll be slow')


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


def test(what='fortran'):
    import lut2func as l2f

    if what == 'purpy':
        l2f.interpolators = l2f.interpolators_pure_python
    elif what == 'numba':
        l2f.interpolators = l2f.interpolators_numba_python
    else:
        l2f.interpolators = l2f.interpolators_fortran

    # example for R^2-->R^3

    luta = np.arange(24, dtype=np.float).reshape(6, 4, order='C')
    lutb = np.arange(24, dtype=np.float).reshape(6, 4, order='F') ** 2
    lutc = np.sqrt(np.arange(24, dtype=np.float).reshape(6, 4, order='F'))
    luts = (luta, lutb, lutc)
    xx = np.array([3., 4., 6., 7., 9., 15.])
    yy = np.array([1., 5., 10, 15])[::-1]
    axes = (xx, yy)

    # Variant A: luts is a tuple of luts
    funca = l2f.lut2func(luts, axes)
    funcav = l2f.lut2func(luts, axes, vectorial=True)

    # Variant B: luts is a Nd Array, last dimension is dimension of result
    luts = np.array((luta, lutb, lutc), dtype=np.float32).transpose([1, 2, 0])
    print(luts.shape)
    funcb = l2f.lut2func(luts, axes)
    funcbv = l2f.lut2func(luts, axes, vectorial=True)

    print(luts.shape, len(axes))
    # sys.exit()

    nn = 10000
    import time
    pos = np.array([3.5, 11.])
    ppos = np.array([pos for i in range(nn)])
    a = time.time()
    print('VEC (%i): map-coordinates' % nn)
    for ff in (funcav, funcbv):
        a = time.time()
        erg = ff(ppos)
        print(erg[0], ': %.2f us' % (((time.time() - a)) / float(nn) * 1.e6))

    print('SEQ (%i): %s, map-coordinates, %s, map-coordinates ' % (nn, what, what))
    for ff in (funca, funcav, funcb, funcbv):
        a = time.time()
        for i in range(nn):
            _ = ff(pos)
        print(_, ': %.2f us' % ((time.time() - a) / float(nn) * 1.e6))


def test3to1():
    # example for R^3-->R^1
    import lut2func as l2f
    # from lut2func import lut2func
    lut3d = np.arange(3 * 4 * 5, dtype=np.float).reshape(3, 4, 5)
    x0 = np.array([3., 4., 6.])
    x1 = np.array([1., 4., 8., 10.])
    x2 = np.array([1., 2., 4., 8., 16.])
    func3d = l2f.lut2func((lut3d,), (x0, x1, x2))
    print(func3d([3.5, 5., 10.]))
    print(l2f.interpolators)


if __name__ == '__main__':
    # test('fortran')
    test('numba')
    # test('purpy')
    # test3to1()
    pass
