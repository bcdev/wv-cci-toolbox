# Copyright (c) 2020
# Author(s):
#   Rene Preusker <rene.preusker@gmail.com>

import time
import math
import numpy as np
import scipy as sp

FASTMATH = True
NBCACHE = True
NOGIL = True

try:
    from numba import njit,jit#,autojit
except ImportError:
    print('No numba. SLOW!')
    print('No numba. SLOW!')
    print('No numba. SLOW!')
    print('No numba. SLOW!')
    print('No numba. SLOW!')
    def njit(*args, **kwargs):
        def njit_intern(func):
            return func
        return njit_intern
    jit = njit
    
    
try:
    from tqdm import tqdm
    has_tqdm = True
except ImportError:
    has_tqdm = False
    



@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbdot11(a,b):
    n = a.shape[0]
    o = 0.
    for i in range(n):
        o += a[i]*b[i]
    return o

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbdot21(a,b):
    n = a.shape[0]
    m = a.shape[1]
    o = np.zeros(m)
    for i in range(n):
        for j in range(m):
            o[i] += a[i,j]*b[j]
    return o

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbdot(a,b):
    n = a.shape[0]
    m = b.shape[1]
    KK = b.shape[0]
    c = np.zeros((n,m))
    for i in range(n):
        for j in range(m):
            for k in range(KK):
                c[i,j] += a[i,k]*b[k,j]
    return c

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbdot_t(a,b):
    n = a.shape[0]
    m = b.shape[1]
    K = b.shape[0]
    c = np.zeros((n,K))
    for i in range(n):
        for j in range(K):
            for k in range(m):
                c[i,j] += a[i,k]*b[j,k]
    return c

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nb_tdot(a,b):
    n = a.shape[1]
    m = b.shape[1]
    K = b.shape[0]
    c = np.zeros((n,m))
    for i in range(n):
        for j in range(m):
            for k in range(K):
                c[i,j] += a[k,i]*b[k,j]
    return c

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbnorm(x,s):
    o = 0.
    n = x.shape[0]
    for j in range(n):
        dum=0.
        for i in range(n):
            dum+=x[i]*s[i,j]
        o+=dum*x[j]
    return o

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def nbnorm2(x,s):
    # np.einsum('ij,jk,kl->il',x,s,x.T)
    I = x.shape[0] 
    J = s.shape[0] 
    K = s.shape[1]
    L = x.shape[0]
    o = np.zeros((I,L)) 
    for i in range(I):
        for k in range(K):
            dum = 0
            for j in range(J):
                dum += x[i,j] *s[j,k]
            for l in range(L):
                o[i,l] += dum * x[l,k]
    return o


#dont know, for some reason numba inopython is slower ...
@jit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def inverse(inn):
    #out = np.asarray(np.linalg.inv(inn))
    out = np.linalg.inv(inn)
    #out = sp.linalg.inv(inn)
    return out

@jit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def left_inverse(inn):
    #np.dot(inverse(np.dot(inn.T, inn)))
    inntinn = nb_tdot(inn,inn)
    i_inntinn = inverse(inntinn)
    out = nbdot_t(i_inntinn,inn)
    return out


@jit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)# , parallel=True)
def inverse_batch(inn):
    out = np.zeros(inn.shape)
    for i in range(out.shape[0]):
        out[i] = np.linalg.inv(inn[i])
    return out



#inverse = np.linalg.inv

#def inverse(inn):
    #try:
        #out = invert(inn)
    #except linalg.LinAlgError:
        ## TODO
        ## Is this always smart?
        ## it breaks functional programming, testing .....
        ## or better to flag ..
        ## out=np.zeros_like(inn)
        #out = inn*0.
    #return out

# @njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def check_increment(ix, sri, eps):
    """
    see Rodgers for details...
    input: 
      ix : increment of x_i+1 = x_i -ix
      sri: inverse of retrieval error co-variance
    """
    #return nbdot11(ix.T, nbdot21(sri, ix)) < (eps * ix.size)
    #conv = np.dot(ix.T, np.dot(sri, ix))
    conv =  nbnorm(ix,sri)
    return conv  < (eps * ix.size)

# @njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def check_increment_batch(ix, sri, eps):
    conv =  np.zeros(ix.shape[0])
    for i in range(ix.shape[0]):
        #conv[i] =  np.dot(ix[i].T, np.dot(sri[i], ix[i]))
        conv[i] =  nbnorm(ix[i],sri[i])
    return conv < (eps * ix.shape[1])


@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def gain_aver_cost(x, y, k , xa, sei, sai, rec):
    """
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
    """
    kt_sei = nbdot(k.T, sei)
    gain = np.dot(rec, kt_sei)
    aver = nbdot(gain, k)
    #cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + np.dot(y.T, np.dot(sei, y))
    cost = nbnorm((xa-x),sai)+nbnorm(y,sei)
    return gain, aver, cost

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def gain_aver_cost_batch(x, y, k , xa, sei, sai, rec):
    gain = np.zeros((x.shape[0],sai.shape[1],sei.shape[1]))
    aver = np.zeros((x.shape[0],sai.shape[1],sai.shape[1]))
    cost = np.zeros(x.shape[0])
    for i in range(x.shape[0]):
        dum =  gain_aver_cost(x[i], y[i], k[i] , xa[i]
                              , sei[i], sai[i], rec[i])
        gain[i] = dum[0]
        aver[i] = dum[1]
        cost[i] = dum[2]
    return gain, aver, cost
    

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def dof_infocont_retrnoise_smootherrr(sa, se, sri, gg, av, ig):
    """
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
    """
    dof = np.trace(av)
    # information content h
    #try:
    if ig ==1:
        w, v = np.linalg.eigh(av)
        h = 0.5 * np.log(1 + w ** 2).sum()
    else:
        h = np.nan
    #except Exception as eee:
        ## print(eee)
        #w, v = np.zeros(av.shape[0]) + np.nan, np.zeros_like(av) + np.nan
    # w, v = linalg.eig(av)
    #sn = np.dot(gg, np.dot(se, gg.T))
    sn = nbnorm2(gg,se)
    ia = np.identity(av.shape[0]) - av
    #sme = np.dot(ia, np.dot(sa, ia.T))
    sme = nbnorm2(ia,sa)
    return dof, h, sn, sme

@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def dof_infocont_retrnoise_smootherrr_batch(sa, se, sri, gg, av, ig):
    dof = np.zeros(sa.shape[0])+np.nan
    h = np.zeros(sa.shape[0])+np.nan
    sn = np.zeros(sa.shape)+np.nan
    sme = np.zeros(sa.shape)+np.nan
    for i in range(sa.shape[0]):
        dum = dof_infocont_retrnoise_smootherrr(sa[i]
                , se[i], sri[i], gg[i], av[i], ig[i])
        dof[i] = dum[0]
        h[i] = dum[1]
        sn[i] = dum[2]
        sme[i] = dum[3]
    return dof, h, sn, sme




@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def gauss_newton_increment_nform_a(x,y,k,xa,sei,sai,se,sa,gamma=0.0,condmax=1000000000.):
    """
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
        illposed: True|False

    Equation: 5.8 (with negative sign put into brackets)
    This variant is an  'n-form' (n = dimension of state)
    """
    kt_sei = nbdot(k.T, sei)
    kt_sei_k = nbdot(kt_sei, k)
    ret_err_cov_i = (1.0 + gamma) * sai + kt_sei_k
    #print('ret_err_cov_i',ret_err_cov_i)
    #print(inverse(ret_err_cov_i))
    #print('cond',np.linalg.cond(ret_err_cov_i))
    #if np.linalg.cond(ret_err_cov_i) > condmax:
    if False:
        out = x, x*0, ret_err_cov_i, ret_err_cov_i*0, 1
    else:
        ret_err_cov = inverse(ret_err_cov_i)
#         try: 
#             ret_err_cov = inverse(ret_err_cov_i)
#         except:
#             print(sai,k.T, sei,kt_sei, kt_sei_k, ret_err_cov_i,'asdfghj')
        kt_sei_y = np.dot(kt_sei, y)
        #print(sai.shape,xa.shape,x.shape)
        #sai_dx = np.dot(sai, xa - x)
        sai_dx = nbdot21(sai, xa - x)
        incr_x = nbdot21(ret_err_cov, kt_sei_y - sai_dx)
        nx = x - incr_x
        out = nx, incr_x, ret_err_cov_i, ret_err_cov, 0 
    return out
#TODO: add the others...

###Newton least square
@njit(cache=NBCACHE, fastmath=FASTMATH, nogil=NOGIL)
def newton_operator_least_square(x,y,k,xa,sei,sai,se,sa,gamma=0.0,condmax=1.e10):
    ki = left_inverse(k)
    incr_x = np.dot(ki, y)
    nx = x - incr_x
    out = nx, incr_x, sai, sa, 0 
    return out


def internal_optimizer(y, xa, fg, sei, sai, se, sa, eps, maxiter,
                       forward, param, full, gnform,ll,ul,clip, 
                       batch=False, progress=False
                        ):
    """
    y:       measurement
    forward:  function to be inverted
    param:   parameter for func 
    xa:      prior state
    fg:      first guess state
    sei:     inverse of measurement error covariance matrix
    sai:     inverse of prior error covariance matrix
    se :     measurement error covariance matrix
    sa :     prior error covariance matrix
    eps:     if (x_i-x_i+1)^T # S_x # (x_i-x_i+1)   < eps * N_x, optimization
             is stopped. This is the *original* as, e.g. proposed by Rodgers
    maxiter: maximum number of number_of_iterations
    ll:      lower limits
    ul:      upper limits
    clip:    if true, actively clips state to given minmax 
    
    if batch is true, with N elements everywhere ...
    """
    if gnform == "m":
        increment = gauss_newton_increment_mform_c
    elif gnform == "mc":
        increment = gauss_newton_increment_mform_c
    elif gnform == "n":
        increment = gauss_newton_increment_nform_a
    elif gnform == "na":
        increment = gauss_newton_increment_nform_a
    elif gnform == "nb":
        increment = gauss_newton_increment_nform_b
    elif gnform == "nc":
        increment = gauss_newton_increment_nform_c
    elif gnform == "ls":
        increment = newton_operator_least_square
    elif gnform == "nlm":
        increment = gauss_newton_increment_nform_a
        gamma = LMSTART
    else:
        raise WrongInputError("Unknown key for internal optimizer: %s" % gnform)

    #TODO: test, if clip of input is realy part of interpolation
    #func = forward
    if clip is True:
        def func(x,p):
            xclip = x.clip(ll, ul)
            return forward(xclip, p)
    else:
        func = forward
    #import inspect
    if batch is True:        
        @njit(cache=NBCACHE, nogil=NOGIL)
        def increment_fun(xn, yn, kk, xa, sei, sai, se, sa,increment=increment):
            nnn = xn.shape[0]
            x, ix, illposed = np.zeros(xn.shape)+np.nan, np.zeros(xn.shape)+np.nan, np.ones(xn.shape)
            sri, sr  = np.zeros(sai.shape)+np.nan, np.zeros(sai.shape)+np.nan
            ok = np.isfinite(xn).sum(axis=1) == xn.shape[1]
            for i in range(nnn):
                if ok[i]:
                    dum =  increment(xn[i], yn[i], kk[i], xa[i], sei[i], sai[i], se[i], sa[i])
                    x[i], ix[i], sri[i], sr[i], illposed[i] = dum[0], dum[1], dum[2], dum[3], dum[4]
            return x, ix, sri, sr, illposed 
        check_increment_fun = check_increment_batch
        gain_aver_cost_fun = gain_aver_cost_batch
        dof_infocont_retrnoise_smootherrr_fun = dof_infocont_retrnoise_smootherrr_batch
        itr = np.ones(y.shape[0],dtype=np.int32)
    else:
        increment_fun = increment
        gain_aver_cost_fun = gain_aver_cost
        dof_infocont_retrnoise_smootherrr_fun = dof_infocont_retrnoise_smootherrr
        check_increment_fun = check_increment
        itr = 0

    # 1. first run 
    xn = fg+0
    # 2. optimization
    if progress and has_tqdm:
        rrrr = tqdm(range(maxiter))
    else:
        rrrr = range(maxiter)
    for ii in rrrr:
        if (ii == 0) or (batch is False):
            kk, kp, yn = func(xn, param)

#             #TODO: clean it to overcome ndims inconsistency if 1d batch
#             #probably need adaption of interpolators ....
#             #since yn need to be of shape (n_samples,1)
#             #but forward interpolator makes (n_samples ...)
#             if (batch and isinstance(yn,np.ndarray) 
#                       and isinstance(y,np.ndarray) 
#                       and (y.ndim==2) 
#                       and (y.shape[1] ==1)):
#                 yn.shape=y.shape
            yn -= y    
            xn, ix, sri, sr, illposed = increment_fun(xn, yn, kk, xa, sei, sai, se, sa)
            conv = check_increment_fun(ix, sri, eps)
            if batch is True:
                idx = conv == False
                if idx.sum() <1: break
            else:
                itr+=1
                if conv: break
        else: #batch is true and i >=1
            #print(idx.sum())
            #for _ in param[idx]: print(_,end=';')
            #for _ in xn[idx]: print(_,end=';')
            dum = func(xn[idx], param[idx])
            kk[idx], kp[idx], yn[idx] = dum[0], dum[1], dum[2]
            yn[idx] -= y[idx]
            dum = increment_fun(xn[idx], yn[idx], kk[idx], xa[idx], sei[idx], sai[idx], se[idx], sa[idx])
            xn[idx], ix[idx], sri[idx], sr[idx], illposed[idx] = dum[0], dum[1], dum[2], dum[3], dum[4]
            conv[idx] = check_increment_fun(ix[idx], sri[idx], eps)
            #is_good = np.isfinite(xn[idx]).sum(axis=1) ==0
            #is_good = np.isfinite(xn[idx]).sum(axis=1) == xn[idx].shape[1]
            itr[idx] +=1
            idx = (conv == False) #& is_good 
            if idx.sum() <1: break

    # 3. exit
    if full is False:
        out =  xn, kk, conv, itr, sr, None, None, None, None, None, None, None, kp
    else:
        gg, av, co = gain_aver_cost_fun(xn, yn, kk, xa, sei, sai, sr)
        if batch is True:
            def is_good(ar):
                # only 2d!
                so = ar.shape
                ar.shape = [so[0],-1]
                gi =  np.isfinite(ar).sum(axis=1) == ar.shape[1]
                ar.shape = so
                return gi
            ig = (is_good(gg) & is_good(av)).astype(np.uint8)
        else:
            ig = int(np.isfinite(gg) & np.isfinite(av))
        #print('sa',np.isnan(sa).sum())
        #print('se',np.isnan(se).sum())
        #print('sri',np.isnan(sri).sum())
        #print('gg',np.isnan(gg).sum())
        #print('av',np.isnan(av).sum())
        #print('ig',(1-ig).sum())
        dof, ico, sn, sme = dof_infocont_retrnoise_smootherrr_fun(sa, se, sri, gg, av,ig)
        out = xn, kk, conv, itr, sr, gg, av, co, dof, ico, sn, sme, kp
    return out


def test_inv():
    from scipy import linalg

    ss =np.array([
       [0.23445437, 0.48230513, 0.44184925, 0.45474439, 0.31608114,0.76399957],
       [0.24227389, 0.48916436, 0.55575777, 0.41286236, 0.50884837,0.4003219 ],
       [0.79079461, 0.18256529, 0.12522867, 0.74455636, 0.39279145,0.08692018],
       [0.95137443, 0.89103774, 0.11335285, 0.11356052, 0.32246709,0.84285783],
       [0.14807889, 0.67690164, 0.27309598, 0.7345113 , 0.01242223,0.41930299],
       [0.98834445, 0.88791449, 0.80382208, 0.65314407, 0.3306576 ,0.6078443 ]])
    #ss = ss[0:3,0:3]+0
    
    a =time.time()
    for i in range(1000): si = inverse(ss)
    print(time.time()-a, 'us')
    a =time.time()
    for i in range(1000): si = np.linalg.inv(ss)
    print(time.time()-a, 'us')
    a =time.time()
    for i in range(1000): si = linalg.inv(ss)
    print(time.time()-a, 'us')
    #print(si.dot(ss))
    
    #a =time.time()
    #for i in range(1000): si = invert_pupy(ss)
    #print(time.time()-a, 'us')
    
    
    
    
if __name__=='__main__':
    test_inv()


