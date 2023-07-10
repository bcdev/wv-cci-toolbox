# Copyright (c) 2018
# Author(s): 
#   Rene Preusker <rene.preusker@gmail.com>
'''
This provides the basic core functionality for optimal estimation
Practically everything is based on: 
Rodgers C. D., Inverse methods for atmospheric sounding, volume 2 of 
Series on Atmospheric Oceanic and Planetary Physics. World Scientific, 2000.
'''
from collections import namedtuple as NT
import warnings
import json
import numpy as np
import typing
#from termcolor import colored

# not yet supported, thus typing testing with mypy is currently useless
NpArrayT = typing.Type[np.array]
#NpArrayT = typing.Iterable[float]

#JacobianParamT = typing.Union[None,typing.Tuple[typing.Any, NpArrayT]]
JacobianParamT = typing.Union[None,typing.Any]
GaussNewtonT = typing.Tuple[NpArrayT, NpArrayT, NpArrayT, NpArrayT]
Diagnostic1T = typing.Tuple[NpArrayT, NpArrayT, NpArrayT]
Diagnostic2T = typing.Tuple[NpArrayT, NpArrayT, NpArrayT, NpArrayT]
CallableT = typing.Callable
NpOrAnyT = typing.Union[None, NpArrayT]
FltOrNoneT = typing.Union[None, float]
InternalOptimizerT = typing.Tuple[NpArrayT, NpArrayT, bool, int, 
                                  NpArrayT, NpOrAnyT, NpOrAnyT, 
                                  FltOrNoneT, FltOrNoneT, FltOrNoneT,
                                  NpOrAnyT, NpOrAnyT,]



EPSIX = np.finfo(float).resolution
# to be larger than ULP
HH = EPSIX**(1./3.)
LMFAC = 4.
LMSTART = 0.1

RESULT = NT('result',
            'state '
            'jacobian '
            'convergence '
            'number_of_iterations '
            'retrieval_error_covariance '
            'gain '
            'averaging_kernel '
            'cost '
            'dof '
            'information_content '
            'retrieval_noise '
            'smoothing_error')

DEFAULTS = {'se': None, 'sa': None, 'xa': None, 'fg': None, 
            'eps': 0.01, 'jaco': None, 'll': None, 'ul': None,
            'mi': 10, 'fparam': None, 'jparam': None, 'clip': True,
            'full': True, 'dx': None, 'gnform':'n'}


class OeCoreError(Exception):
    pass
    #def __init__(self, message,errors):
        #super().__init__()
        #self.message = colored(message,'red')
class LinAlgError(OeCoreError):
    pass
class NonSquareMatrixError(OeCoreError):
    pass
class MissingInputError(OeCoreError):
    pass
class WrongInputError(OeCoreError):
    pass


def inverse(inn: NpArrayT) -> NpArrayT:
    return invert_np(inn)
    #return invert_pupy(inn)


def right_inverse(inn: NpArrayT) -> NpArrayT:
    return np.dot(inn.T, inverse(np.dot(inn, inn.T)))

def left_inverse(inn: NpArrayT) -> NpArrayT:
    return np.dot(inverse(np.dot(inn.T, inn)), inn.T)



def invert_np(inn: NpArrayT) -> NpArrayT:
    try: 
        out = np.linalg.inv(inn)
    except np.linalg.LinAlgError:
        # TODO
        # Is this always smart?
        # it breaks functional programming, testing .....
        #or better to flag ..
        #out=np.zeros_like(inn)        
        out = np.random.rand(*inn.shape)/np.nanmean(inn)
    return out

def invert_pupy(inn: NpArrayT) -> NpArrayT:
    '''
    pure python inversion of a 
    square matrix
    '''
    try: 
        l, u, p = lu_decomposition(inn)
        il = invert_lower(l)
        iu = invert_upper(u)
        out = iu.dot(il).dot(p)
    except LinAlgError:
        # TODO
        # Is this always smart?
        #or better to flag ..
        #out=np.zeros_like(inn)        
        out = np.random.rand(*inn.shape)/np.nanmean(inn)
    return out

def pivot(a: NpArrayT) -> NpArrayT:
    '''
    returns p , so that p*a is sorted
    '''
    nr, nc = a.shape
    if nr != nc:
        raise NonSquareMatrixError
    # start with unit matrix
    e = np.eye(nr)
    # sorting is with respect to abs value
    a_abs = np.abs(a)
    for j in range(nc):
        maxidx = a_abs[j:nr, j].argmax()+j
        #exchange rows
        e[[j, maxidx]] = e[[maxidx, j]]
    return e


def lu_decomposition(ua: NpArrayT) -> typing.Tuple[NpArrayT, NpArrayT, NpArrayT]:
    '''
    Decomposes A into P*A=L*U 
    L is lower
    U is upper 
    P pivot sorting
    '''
    nr, nc = ua.shape
    p = pivot(ua)
    a = p.dot(ua)
    l = np.eye(nr)
    u = np.zeros_like(a)
    for j in range(nc):
        for i in range(j+1):
            sm = (u[0:i, j] * l[i, 0:i]).sum()
            u[i, j] = a[i, j] - sm
        if np.isclose(u[j, j], 0.):
            raise LinAlgError
        for i in range(j, nr):
            sm = (u[0:j, j] * l[i, 0:j]).sum()
            l[i, j] = (a[i, j] - sm)/u[j, j]
    return l, u, p

def invert_lower(lo: NpArrayT) -> NpArrayT:
    '''
    Calculates the inverse of 
    a lower left triangular square matrix
    by forward substitution
    '''
    nr, nc = lo.shape
    ilo = np.zeros_like(lo)
    for j in range(nc):
        ilo[j, j] = 1./lo[j, j]
        if np.isclose(lo[j, j], 0.):
            raise LinAlgError
        for i in range(j+1, nr):
            sm = -(lo[i, :] * ilo[:, j]).sum()
            ilo[i, j] = sm / lo[i, i]
    return ilo

def invert_upper(up: NpArrayT) -> NpArrayT:
    return invert_lower(up.T).T

def approximate_jacobian_function(func: typing.Callable) -> typing.Callable:
    '''
    returns numerical jacobian function
    for a given function ff

    If func: R^N --> R^M
    then jac:  R^N --> R^(MxN)

    IMPORTANT:
        It is assumed, that *func* takes two 
        arguments: 
          x:      1d np-array (the state)
          p:      any kind of parameter object...
        Similar to func, *jac_func* takes two+one = three arguments.
        The first two are state and params, the third is delta_x, 
        a 1d np-array of the same size as x, used for the numerical 
        differentiation. If delta_x is not given, a 'smart' choice
        is taken.
    '''
    def jac_func(x:NpArrayT, params:JacobianParamT = None, dx:NpArrayT = None) -> NpArrayT:
        '''
        Jacobian function of %s
        '''
        nx = x.size
        if dx is None:
            #warnings.warn('No sensible delta_x given to jacobian. '
            #              'Results could be unpredictable!'
            #              , stacklevel=3)
            sign = np.sign(x)
            dx = np.where((sign*x) < HH, sign*HH, x*HH)
        for ix in range(nx):
            dxm = x*1.
            dxp = x*1.
            dxm[ix] = dxm[ix]-dx[ix]
            dxp[ix] = dxp[ix]+dx[ix]
            dyy = func(dxp, params)-func(dxm, params)
            # first run: now I know size of y
            if ix == 0:
                ny = dyy.size
                j = np.zeros((ny, nx), dtype=x.dtype) # zeilen zuerst, spalten später
            j[:, ix] = dyy[:]/dx[ix]/2.
        return j
    jac_func.__doc__ = jac_func.__doc__ % func.__name__
    return jac_func

def gauss_newton_increment_nform_a(x: NpArrayT, y: NpArrayT,
                           k: NpArrayT, xa: NpArrayT,
                           sei: NpArrayT, sai: NpArrayT,
                           se: NpArrayT, sa: NpArrayT,
                           gamma = 0.) -> GaussNewtonT:
    '''
    input:
        x: state vector
        y: forward(x) - measurement
        k: jacobian(x)
        xa: prior state 
        sei: inverse of measurement error co-variance
        sai: inverse of prior error co-variance
        se: measurement error co-variance
        sa: prior error co-variance
    return: 
        nx: optimal state respecting measurement and prior
                and coresponding uncertainties
                all for for the linear case
        inc_r: increment of x, 
        ret_err_cov_i: inverse of retrieval error covariance
        ret_err_cov:  retrieval error covariance

    Equation: 5.8 (with negative sign put into brackets)
    This variant is an  'n-form' (n = dimension of state)
    '''
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = (np.dot(kt_sei, k))
    ret_err_cov_i = (1.+gamma)*sai + kt_sei_k
    ret_err_cov = inverse(ret_err_cov_i)
    kt_sei_y = np.dot(kt_sei, y)
    sai_dx = np.dot(sai, xa - x)
    incr_x = np.dot(ret_err_cov, kt_sei_y - sai_dx)
    nx = x - incr_x
    return nx, incr_x, ret_err_cov_i, ret_err_cov


def gauss_newton_increment_nform_b(x: NpArrayT, y: NpArrayT,
                           k: NpArrayT, xa: NpArrayT,
                           sei: NpArrayT, sai: NpArrayT,
                           se: NpArrayT, sa: NpArrayT,
                           gamma = 0.) -> GaussNewtonT:
    '''
    input:
        x: state vector
        y: forward(x) - measurement
        k: jacobian(x)
        xa: prior state 
        sei: inverse of measurement error co-variance
        sai: inverse of prior error co-variance
        se: measurement error co-variance
        sa: prior error co-variance
    return: 
        nx: optimal state respecting measurement and prior
                and coresponding uncertainties
                all for for the linear case
        inc_r: increment of x, 
        ret_err_cov_i: inverse of retrieval error covariance
        ret_err_cov:  retrieval error covariance
    
    '''
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = (np.dot(kt_sei, k))
    #sei_k = np.dot(sei,k)
    sa_kt = np.dot(sa,k.T)
    sa_kt_sei = np.dot(sa_kt,sei)
    sa_kt_sei_k_one = np.dot(sa_kt_sei,k) + (1.+gamma)*np.identity(x.size)
    sa_kt_sei_k_one_i = inverse(sa_kt_sei_k_one) 
    sa_kt_sei_y_dx = np.dot(sa_kt_sei, y)-(xa - x)

    incr_x = np.dot(sa_kt_sei_k_one_i,sa_kt_sei_y_dx)
    nx = x - incr_x
    ret_err_cov_i = sai + kt_sei_k
    ret_err_cov = inverse(ret_err_cov_i)
    return nx, incr_x, ret_err_cov_i, ret_err_cov


def gauss_newton_increment_nform_c(x: NpArrayT, y: NpArrayT,
                           k: NpArrayT, xa: NpArrayT,
                           sei: NpArrayT, sai: NpArrayT,
                           se: NpArrayT, sa: NpArrayT,
                           gamma = 0.) -> GaussNewtonT:
    '''
    input:
        x: state vector
        y: forward(x) - measurement
        k: jacobian(x)
        xa: prior state 
        sei: inverse of measurement error co-variance
        sai: inverse of prior error co-variance
        se: measurement error co-variance
        sa: prior error co-variance
    return: 
        nx: optimal state respecting measurement and prior
                and coresponding uncertainties
                all for for the linear case
        inc_r: increment of x, 
        ret_err_cov_i: inverse of retrieval error covariance
        ret_err_cov:  retrieval error covariance
    
    '''
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = np.dot(kt_sei, k)
    ret_err_cov_i = (1.+gamma)*sai + kt_sei_k
    ret_err_cov = inverse(ret_err_cov_i)
    kt_sei_k_sai_kt_sei = np.dot(ret_err_cov,kt_sei)
    k_dx_y = np.dot(k, xa - x) +y
    nx = xa - np.dot(kt_sei_k_sai_kt_sei,k_dx_y)
    incr_x = x -nx

    return nx, incr_x, ret_err_cov_i, ret_err_cov

def gauss_newton_increment_mform_c(x: NpArrayT, y: NpArrayT,
                           k: NpArrayT, xa: NpArrayT,
                           sei: NpArrayT, sai: NpArrayT,
                           se: NpArrayT, sa: NpArrayT,
                           gamma = 0.) -> GaussNewtonT:
    '''
    input:
        x: state vector
        y: forward(x) - measurement
        k: jacobian(x)
        xa: prior state 
        sei: inverse of measurement error co-variance
        sai: inverse of prior error co-variance
        se: measurement error co-variance
        sa: prior error co-variance
    return: 
        nx: optimal state respecting measurement and prior
                and coresponding uncertainties
                all for for the linear case
        inc_r: increment of x, 
        ret_err_cov_i: inverse of retrieval error covariance
        ret_err_cov:  retrieval error covariance
    This variant is an  'm-form' (m = dimension of measurement)
    '''
    sa_kt = np.dot(sa,k.T)
    k_sa_kt = np.dot(k,sa_kt)    

    k_sa_kt_se = k_sa_kt + se
    k_sa_kt_se_i = inverse(k_sa_kt_se)
    sa_kt_k_sa_kt_se_i = np.dot(sa_kt, k_sa_kt_se_i)
    k_dx_y = np.dot(k, xa - x) +y

    nx = xa - np.dot(sa_kt_k_sa_kt_se_i,k_dx_y )
    incr_x = x - nx

    ret_err_cov_i = sai + np.dot(np.dot(k.T, sei), k)
    ret_err_cov = sa - np.dot(sa_kt_k_sa_kt_se_i,np.dot(k,sa))

    return nx, incr_x, ret_err_cov_i, ret_err_cov


def check_increment(ix: NpArrayT, sri: NpArrayT, eps: float) -> bool:
    '''
    see Rodgers for details...
    input: 
      ix : increment of x_i+1 = x_i -ix
      sri: inverse of retrieval error co-variance
    '''
    return np.dot(ix.T, np.dot(sri, ix)) < (eps * ix.size)

def gain_aver_cost(x: NpArrayT, y: NpArrayT, k: NpArrayT,
                   xa: NpArrayT, sei: NpArrayT, sai: NpArrayT,
                   rec: NpArrayT) -> Diagnostic1T:
    '''
    input:
        x: state vector
        y: forward(x) - measurement
        k: jacobian(x)
        xa: prior state 
        sei: inverse of measurement error co-variance
        sai: inverse of prior error co-variance
        rec: retrieval error covarince
    return: 
        gain: gain matrix
        aver: averaging kernel
        cost: cost function
    '''
    gain = np.dot(rec, np.dot(k.T, sei))
    aver = np.dot(gain, k)
    cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + \
        np.dot(y.T, np.dot(sei, y))
    return gain, aver, cost

def dof_infocont_retrnoise_smootherrr(sa: NpArrayT, se: NpArrayT, sri: NpArrayT, 
                                      gg: NpArrayT, av: NpArrayT)-> Diagnostic2T:
    '''
    Calculates additional diagnostics  (see Rodgers)
    input: 
        sa:  prior error co-variance
        se:  mess. error co-variance
        sri: inverse of retrieval error co-variance (currently unused)
        gg:  gain
        av:  averaging kernel
    
    return:
        dof: degree of Freedom 
        h:   information_content
        sn:  retrieval_noise
        sme: smoothing_error
    '''

    dof = np.trace(av)
    # information content h
    try: 
        w, v = np.linalg.eigh(av)
    except Exception as eee:
        #print(eee)
        w, v = np.zeros(av.shape[0])+np.nan, np.zeros_like(av)+np.nan
    #w, v = np.linalg.eig(av)
    h = 0.5* np.log(1+w**2).sum()
    sn = np.dot(gg,np.dot(se,gg.T))
    ia = np.identity(av.shape[0])-av
    sme = np.dot(ia, np.dot(sa,ia.T))
    
    return dof, h, sn, sme


def internal_optimizer(y: NpArrayT,
                       func: CallableT, fparam: NpOrAnyT,
                       jaco: CallableT, jparam: NpOrAnyT,
                       xa: NpArrayT, fg: NpArrayT, 
                       sei: NpArrayT, sai: NpArrayT,
                       se: NpArrayT, sa: NpArrayT,
                       dx: NpOrAnyT, 
                       eps: float, maxiter: int,
                       full: bool, gnform: str) -> InternalOptimizerT:
    '''
    y:       measurement
    func:    function to be inverted
    fparam:  parameter for func 
    jaco:    function that returns the jacobian of func
    jparam:  parameter for jaco
    xa:      prior state
    fg:      first guess state
    sei:     inverse of measurement error covariance matrix
    sai:     inverse of prior error covariance matrix
    se :     measurement error covariance matrix
    sa :     prior error covariance matrix
    eps:     if (x_i-x_i+1)^T # S_x # (x_i-x_i+1)   < eps * N_x, optimization
             is stopped. This is the *original* as, e.g. proposed by Rodgers
    maxiter: maximum number of number_of_iterations
    '''
    if gnform == 'm':
        increment = gauss_newton_increment_mform_c
    elif gnform == 'mc':
        increment = gauss_newton_increment_mform_c
    elif gnform == 'n': 
        increment = gauss_newton_increment_nform_a
    elif gnform == 'na': 
        increment = gauss_newton_increment_nform_a
    elif gnform == 'nb': 
        increment = gauss_newton_increment_nform_b
    elif gnform == 'nc': 
        increment = gauss_newton_increment_nform_c
    elif gnform == 'nlm': 
        increment = gauss_newton_increment_nform_a
        gamma = LMSTART
    else:
        raise WrongInputError('Unknown key for internal optimizer: %s'%gnform)

    #1. prior as first guess ...
    if fg is None:
        xn = xa
    else:
        xn = fg

    #2. optimization
    ii, conv = 0, False
    while not conv and (ii <= maxiter):
        if 'lm' in gnform:
        # Levenberg Marquard is a heuristic mixture
        # of Gradient descent and Gauss Newton
        # using gamma as weighting. As bigger gamma is, as 
        # more "gradient-descentic" it it behaves... If gamma 
        # aproaches zero, it is like Gauss Newton
            if ii == 0:
                yn = func(xn, fparam) - y
                prior_cost = np.dot((xa - xn).T, np.dot(sai, xa - xn)) + \
                        np.dot(yn.T, np.dot(sei, yn))
            kk = jaco(xn, jparam, dx)
            while (ii <= maxiter):
                ii +=1
                xnlm, ixlm, srilm, srlm = increment(xn, yn, kk, xa, sei, sai, se, sa, gamma)
                conv = check_increment(ixlm, srilm, eps)
                if conv:
                    xn, ix, sri, sr = xnlm, ixlm, srilm, srlm
                    break
                ynlm = func(xnlm, fparam) - y
                new_cost = np.dot((xa - xnlm).T, np.dot(sai, xa - xnlm)) + \
                                    np.dot(ynlm.T, np.dot(sei, ynlm))
                if (new_cost < prior_cost):
                    gamma /= LMFAC
                    xn, ix, sri, sr, yn, prior_cost = xnlm, ixlm, srilm, srlm, ynlm, new_cost
                    break
                else:
                    gamma *= LMFAC

        else:
            ii += 1
            yn = func(xn, fparam) - y
            kk = jaco(xn, jparam, dx)
            xn, ix, sri, sr = increment(xn, yn, kk, xa, sei, sai, se, sa)
            conv = check_increment(ix, sri, eps)

    #3. exit
    if full is False:
        return xn, kk, conv, ii, sr, None, None, None, None, None, None, None
    #else:
    yn = func(xn, fparam) - y
    gg, av, co = gain_aver_cost(xn, yn, kk, xa, sei, sai, sr)
    dof, ico, sn, sme = dof_infocont_retrnoise_smootherrr(sa, se, sri, gg, av)
    return xn, kk, conv, ii, sr, gg, av, co, dof, ico, sn, sme

def invert_function(func, **args):
    '''
    This solves the following equation:
    y=func(x,params)
    and returns a function which is effectively
    the inverse of func
    x=func⁻¹(y,fparam=params)

    mandatory INPUT:
       func  = function to be inverted
               y=func(x)
     optional INPUT:
     jaco   = function that returns jacobian of func,
              if not given, numerical differentiation is used
     fparam = default additional parameter for func
     jparam = default additional parameter for corresponding jacobian
              (if no jacobian function is given, and numerical differentiation must
              performed internaly, then jparam shall be either a 2-tupel, where the 
              first element is the same parameter object as for *func* and the second 
              element is te delta_x. ) 
      eps   = default convergence criteria when iteration is stopped (Rodgers),xtol=0.001
       xa   = default prior x
       fg   = default first guess x
       sa   = default prior error co-variance
       se   = default measurement error co-variance
       mi   = default maximum number of iterations 
     clip   = default lower and upper limit
   gnform   = default Gauss Newton form ('n' or 'm')
    OUTPUT:
       func_inverse    inverse of func
    every optional input can be overwritten in the function   
    '''
    for kk in DEFAULTS:
        if kk not in args:
            args[kk] = DEFAULTS[kk]
    if args['jaco'] is None:
        args['jaco'] = approximate_jacobian_function(func)

    def func_inverse(
            yy,
            fparam=args['fparam'],
            jparam=args['jparam'],
            ll=args['ll'],
            ul=args['ul'],
            se=args['se'],
            sa=args['sa'],
            xa=args['xa'],
            dx=args['dx'],
            eps=args['eps'],
            fg=args['fg'],
            jaco=args['jaco'],
            maxiter=args['mi'],
            full=args['full'],
            clip=args['clip'],
            gnform=args['gnform']):
        '''
        Inverse function of *%(name)s*. Estimates the 
        optimal *state*, that explains the measurement *yy*.

        Input:
            yy   = measurement

        optional Input:    
          fparam = additional parameter for func
          jparam = additional parameter for corresponding jacobian
              ll = lower limit (same size and type as state)
              ul = upper limit (same size and type as state)
              se = measurement error co-variance matrix
              sa = prior error co-variance matrix
              xa = prior
              dx = for numerical jacobian
              fg = first guess 
             eps = Rodgers convergence criteria eps
            jaco = function that returns jacobian of func
          gnform = Gauss Newton increment ('n' form or 'm' form)

        Output:
            named tuple, containing: 
                state  
                jacobian
                convergence
                number_of_iterations
                gain
                averaging_kernel
                retrieval_error_covariance
                cost
                dof
                information_content
                retrieval_noise
                smoothing_error
            '''
        if sa is None:
            raise MissingInputError('sa is missing')
        if se is None:
            raise MissingInputError('se is missing')
        if xa is None:
            raise MissingInputError('xa (prior) is missing')

        if isinstance(yy, list):
            yyy = np.array(yy)
        elif isinstance(yy, float):
            yyy = np.array([yy])
        else:
            yyy = yy

        if sa.ndim != 2:
            raise WrongInputError('sa has wrong dimensions')
        if se.ndim != 2:
            raise WrongInputError('se has wrong dimensions')
        if xa.ndim != 1:
            raise WrongInputError('xa has wrong dimensions')
        if yyy.ndim != 1:
            raise WrongInputError('yy has wrong dimensions')
        if sa.shape[0] != sa.shape[1]:
            raise WrongInputError('sa is not quadratic')
        if se.shape[0] != se.shape[1]:
            raise WrongInputError('se is not quadratic')
        if sa.shape[0] != xa.shape[0]:
            raise WrongInputError('sa and xa are incompatible')
        if se.shape[0] != yyy.shape[0]:
            raise WrongInputError('se and yy are incompatible')



        sai = inverse(sa)
        sei = inverse(se)

        if jparam is None:
            jparam = fparam

        if clip is True:
            if ll is None: 
                raise MissingInputError('ll (lower limit of state) is missing')
            if ul is None: 
                raise MissingInputError('ul (upper limit of state) is missing')

            def cfunc(*pargs, **kwargs):
                return func(np.clip(pargs[0], ll, ul), *pargs[1:], **kwargs) 
            def cjaco(*pargs, **kwargs):
                return jaco(np.clip(pargs[0], ll, ul), *pargs[1:], **kwargs) 
            #cfunc = lambda x, p: func(np.clip(x, ll, ul), p)
            #cjaco = lambda x, p, dx: jaco(np.clip(x, ll+dx, ul-dx), p, dx)
        else:
            cfunc, cjaco = func, jaco 

        result = internal_optimizer(y=yyy, xa=xa, fg=fg, sei=sei, sai=sai, 
                                    se=se, sa=sa, dx = dx,
                                    eps=eps, maxiter=maxiter, func=cfunc, fparam=fparam, 
                                    jaco=cjaco, jparam=jparam, full=full, gnform=gnform)

        return RESULT(*result)

    func_inverse.__doc__ = func_inverse.__doc__%{'name':func.__name__}
    func_inverse.__doc__ += "\n      Defaults are:\n"
    #func_inverse.__doc__ += json.dumps(args, default=lambda x: str(x), sort_keys=True, indent=12)
    func_inverse.__doc__ += json.dumps(args, default=str, sort_keys=True, indent=12)
    return func_inverse


