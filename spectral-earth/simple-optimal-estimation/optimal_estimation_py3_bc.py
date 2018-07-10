# -*- coding: utf-8 -*-
__author__ = 'rene'
__version__ = '1.1'  # 04-08-2015
__author__ = 'Rene Preusker, rene.preusker@fu-berlin.de'

# BC test sandbox

import collections

# Todo catch math errors
import numpy as np
import optimal_estimation_core_pure_python as oecpy

# import optimal_estimation_core_32 as oec32
# import optimal_estimation_core as oec64
oec32 = oecpy
oec64 = oecpy

EPSIX = np.finfo(np.float32).resolution

# if you like *besser* naming, change it here
result = collections.namedtuple('result', 'x j conv ni g a sr cost')

GR = (np.sqrt(5) + 1) / 2


def golden_section_search(f, a, b):
    '''
    golden section search
    to find the minimum of f on [a,b]
    f: a strictly unimodal function on [a,b]
    (source from wikipedia)

    '''
    c = b - (b - a) / GR
    d = a + (b - a) / GR
    while abs(c - d) > EPSIX:
        if f(c) < f(d):
            b = d
        else:
            a = c
        # we recompute both c and d here 
        # to avoid loss of precision which 
        # may lead to incorrect results or infinite loop
        c = b - (b - a) / GR
        d = a + (b - a) / GR
    return (b + a) / 2


def numerical_jacoby(a, b, x, fnc, nx, ny, delta, dtype=np.float64):
    '''

    :param a:
    :param b:
    :param x:
    :param fnc:
    :param nx:
    :param ny:
    :param delta:
    :return: Jacobian of fnc
    '''
    # very coarse but sufficient for this excercise
    # dx = np.array((b - a) * delta)
    dx = (b - a) * delta
    jac = np.empty((ny, nx), order='F', dtype=dtype)  # zeilen zuerst, spalten später!!!!!!!
    for ix in range(nx):
        dxm = x * 1.
        dxp = x * 1.
        dxm[ix] = dxm[ix] - dx[ix]
        dxp[ix] = dxp[ix] + dx[ix]
        dyy = fnc(dxp) - fnc(dxm)
        jac[:, ix] = dyy / dx[ix] / 2.
    return jac


def numerical_jacoby_minus(a, b, x, fnc, nx, ny, delta, dtype=np.float64):
    '''

    :param a:
    :param b:
    :param x:
    :param fnc:
    :param nx:
    :param ny:
    :param delta:
    :return: Jacobian of fnc
    '''
    # very coarse but sufficient for this excercise
    # dx = np.array((b - a) * delta)
    dx = (b - a) * delta
    jac = np.empty((ny, nx), order='F', dtype=dtype)  # zeilen zuerst, spalten später!!!!!!!
    fnc_x = fnc(x)
    for ix in range(nx):
        dxm = x * 1.
        dxm[ix] = dxm[ix] - dx[ix]
        dyy = fnc_x - fnc(dxm)
        jac[:, ix] = dyy / dx[ix]
    return jac


def my_optimizer(
        a,
        b,
        y,
        func,
        fparams,
        jaco,
        jparams,
        xa,
        fg,
        sei,
        sai,
        eps,
        maxiter,
        method=2,
        delta=0.001,
        epsx=EPSIX * 2.,
        epsy=0.000001,
        full=False,
        dtype=np.float64):
    '''

    a:      lower limit of state
    b:      upper limit of state
    y:      measurement
    func:   function to be inverted
    jaco:   that returns the jacobian of func
    sei:    inverse of measurement error covariance matrix
    sai:    inverse of prior error covariance matrix
    xa:     prior state
    fg:     first guess state
    delta:  dx=delta*(b-a) to be used for jacobian
    epsy:   if norm(func(x)-y)  < epsy, optimization is stopped
    epsx:   if max(new_x^2/(b-a)) < epsx, optimization is stopped
    eps:    if (x_i-x_i+1)^T # S_x # (x_i-x_i+1)   < eps * N_x, optimization
            is stopped. This is the *original* as, e.g. proposed by Rodgers
    params: are directly passed to func (e.g. geometry, special temperatures, profiles aerosols ...)
   jparams: are directly passed to jaco (e.g. geometry, special temperatures, profiles aerosols ...)
    method: optimizer (0:  pure GaussNewton, 1: gaussnewton with measurement error  2: optimal
            estimisation with Gauss Newton optimizer )

    '''

    ### put some curry to the meet
    ###
    # function to root-find
    if dtype == np.float32:
        oec = oec32
    else:
        oec = oec64

    def fnc(x):
        return func(oec.clipper(a, b, x), fparams) - y

    # numerical derivation of fnc (central differential ...)
    if jaco is None:
        def dfnc(x):
            return numerical_jacoby(a, b, x, fnc, x.size, y.size, delta, dtype=dtype)
    else:
        def dfnc(x):
            return jaco(x, jparams)

    if method == 0:
        # Gauss Newton Step
        def operator(x, y, k):
            return oec.gauss_newton_operator(a, b, x, y, k)

        def reterrcov(k):
            return np.zeros((k.shape[1], k.shape[1]))

        def diagnose(x, y, k, sr):
            return oec.gauss_newton_gain_aver_cost(x, y, k)
    elif method == 1:
        # Gauss Newton with measurement error
        def operator(x, y, k):
            return oec.gauss_newton_operator_with_se(a, b, x, y, k, sei)

        def reterrcov(k):
            return oec.gauss_newton_se_ret_err_cov(k, sei)

        def diagnose(x, y, k, sr):
            return oec.gauss_newton_se_gain_aver_cost(x, y, k, sei, sr)
    elif method == 2:
        # Gauss newton Optimal Estimation
        def operator(x, y, k):
            return oec.optimal_estimation_gauss_newton_operator(a, b, x, y, k, sei, sai, xa)

        def reterrcov(k):
            return oec.oe_ret_err_cov(k, sei, sai)

        def diagnose(x, y, k, sr):
            return oec.oe_gain_aver_cost(x, y, k, xa, sei, sai, sr)
    elif method == 3:
        # Levenberg Marquardt
        def operator(x, y, k):
            def lm_cstfnc(y):
                return oec.leve_marq_cost(y)

            def lm_operat(l):
                return oec.leve_marq_operator(a, b, x, y, k, l)

            return lm_scaler(lm_cstfnc, lm_operat, lm_cstfnc(y))

        def reterrcov(k):
            return np.zeros((k.shape[1], k.shape[1]))

        def diagnose(x, y, k, sr):
            return oec.leve_marq_gain_aver_cost(x, y, k)
    elif method == 4:
        # Levenberg Marquardt with measurement error
        def operator(x, y, k):
            def lm_cstfnc(y):
                return oec.leve_marq_se_cost(y, sei)

            def lm_operat(l):
                return oec.leve_marq_operator_with_se(a, b, x, y, k, sei, l)

            return lm_scaler(lm_cstfnc, lm_operat, lm_cstfnc(y))

        def reterrcov(k):
            return oec.leve_marq_se_ret_err_cov(k, sei)

        def diagnose(x, y, k, sr):
            return oec.leve_marq_se_gain_aver_cost(x, y, k, sei, sr)
    elif method == 5:
        # Levenberg Marquardt optimal estimation
        def operator(x, y, k):
            def lm_cstfnc(y):
                return oec.oe_leve_marq_cost(x, xa, y, sei, sai)

            def lm_operat(l):
                return oec.oe_leve_marq_operator(a, b, x, y, k, sei, sai, xa, l)

            return lm_scaler(lm_cstfnc, lm_operat, lm_cstfnc(y))

        def reterrcov(k):
            return oec.oe_leve_marq_ret_err_cov(k, sei, sai)

        def diagnose(x, y, k, sr):
            return oec.oe_leve_marq_gain_aver_cost(x, y, k, xa, sei, sai, sr)

    if method >= 3:
        # levenberg marquardt damping optimizer
        #        def lm_scaler(cstf,oper, x, y, k):
        def lm_scaler(cstf, oper, cst_0):
            def ffff(llll):
                xxxx = oper(2 ** llll)
                out = cstf(fnc(xxxx[0]))
                # print 'out',out,llll#,cst_0
                return out

            lam = golden_section_search(ffff, -15., 5.)
            print(lam, ffff(lam), oper(2 ** lam)[0])
            return oper(2 ** lam)

    # prior as first guess ...
    if fg is None:
        xn = xa
    else:
        xn = fg

    ### Do the iteration
    yn = fnc(xn)
    ii, conv = 0, False
    while True:
        ii += 1
        # Iteration step
        yn = fnc(xn)
        kk = dfnc(xn)
        xn, ix, sri, sr = operator(xn, yn, kk)
        # print diagnose(xn, yn, kk, sr)[2]

        # Rodgers convergence criteria
        eps = 0.000001
        if method in (1, 2, 4, 5):
            if oec.norm_error_weighted_x(ix, sri) < eps * ix.size:
                conv = True
                break

        # only aplicable if *no* prior knowledge is used
        if method in (0, 1, 3):
            if oec.norm_y(yn) < epsy:
                conv = True
                break

        # if x doesnt change ,  stop
        # if normx(ix) < epsx:
        #    print 'epsx',epsx,ix,xn
        #    conv=True
        #    break

        # if maxiter is reached,  no-converge and stop
        if ii > maxiter:
            conv = False
            break

    ### Diagnose some Output
    if full is False:
        return xn, kk, conv, ii, None, None, None, None
    elif full == 'fast':
        # take the last-but-one
        gg, av, co = diagnose(xn, yn, kk, sr)
        return xn, kk, conv, ii, av, gg, sr, co
    elif full is True:
        # calculate latest yn, kk, sr
        yn = fnc(xn)
        kk = dfnc(xn)
        sr = reterrcov(kk)
        # calculate diagnose quantities
        gg, av, co = diagnose(xn, yn, kk, sr)
        return xn, kk, conv, ii, av, gg, sr, co


def my_inverter(func, a, b, **args):
    '''
    This invertes (solves) the following equation:
    y=func(x,params)
    and returns a function which is effectively
    the inverse of func
    x=func⁻¹(y,fparams=params)

    mandatory INPUT:
           a = lower limit
           b = upper limit
       func  = function to be inverted
               y=func(x)
     optional INPUT:
       eps   = convergence criteria when iteration is stopped (Rodgers),xtol=0.001
       xad   = default prior x
       fgd   = default first guess x
       sad   = default prior error co-variance
       sed   = default measurement error co-variance
   methodd   = default operator (2=optimal estimation)
      jaco   = function that returns jacobian of func
               jacobian = jaco(x)

    OUTPUT:
       func_inverse    inverse of func
    '''
    for kk in ['sed', 'sad', 'xad', 'fgd', 'eps', 'methodd', 'jaco', 'dtype']:
        if kk not in args:
            args[kk] = None
    if args['methodd'] is None:
        args['methodd'] = 2
    if args['eps'] is None:
        args['eps'] = 0.01
    if args['fgd'] is None:
        args['fgd'] = (a + b) / 2.
    if args['dtype'] is None:
        args['dtype'] = np.float64

    def func_inverse(
            yy,
            se=args['sed'],
            sa=args['sad'],
            xa=args['xad'],
            eps=args['eps'],
            fg=args['fgd'],
            method=args['methodd'],
            jaco=args['jaco'],
            maxiter=20,
            full=False,
            fparams=None,
            jparams=None,
            check_input=False,
            dtype=args['dtype']):
        '''
        Input:
            yy   = measurement
            se   = measurement error co-variance matrix
            sa   = prior error co-variance matrix
            xa   = prior
            fg   = first guess
          method = 0-newton 1-newto+se  2-optimal_estimation
        linesrch = False  no additionaö line search True
                   aditional linesearch (only if nonlinear, more than quadratic)
          jaco   = function that returns jacobian of func
         fparams = additional parameter for func
         jparams = additional parameter for jaco

        Output:
            x    = retrieved state
            sr   = retrieval error co-variance matrix
            a    = averaging kernel matrix
            g    = gain matrix
            j    = jacobian
            ni   = number of iterations
            conv = convergence (True or False)
            cost = cost function
        '''
        if dtype == np.float32:
            oec = oec32
        else:
            oec = oec64

        if method == 0 or method == 3:
            sei = np.zeros((yy.size, yy.size))
            sai = np.zeros((a.size, a.size))
        elif method == 1 or method == 4:
            sai = np.zeros((a.size, a.size))
            sei = oec.inverse(se)
        elif method == 2 or method == 5:
            sai = oec.inverse(sa)
            sei = oec.inverse(se)
        else:
            sei = np.zeros((yy.size, yy.size))
            sai = np.zeros((a.size, a.size))

        if isinstance(yy, list):
            yyy = np.array(yy)
        elif isinstance(yy, float):
            yyy = np.array([yy])
        else:
            yyy = yy
        xxx, jjj, ccc, nnn, aaa, ggg, sss, cst = my_optimizer(a=a, b=b
                                                              , y=yyy, xa=xa, fg=fg
                                                              , sei=sei, sai=sai
                                                              , eps=eps, maxiter=maxiter, method=method
                                                              , func=func, fparams=fparams, jaco=jaco, jparams=jparams
                                                              , full=full, dtype=dtype)
        if check_input:
            for ele in ('a', 'b', 'yyy', 'xa', 'fg', 'sei', 'sai'):
                # print(ele, eval('type(%s)' % ele), end=' ')
                # print(eval('%s.flags' % ele), end=' ')
                print(eval('%s.dtype' % ele))
        if full is False:
            return xxx
        else:
            return result(xxx, jjj, ccc, nnn, ggg, aaa, sss, cst)

    return func_inverse


def test():
    """
    Tested are 3 cases 'funca', 'funcb', 'funcc'
    a:  linear R^2 --> R^3
    b:  nonlinear R^2 --> R^3
    c:  nonlinear R^3 --> R^2  (only OE)
    """
    dtype = np.float32
    # dtype=np.float64

    # lower bound of state
    A = np.array([0.1, 0.1, 0.1], order='F', dtype=dtype)
    # upper bound of state
    B = np.array([10., 10., 10.], order='F', dtype=dtype)
    AA = {'a': A[0:2], 'b': A[0:2], 'c': A}
    BB = {'a': B[0:2], 'b': B[0:2], 'c': B}

    # SE measurement error covariance
    SEa = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 100.]], order='F', dtype=dtype) * .1
    SEb = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 10.]], order='F', dtype=dtype) * 10.
    SEc = np.array([[10., 0.], [0., 10.]], order='F', dtype=dtype) * 0.1
    SE = {'a': SEa, 'b': SEb, 'c': SEc}

    # SA apriori error covariance
    SAa = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAb = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAc = np.array([[1., 0., 0.], [0., 1., 0.], [0., 0., 1.]], order='F', dtype=dtype) * 100.
    SA = {'a': SAa, 'b': SAb, 'c': SAc}

    # XA prior knowledge
    XA = {'a': np.array([3.7, 5.6], order='F', dtype=dtype),
          'b': np.array([3.7, 5.6], order='F', dtype=dtype),
          'c': np.array([3.7, 5.6, 8.5], order='F', dtype=dtype)
          }

    # XT to test
    XT = {'a': np.array([3.5, 6.5], order='F', dtype=dtype),
          'b': np.array([3.5, 6.5], order='F', dtype=dtype),
          'c': np.array([3.5, 6.5, 5.8], order='F', dtype=dtype)
          }

    def funca(x, *args, **kwargs):
        '''
        simple linear R^2-->R^3 test function
        '''
        return np.array([13. + 6 * x[0] + 4 * x[1]
                            , 2. - 3 * x[0] + 2 * x[1]
                            , x[0] - 5 * x[1]
                         ], order='F', dtype=dtype)

    def funcb(x, *args, **kwargs):
        '''
        simple non-linear R^2-->R^3 test function
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] + 0.7 * np.power(x[0] * x[1], 2)
                            , 2 - 3 * x[0] + 2 * x[1] + np.sqrt(x[0]) * np.log(x[1])
                            , x[0] - 5 * x[1] - np.sqrt(x[0] * x[1])
                         ], order='F', dtype=dtype)

    def funcc(x, *args, **kwargs):
        '''
        simple linear R^3-->R^2 test function.
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] - 2 * x[2]
                            , 2 - 3. * x[0] + 5. * x[1] + 7 * x[2]
                         ], order='F', dtype=dtype)

    FUNC = {'a': funca, 'b': funcb, 'c': funcc}

    method = ('Newton', 'Newton+SE', 'OE')

    #    for func_key in ['b',]:
    for func_key in ['a', 'b', 'c']:
        print('-' * 30)
        print(func_key * 30)
        print('-' * 30)
        # func_key,rr='b',range(0,3)
        # func_key,rr='a',range(0,3)
        print('XA', XA[func_key], '--> YA:', FUNC[func_key](XA[func_key]))
        print('SE')
        print(SE[func_key])
        print('SA')
        print(SA[func_key])
        yt = FUNC[func_key](XT[func_key])
        inv_func = my_inverter(FUNC[func_key], AA[func_key], BB[func_key], dtype=dtype)

        print()
        print('Test with x =', XT[func_key], '-->  y=', yt)
        for i, meth in enumerate(method):
            if i != 2 and func_key == 'c':
                continue
            erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=i,
                           maxiter=100)
            print('    retrieved X: ', erg.x)
            if i > 0:
                print('    diag avKern: ', [erg.a[j, j] for j in range(erg.a.shape[0])])
                print('    diag ret.er: ', [erg.sr[j, j] for j in range(erg.sr.shape[0])])
            print('         nitter: ', erg.ni)
            print('           cost: ', erg.cost)
            print('    f(retrie X): ', FUNC[func_key](erg.x))
            print('    -')

        yt[-1] = yt[-1] + 1
        print()
        print('Test with x =', XT[func_key], '-->  disturbed y=', yt)
        for i, meth in enumerate(method):
            if i != 2 and func_key == 'c':
                continue
            erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=i)
            # for _ in range(1000): dum=inv_func(yt,full=True,sa=SA[func_key],se=SE[func_key],xa=XA[func_key], eps=0.001,method=i)
            print('    retrieved X: ', erg.x)
            if i > 0:
                print('    diag avKern: ', [erg.a[j, j] for j in range(erg.a.shape[0])])
                print('    diag ret.er: ', [(erg.sr[j, j]) for j in range(erg.sr.shape[0])])
            print('         nitter: ', erg.ni)
            print('           cost: ', erg.cost)
            print('    f(retrie X): ', FUNC[func_key](erg.x))
            print('    -')


def test_lm():
    '''
    Tested are 3 cases 'funca', 'funcb', 'funcc'
    a:  linear R^2 --> R^3
    b:  nonlinear R^2 --> R^3
    c:  nonlinear R^3 --> R^2  (only OE)
    '''
    # dtype=np.float32
    dtype = np.float64

    # lower bound of state
    A = np.array([0.1, 0.1, 0.1], order='F', dtype=dtype)
    # upper bound of state
    B = np.array([10., 10., 10.], order='F', dtype=dtype)
    AA = {'a': A[0:2], 'b': A[0:2], 'c': A}
    BB = {'a': B[0:2], 'b': B[0:2], 'c': B}

    # SE measurement error covariance
    SEa = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 100.]], order='F', dtype=dtype) * .1
    SEb = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 10.]], order='F', dtype=dtype) * 10.
    SEc = np.array([[10., 0.], [0., 10.]], order='F', dtype=dtype) * 0.1
    SE = {'a': SEa, 'b': SEb, 'c': SEc}

    # SA apriori error covariance
    SAa = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAb = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAc = np.array([[1., 0., 0.], [0., 1., 0.], [0., 0., 1.]], order='F', dtype=dtype) * 1.
    SA = {'a': SAa, 'b': SAb, 'c': SAc}

    # XA prior knowledge
    XA = {'a': np.array([3.7, 5.6], order='F', dtype=dtype),
          'b': np.array([3.7, 5.6], order='F', dtype=dtype),
          'c': np.array([3.7, 5.6, 8.5], order='F', dtype=dtype)
          }

    # XT to test
    XT = {'a': np.array([3.5, 6.5], order='F', dtype=dtype),
          'b': np.array([3.5, 6.5], order='F', dtype=dtype),
          'c': np.array([3.5, 6.5, 5.8], order='F', dtype=dtype)
          }

    def funca(x, *args, **kwargs):
        '''
        simple linear R^2-->R^3 test function
        '''
        return np.array([13. + 6 * x[0] + 4 * x[1]
                            , 2. - 3 * x[0] + 2 * x[1]
                            , x[0] - 5 * x[1]
                         ], order='F', dtype=dtype)

    def funcb(x, *args, **kwargs):
        '''
        simple non-linear R^2-->R^3 test function
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] + 0.7 * np.power(x[0] * x[1], 3.4)
                            , 2 - 3 * x[0] + 2 * x[1] + np.sqrt(x[0]) * np.log(x[1])
                            , x[0] - 5 * x[1] - np.sqrt(x[0] * x[1])
                         ], order='F', dtype=dtype)

    def funcc(x, *args, **kwargs):
        '''
        simple linear R^3-->R^2 test function.
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] - 2 * x[2]
                            , 2 - 3. * x[0] + 5. * x[1] + 7 * x[2]
                         ], order='F', dtype=dtype)

    FUNC = {'a': funca, 'b': funcb, 'c': funcc}
    func_key = 'c'
    yt = FUNC[func_key](XT[func_key])
    inv_func = my_inverter(FUNC[func_key], AA[func_key], BB[func_key], dtype=dtype)
    print(inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=2, maxiter=10))
    print(inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=5, maxiter=100))


def test_single(func_key, method):
    """
    Tested are 3 cases 'funca', 'funcb', 'funcc'
    a:  linear R^2 --> R^3
    b:  nonlinear R^2 --> R^3
    c:  nonlinear R^3 --> R^2  (only OE)
    """

    if method != 2 and func_key == 'c':
        raise ValueError('Combination of inversion method ' + str(method) + ' and functions type ' + func_key +
                         ' not supported.')

    dtype = np.float32
    # dtype=np.float64

    # lower bound of state
    A = np.array([0.1, 0.1, 0.1], order='F', dtype=dtype)
    # upper bound of state
    B = np.array([10., 10., 10.], order='F', dtype=dtype)
    AA = {'a': A[0:2], 'b': A[0:2], 'c': A}
    BB = {'a': B[0:2], 'b': B[0:2], 'c': B}

    # SE measurement error covariance
    SEa = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 100.]], order='F', dtype=dtype) * .1
    SEb = np.array([[10., 0., 0.], [0., 10., 0.], [0., 0., 10.]], order='F', dtype=dtype) * 10.
    SEc = np.array([[10., 0.], [0., 10.]], order='F', dtype=dtype) * 0.1
    SE = {'a': SEa, 'b': SEb, 'c': SEc}

    # SA apriori error covariance
    SAa = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAb = np.array([[1., 0.], [0., 1.]], order='F', dtype=dtype) * 1.
    SAc = np.array([[1., 0., 0.], [0., 1., 0.], [0., 0., 1.]], order='F', dtype=dtype) * 100.
    SA = {'a': SAa, 'b': SAb, 'c': SAc}

    # XA prior knowledge
    XA = {'a': np.array([3.7, 5.6], order='F', dtype=dtype),
          'b': np.array([3.7, 5.6], order='F', dtype=dtype),
          'c': np.array([3.7, 5.6, 8.5], order='F', dtype=dtype)
          }

    # XT to test
    XT = {'a': np.array([3.5, 6.5], order='F', dtype=dtype),
          'b': np.array([3.5, 6.5], order='F', dtype=dtype),
          'c': np.array([3.5, 6.5, 5.8], order='F', dtype=dtype)
          }

    def funca(x, *args, **kwargs):
        '''
        simple linear R^2-->R^3 test function
        '''
        return np.array([13. + 6 * x[0] + 4 * x[1]
                            , 2. - 3 * x[0] + 2 * x[1]
                            , x[0] - 5 * x[1]
                         ], order='F', dtype=dtype)

    def funcb(x, *args, **kwargs):
        '''
        simple non-linear R^2-->R^3 test function
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] + 0.7 * np.power(x[0] * x[1], 2)
                            , 2 - 3 * x[0] + 2 * x[1] + np.sqrt(x[0]) * np.log(x[1])
                            , x[0] - 5 * x[1] - np.sqrt(x[0] * x[1])
                         ], order='F', dtype=dtype)

    def funcc(x, *args, **kwargs):
        '''
        simple linear R^3-->R^2 test function.
        '''
        return np.array([13 + 6 * x[0] + 4 * x[1] - 2 * x[2]
                            , 2 - 3. * x[0] + 5. * x[1] + 7 * x[2]
                         ], order='F', dtype=dtype)

    FUNC = {'a': funca, 'b': funcb, 'c': funcc}

    print('-' * 30)
    print(func_key * 30)
    print('-' * 30)
    # func_key,rr='b',range(0,3)
    # func_key,rr='a',range(0,3)
    print('XA', XA[func_key], '--> YA:', FUNC[func_key](XA[func_key]))
    print('SE')
    print(SE[func_key])
    print('SA')
    print(SA[func_key])
    yt = FUNC[func_key](XT[func_key])
    inv_func = my_inverter(FUNC[func_key], AA[func_key], BB[func_key], dtype=dtype)

    print()
    print('Test with x =', XT[func_key], '-->  y=', yt)
    erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=method,
                   maxiter=100)
    print('    retrieved X: ', erg.x)
    if method > 0:
        print('    diag avKern: ', [erg.a[j, j] for j in range(erg.a.shape[0])])
        print('    diag ret.er: ', [erg.sr[j, j] for j in range(erg.sr.shape[0])])
    print('         nitter: ', erg.ni)
    print('           cost: ', erg.cost)
    print('    f(retrie X): ', FUNC[func_key](erg.x))
    print('    -')

    yt[-1] = yt[-1] + 1
    print()
    print('Test with x =', XT[func_key], '-->  disturbed y=', yt)
    erg = inv_func(yt, full=True, sa=SA[func_key], se=SE[func_key], xa=XA[func_key], eps=0.001, method=method)
    print('    retrieved X: ', erg.x)
    if method > 0:
        print('    diag avKern: ', [erg.a[j, j] for j in range(erg.a.shape[0])])
        print('    diag ret.er: ', [(erg.sr[j, j]) for j in range(erg.sr.shape[0])])
    print('         nitter: ', erg.ni)
    print('           cost: ', erg.cost)
    print('    f(retrie X): ', FUNC[func_key](erg.x))
    print('    -')


if __name__ == '__main__':
    # test()
    # test_lm()

    # FUNC = {'a': funca, 'b': funcb, 'c': funcc}
    #  a: linear R^2-->R^3
    #  b: nonlinear R^2-->R^3
    #  c: linear R^3-->R^2
    # method = ('Newton', 'Newton+SE', 'OE')
    #  provide index 0, 1, 2
    # test_single('a', 0)
    # test_single('b', 0)
    test_single('a', 1)
    # test_single('b', 1)
    # test_single('a', 2)
    # test_single('b', 2)
    # test_single('c', 2)
