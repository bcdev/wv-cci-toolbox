from __future__ import print_function
import numpy as np
from scipy import interpolate as intp
from scipy.ndimage.interpolation import map_coordinates as mcrd
#import pdb

class interpolate_n:
    '''
    wrapper arround 'map_coordinates' 
    n-dimensional linear interpolation
    Usage: 
    1. initialisation:
            >>> import interpolate_mc as intpn
            >>> pn=intpn.interpolate_n(LUT,axes)
               LUT   is a Nd numpy array
               axes  is a tupel of 1d numpy arrays
                     The number of axes must correspond to the number 
                     of dimensions an the size of the axes must 
                     correspond to the size of the dimensions
    2. recall:
             >>> result=pn.recall(pos)
               pos   is a (ndim,nsample) array with the positions
                     if is_index=True pos is already a float index 
    Achtung, if you use monotonically decreasing axes be carefull
    with the index, better to use the build-in axes_intp[dim_number]()
    '''
    def __init__(self,lut,axes):
        if not isinstance(lut,np.ndarray):
            print ('Input is not an Numpy ndarray')
            return None
        self.ndim=lut.ndim
        
        if not isinstance(axes,tuple):
            print ('Axes is not a tuple') 
            return None

        if len(axes) != self.ndim:
            print ('Number of elements of axes (%i)'%len(axes)) 
            print ('is not equal the number of dimensions (%i)'%self.ndim)
            return None
        
        self.imi=[]
        self.imd=[]
        for i,dim in enumerate(axes):
            if not isinstance(dim,np.ndarray): 
                print ('Axes %i is not an Numpy ndarray' % i)
                return None
            if not dim.ndim == 1:
                print ('Axes %i is not an 1D Numpy ndarray' % i)
                return None
            if len(dim) != lut.shape[i]:
                print ('Axes %i does not agree with the shape of LUT' % i)
                print ('Axes %i has %i elements but LUT needs %i.' % (i,len(dim),lut.shape[i]))
                return None
            self.imi.append(self.is_monotonically_increasing(dim))
            self.imd.append(self.is_monotonically_decreasing(dim))
            if not (self.imi[-1] or self.imd[-1]) :
                print ('Axes %i is not monotonically increasing or decreasing' % i)
                print (dim)
                print ('Re-organize your data!')
                return None
            #if (self.imi[-1] and self.imd[-1]) :
                #print ('Axes %i is constant' % i)
                #print dim
                #print ('Re-organize your data!')
                #return None

        self.lut=lut
        self.axes_intp=[]
        self.axes_max=[]
        self.axes_min=[]
        for i in range(self.ndim):
            if not self.imi[i]:
              #print ('inverting dimension')
              _ = intp.interp1d(-axes[i],np.arange(axes[i].shape[0]),kind="linear")
              self.axes_intp.append(lambda x:_(-x))
              self.axes_max.append(axes[i][0])
              self.axes_min.append(axes[i][-1])
            else:  
              self.axes_intp.append(intp.interp1d(axes[i],np.arange(axes[i].shape[0]),kind="linear"))
              self.axes_max.append(axes[i][-1])
              self.axes_min.append(axes[i][0])
        self.axes=axes
        
    def is_monotonically_increasing(self,v):
        for i, e in enumerate(v[1:],1):
            if e <= v[i-1]:
                return False
        return True

    def is_monotonically_decreasing(self,v):
        for i, e in enumerate(v[1:],1):
            if e >= v[i-1]:
                return False
        return True

    def recall(self,positions,is_index=False,clip=False):
        if positions.ndim == 2:
          if positions.shape[1] != self.ndim:
            print ('Dimensions of position are (%i,%i)'% positions.shape)
            print ('but should be (Nsample,%i)'%self.ndim)
            return None
          pss=positions.copy()                
        elif positions.ndim == 1:  
          if positions.shape[0] != self.ndim:
            print ('Dimensions of position are (%i)'% positions.shape)
            print ('but should be (%i)'%self.ndim)
            return None
          pss=np.array([positions.copy()])   
        else:
            print ('Dimensions of position are' , positions.shape)
            print ('but should be (Nsample,%i)'%self.ndim)
            return None
          
      
        if not is_index:
            for i in range(self.ndim):
                if not clip:
                    pss[:,i]=self.axes_intp[i](pss[:,i])
                else:
                    pss[:,i]=self.axes_intp[i](pss[:,i].clip(self.axes_min[i],self.axes_max[i]))
        else:
            for i in range(self.ndim):
                if not clip: pass
                else:
                    pss[:,i]=pss[:,i].clip(0,self.lut.shape[i]-1)
                    #print  pss[:,i]
        if positions.ndim == 1:
          return mcrd(self.lut,pss.T,order=1)[0]
        else:  
          return mcrd(self.lut,pss.T,order=1)

                        

def test():
    import numpy as np
    import interpolate_mc as intpn
            
    # Two dimensions initialisation
    lut=np.arange(12,dtype=np.float).reshape(3,4)
    xx=np.array([3.,4.,6.])
    yy=np.array([1.,5.,10,15])[::-1]
    axes=(xx,yy)
    pn=intpn.interpolate_n(lut,axes)
    print (pn.is_monotonically_increasing([1,2,3,4,5]),True)
    print (pn.is_monotonically_increasing([1,2,2,3,4,5]),False)
    #recall
    pos= np.array([[4.5,1.5],[3.5,1.5],[5.5,2.5],[4.5,1.5]]) # (xx,yy),(xx,yy),(xx,yy),(xx,yy)
    print (pos)
    print (pos.shape)
    print (pn.recall(pos))
    pos= np.array([[4.5,1.5]]) # (xx,yy)
    print (pn.recall(pos))

    pos= np.array([6.,15]) # (xx,yy)
    print (pn.recall(pos,clip=False))
    print ('==')
    pos= np.array([1334.5,1111.5]) # (xx,yy)
    print (pn.recall(pos,clip=True))
    
    pos= np.array([3.,1.]) # (xx,yy)
    print (pn.recall(pos,clip=False))
    print ('==')
    pos= np.array([-1334.5,-1111.5]) # (xx,yy)
    print (pn.recall(pos,clip=True))

    # Four dimensions initialisation
    lut=np.arange(2*3*4*5,dtype=np.float).reshape(2,3,4,5)
    x1=np.array([3.,7.])
    x2=np.array([1.,5.,10.])[::-1]
    x3=np.array([1.,2., 4.,12.])
    x4=np.array([0.,1., 3., 8.,10.])[::-1]
    axes=(x1,x2,x3,x4)
    pn=intpn.interpolate_n(lut,axes)
    #recall
    pos= np.array([[3.5,5.5,4.2,8.8],[4.5,4.5,5.2,7.8]]) # (x1,x2,x3,x4),(x1,x2,x3,x4)
    print (pos.shape)
    print (pn.recall(pos))
    pos=np.array([[4.5,1.5,3.8,8.]])  # (x1,x2,x3,x4)
    print (pn.recall(pos))
    pos=np.array([[3.5,5.5,12.,8.8]])  # (x1,x2,x3,x4)
    
    print (pn.axes_intp[0](3.5))
    print (pn.axes_intp[1](5.5))
    print (pn.axes_intp[2](12.))
    print (pn.axes_intp[3](8.8))
    print (pn.recall(pos))
    print ('==')
    print (pn.recall(np.array([[ 0.125, 1.5, 122.025, 0.6 ]]),clip=True,is_index=True)) # (idx1,idx2,idx3,idx4)
    
    
        
if __name__ == "__main__":
    test()
 







