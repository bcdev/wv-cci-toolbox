# Copyright (c) 2018
# Author(s): 
#   Rene Preusker <rene.preusker@gmail.com>
'''
This is a wrapper around interpolators_pure_python
'''
import numpy as np
from collections import OrderedDict
try:
    #from .external import interpolators_fortran as interpolators
    #from .external import interpolators_cython as interpolators
    #from .external import interpolators_numba_python as interpolators
    from . import interpolators_numba_python as interpolators
    print('FAST NUMBA')
except (ImportError,OSError,ModuleNotFoundError) as err:
    print(err)
    from . import interpolators_pure_python as interpolators
    print('WARNING: YOU ARE USING PURE PYTHON SLOW INTERPOLATION. TALK TO RENE!')


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
    return lambda x, xx=np.array(xx,order='F'): interpolators.linint2index(x,xx)
def generate_interpol_to_index_rev(xx):
    return lambda x, xx=-np.array(xx,order='F'): interpolators.linint2index(-x,xx)


def check_luts(luts):
    for ilut,lut in enumerate(luts):
        print('checking Lut %i'%ilut)
        _= checklut(lut)
    
def check_axes(axes):
    for idim,dim in enumerate(axes):
        print('checking Axes %i'%idim)
        _=checklut(dim)


#lut to func can be combined with a simplified 'last rescent used' (LRU) chache
class RestrictedOrderedDict(OrderedDict):
    def __init__(self, *args, size=int((2**14)//3)-1, restricted=True, **kwargs):
        self._max = size
        self._n = 0
        self._restricted = restricted
        super().__init__(*args, **kwargs)

    #overload __setitem__
    def __setitem__(self, key, value):
        OrderedDict.__setitem__(self, key, value)
        #self.__setitem__(key, value)<- not working ... ups, recursive overload :)
        if self._restricted:
            self._n += 1
            if self._n > self._max:
                self._n -= 1
                # remove the latest in ordered dict
                self.popitem(False)
# My LRU
def simple_restricted_memoizer(func,acc,size=int((2**14)//3)-1):
    cache=RestrictedOrderedDict(size=size)
    fmt="{:.%ie}"%acc
    def new_func(woo):
        uniq_key= ','.join(fmt.format(w) for w in woo)
        if uniq_key not in cache:
            cache[uniq_key]=func(woo)
        return cache[uniq_key]
    return new_func




def lut2func(luts,axes,verbose=False,check_nans=False,lru=False, lru_size=5460, lru_prec=2):
    '''
    Is actually a wrapper around an n-dimensional linear interpolation
    from R^m --> R^n
    
    Input:
    
        lut : tupel of n  m-dimensional np-arrays
              or an m+1 dimensional np-array
        axes: tupel of m 1d-numpy arrays
        
        check nans works only on tuple luts!
        
    Returns a function    
    
    '''
 
    
    #1. check validity of input:
    if isinstance(luts,tuple):
        if verbose: check_luts(luts)
        for ilut,lut in enumerate(luts):
            if not isinstance(lut,np.ndarray):
                print(ilut,' element is not an Numpy ndarray')
                return None
            if ilut==0:
                lut_dtype=lut.dtype
            else:
                if lut.dtype != lut_dtype:
                    print('Lut ',ilut,' dtype does not agree with Lut 0 dtype')
                    print(lut.dtype, '<>', lut_dtype)
                    return None
                                    
        
        shap=luts[0].shape
        ndim=luts[0].ndim
            
        for ilut,lut in enumerate(luts):
            if shap != lut.shape:
                print(ilut,' ndarray has not the shape of ' , shap) 
                
    elif isinstance(luts,np.ndarray):
        if verbose: check_luts((luts,))
        lut_dtype=luts.dtype
        shap=luts.shape[:-1]
        ndim=luts.ndim-1
        
    else:
        print('Input is neither an tupel of ndarrays nor an ndarray')
        return None       
 
    if not isinstance(axes,tuple):
        print('Axes is not a tuple') 
        return None

    if len(axes) != ndim:
        print('Number of elements of axes (%i)'%len(axes)) 
        print('is not equal the number of dimensions (%i)'%ndim)
        return None
    

    #2. analyze axes
    #
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

 
    #3. generate interpolating function
    if isinstance(luts,tuple):
        if check_nans is True:
            itps=[interpolators.generate_nan_itp(lut) for lut in luts]
        else:   
            itps=[interpolators.generate_itp(lut) for lut in luts]
    elif isinstance(luts,np.ndarray):
        if check_nans is True:
            print("WARNING: Check_nans works only on tuple luts" )
            print('Re-organize your data!')
            return None
        itps=interpolators.generate_itp_pn(luts)

    #3. generate axes 1d interpolators    
    axes_max=[]
    axes_min=[]
    axes_int=[]
    for idim,dim in enumerate(axes):
        
        if not axes_imi[idim]:
            #print 'inverting dimension'
            axes_int.append(generate_interpol_to_index_rev(dim.astype(lut_dtype)))
            axes_max.append(dim[0])
            axes_min.append(dim[-1])
        else:
            axes_int.append(generate_interpol_to_index(dim.astype(lut_dtype)))
            axes_max.append(dim[-1])
            axes_min.append(dim[0])
    
    if isinstance(itps,list):
        def function(wo):
            woo=np.array([axe(w) for axe,w in zip(axes_int,wo)],order='F',dtype=lut_dtype)
            out=np.array([itp(woo) for itp in itps],order='F')
            return out
    else:
        def function(wo):
            woo=np.array([axe(w) for axe,w in zip(axes_int,wo)],order='F',dtype=lut_dtype)
            out=itps(woo) 
            return out
    if lru is True:
        return simple_restricted_memoizer(function,lru_prec,lru_size)
    else:
        return function

def test():
    #import lut2func as l2f
           
    #example for R^2-->R^3
    
    luta=np.arange(24,dtype=np.float).reshape(6,4,order='C')
    lutb=np.arange(24,dtype=np.float).reshape(6,4,order='F')**2
    lutc=np.sqrt(np.arange(24,dtype=np.float).reshape(6,4,order='F'))
    luts=(luta,lutb,lutc)
    xx=np.array([3.,4.,6.,7.,9.,15.])
    yy=np.array([1.,5.,10,15])[::-1]
    axes=(xx,yy)
    
    # Variant A: luts is a tuple of luts
    funca=lut2func(luts,axes)

    # Variant B: luts is a Nd Array, last dimension is dimension of result
    luts=np.array((luta,lutb,lutc),dtype=np.float32).transpose([1,2,0])
    funcb=lut2func(luts,axes)
    
    
     
    #sys.exit()
    
    nn=10000
    import time
    pos=np.array([3.5,11.])
    a=time.time()
    for ff in (funca,funcb):
        a=time.time()
        for i in range(nn):
            _=ff(pos)
        print(_,': %.2f us'%((time.time()-a)/float(nn)*1.e6))


def test3to1():        
    #example for R^3-->R^1
    #import lut2func as l2f
    lut3d=np.arange(3*4*5,dtype=np.float).reshape(3,4,5)
    x0=np.array([3.,4.,6.])
    x1=np.array([1.,4.,8.,10.])
    x2=np.array([1.,2.,4.,8.,16.])
    func3d=lut2func((lut3d,),(x0,x1,x2))
    print(func3d([3.5,5.,10.]))
    

if __name__=='__main__':
    test()
    test3to1()
    
    




