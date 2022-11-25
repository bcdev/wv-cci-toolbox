# Copyright (c) 2020
# Author(s):
#   Rene Preusker <rene.preusker@gmail.com>
import numpy as np
import math

# joblib or numba ...


FASTMATH = True  ## warn if true nan test seem not to work!!!
NBCACHE = True
NOGIL = True

try: 
    import psutil
    CPU_COUNT = psutil.cpu_count(logical=False)
except ImportError:
    try: 
        import joblib
        CPU_COUNT = joblib.cpu_count()
    except ImportError:
        CPU_COUNT = 4
except ImportError:
    CPU_COUNT = 4

#CPU_COUNT = 4
print('CPU%sCOUNT %i'%(CPU_COUNT*'_',CPU_COUNT))


try:
    from numba import njit #,jit
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

try: 
    from joblib import Parallel
except ImportError:
    print('No joblib.--> no parallel!')
    print('No joblib.--> no parallel!')
    print('Use batch!')



@njit(cache=NBCACHE, fastmath=FASTMATH)
def find_hypercube(w,s,n):
    out = np.empty((n,2),dtype=np.int64)
    for i in range(n):
        ss = s[i]-1
        ww = w[i]
        out[i,0] = max(min(int(math.floor(ww)),ss),0)
        out[i,1] = max(min(int(math.floor(ww+1)),ss),0)
        #out[i,1] = min(int(math.floor(ww+1)),ss)
        #out[i,0] = max(out[i,1]-1,0)
    return out

@njit(cache=NBCACHE, fastmath=FASTMATH)
def calc_dist(w,c,n,tol=0.):
    #mol,tol prevents 0/0 in calculations of jacobian
    mol = 1.-tol
    out = np.empty((n,2))
    for i in range(n):
        out[i,1] = max(min(w[i]-c[i,0],mol),tol)
        out[i,0] = 1. - out[i,1]
    return out

@njit(cache=NBCACHE, fastmath=FASTMATH)
def interpol_n(wo, lut, sh, mul):
    '''
    That tiny piece of code made me some headache ...
    wo     N element float index of point for interpolation
    lut    N dimensional LUT, flatend
    sh     orig shape of LUT
    mul    multiplicators, to convert from n-dim to 1-dim
    '''
    nn = sh.size
    n2 = 2**nn
    ## lower and upper coordinates of neighbors
    ## and 1-distance relative to wo
    ## coor is tuple of n-dim times 2d-tuple with lower and upper
    ##      coordinate
    ## dist is tuple of n-dim times 2d-tuple with lower and upper
    ##      (1-distance) to point at wo
    coor = find_hypercube(wo,sh,nn)  # 1 us
    dist = calc_dist(wo,coor,nn)     # 1 us
    out = 0.
    for i in range(n2):
        weght = 1.
        adres = 0
        for j in range(nn):
            k = (i>>j) & 1   #its magic ...give it a try in a notebook ....
            adres += mul[j]*coor[j,k] 
            weght *= dist[j,k]
        out += lut[adres]*weght
    return out


@njit(cache=NBCACHE, fastmath=FASTMATH)
def interpol_n_jac(wo,lut,sh,mul):
    '''
    That  made even more headache ...
    As interpol_n. Adionally returning the jacobian
    (relative to a sampling distance of '1') 
    and the cordinates (needed later for 
    scaling of jacobians)

    wo     N element float index of point for interpolation
    lut    N dimensional LUT, flatend
    sh     orig shape of LUT
    mul    multiplicators, to convert from n-dim to 1-dim
    '''
    nn = sh.size
    n2 = 2**nn
    
    coor = find_hypercube(wo,sh,nn)
    disti = calc_dist(wo,coor,nn)
    distj = calc_dist(wo,coor,nn,tol=1.e-7)
    
    out = 0.
    jaco = np.zeros(nn,dtype=np.float64)
    adre = np.zeros((n2),dtype=np.int64)
    owegi = np.ones((n2),dtype=np.float64)
    owegj = np.ones((n2),dtype=np.float64)

    for in2 in range(n2):
        for inn in range(nn):
            k = (in2>>inn) & 1
            adre[in2] += mul[inn]*coor[inn,k] 
            owegi[in2] *= disti[inn,k]
            owegj[in2] *= distj[inn,k]
        for inn in range(nn):
            k = (in2>>inn) & 1   # is (0|1)
            vz = 2*k-1  # vorzeichen = sign
            weght = owegj[in2]/vz/distj[inn,k]
            jaco[inn] += lut[adre[in2]]*weght
        out += lut[adre[in2]]*owegi[in2]
    return jaco,out,coor

@njit(cache=NBCACHE)
def interpol_n_nan(wo,lut,sh,mul):
    '''
    As interpol_n, but ignoring nans (and thus slower) 
    wo     N element float index of point for interpolation
    lut    N dimensional LUT, flatend
    sh     orig shape of LUT
    mul    multiplicators, to convert from n-dim to 1-dim
    '''
    nn = sh.size
    n2 = 2**nn
 
    coor = find_hypercube(wo,sh,nn)  # 1 us
    dist = calc_dist(wo,coor,nn)     # 1 us
    
    out = 0.
    nrm = 0.
    god = 0
    for i in range(n2):
        weght = 1.
        adres = 0
        for j in range(nn):
            k = (i>>j) & 1
            #all_coor[j] = coor[j,k]
            adres += mul[j]*coor[j,k] 
            weght *= dist[j,k]
        neibr = lut[adres]
        if not math.isfinite(neibr):
            continue
        god += 1
        nrm += weght 
        out += neibr*weght
    if god == 0:
        out = np.nan
    else:
        out /= nrm
    return out

@njit(cache=NBCACHE, fastmath=FASTMATH)
def interpol_npn(wo,lut,sh,ex,mul):
    '''
    As interpol_n, but last dimension of LUT is objective...
    instead  of returning a single number, it returns a vector 
    with 'ex' elements
    
    wo     N element float index of point for interpolation
    lut    N dimensional LUT, flatend
    sh     orig shape of LUT
    ex     number of elements of last Dimension (the goal)
    mul    multiplicators, to convert from n-dim to 1-dim
    '''
    nn = sh.size
    n2 = 2**nn
    
    coor = find_hypercube(wo,sh,nn)
    dist = calc_dist(wo,coor,nn)

    out = np.zeros(ex,dtype=np.float64)
    for i in range(n2):
        weght = 1.
        adres = 0
        for j in range(nn):
            k = (i>>j) & 1
            adres += mul[j]*coor[j,k] 
            weght *= dist[j,k]
        neibr = lut[adres]
        for j in range(ex):
            out[j] += neibr[j]*weght
    return out

@njit(cache=NBCACHE, fastmath=FASTMATH)
def interpol_npn_jac(wo,lut,sh,ex,mul):
    '''
    As interpol_npn. Additionally returning the jacobian
    (relativ to a sampling distance of '1') 
    and the cordinates (needed later for 
    scaling of jacobians)
    
    wo     N element float index of point for interpolation
    lut    N dimensional LUT, flatend
    sh     orig shape of LUT
    ex     number of elements of last Dimension (the goal)
    mul    multiplicators, to convert from n-dim to 1-dim
    '''
    nn = sh.size
    n2 = 2**nn
    
    coor = find_hypercube(wo,sh,nn)
    dist = calc_dist(wo,coor,nn, tol=1.e-7)

    out = np.zeros(ex,dtype=np.float64)
    jaco = np.zeros((ex,nn),dtype=np.float64)
    adre = np.zeros((n2,),dtype=np.int64)
    oweg = np.ones((n2,),dtype=np.float64)

    for in2 in range(n2):
        for inn in range(nn):
            k = (in2>>inn) & 1
            adre[in2] += mul[inn]*coor[inn,k] 
            oweg[in2] *= dist[inn,k]
        for inn in range(nn):
            k = (in2>>inn) & 1
            vz = 2*k-1
            weght = oweg[in2]/vz/dist[inn,k]
            for iex in range(ex):
                jaco[iex,inn] += lut[adre[in2],iex]*weght
        for iex in range(ex):
            out[iex] += lut[adre[in2],iex]*oweg[in2]
    return jaco,out,coor


#def linint2index(x,xtab):
    #return np.interp(x,xtab,np.arange(len(xtab)))
@njit(cache=NBCACHE, fastmath=FASTMATH)
def linint2index(x,xtab):
    n = xtab.size
    if x <= xtab[0]:
        out = 0.
    elif x >= xtab[-1]:
        out = n-1.
    else:
        for i in range(1,n):
            if x < xtab[i]:
                break
        out = i + (x - xtab[i-1])/(xtab[i]-xtab[i-1])-1
    return out
   
@njit(cache=NBCACHE, fastmath=FASTMATH)
def possca2scc(coor,sca,n):
    '''
    returns the right scaling for jacobian 
    (which is otherwise in index dimension)
    inputs are coordinates and all scalings 
    '''
    out = np.empty(n,dtype=np.float64)
    for i in range(n):
        l = coor[i,0]
        u = coor[i,1]
        if l == 0:
            out[i] = sca[i][l]
        elif u == n-1:
            out[i] = sca[i][u-1]
        elif l == u:
            out[i] = 0.5*(sca[i][u-1]+sca[i][u])
        else:
            out[i] = sca[i][l]
    return out

@njit(cache=NBCACHE, fastmath=FASTMATH)
def scale_jacobian(coor,sca,jac):
    '''
    calculates the right scaling for jacobian 
    (which is otherwise in index dimension)
    '''
    s_jac = np.empty(jac.shape)
    n = jac.shape[1]
    for i in range(n):
        m = sca[i].size-1
        l = coor[i,0]
        u = coor[i,1]
        if l <= 0:
            scc = sca[i][0]
        elif u >= m:
            scc = sca[i][m]
        elif l == u:
            scc = 0.5*(sca[i][u-1]+sca[i][u])
        else:
            scc = sca[i][l]
        s_jac[:,i] = jac[:,i] / scc
    return s_jac

@njit(cache=NBCACHE, fastmath=FASTMATH)
def scale_jacobian_1d(coor,sca,jac):
    '''
    calculates the right scaling for jacobian 
    (which is otherwise in index dimension)
    '''
    s_jac = np.empty(jac.shape)
    n = jac.shape[0]
    for i in range(n):
        m = sca[i].size-1
        l = coor[i,0]
        u = coor[i,1]
        if l <= 0:
            scc = sca[i][0]
        elif u >= m:
            scc = sca[i][m]
        elif l == u:
            scc = 0.5*(sca[i][u-1]+sca[i][u])
        else:
            scc = sca[i][l]
        s_jac[i] = jac[i] / scc
    return s_jac


#@njit(cache=NBCACHE, fastmath=FASTMATH)
#def scale_jac(jac,scc):
    #out =np.empty(jac.shape,dtype=jac.dtype)
    #for i in range(jac.shape[0]):
        #for j in range(jac.shape[1]):
            #out[i,j] = jac[i,j] / scc[j]
    #return out

######################################################################
#Below all necessary wraper functions...

@njit(cache=NBCACHE, nogil=NOGIL)
def linint2index_batch(x,xtab):
    out = np.zeros(x.shape)+np.nan
    ok = np.isfinite(x)#.sum(axis=1) == x.shape[1]
    for i in range(x.shape[0]):
        if ok[i]:
            out[i] = linint2index(x[i],xtab)
    return out

def linint2index_parallel(x,xtab,nj=CPU_COUNT):
    '''
    overhead costs are high, compared to batch ...
    '''
    @njit(cache=NBCACHE, nogil=NOGIL)
    def _linint2index_batch(x,xtab,itr):
        out = np.zeros(x.shape)
        for i in range(x.shape[0]):
            out[i] = linint2index(x[i],xtab)
        return (itr,out)
    ch = x.shape[0]//nj
    jobs=[(_linint2index_batch,[x[ch*i:ch*(i+1)],xtab,i],{}) for i in range(nj+1)]
    if (ch > 0) and (nj > 1):
        with Parallel(n_jobs=nj,backend='threading') as parallel:
                results=parallel(jobs)
        out = np.zeros(x.shape)
        for r in results:
            out[ch*r[0]:ch*(r[0]+1)] = r[1]
    else:
        _, out =_linint2index_batch(x,xtab,0)
    return out

def generate_itp(lut):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    def function(wo):
        return interpol_n(wo,put,sha,mul)
    return function
   
def generate_itp_pn_jac(lut,sca=None):
    exx=lut.shape[-1]
    put = np.array(lut.reshape((-1,exx)),dtype=np.float64)
    sha = np.array(lut.shape[:-1],dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    if sca is None:
        sca = tuple(np.ones(s) for s in sha)
    def function(wo):
        jac,itp,pos = interpol_npn_jac(wo, put, sha, exx, mul)
        jac = scale_jacobian(pos,sca,jac)
        return  jac, itp
    return function


def generate_itp_jac(lut,sca=None):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    if sca is None:
        sca = tuple(np.ones(s) for s in sha)
    def function(wo):
        jac,itp,pos = interpol_n_jac(wo,put,sha,mul)
        jac = scale_jacobian_1d(pos,sca,jac)
        return jac, itp
    return function

def generate_itp_nan(lut):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    def function(wo): 
        return interpol_n_nan(wo,put,sha,mul)
    return function


def generate_itp_pn(lut):
    exx=lut.shape[-1]
    put = np.array(lut.reshape((-1,exx)),dtype=np.float64)
    sha = np.array(lut.shape[:-1],dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    def function(wo): 
        return interpol_npn(wo, put, sha, exx,mul)
    return function


def generate_itp_parallel(lut,batch=False):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    
    @njit(nogil=NOGIL)
    def _function_itp_b(wo,itr=None,put=put,sha=sha,mul=mul):
        itp = np.zeros(wo.shape[0])
        ok = np.isfinite(wo).sum(axis=1) == wo.shape[1]
        for i in range(wo.shape[0]):
            if ok[i]:
                itp[i] = interpol_n(wo[i],put,sha,mul) 
        return (itr,itp)
    if batch is True:
        def function_itp_bp(wo):
            # omitting not needed chunk number
            _,itp = _function_itp_b(wo)
            return itp
    else:
        def function_itp_bp(wo, nj=CPU_COUNT):
            ch = wo.shape[0]//nj
            if (ch > 0) and (nj > 1):
                jobs=[(_function_itp_b,[wo[ch*i:ch*(i+1)],i],{}) for i in range(nj+1)]
                with Parallel(n_jobs=nj,backend='threading') as parallel:
                    results=parallel(jobs)
                itp = np.zeros((wo.shape[0],))
                for r in results:
                    itp[ch*r[0]:ch*(r[0]+1)] = r[1]
            else:
                _,itp = _function_itp_b(wo,0)
            return itp
    return function_itp_bp
def generate_itp_batch(lut):
    return generate_itp_parallel(lut,batch=True)



def generate_itp_jac_parallel(lut,sca=None,batch=False):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    if sca is None:
        sca = tuple(np.ones(s) for s in sha)
    @njit(nogil=NOGIL)
    def _function_itp_j_b(wo,itr=None,put=put,sha=sha,mul=mul,sca=sca):
        itp = np.zeros(wo.shape[0])
        jac = np.zeros(wo.shape)
        ok = np.isfinite(wo).sum(axis=1) == wo.shape[1]
        for i in range(wo.shape[0]):
            if ok[i]:
                _jac,_itp,pos = interpol_n_jac(wo[i],put,sha,mul)
                jac[i] = scale_jacobian_1d(pos,sca,_jac)
                itp[i] = _itp
        return itr, jac, itp
    if batch is True:
        def function_itp_j_bp(wo):
            # omitting not needed chunk number
            _,jac,itp = _function_itp_j_b(wo)
            return jac,itp
    else:
        def function_itp_j_bp(wo, nj=CPU_COUNT):
            ch = wo.shape[0]//nj
            if (ch > 0) and (nj > 1):
                jobs=[(_function_itp_j_b,[wo[ch*i:ch*(i+1)],i],{}) for i in range(nj+1)]
                with Parallel(n_jobs=nj,backend='threading') as parallel:
                    results=parallel(jobs)
                #first = True
                itp = np.zeros((wo.shape[0],))
                jac = np.zeros(wo.shape)
                for r in results:
                    itp[ch*r[0]:ch*(r[0]+1)] = r[2]
                    jac[ch*r[0]:ch*(r[0]+1)] = r[1]
                return jac,itp
            else:
                _,jac,itp = _function_itp_b(wo)
            return jac,itp
    return function_itp_j_bp
def generate_itp_jac_batch(lut,sca=None):
    return generate_itp_jac_parallel(lut,sca=sca,batch=True)


def generate_itp_nan_parallel(lut,batch=False):
    put = np.array(lut.flatten(),dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    @njit(nogil=NOGIL)
    def _function_itp_n_b(wo,itr=None,put=put,sha=sha,mul=mul):
        itp = np.zeros(wo.shape[0])
        ok = np.isfinite(wo).sum(axis=1) == wo.shape[1]
        for i in range(wo.shape[0]):
            if ok[i]:
                itp[i] = interpol_n_nan(wo[i],put,sha,mul) 
        return (itr,itp)
    if batch is True:
        def function_itp_n_bp(wo):
            # omitting not needed chunk number
            _,itp = _function_itp_n_b(wo)
            return itp
    else:
        def function_itp_n_bp(wo, nj=CPU_COUNT):
            ch = wo.shape[0]//nj
            if (ch > 0) and (nj > 1):
                jobs=[(_function_itp_n_b,[wo[ch*i:ch*(i+1)],i],{}) for i in range(nj+1)]
                with Parallel(n_jobs=nj,backend='threading') as parallel:
                    results=parallel(jobs)
                itp = np.zeros((wo.shape[0],))
                for r in results:
                    itp[ch*r[0]:ch*(r[0]+1)] = r[1]
            else:
                _,itp = _function_itp_n_b(wo,0)
            return itp
    return function_itp_n_bp

def generate_itp_nan_batch(lut):
    return generate_itp_nan_parallel(lut,batch=True)


def generate_itp_pn_parallel(lut,batch=False):
    exx = lut.shape[-1]
    put = np.array(lut.reshape((-1,exx)),dtype=np.float64)
    sha = np.array(lut.shape[:-1],dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    @njit(nogil=NOGIL)
    def _function_itp_pn_b(wo,itr=None,put=put,sha=sha,mul=mul,exx=exx):
        itp = np.zeros((wo.shape[0],exx))
        ok = np.isfinite(wo).sum(axis=1) == wo.shape[1]
        for i in range(wo.shape[0]):
            if ok[i]:
                itp[i] = interpol_npn(wo[i], put, sha, exx, mul) 
        return (itr,itp)
    if batch is True:
        def function_itp_pn_bp(wo):
            # omitting not needed chunk number
            _,itp = _function_itp_pn_b(wo)
            return itp
    else:
        def function_itp_pn_bp(wo, nj=CPU_COUNT):
            ch = wo.shape[0]//nj
            if (ch > 0) and (nj > 1):
                jobs=[(_function_itp_pn_b,[wo[ch*i:ch*(i+1)],i],{}) for i in range(nj+1)]
                with Parallel(n_jobs=nj,backend='threading') as parallel:
                    results=parallel(jobs)
                itp = np.zeros((wo.shape[0],exx))
                for r in results:
                    itp[ch*r[0]:ch*(r[0]+1)] = r[1]
            else:
                _,itp = _function_itp_pn_b(wo,0)
            return itp
    return function_itp_pn_bp

def generate_itp_pn_batch(lut):
    return generate_itp_pn_parallel(lut,batch=True)


def generate_itp_pn_jac_parallel(lut,sca=None,batch=False):
    exx=lut.shape[-1]
    put = np.array(lut.reshape((-1,exx)),dtype=np.float64)
    sha = np.array(lut.shape[:-1],dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    if sca is None:
        sca = tuple(np.ones(s) for s in sha)
    @njit(nogil=NOGIL)
    def _function_itp_np_j_b(wo, itr = None, put=put,sha=sha,mul=mul,exx=exx,sca=sca):
        '''
        the speed of individual chunks of parallel processing
        is unpredictable, itr (the chunk number) 
        is needed to reorder the results
        '''
        itp = np.zeros((wo.shape[0],exx))
        jac = np.zeros((wo.shape[0],exx,wo.shape[1]))
        ok = np.isfinite(wo).sum(axis=1) == wo.shape[1]
        for i in range(wo.shape[0]):
            if ok[i]:
                _jac,_itp,pos = interpol_npn_jac(wo[i], put, sha, exx, mul)
                jac[i] = scale_jacobian(pos,sca,_jac)
                itp[i] = _itp

        return itr, jac, itp
    if batch is True:
        def function_itp_np_j_bp(wo):
            # omitting not needed chunk number
            _,jac,itp = _function_itp_np_j_b(wo)
            return jac,itp
    else:
        def function_itp_np_j_bp(wo, nj=CPU_COUNT):
            ch = wo.shape[0]//nj
            if (ch > 0) and (nj > 1):
                jobs=[(_function_itp_np_j_b,[wo[ch*i:ch*(i+1)],i],{}) for i in range(nj+1)]
                with Parallel(n_jobs=nj,backend='threading') as parallel:
                    results=parallel(jobs)
                itp = np.zeros((wo.shape[0],exx))
                jac = np.zeros((wo.shape[0],exx,wo.shape[1]))
                for r in results:
                    itp[ch*r[0]:ch*(r[0]+1)] = r[2]
                    jac[ch*r[0]:ch*(r[0]+1)] = r[1]
                return jac,itp
            else:
                _,jac,itp = _function_itp_np_j_b(wo,0)
            return jac,itp
    return function_itp_np_j_bp

def generate_itp_pn_jac_batch(lut,sca=None):
    return generate_itp_pn_jac_parallel(lut,sca=sca,batch=True)






######################################################################
#Below some test functions...

HH=0.001
def _test_approximate_jacobian_function(func):
    '''
    just for testing ....
    '''
    def jac_func( x, dx = None):
        #print('x->',x,x.shape,'<-x')
        nx = x.size
        if dx is None:
            # warnings.warn('No sensible delta_x given to jacobian. '
            #              'Results could be unpredictable!'
            #              , stacklevel=3)
            sign = np.sign(x)
            dx = np.where((sign * x) < HH, sign * HH, x * HH)
        for ix in range(nx):
            dxm = x * 1.0
            dxp = x * 1.0
            dxm[ix] = dxm[ix] - dx[ix]
            dxp[ix] = dxp[ix] + dx[ix]
            dyy = func(dxp) - func(dxm)
            # first run: now I know size of y
            #print('y->',dyy,dyy.shape,'<-y')
            if ix == 0:
                ny = dyy.size
                j = np.zeros((ny, nx), dtype=x.dtype)  # zeilen zuerst, spalten später
            j[:, ix] = dyy[:] / dx[ix] / 2.0
        return j
    return jac_func

def simple_test():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    put = np.array(lut.flatten(),dtype=np.float64)
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    sha = np.array(lut.shape,dtype=np.int32)
    ff = generate_itp(lut)
    print('3480.7 =',ff(woo),'\n')
    #import sys
    #sys.exit()
    
    ff = generate_itp_pn(lut)
    #print(interpol_npn(woo[:-1],lut),
          #'= \n 3475.5 3476.5 3477.5 3478.5 3479.5 3480.5 3481.5 =\n'
          #,ff(woo[:-1]),'\n')
    print('3475.5 3476.5 3477.5 3478.5 3479.5 3480.5 3481.5 =\n'
          ,ff(woo[:-1]),'\n')

    lut[1,2,3,4,5,6]=np.nan
    ff = generate_itp_nan(lut)
    print('3468.924 =', ff(woo))

def jaco_test_1d():
    #lut = np.array(np.arange(2*3*4,dtype=np.float).reshape(2,3,4))
    #put = np.array(lut.flatten(),dtype=np.float64)
    #woo = np.array([0.6,1.5,2.4],dtype=np.float64)
    #sha = np.array(lut.shape,dtype=np.int32)
    #mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])
    lut = np.array(np.arange(3*4*5,dtype=np.float).reshape(3,4,5))
    put = np.array(lut.flatten(),dtype=np.float64)
    woo = np.array([1.99,2.00,3.7],dtype=np.float64)
    sha = np.array(lut.shape,dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])

    _ff = generate_itp(lut)
    ffj = generate_itp_jac(lut)
    ff = lambda x: np.array([_ff(x)]) 
    jff = _test_approximate_jacobian_function(ff)
    print(ff(woo),jff(woo))
    print(interpol_n_jac(woo, put, sha, mul))
    print(ff(woo),ffj(woo))


def jaco_test_2d():
    lut = np.array(np.arange(2*3*4*5,dtype=np.float).reshape(2,3,4,5))
    exx = lut.shape[-1]
    put = np.array(lut.reshape((-1,exx)),dtype=np.float64)
    woo = np.array([0.1,1.,2.4],dtype=np.float64)
    sha = np.array(lut.shape[:-1],dtype=np.int32)
    mul = np.array([sha[i:].prod() for i in range(1,len(sha))]+[1])

    _ff = generate_itp_pn(lut)
    ff = lambda x: np.array([_ff(x)]) 
    jff = _test_approximate_jacobian_function(ff)
    print(lut.shape)
    print(_ff(woo))
    print(ff(woo))
    print(jff(woo))
    print(interpol_npn_jac(woo, put, sha, exx, mul))
    
def test_clipping_1d():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    put = np.array(lut.flatten(),dtype=np.float64)
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    sha = np.array(lut.shape,dtype=np.int32)
    
    ff = generate_itp(lut)
    print('3480.7 =',ff(woo),'\n')
    
    for x in np.linspace(0.6,-0.4,11):
        woo[0] = x
        print(x,':',ff(woo))

def test_batch():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    woo = np.tile(woo,(1500000,1))
    sha = np.array(lut.shape,dtype=np.int32)
    print(woo,woo.shape)
    ff = generate_itp_batch(lut)
    ffn = generate_itp_nan_batch(lut)
    ffj = generate_itp_jac_batch(lut)
    ffp = generate_itp_pn_batch(lut)
    ffpj = generate_itp_pn_jac_batch(lut)
    print(ff(woo).shape)
    print(ffj(woo)[0].shape)
    print(ffn(woo).shape)
    print(ffp(woo[:,:-1]).shape)
    print(ffpj(woo[:,:-1])[0].shape)
    print(linint2index(2.,np.array([0.,0.5,1.,5.,6.,7.]))) 
    print(linint2index_batch(np.array([2.,3.,2.,]),np.array([0.,0.5,1.,5.,6.,7.]))) 

def test_batch_nan():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    woo = np.tile(woo,(150000,1))
    woo[123,2]=np.nan
    sha = np.array(lut.shape,dtype=np.int32)
    print(woo,woo.shape)
    ok = np.isfinite(woo).sum(axis=1) == woo.shape[1]
    print(ok,ok[123])
    ff = generate_itp_batch(lut)
    ffn = generate_itp_nan_batch(lut)
    ffj = generate_itp_jac_batch(lut)
    ffp = generate_itp_pn_batch(lut)
    ffpj = generate_itp_pn_jac_batch(lut)
    
    print(ff(woo).shape)
    print(ffj(woo)[0].shape)
    print(ffn(woo).shape)
    print(ffp(woo[:,:-1]).shape)
    print(ffpj(woo[:,:-1])[0].shape)
    print(ffpj(woo[122:124,:-1]))
    print(linint2index(2.,np.array([0.,0.5,1.,5.,6.,7.]))) 
    print(linint2index_batch(np.array([2.,3.,2.,np.nan,]),np.array([0.,0.5,1.,5.,6.,7.]))) 


def test_parallel_itp():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    nnn = 150001*40
    woo = np.tile(woo,(nnn,1))
    woo = woo*(1.+ np.random.normal(size=woo.shape)/1000)
    #print(woo,woo.shape)
    ffp = generate_itp_parallel(lut)
    ffb = generate_itp_batch(lut)
    ff = generate_itp(lut)
    #print(len(ffp(woo)))
    #print(woo[-1],ffp(woo)[-1],ffp(woo[-1:]))
    import time
    a = time.time()
    e = ffp(woo)[-1]
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e)
    a = time.time()
    e = ffb(woo)[-1]
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e)
    a = time.time()
    e = [ff(woo[i]) for i in range(nnn)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn),'us    ',e)

def test_parallel_itp_nan():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))    
    lut[1,2,3,4,5,6]=np.nan
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    nnn = 150001*40
    woo = np.tile(woo,(nnn,1))
    woo = woo*(1.+ np.random.normal(size=woo.shape)/1000)
    #print(woo,woo.shape)
    ffp = generate_itp_nan_parallel(lut)
    ffb = generate_itp_nan_batch(lut)
    ff = generate_itp_nan(lut)
    #print(len(ffp(woo)))
    #print(woo[-1],ffp(woo)[-1],ffp(woo[-1:]))
    import time
    a = time.time()
    e = ffp(woo)[-1]
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e)
    a = time.time()
    e = ffb(woo)[-1]
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e)
    a = time.time()
    e = [ff(woo[i]) for i in range(nnn)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn),'us    ',e)

def test_parallel_itp_jac():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    ttt = 60
    nnn = 150001*ttt+1
    woo = np.tile(woo,(nnn,1))
    woo = woo*(1.+ np.random.normal(size=woo.shape)/1000)
    #print(woo,woo.shape)
    ffp = generate_itp_jac_parallel(lut)
    ffb = generate_itp_jac_batch(lut)
    ff = generate_itp_jac(lut)
    #print(len(ffp(woo)))
    #print(woo[-1],ffp(woo)[-1],ffp(woo[-1:]))
    import time
    a = time.time()
    e = ffp(woo)
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e[0][-1],e[1][-1])
    a = time.time()
    e = ffb(woo)
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e[0][-1],e[1][-1])
    a = time.time()
    e = [ff(woo[i]) for i in range(nnn//ttt)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn//ttt),'us    ',ff(woo[-1]))
    
def test_parallel_itp_pn():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5],dtype=np.float)
    ttt = 59
    nnn = 150000*ttt
    woo = np.tile(woo,(nnn,1))
    woo = woo*(1.+ np.random.normal(size=woo.shape)/1000)
    #print(woo,woo.shape)
    ffp = generate_itp_pn_parallel(lut)
    ffb = generate_itp_pn_batch(lut)
    ff = generate_itp_pn(lut)
    #print(len(ffp(woo)))
    #print(woo[-1],ffp(woo)[-1],ffp(woo[-1:]))
    import time
    a = time.time()
    e = ffp(woo)
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e[-1])
    a = time.time()
    e = ffb(woo)
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e[-1])
    a = time.time()
    e = [ff(woo[i]) for i in range(nnn//ttt)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn//ttt),'us    ',ff(woo[-1]))
 
def test_parallel_itp_pn_jac():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5],dtype=np.float)
    ttt = 60
    #ttt = 2
    nnn = 150001*ttt
    woo = np.tile(woo,(nnn,1))
    woo = woo*(1.+ np.random.normal(size=woo.shape)/1000)
    #print(woo,woo.shape)
    ffp = generate_itp_pn_jac_parallel(lut)
    ffb = generate_itp_pn_jac_batch(lut)
    ff = generate_itp_pn_jac(lut)
    #print(len(ffp(woo)))
    #print(woo[-1]-woo[-1:])
    #print(ff(woo[-1])[1])
    #print(ffp(woo)[1][-1],ffb(woo)[1][-1]-ff(woo[-1])[1])
    #print(ffp(woo[-1:])[1],ffb(woo[-1:])[1],ffb(woo[-1:])[1]-ff(woo[-1])[1])
    ##import sys ; sys.exit()
    import time
    a = time.time()
    e = ffp(woo)
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e[0][-1],e[1][-1])
    a = time.time()
    e = ffb(woo)
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e[0][-1],e[1][-1])
    a = time.time()
    e = [ff(woo[i]) for i in range(nnn//ttt)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn//ttt),'us    ',e)

def test_parallel_itp_linint2idx():
    nnn = 10000000
    xx = np.linspace(2.,3.,nnn)
    xtab = np.array([0.,0.5,1.,5.,6.,7.])
    print(linint2index(2.,xtab)) 
    print(xx.shape)
    print(linint2index_batch(xx,xtab).shape) 
    print(linint2index_parallel(xx,xtab).shape)
    import time
    a = time.time()
    e = linint2index_parallel(xx,xtab)    
    print('parallel: ',(time.time()-a)*1000000/(nnn),'us    ',e[0])
    a = time.time()
    e = linint2index_batch(xx,xtab)    
    print('batch: ',(time.time()-a)*1000000/(nnn),'us    ',e[0])
    a = time.time()
    e = [linint2index(xx[i],xtab) for i in range(nnn)][-1]
    print('seriell: ',(time.time()-a)*1000000/(nnn),'us    ',e)

def test_parallel_nan():
    lut = np.array(np.arange(120*6*7,dtype=np.float).reshape(2,3,4,5,6,7))
    woo = np.array([0.6,1.5,2.5,3.5,4.5,5.2],dtype=np.float)
    woo = np.tile(woo,(150000,1))
    woo[123,2]=np.nan
    sha = np.array(lut.shape,dtype=np.int32)
    print(woo,woo.shape)
    ok = np.isfinite(woo).sum(axis=1) == woo.shape[1]
    print(ok,ok[123])
    ff = generate_itp_parallel(lut)
    ffn = generate_itp_nan_parallel(lut)
    ffj = generate_itp_jac_parallel(lut)
    ffp = generate_itp_pn_parallel(lut)
    ffpj = generate_itp_pn_jac_parallel(lut)
    print(ff(woo).shape)
    print(ffj(woo)[0].shape)
    print(ffn(woo).shape)
    print(ffp(woo[:,:-1]).shape)
    print(ffpj(woo[:,:-1])[0].shape)
    print(ffpj(woo[122:124,:-1]))
    print(linint2index(2.,np.array([0.,0.5,1.,5.,6.,7.]))) 
    print(linint2index_batch(np.array([2.,3.,2.,np.nan,]),np.array([0.,0.5,1.,5.,6.,7.]))) 

if __name__ == '__main__':
    simple_test()
    jaco_test_1d()
    jaco_test_2d()
    #test_clipping_1d()
    #test_batch()
    #test_parallel_itp()
    #test_parallel_itp_nan()
    #test_parallel_itp_jac()
    #test_parallel_itp_pn()
    #test_parallel_itp_pn_jac()
    #test_parallel_itp_linint2idx()
    #test_batch_nan()
    #test_parallel_nan()



