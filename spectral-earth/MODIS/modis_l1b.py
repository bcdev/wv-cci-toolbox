# -*- coding: utf-8 -*-
import numpy as np
#import scipy.meshgrid as mg
import scipy as sp
import scipy.interpolate as spi
import os
from netCDF4 import Dataset
#import ncdf4tools as nctools
import sys
# import hdf_sd
M1B=[ '1','2','3','4','5','6','7','8','9','10','11','12'   \
     ,'13lo','13hi','14lo','14hi','15','16','17','18','19' \
     ,'20','21','22','23','24','25','26','27','28','29','30'  \
     ,'31','32','33','34','35','36','37','38'] 
SDS=["EV_1KM_RefSB","EV_1KM_Emissive","EV_250_Aggr1km_RefSB"  \
    ,"EV_500_Aggr1km_RefSB"]



def ncdf4getelement(ncfile,ele,autoscale=True):
    '''
    reads a single element (var or group or attribute)
    ele is given by its path /groupA/groupB/element
    (use my command line tool 
    ncdfls /my/ncdf/file.nc4, or use 
    ncdump -h /my/ncdf/file.nc4)
    '''
    if not os.path.exists(ncfile):
            print("File not found: %s"%ncfile)
            return None
    grps = [g for g in ele.split('/')][1:-1]
    elle = ele.split('/')[-1]
    with Dataset(ncfile, 'r') as root:
        out = _ncdf4getelement(root,grps,elle,autoscale)
    return out
    
def _ncdf4getelement(root,grps,elle,autoscale):
    for g in grps:
        if g in root.groups:
            root=root.groups[g]
        elif g in root.variables:
            if elle in root.variables[g].ncattrs():
                return root.variables[g].getncattr(elle)
    if elle in root.groups:
        return _ncdf42dict_full(root)
    if elle in root.ncattrs():
        return root.getncattr(elle)
    if elle in root.variables:
        vatt={}
        for att in root.variables[elle].ncattrs():
            vatt[att]= root.variables[elle].getncattr(att)
        root.variables[elle].set_auto_scale(autoscale)
        out= {'attributes':vatt,
                'dimensions':root.variables[elle].dimensions,
                'value':root.variables[elle][:]}
        return out





class modis_l1b:
    def __init__(self,modfile):
        if not os.path.exists(modfile):
            print( 'No such file: '+ modfile)
            return None
        self.modfile=modfile
        try:
            with Dataset(modfile,'r') as fh:
                for sds in SDS:
                    if sds not in fh.variables:
                        print( modfile+" does not  ")
                        print( 'have "' + sds + '"! Is it realy a MODIS L1B')
                        print( '1km aggregated?')
                        raise Exception('MODIS L1B element not found')
                self.sdsatr={name: ncdf4getelement(modfile,name)['attributes'] for name in fh.variables}
                dims=fh.variables['EV_1KM_RefSB'].shape
                #print(dims)
                self.nlin=dims[2]
                self.ncol=dims[1]
        except Exception as inst:
            print( "Unexpected error opening %s: %s" % (modfile, inst) )
            return None

        self.wo_ist_band=-np.ones([2,38],np.int8)
        for j,sds in enumerate(SDS):
            bn=self.sdsatr[sds]['band_names'].split(',')
            for i,m1b in enumerate(M1B):
                cc=bn.count(m1b)
                if cc == 1:
                    self.wo_ist_band[0,i]=j
                    self.wo_ist_band[1,i]=bn.index(m1b)
        missing = self.wo_ist_band[1,:] == -1
        if missing.sum() > 0:
            print('Missing bands', [M1B[i] for i,_ in enumerate(missing) if _] )
            print( 'Is it really a MODIS L1B 1km aggregated?')
    

    def get_band(self,i_band,count=None,stride=None,offset=None,reflectance=False,fillvalue=None):
            
        # a bit dirty
        if not isinstance(i_band, str):
                band='%i'% int(i_band)
        else: band=i_band
       
        # find the corresponding sds 
        cnt=M1B.count(band)
        if cnt == 0: 
            print( '"',i_band ,'" is not a valid band identifier!')
            return None
        idx=M1B.index(band)

        sds=SDS[self.wo_ist_band[0,idx]]
        band_idx=self.wo_ist_band[1,idx]
        ret=ncdf4getelement(self.modfile,sds,autoscale=False)['value'][band_idx]
        orig_shape=ret.shape
        # now apply  scale and offset
        if reflectance and (sds == SDS[1]):
                print( iband, "is an emissive band and has no reflectance")
                return None
        
        if reflectance:
                #[sdsname][atrname][tuple_number][band_idx]
                offs=self.sdsatr[sds]['reflectance_offsets'][self.wo_ist_band[1,idx]]
                scal=self.sdsatr[sds]['reflectance_scales'][self.wo_ist_band[1,idx]]
        else:
                offs=self.sdsatr[sds]['radiance_offsets'][self.wo_ist_band[1,idx]]
                scal=self.sdsatr[sds]['radiance_scales'][self.wo_ist_band[1,idx]]

        # handle fillvalues
        fv=self.sdsatr[sds]['_FillValue']                 
        if fillvalue is None: fillvalue=fv
        
        idx_fail=np.where(ret>32767)
        ret[idx_fail]=fillvalue
        ret=ret.astype(np.float32).flatten()
        odx=np.nonzero(ret != fv)
        fdx=np.nonzero(ret == fv)
        if len(odx) > 0: ret[odx[0]]=scal*(ret[odx[0]]-offs)
        if len(fdx) > 0: ret[fdx[0]]=fillvalue

        if count is None : ic = [self.ncol,self.nlin] 
        else: ic = count
        if stride is None : ir = [1,1] 
        else: ir = stride
        if offset is None : io = [0,0] 
        else: io = offset

        return ret.reshape(orig_shape)[io[0]:io[0]+ic[0]:ir[0],io[1]:io[1]+ic[1]:ir[1]]+0 # sonst view = memory leak!
            
    def expand_tie_points(self,zzz,azimuth):
        xxx=sp.linspace(3,self.ncol-2,zzz.shape[0])
        yyy=sp.linspace(3,(zzz.shape[1]-1)*5+3,zzz.shape[1])
        if (azimuth is True):
                im=np.cos((zzz)*np.pi/180.)
                re=np.sin((zzz)*np.pi/180.)
                spl=sp.interpolate.RectBivariateSpline(xxx, yyy, im)
                imm=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
                spl=sp.interpolate.RectBivariateSpline(xxx, yyy, re)
                ree=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
                out=np.arctan2(ree,imm)*180./np.pi
        else:
                spl=spi.RectBivariateSpline(xxx, yyy, zzz) 
                out=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
        return out
            
            
    def _get_tie(self,name,count=None,stride=None,offset=None,azimuth=False):
        # prepare count,stride, offset
        if count is None : ic = [self.ncol,self.nlin] 
        else: ic = count
        if stride is None : ir = [1,1] 
        else: ir = stride
        if offset is None : io = [0,0] 
        else: io = offset
        #ret=self.fh.get_sds(name)
        ret=ncdf4getelement(self.modfile,name,autoscale=False)['value']
        if ret is None: return None
        if 'scale_factor' in self.sdsatr[name]:
                scale=self.sdsatr[name]['scale_factor']
                ret=ret*float(scale)
        rett=self.expand_tie_points(ret,azimuth)
        
        # Achtung python count notation gibt nicht das letzte Element an, sondern eines danach!  
        return rett[io[0]:io[0]+ic[0]:ir[0],io[1]:io[1]+ic[1]:ir[1]]+0 # sonst view = memory leak!
            
    def get_solar_zenith(self,count=None,stride=None,offset=None):
        return self._get_tie("SolarZenith",count=count,stride=stride,offset=offset)
    def get_solar_azimuth(self,count=None,stride=None,offset=None):
        return self._get_tie("SolarAzimuth",count=count,stride=stride,offset=offset,azimuth=True)
    def get_sensor_zenith(self,count=None,stride=None,offset=None):
        return self._get_tie("SensorZenith",count=count,stride=stride,offset=offset)
    def get_sensor_azimuth(self,count=None,stride=None,offset=None):
        return self._get_tie("SensorAzimuth",count=count,stride=stride,offset=offset,azimuth=True)
    def get_longitude(self,count=None,stride=None,offset=None):
        return self._get_tie("Longitude",count=count,stride=stride,offset=offset,azimuth=True)
    def get_latitude(self,count=None,stride=None,offset=None):
        return self._get_tie("Latitude",count=count,stride=stride,offset=offset)
    def get_height(self,count=None,stride=None,offset=None):
        return self._get_tie("Height",count=count,stride=stride,offset=offset)

               


def test():
    import modis_l1b
    from matplotlib import pyplot as pl
    #import fub_image
    #o=modis_l1b.modis_l1b('/people/rene/idltools/MOD021KM.A2005115.1130.005.2010151211431.hdf')
    o=modis_l1b.modis_l1b('MYD021KM.A2009229.1100.005.2009230172510.hdf')
    #o=modis_l1b.modis_l1b('/tmp/MOD021KM.A2006319.1225.006.2014221160029.h5')


    stride=[5,5] 
    #stride=None
    #offset=[45,100]
    offset=[0,2]
    count=[2030,1350]
    ttt=o._get_tie('SolarZenith',stride=stride,offset=offset,count=count)
    print( ttt.shape)
    sys.exit()
    #count=[300,300]
    #count=None

    bla=o.get_band(31,reflectance=False,fillvalue=-1,stride=stride,offset=offset,count=count)
    blo=o.get_band(19,reflectance=True,stride=stride,offset=offset,count=count)
    lat=o._get_tie('Latitude',stride=stride,offset=offset,count=count)
    ttt=o._get_tie('SolarZenith',stride=stride,offset=offset,count=count)
    lon=o._get_tie('Longitude',azimuth=True,stride=stride,offset=offset,count=count)
    hgt2=o._get_tie('Height',stride=stride,offset=offset,count=count)
    hgt1=o.get_height(stride=stride,offset=offset,count=count)
    print( (hgt1-hgt2).min(),(hgt1-hgt2).max())
    
    
    print( bla.shape,blo.shape,lat.shape,lon.shape)
 #   return
    
    print( lon.min(),lon.max())
    print( lat.min(),lat.max())
  
    #print( bla[123,32]
    #pylab.plot(bla)
    
    print( ttt.shape)
    #o.get_band('a')
    #pylab.plot(ttt[:30,3])
    #print( ttt[:10,:10]
    pl.imshow(blo)
    pl.colorbar(format="%.2f")
    pl.show()
    #klon=(lon-lon.min())/(lon.max()-lon.min())*360.-180.
    #fub_image.fub_image(blo,lon,lat,method="mesh",mini=0.,maxi=0.25,colorbar=False,fac=1.1 )
    #fub_image.fub_image(hgt,lon,lat,method="mesh",figsize=(20,20),colorbar=False,polar=True,mini=-1000.)

        


if __name__ == "__main__": test()



