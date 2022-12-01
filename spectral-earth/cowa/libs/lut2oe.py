# Copyright (c) 2020
# Author(s):
#   Rene Preusker <rene.preusker@gmail.com>

import os,time
from collections import namedtuple as NT
#from namedlist import namedlist as NL
import numpy as np
import xarray as xr 
import math
from . import interpolators_pupy_numba as interpolators
from . import lut2oe_core as loec
#import interpolators_pupy_numba as interpolators
#import lut2oe_core as loec

#RESULT = NL(
RESULT = NT(
    "result",
    "state "
    "jacobian "
    "convergence "
    "number_of_iterations "
    "retrieval_error_covariance "
    "gain "
    "averaging_kernel "
    "cost "
    "dof "
    "information_content "
    "retrieval_noise "
    "smoothing_error "
    "parameter_jacobian",
    )

class OeCoreError(Exception):
    pass
class LinAlgError(OeCoreError):
    pass
class NonSquareMatrixError(OeCoreError):
    pass
class MissingInputError(OeCoreError):
    pass
class WrongInputError(OeCoreError):
    pass



def checklut(x):
    if not isinstance(x,np.ndarray) : 
        print('not numpy array',type(x))
    if (x.dtype != np.float64) and (x.dtype != np.float32) : 
        print('not float')

def is_monotonically_increasing(v):
    for i, e in enumerate(v[1:],1):
        if e <= v[i-1]:
            return False
    return True

def is_monotonically_decreasing(v):
    for i, e in enumerate(v[1:],1):
        if e >= v[i-1]:
            return False
    return True

def generate_interpol_to_index(xx):
    return lambda x ,xx=np.array(xx): interpolators.linint2index(x,xx)
def generate_interpol_to_index_rev(xx):
    return lambda x ,xx=-np.array(xx): interpolators.linint2index(-x,xx)

def generate_interpol_to_index_batch(xx):
    return lambda x ,xx=np.array(xx): interpolators.linint2index_batch(x,xx)
def generate_interpol_to_index_rev_batch(xx):
    return lambda x ,xx=-np.array(xx): interpolators.linint2index_batch(-x,xx)

def check_luts(luts):
    for ilut,lut in enumerate(luts):
        print('checking Lut %i'%ilut)
        _= checklut(lut)
        
def check_axes(axes):
    for idim,dim in enumerate(axes):
        print('checking Axes %i'%idim)
        _=checklut(dim)

def xarray2func(ds, arrayname, nd=True, verbose=True, batch=False, parallel=False):
    '''
    a wraper around lut2func 
    usage see example..
    input:
        ds:         xarray dataset
        arrayname:  array within ds, which is used to create the 
                    interpolating function
        nd:         True|False, 
                       * if true (default), the last dimension
                         is used as the value to interpolate to
                         basically an interpolation
                                R^(N-1) --> R^(number of elements 
                                of last dimension) 
                       * if false, the full array is used, basically 
                         an interpolation R^N --> R^1 
    '''
    if arrayname not in ds:
        print('%s is not a dataset'%arrayname) 
        return None
    lut = np.array(ds[arrayname][:])
    
    if nd is True:
        axes_names = tuple(di for di in ds[arrayname].dims)[:-1]
        lut_name = ds[arrayname].dims[-1]
    else:
        axes_names = tuple(di for di in ds[arrayname].dims)
        lut_name = arrayname
    axes = []
    for an in axes_names:
        if an in ds:
            axes.append(np.array(ds[an][:]))
        else:
            axes.append(np.arange(ds[arrayname].sizes[an],dtype=np.float64))
    axes = tuple(axes)
    return lut2func(lut,axes,verbose=verbose,axes_names=axes_names,lut_name=lut_name
                    ,batch=batch,parallel=parallel)


def xarray2oe(ds, arrayname,state_index=None, nd=True, verbose=True, batch=False, parallel=False):
    '''
    a wraper around lut2oe 
    usage see example..
    input:
        ds:         xarray dataset
        arrayname:  array within ds, which is used to create the 
                    interpolating function
        state_index
        nd:         True|False, 
                       * if true (default), the last dimension
                         is used as the value to interpolate to
                         basically an interpolation
                                R^(N-1) --> R^(number of elements 
                                of last dimension) 
                       * if false, the full array is used, basically 
                         an interpolation R^N --> R^1 
    '''
    if arrayname not in ds:
        print('%s is not a dataset'%arrayname) 
        return None
    lut = np.array(ds[arrayname][:])
    
    if nd is True:
        axes_names = tuple(di for di in ds[arrayname].dims)[:-1]
        lut_name = ds[arrayname].dims[-1]
    else:
        axes_names = tuple(di for di in ds[arrayname].dims)
        lut_name = arrayname
    axes = []
    for an in axes_names:
        if an in ds:
            axes.append(np.array(ds[an][:]))
        else:
            axes.append(np.arange(ds[arrayname].sizes[an],dtype=np.float64))
    axes = tuple(axes)
    
    return lut2oe(lut, axes, state_index = state_index,verbose=verbose
                    ,axes_names=axes_names,lut_name=lut_name,batch=batch,parallel=parallel)



def lut2func(lut,axes,verbose=True,axes_names=(),lut_name='',batch=False, parallel=False):
    '''
    Is actually a wrapper around an n-dimensional linear interpolation
    from R^n --> R^m
    
    Input:
    
        lut : an n+1 dimensional np-array, last dimension has m elements
              if lut has n dimension, R^n -> R^1 is assumed 
        axes: tupel of n 1d-numpy arrays
    Returns a function    
    '''
    if verbose: 
        check_luts((lut,))
        
    if not isinstance(axes,tuple):
        print('Axes is not a tuple') 
        return None
    if len(axes) == lut.ndim:
        shap = lut.shape
        ndim = lut.ndim
        nout = 1
        #is_1d = True
        lut = lut[...,np.newaxis]
        is_1d = False
    elif len(axes) == lut.ndim -1:
        shap = lut.shape[:-1]
        ndim = lut.ndim-1
        nout = lut.shape[-1]
        is_1d = False
    else:
        print('Number of elements of axes (%i)'%len(axes)) 
        print('is not equal the number of dimensions (%i)'%ndim)
        print('or the number of dimensions -1 (%i)'%(ndim-1))
        return None

    # analyze axes
    axes_imi=[]
    axes_imd=[]

    for idim,dim in enumerate(axes):
        if not isinstance(dim,np.ndarray): 
            print('Axes %i is not an Numpy ndarray' % idim)
            return None
        if not dim.ndim == 1:
            print('Axes %i is not an 1D Numpy ndarray' % idim)
            return None
        if len(dim) != shap[idim]:
            print('Axes %i does not agree with the shape of LUT' % idim)
            print('Axes %i has %i elements but LUT needs %i.' % (idim,len(dim),shap[idim]))
            return None
        axes_imi.append(is_monotonically_increasing(dim))
        axes_imd.append(is_monotonically_decreasing(dim))
        if not (axes_imi[-1] or axes_imd[-1]) :
            print('Axes %i is neither monotonically increasing nor decreasing' % idim)
            print(dim)
            print('Re-organize your data!')
            return None
    #scalings for jacobian        
    sca = tuple(a[1:]-a[:-1] for a in axes)

    if axes_names == ():
        axes_names = ['inp_%i'%i for i in range(len(axes))]

    if batch is True and parallel is False:
        gen_itp_pn_jac = interpolators.generate_itp_pn_jac_batch
        gen_itp_jac = interpolators.generate_itp_jac_batch
        gen_itp_pn = interpolators.generate_itp_pn_batch
        gen_itp = interpolators.generate_itp_batch
        ind2idx_r = generate_interpol_to_index_rev_batch
        ind2idx = generate_interpol_to_index_batch
    elif parallel is True:
        gen_itp_pn_jac = interpolators.generate_itp_pn_jac_parallel
        gen_itp_jac = interpolators.generate_itp_jac_parallel
        gen_itp_pn = interpolators.generate_itp_pn_parallel
        gen_itp = interpolators.generate_itp_parallel
        ind2idx_r = generate_interpol_to_index_rev_batch
        ind2idx = generate_interpol_to_index_batch
    else:
        gen_itp_pn_jac = interpolators.generate_itp_pn_jac
        gen_itp_jac = interpolators.generate_itp_jac
        gen_itp_pn = interpolators.generate_itp_pn
        gen_itp = interpolators.generate_itp
        ind2idx_r = generate_interpol_to_index_rev
        ind2idx = generate_interpol_to_index

    if is_1d:
        _itps_jaco = gen_itp_jac(np.ascontiguousarray(lut),sca)
        def itps_jaco(*args,**kwargs):
            j,s = _itps_jaco(*args,**kwargs)
            return j[np.newaxis,...],s
        itps_jaco.__doc__  = _itps_jaco.__doc__
        itps = gen_itp(np.ascontiguousarray(lut))
    else:
        itps_jaco = gen_itp_pn_jac(np.ascontiguousarray(lut),sca)
        itps = gen_itp_pn(np.ascontiguousarray(lut))   
        
    # generate axes 1d functions    
    axes_int = []
    for idim,dim in enumerate(axes):
        if not axes_imi[idim]:
            axes_int.append(ind2idx_r(dim))
        else:
            axes_int.append(ind2idx(dim))

    def function(wo, jaco=True, itps=itps, itps_jaco=itps_jaco):
        woo=np.array([axe(w) for axe,w in zip(axes_int,wo.T)]).T
        if jaco:
            out = itps_jaco(woo)
        else:
            out = itps(woo) 
        return out
    doc = '''
        This function provides an interpolation of
        R^%i --> R^%i
        (%s) to (%s)
        %s

        Input 
            wo:    (%s %i) element  1-d np.array
          jaco:    True|False, if true jacobian is returned

        Output
         jaco, y  if jacobian is set True
         y        if jacobian is not set True
        '''
    if batch or parallel: 
        s,ss = 'for N samples (in first dimension)', 'N ,'
    else:
        s,ss = '',''
    function.__doc__ = doc % (ndim, nout, 
                        ', '.join(axes_names), lut_name,s,ss,ndim)
    
    return function

def lut2oe(lut, axes, state_index=None, verbose=True, axes_names=(), lut_name='result',
           clip_delta=1.e-5, batch=False, parallel = False):
    """
    This finds the roots of the following equation:
    y=func(x,params)
    and returns a function which is effectively
    the inverse of func
    x=func⁻¹(y,fparam=params)
    
    The speciallity here is, that func is a n-linear interpolation 
    within LUT. If LUT, axes and state index (see below) are well 
    defined, everything should work easily 
    
    lut:  numpy nd.array. The look up table. It is assumed, that the last dimension 
          contains all measurements, all other dimension contain state and params
          
    axes: tupel of m 1d-numpy arrays, giving the physical quantities at the sampling 
          points of the lut
    
    The lut 'contains' the function values for   R^n --> R^m, thus it must have 
    n+1 dimensions!  The last dimension must have m sampling point/elements!
        
    state_index:
        gives the place( =index) where the input vector
        stopes to describe the *variable* state and begins to describe
        the *fully constrained* state. (From RTM point of view there is no difference, 
        but of course from physical retrieval point of view...). If not given 
        or equal to the number of axes, no fully constrained parameter are asumed
    """

    generic_forward = lut2func(lut,axes,verbose,axes_names=(),lut_name='', batch=batch, parallel=parallel)
    if generic_forward is None:
        return None

    parch = batch or parallel
    
    len_axes = len(axes)
    
    if state_index is None:
        state_index = len_axes 
    
    state_index_ok = ( isinstance(state_index,int)
                      and (state_index <= len_axes)
                      and (state_index >= 1))

    if not state_index_ok:
        print('State index must be an integer: 1 <= state_index < %i'%len_axes, ', but is:',state_index)
        return None
    
    if len(axes_names) != len_axes:
        print(axes_names)
        axes_names =tuple('inp_%i'%i for i in range(len_axes))

    len_xx = state_index
    len_pa = len_axes - len_xx
    if len_axes == lut.ndim:  
        len_yy = 1
    else:
        len_yy = lut.shape[-1]

    if parch:
        def forward(x,p=None):    
            inp = np.zeros((x.shape[0],len_axes))
            inp[...,:state_index] = x
            inp[...,state_index:] = p
            j, r = generic_forward(inp)
            return j[...,:state_index],j[...,state_index:],r  # jacobian, parameter_jacobian, interpolated
    else:
        inp = np.zeros((len_axes))
        def forward(x,p=None):    
            inp[:state_index] = x
            inp[state_index:] = p
            j, r = generic_forward(inp)
            return j[...,:state_index],j[...,state_index:],r  # jacobian, parameter_jacobian, interpolated
    doc='''
        Forward function of given lut. Interpolates %s
        at : 
         x   state      (%s)
         p   parameter  (%s)
        returns jacobian, jacobian with respect to parameter and to interpolated value.
        
        Basically this function provides an interpolation of
        (R^%i, R^%i) --> R^%i
        ((state), (parameter)) --> (measurement)
        ((%s), (%s)) --> (%s)

        Input 
            wo:    %s %i) element 1-d np.array
            p:     %s %i) element 1-d np.array

        Output
         jaco, jaco_p, y 
        '''
    dum = '(N,' if parch else '('
    forward.__doc__ = doc%(lut_name,
                           ', '.join(axes_names[:state_index]),
                           ', '.join(axes_names[state_index:]),
                           len_xx,len_pa,len_yy,
                                       ', '.join(axes_names[:state_index]),
                                       ', '.join(axes_names[state_index:]),
                                       lut_name,dum,len_xx,dum,len_pa)
    
    def min_plus_delta(ax):
        # assume is monoton (has been tested before)
        if ax[0] < ax[-1]:
            out = ax[ 0] + (ax[ 1] - ax[ 0])*clip_delta
        else:
            out = ax[-1] + (ax[-2] - ax[-1])*clip_delta
        return out
    def max_minus_delta(ax):
        # assume is monoton (has been tested before)
        if ax[0] < ax[-1]:
            out = ax[-1] - (ax[-1] - ax[-2])*clip_delta
        else:
            out = ax[ 0] - (ax[ 0] - ax[ 1])*clip_delta
        return out


    #lower and upper limits
    ll= np.array([min_plus_delta(ax) for ax in axes])[:state_index]
    ul= np.array([max_minus_delta(ax) for ax in axes])[:state_index]
    

    def func_inverse(yy, pa=None, se=None, sa=None, xa=None, eps=0.01,
                    # fg=None, maxiter=10, full=True, clip=True, gnform='n',
                    fg=None, maxiter=10, full=True, clip=True, gnform='n',
                    ll=ll, ul=ul, sp=None, progress=False):
        """
        Inverse function of given lut. Estimates the 
        optimal state *xx* , that explains the measurement *yy*.
        Actually this function provides an optimal estimation of
        
        (R^%i, R^%i) --> R^%i
        ((measurement), (parameter)) --> (state)
        ((%s), (%s)) --> (%s)
        
        assuming an apriori of (%s) and respective 
        error co-variances.

        necessary Input:
              yy = measurement (@%i)
              pa = parameter  (@%i)
              se = measurement error co-variance matrix (@%i,%i)
              sa = prior error co-variance matrix (@%i,%i)
              xa = prior (@%i)
        optional Input:
              fg = first guess (=xa if not set)
             eps = Rodgers convergence criteria eps
          gnform = Gauss Newton increment ('n' form or 'm' form)
            full = full output: boolean
            clip = clip state to min max: boolean
              sp = parameter error co-variance matrix (@%i,%i)
              ll = lower limit of state (%i)
              ul = upper limit of state (%i)
        Output:
            named list, containing the optimal state and diagnostics
        """

        if sa is None:
            raise MissingInputError("sa is missing")
        if se is None:
            raise MissingInputError("se is missing")
        if xa is None:
            raise MissingInputError("xa (prior) is missing")
        if (pa is None): 
            if (state_index < (len(axes)-1)): 
                raise MissingInputError("pa is missing")
        else:
            if (pa.ndim != 2 and parch) or (pa.ndim != 1 and not parch):
                eee = [1,2][parch]
                raise WrongInputError("pa has wrong number of dimensions. Expected %i got %i" %(eee,pa.ndim))
            if len_pa != pa.shape[-1]:
                raise WrongInputError("pa has wrong number of elements")
            #if parch and (pa.ndim != 3):
            #     raise WrongInputError("pa has wrong dimensions")               
        if (sa.ndim != 3 and parch) or (sa.ndim != 2 and not parch):
            raise WrongInputError("sa has wrong dimensions")
        if (se.ndim != 3 and parch) or (se.ndim != 2 and not parch):
            raise WrongInputError("se has wrong dimensions")
        if (xa.ndim != 2 and parch) or (xa.ndim != 1 and not parch):
            raise WrongInputError("xa has wrong dimensions")
        if (yy.ndim != 2 and parch) or (yy.ndim != 1 and not parch):
            raise WrongInputError("yy has wrong dimensions")
        if sa.shape[-2] != sa.shape[-1]:
            raise WrongInputError("sa is not quadratic")
        if se.shape[-2] != se.shape[-1]:
            raise WrongInputError("se is not quadratic")
        if sa.shape[-1] != xa.shape[-1]:
            raise WrongInputError("sa and xa are incompatible")
        if se.shape[-1] != yy.shape[-1]:
            raise WrongInputError("se and yy are incompatible")
        if len_yy != yy.shape[-1]:
            raise WrongInputError("yy has wrong number of elements")
        if len_xx != xa.shape[-1]:
            raise WrongInputError("xa has wrong number of elements")
        #TODO: more checks ...

        if parch:
            sai = loec.inverse_batch(sa)
            sei = loec.inverse_batch(se)
        else:
            sai = loec.inverse(sa)
            sei = loec.inverse(se)
        
        # 1. prior as first guess if not set
        if fg is None:
            fg = xa+0
 
        
        result = RESULT(*loec.internal_optimizer(y=yy, xa=xa, fg=fg, sei=sei,
                 sai=sai, se=se, sa=sa, eps=eps, maxiter=maxiter,
                 forward=forward, param=pa, full=full, gnform=gnform,
                 ll=ll, ul=ul, clip=clip, batch=parch, progress=progress) )
        if clip:
            result.state[:] = result.state.clip(ll,ul)
        return result
    func_inverse.__doc__ = func_inverse.__doc__%(len_yy,len_pa,len_xx,
                                       lut_name,
                                       ', '.join(axes_names[state_index:]),
                                       ', '.join(axes_names[:state_index]),
                                       ', '.join(axes_names[:state_index]),
                                       len_yy,len_pa,len_yy,len_yy,
                                       len_xx,len_xx,len_xx,len_pa,len_pa,
                                       len_xx,len_xx,)
    
    
    func_inverse.__doc__ += '\n              '.join(RESULT._fields)
    func_inverse.__doc__ += '\n        Default (lut) limits:\n'
    str_limits = '\n           ' + '\n           '.join(
        '%s: %f ... %f'%(an,aa.min(),aa.max()) for  aa,an  in zip(axes,axes_names))
    func_inverse.__doc__ += str_limits

    if parch is True:
        func_inverse.__doc__ += '\nbatch/parallel has been set, every input must be ' 
        func_inverse.__doc__ += 'N-fold (in additional first dimension...)'
        func_inverse.__doc__ = func_inverse.__doc__.replace('@','N,')
    else:
        func_inverse.__doc__ = func_inverse.__doc__.replace('@','')        

    return func_inverse, forward






def test_2to3():
    #example for R^2-->R^3
    import lut2oe as l2oe
    luta=np.arange(24,dtype=np.float64).reshape(6,4,order='C')
    lutb=(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F')**2).reshape((6,4),order='C')
    lutc=np.sqrt(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F'))
    

    xx=np.array([3.,4.,6.,7.,9.,15.])
    yy=np.array([1.,5.,10,15])[::-1]
    axes=(xx,yy)
    luts=np.array((luta,lutb,lutc)).transpose([1,2,0])
    #print(luts.shape)
    func=l2oe.lut2func(luts,axes,verbose=False)
    #print(luts.shape,len(axes))
    #sys.exit()
    pos=np.array([3.5,11.])
    #Jacobians are not working at and behind limits
    #pos=np.array([14.,14.9])
    #pos=np.array([3.1,1.1])
    #pos=np.array([3.,1.])/1.2
    j, r = func(pos)
    print('default:\n','2.8 34.1 2.13809642')
    print('intern:\n',r)
    
    sfun = lambda x: func(x,jaco=False)
    jff = interpolators._test_approximate_jacobian_function(sfun)
    print('default:\n',jff(pos,dx=np.array([0.001,0.001])), '\n intern: \n',j)

def test_3to1():        
    #example for R^3-->R^1
    import lut2oe as l2oe
    lut3d=np.arange(3*4*5,dtype=np.float64).reshape(3,4,5)
    x0=np.array([1.,4.,6.])
    x1=np.array([1.,4.,8.,10.])
    x2=np.array([1.,2.,4.,8.,16.])
    func3d=l2oe.lut2func(lut3d,(x0,x1,x2))
    pos = np.array([1.5,2.3,3.7])
    j,r = func3d(pos)
    sfun = lambda x: np.array([func3d(x,jaco=False)])
    jff = interpolators._test_approximate_jacobian_function(sfun)
    print('default:\n',jff(pos,dx=np.array([0.001,0.001,0.001])), '\n intern: \n',j,j.shape)

def test_oe1():
    import lut2oe as l2oe
    luta=np.arange(24,dtype=np.float64).reshape(6,4,order='C')
    lutb=(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F')**2).reshape((6,4),order='C')
    lutc=np.sqrt(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F'))
    x0=np.array([3.,4.,6.,7.,9.,15.])
    x1=np.array([1.,5.,10,15])[::-1]
    axes=(x0,x1)
    luts=np.array((luta,lutb,lutc)).transpose([1,2,0])
    func=l2oe.lut2func(luts,axes,verbose=False)
    pos = np.array([3.5,11.])

    print(func(pos)[1])
    
    ifun, forw = l2oe.lut2oe(luts,axes,2,verbose=False)
    print(ifun.__doc__)
    
    sa=np.eye(2)*10.
    se=np.eye(3)
    
    print(forw(pos))
    yy = np.array([2.8,34.1,2.13809642])
    xa = np.array([4,10.])
    
    print(ifun(yy=yy,se=se,sa=sa,xa=xa))
    np.testing.assert_array_almost_equal(ifun(yy=yy,se=se,sa=sa,xa=xa).state, pos, decimal=2)

def test_oe_1d():
    import lut2oe as l2oe
    luta=np.arange(24,dtype=np.float64).reshape(6,4,order='C')
    lutb=(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F')**2).reshape((6,4),order='C')
    lutc=np.sqrt(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F'))
    x0=np.array([3.,4.,6.,7.,9.,15.])
    x1=np.array([1.,5.,10,15])[::-1]
    x2=np.array([-10,-2,5.])
    axes=(x0,x1,x2)
    luts=np.array((luta,lutb,lutc)).transpose([1,2,0])
    print(luts.shape)
    func=l2oe.lut2func(luts,axes,verbose=False)
    pos = np.array([3.5,11.,-2])

    print(func.__doc__)
    print('hallo')
    print(func(pos))
    #
    
    ifun, forw = l2oe.lut2oe(luts,axes,1,verbose=False)
    print(forw.__doc__)
    print(ifun.__doc__)
    
    sa=np.eye(1)*10.
    se=np.eye(1)/1000
    
    fpos = pos[0:1], pos[1:]
    print(forw(*fpos))
    pa = fpos[1]
    yy = np.array([34.1])
    xa = np.array([3.5])
    #import sys; sys.exit()   
    print(ifun(yy=yy,pa=pa,se=se,sa=sa,xa=xa).state, fpos[0])
    np.testing.assert_array_almost_equal(ifun(yy=yy,pa=fpos[1],se=se,sa=sa,xa=xa).state, fpos[0], decimal=2)
    
    bifun, bforw = l2oe.lut2oe(luts,axes,1,verbose=False,batch=True)
    nnn = 10
    byy = np.tile(yy,(nnn,1))
    bpa = np.tile(pa,(nnn,1))
    bxa = np.tile(xa,(nnn,1))
    bse = np.tile(se,(nnn,1,1))
    bsa = np.tile(sa,(nnn,1,1))
    print(bifun(yy=byy,pa=bpa,se=bse,sa=bsa,xa=bxa).state[0], fpos[0])
  
    
    
    

def test_leastsquares1():
    import lut2oe as l2oe
    luta=np.arange(24,dtype=np.float64).reshape(6,4,order='C')
    lutb=(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F')**2).reshape((6,4),order='C')
    lutc=np.sqrt(np.arange(24,dtype=np.float64).reshape(6,4,order = 'F'))
    x0=np.array([3.,4.,6.,7.,9.,15.])
    x1=np.array([1.,5.,10,15])[::-1]
    axes=(x0,x1)
    luts=np.array((luta,lutb,lutc)).transpose([1,2,0])
    func=l2oe.lut2func(luts,axes,verbose=False)
    pos = np.array([3.5,11.])

    print(func(pos)[1])
    
    ifun, forw = l2oe.lut2oe(luts,axes,2,verbose=False)
    print(ifun.__doc__)
    
    sa=np.eye(2)*10.
    se=np.eye(3)
    
    print(forw(pos)[2])
    yy = np.array([2.8,34.1,2.13809642])
    xa = np.array([4,10.])
    
    print(ifun(yy=yy,se=se,sa=sa,xa=xa,gnform='ls',maxiter=10,full=False))



def test_xarray2func_batch():
    import lut2oe as lut2oe 
    fn='bla_local.nc'
    ds = xr.load_dataset(fn)

    func = xarray2func(ds,'lut',batch=True)
    pfunc = xarray2func(ds,'lut',parallel=True)
    rfunc = lut2oe.xarray2func(ds,'lut')

    wo = np.array([3., 0.1,0.11,0.22,6.94, 290., 110., 23.,35.])
    nnn,jaco=500000*2,False
    woo = np.tile(wo,(nnn,1))
    woo = woo *(1.+ np.random.normal(size=woo.shape)/1000)

    a = time.time()
    erg = func(woo,jaco=jaco)
    print('b',(time.time()-a)*1000/nnn,'ms')

    a = time.time()
    erg = pfunc(woo,jaco=jaco)
    print('p',(time.time()-a)*1000/nnn,'ms')

    a = time.time()
    for i in range(nnn): org = rfunc(woo[i],jaco=jaco)[0]
    print('s',(time.time()-a)*1000/nnn,'ms')

    print(rfunc(woo[10],jaco=jaco)-erg[10])

def test_xarray2oe_batch():
    import lut2oe as lut2oe 
    fn='./bla_local.nc'
    #fn='../tests/test_data/bla_local.nc'
    ds = xr.load_dataset(fn)
    #print(ds)
    ifunc,func = xarray2oe(ds,'lut',3,batch=True)
    pifunc,pfunc = xarray2oe(ds,'lut',3,parallel=True)
    rifunc,rfunc = lut2oe.xarray2oe(ds,'lut',3)
    
    #print(ifunc.__doc__)
    nnn=200000
    wo,pa = np.array([3., 0.1,0.11]),np.array([0.22,6.94, 290., 110., 23.,35.])
    woo = np.tile(wo,(nnn,1))
    woo = woo *(1.+ np.random.normal(size=woo.shape)/1000)    
    par = np.tile(pa,(nnn,1))
    par = par *(1.+ np.random.normal(size=par.shape)/1000)    
    
    yy = np.tile(np.array([0.028, 0.030, 0.14, 0.54]),(nnn,1))
    xa = np.tile(np.array([0.1, 0.11,0.09]),(nnn,1))
    se = np.tile(np.eye(4),(nnn,1,1))
    sa = np.tile( np.eye(3)*100,(nnn,1,1))
    
    
    xa = np.tile(np.array([0.1, 0.11,0.09]),(nnn,1))
    a = time.time()
    borg=pifunc(yy,pa=par,sa=sa,se=se,xa=xa,full=False,gnform='ls',maxiter=3)
    print('**p_ls**', (time.time()-a)*1000/nnn,'ms')
    print(borg.state[-1])

    #import sys; sys.exit()
    xa = np.tile(np.array([0.1, 0.11,0.09]),(nnn,1))
    a = time.time()
    borg=ifunc(yy,pa=par,sa=sa,se=se,xa=xa,full=False,maxiter=3)
    print('**b**', (time.time()-a)*1000/nnn,'ms')
    print(borg.state[-1])

    xa = np.tile(np.array([0.1, 0.11,0.09]),(nnn,1))
    a = time.time()
    borg=pifunc(yy,pa=par,sa=sa,se=se,xa=xa,full=False,maxiter=3)
    print('**p**', (time.time()-a)*1000/nnn,'ms')
    print(borg.state[-1])

    ttt=20
    xa = np.tile(np.array([0.1, 0.11,0.09]),(nnn,1))
    a = time.time()
    for i in range(nnn//ttt): org =rifunc(yy[i],pa=par[i],sa=sa[i],se=se[i],xa=xa[i])
    print('**s**',(time.time()-a)*1000/(nnn//ttt),'ms')
    print(rifunc(yy[i],pa=par[-1],sa=sa[-1],se=se[-1],xa=xa[-1]).state)

    
    


if __name__=='__main__':
    #test_oe1()
    test_oe_1d()
    #test_2to3()
    #test_3to1()
    #test_xarray2func_batch()
    #test_xarray2oe_batch()
    #test_leastsquares1()
 
