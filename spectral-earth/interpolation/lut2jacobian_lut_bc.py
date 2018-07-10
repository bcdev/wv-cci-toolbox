#generates a lut of jacobians from a lut
import numpy as np
import lut2func as l2f
from netCDF4 import Dataset
try:
    from wln import wln
except (ImportError,OSError):
    class wln:
        def __init__(self,*args,**kwargs):
            pass
        def event(self,*args,**kwargs):
            pass
        



def numerical_jacoby(a, b, x, fnc, nx, ny, delta=0.01,dx=None,xidx=None):
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
    if dx is None:
        # very coarse but sufficient for this excercise
        dx=(b - a) * delta
    
    jac = np.empty((ny, nx),order='F')  # zeilen zuerst, spalten spaeter!!!!!!!
    if xidx is None:
        xidx = list(range(nx))
        
    for ii,ix in enumerate(xidx):
        dxm = x * 1.
        dxp = x * 1.
        dxm[ix] = (dxm[ix] - dx[ix]).clip(a[ix],b[ix])
        dxp[ix] = (dxp[ix] + dx[ix]).clip(a[ix],b[ix])
        if dxp[ix] == dxm[ix]: 
            jac[:,ii]=0.
        else:
            dyy = fnc(dxp) - fnc(dxm)
            jac[:,ii]= dyy/(dxp[ix] - dxm[ix])       
    return jac


def generate_jacobian_lut(luts,axes,xidx=None):
    '''
    generates a jacobian-lut at exactly the same 
    positions (axes points) as the original luts
    
    xidx gives the positions, where the jacobian 
    will be calulated, the other positions are ignored
    (e.g.  geometry in LUTs )
    
    '''   
    func=l2f.lut2func(luts,axes)
    
    if isinstance(luts,tuple):
        ny = len(luts)
        dtype=luts[0].dtype
    elif isinstance(luts,np.ndarray):
        ny = luts.shape[-1]
        dtype=luts.dtype
    else:
        print('Lut is  not of right type')
        return None
    
    if xidx is None:
        nx=len(axes)
    else:
        nx=len(xidx)
        
    dimi=np.array([len(ax) for ax in (axes)])
    dimj=[len(ax) for ax in (axes)]
    dimj.append(ny*nx)
    out = np.zeros(dimj,dtype=dtype)
 
    a=np.zeros(len(axes))
    b=np.zeros(len(axes))
    
    a=np.array([ax.min() for ax in axes])
    b=np.array([ax.max() for ax in axes])
    
    def njac(x,dx=None):
        return numerical_jacoby(a, b, x, func, nx, ny, delta=0.01,xidx=xidx,dx=dx)
    
    def optimal_dx(i,ax):
        if i == 0:
            dx=np.abs(ax[1]-ax[0])/2.
        elif i == ax.size-1:
            dx=np.abs(ax[-1]-ax[-2])/2.
        else:
            dx=min(np.abs(ax[i+1]-ax[i]), np.abs(ax[i]-ax[i-1]))/2.
        return dx    
    
    print('Filling the jacobian ...')
    progress=wln(dimi.prod(),'Progress',modulo=10000)
    for i in np.arange(dimi.prod()): 
        idx=np.unravel_index(i,dimi)
        wo = np.array([ax[i] for i,ax in zip(idx,axes)])
        dx = np.array([optimal_dx(i,ax) for i,ax in zip(idx,axes)])
        out[idx]=njac(wo,dx).ravel()
        progress.event()
    print('')
    print('Done')

    return {'lut':out,'ny':ny,'nx':nx,'axes':axes,'xidx':xidx}

def lut2ncdf(jlut,nfile):
    '''
    stores the  jacobian-lut and auxillary data
    in a ncdf
    
    '''
    with Dataset(nfile, 'w', format='NETCDF4') as nco:
        dim_ids=[]
        for i,a in enumerate(jlut['axes']):
            dimname='jlut_dimension_%i'%i
            nco.createDimension(dimname,a.size)
            dim_ids.append(dimname)
            ncdum=nco.createVariable('axes_%i'%i,a.dtype,dim_ids[-1])
            ncdum[:]=a
        dimname='jlut_dimension_%i'%(i+1)
        nco.createDimension(dimname,jlut['ny']*jlut['nx'])
        dim_ids.append(dimname)
        ncdum=nco.createVariable('jlut',jlut['lut'].dtype,dim_ids)
        ncdum[:]=jlut['lut']
        nco.createDimension('jaco_dimension',2)
        ncdum=nco.createVariable('ny_nx',np.int,'jaco_dimension')
        ncdum[:]=np.array([jlut['ny'],jlut['nx']])
        if jlut['xidx'] is None:
            nco.xidx='None'
        else:
            nco.xidx=','.join([str(_) for _ in jlut['xidx']])
        
def ncdf2func(nfile):
    '''
    reads jlut ncdf and makes a func from it
    '''
    with Dataset(nfile, 'r', format='NETCDF4') as nco:        
        jdims=nco.variables['jlut'].dimensions
        
        axes=tuple([nco.variables['axes_%i'%i][:] for i in range(len(jdims[:-1]))])
        jlut=nco.variables['jlut'][:]
        nynx=nco.variables['ny_nx'][:]

    dum={'lut':jlut,'ny':nynx[0],'nx':nynx[1],'axes':axes}   
    return jlut2func(dum)

def jlut2func(dum):
    func=l2f.lut2func(dum['lut'],dum['axes'])    
    def jfunc(woo):
        #return func(woo).reshape(dum['ny'],dum['nx'],order='F')
        return np.array(func(woo).reshape(dum['ny'],dum['nx']),order='F')
    return jfunc
    

if __name__ == '__main__':
    #How to use
    # first create a LUT 
    # for a function R^3 --> R^4
    dtype=np.float32
    
    luta=np.arange(240000,dtype=dtype).reshape(6,400,100,order='C')
    lutb=np.arange(240000,dtype=dtype).reshape(6,400,100,order='F')**2
    lutc=np.sqrt(np.arange(240000,dtype=dtype).reshape(6,400,100,order='F'))
    lutd=1./(1+np.arange(240000,dtype=dtype).reshape(6,400,100,order='F'))
    # either as Tuple 
    luts1=(luta,lutb,lutc,lutd)
    # or as single np.array
    luts2=np.array((luta,lutb,lutc,lutd)).transpose([1,2,3,0])

    print('R^3 --> R^4, LUT shape:', luts2.shape)

    #second create the axes 
    xx1=np.array([3.,4.,6.,7.,9.,15.],dtype=dtype)
    xx2=np.linspace(-10.,30.,400,dtype=dtype)
    xx3=np.linspace(-1.,3.,100,dtype=dtype)
    #as a tuple
    axes=(xx1,xx2,xx3)
    
    #finaly create the func
    func1=l2f.lut2func(luts1,axes)
    func2=l2f.lut2func(luts2,axes)
   
    
    #for the jacobians
    #use the function luts
    jlut= generate_jacobian_lut(luts2,axes,xidx=[0,2])
    #save the jacobians 
    lut2ncdf(jlut,'jlut3_test.nc')
    #and make a function from it
    jfun=ncdf2func('jlut3_test.nc')
    
    # now test the jacobian function:
    print('interploated:')
    print(jfun([3.,10.,1.])) 
    #and compare it with a numerical_jacoby
    a=np.array([3.,-10.,-1.])
    b=np.array([15,30.,3.])
    x=np.array([3,10.,1.])
    print('numerical:')
    print(numerical_jacoby(a, b, x, func1, 2, 4, delta=0.01,xidx=[0,2,]))
    print('delta [%]:')
    print((numerical_jacoby(a, b, x, func1, 2, 4, delta=0.01,xidx=[0,2,])-jfun(x))/jfun([3.,10.,1.])*100.)
    
    
    import time
    t=time.time()
    for i in range(10000): dum=jfun(x)
    print('jfun takes %i us' %((time.time()-t)*100))
    t=time.time()
    for i in range(10000): dum=numerical_jacoby(a, b, x, func2, 2, 4, delta=0.01,xidx=[0,2])
    print('numerical takes %i us' %((time.time()-t)*100))
    
    


