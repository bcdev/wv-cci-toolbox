import numpy as np
#import scipy.meshgrid as mg
import scipy as sp
import os
import hdf_sd
from scipy.interpolate import RectBivariateSpline



M1B=[ '1','2','3','4','5','6','7','8','9','10','11','12'   
     ,'13lo','13hi','14lo','14hi','15','16','17','18','19' 
     ,'20','21','22','23','24','25','26','27','28','29','30'  
     ,'31','32','33','34','35','36','37','38'] 
SDS=["EV_1KM_RefSB","EV_1KM_Emissive","EV_250_Aggr1km_RefSB"  
    ,"EV_500_Aggr1km_RefSB"]


class modis_l1b:
        def __init__(self,modfile):
                if not os.path.exists(modfile):
                        print('No such file: '+ modfile)
                        return None
                self.modfile=modfile
                try:
                       self.fh=hdf_sd.hdf_sd(modfile)
                except Exception as inst:
                        print("Unexpected error opening %s: %s" % (hdffile, inst)) 
                        return None
                for sds in SDS:
                        if sds not in self.fh.sds:
                                print(modfile+" does not  ")
                                print('have "' + sds + '"! Is it realy a MODIS L1B')
                                print('1km aggregated?')
                                return None
                self.wo_ist_band=-np.ones([2,38],np.int8)
                for j,sds in enumerate(SDS):
                        bn=self.fh.sdsatr[sds]['band_names'][0].split(',')
                        #print sds,bn
                        for i,m1b in enumerate(M1B):
                                cc=bn.count(m1b)
                                if cc == 1:
                                        self.wo_ist_band[0,i]=j
                                        self.wo_ist_band[1,i]=bn.index(m1b)
                cc=np.nonzero(self.wo_ist_band[1,:] == -1)
                if len(cc[0]) > 0:
                        print('File does not contain the band(s):')
                        for icc in cc[0]: print('"',M1B[int(icc)],'",', end=' ')
                        print('')
                        print('Is it realy a MODIS L1B 1km aggregated? ')
                dims=self.fh.sdsdims['EV_1KM_RefSB']['dims']
                self.nlin=dims[2]
                self.ncol=dims[1]
        

        def get_band(self,i_band,count=None,stride=None,offset=None,reflectance=False,fillvalue=None):
                
                # a bit dirty
                if not isinstance(i_band, str):
                        band='%i'% int(i_band)
                else: band=i_band
               
                # find the corresponding sds 
                cnt=M1B.count(band)
                if cnt == 0: 
                        print('"',i_band ,'" is not a valid band identifier!')
                        return None
                idx=M1B.index(band)

                h_count = [1,self.ncol,self.nlin] 
                h_stride = [1,1,1] 
                h_offset = [0,0,0] 
                # addapt the offset
                sds=SDS[self.wo_ist_band[0,idx]]
                h_offset[0]=self.wo_ist_band[1,idx]
                

                ret=self.fh.get_sds(sds,count=h_count,stride=h_stride,offset=h_offset).squeeze()
                orig_shape=ret.shape
                
                # ret=self.fh.get_sds(sds,count=i_count,stride=i_stride,offset=i_offset).flatten()
                # now apply  scale and offset
                
                if reflectance and (sds == SDS[1]):
                        print(iband, "is an emissive band and has no reflectance")
                        return None
                
                if reflectance:
                        #[sdsname][atrname][tuple_number][band_idx]
                        offs=self.fh.sdsatr[sds]['reflectance_offsets'][0][self.wo_ist_band[1,idx]]
                        scal=self.fh.sdsatr[sds]['reflectance_scales'][0][self.wo_ist_band[1,idx]]
                else:
                        offs=self.fh.sdsatr[sds]['radiance_offsets'][0][self.wo_ist_band[1,idx]]
                        scal=self.fh.sdsatr[sds]['radiance_scales'][0][self.wo_ist_band[1,idx]]
                        
                # handle fillvalues
                fv=self.fh.sdsatr[sds]['_FillValue'][0]                 #[self.wo_ist_band[1,idx]]
                #print "fv",fv
                if fillvalue==None: fillvalue=fv
                       
                ret=ret.astype(np.float32).flatten()
                odx=np.nonzero(ret != fv)
                fdx=np.nonzero(ret == fv)
                if len(odx) > 0: ret[odx[0]]=scal*(ret[odx[0]]-offs)
                if len(fdx) > 0: ret[fdx[0]]=fillvalue

                if count == None : ic = [self.ncol,self.nlin] 
                else: ic = count
                if stride == None : ir = [1,1] 
                else: ir = stride
                if offset == None : io = [0,0] 
                else: io = offset
                
                return ret.reshape(orig_shape)[io[0]:io[0]+ic[0]:ir[0],io[1]:io[1]+ic[1]:ir[1]]
                
        def expand_tie_points(self,zzz,longitude=False,sensor_azimuth=False):
                #print (zzz.shape[1]-1)*5+3
                xxx=sp.linspace(3,self.ncol-2,zzz.shape[0])
                yyy=sp.linspace(3,(zzz.shape[1]-1)*5+3,zzz.shape[1])
                if longitude:
                        im=np.cos((zzz)*np.pi/180.)
                        re=np.sin((zzz)*np.pi/180.)
                        spl=RectBivariateSpline(xxx, yyy, im)#, bbox=[0,0,self.ncol,self.nlin],kx=3,ky=3,s=1) 
                        imm=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
                        spl=RectBivariateSpline(xxx, yyy, re)#, bbox=[0,0,self.ncol,self.nlin],kx=3,ky=3,s=1) 
                        ree=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
                        out=np.arctan2(ree,imm)*180./np.pi
                elif sensor_azimuth:
                        zaa=np.where(zzz > 0, zzz, 360.+zzz)
                        spl=RectBivariateSpline(xxx, yyy, zaa)#, bbox=[0,0,self.ncol,self.nlin],kx=3,ky=3,s=1) 
                        out=spl(np.arange(0,self.ncol),np.arange(0,self.nlin)) 
                else:
                        spl=RectBivariateSpline(xxx, yyy, zzz)#, bbox=[0,0,self.ncol,self.nlin],kx=3,ky=3,s=1) 
                        out=spl(np.arange(0,self.ncol),np.arange(0,self.nlin))
                        
                return out
                
                
        def _get_tie(self,name,count=None,stride=None,offset=None,longitude=False,sensor_azimuth=False):
                # prepare count,stride, offset
                if count == None : ic = [self.ncol,self.nlin] 
                else: ic = count
                if stride == None : ir = [1,1] 
                else: ir = stride
                if offset == None : io = [0,0] 
                else: io = offset
                ret=self.fh.get_sds(name)
                if ret is None: return None
                if 'scale_factor' in self.fh.sdsatr[name]:
                        scale=self.fh.sdsatr[name]['scale_factor'][0]
                        ret=ret*float(scale)
                rett=self.expand_tie_points(ret,longitude=longitude,sensor_azimuth=sensor_azimuth)
                
                # Achtung python count notation gibt nicht das letzte Element an, sondern eines danach!  
                return rett[io[0]:io[0]+ic[0]:ir[0],io[1]:io[1]+ic[1]:ir[1]]
                
        def get_solar_zenith(self,count=None,stride=None,offset=None):
                return self._get_tie("SolarZenith",count=count,stride=stride,offset=offset)
        def get_solar_azimuth(self,count=None,stride=None,offset=None):
                return self._get_tie("SolarAzimuth",count=count,stride=stride,offset=offset)
        def get_sensor_zenith(self,count=None,stride=None,offset=None):
                return self._get_tie("SensorZenith",count=count,stride=stride,offset=offset)
        def get_sensor_azimuth(self,count=None,stride=None,offset=None):
                return self._get_tie("SensorAzimuth",count=count,stride=stride,offset=offset,sensor_azimuth=True)
        def get_longitude(self,count=None,stride=None,offset=None):
                return self._get_tie("Longitude",count=count,stride=stride,offset=offset,longitude=True)
        def get_latitude(self,count=None,stride=None,offset=None):
                return self._get_tie("Latitude",count=count,stride=stride,offset=offset)
        def get_height(self,count=None,stride=None,offset=None):
                return self._get_tie("Height",count=count,stride=stride,offset=offset)

               


def test():
        import modis_l1b_alt as modis_l1b
        import pylab
        #import fub_image
        #o=modis_l1b.modis_l1b('/home/rene/python/scientific_data/MOD021KM.A2005115.1130.005.2010151211431.hdf')
        o=modis_l1b.modis_l1b('MYD021KM.A2009229.1100.005.2009230172510.hdf')


        stride=[10,10] 
        #stride=None
        #offset=[45,100]
        offset=None
        
        #count=[300,300]
        count=None

        bla=o.get_band(31,reflectance=False,fillvalue=-1,stride=stride,offset=offset,count=count)
        blo=o.get_band(19,reflectance=True,stride=stride,offset=offset,count=count)
        #lat=o._get_tie('Latitude',stride=stride,offset=offset,count=count)
        #ttt=o._get_tie('SolarZenith',stride=stride,offset=offset,count=count)
        #lon=o._get_tie('Longitude',longitude=True,stride=stride,offset=offset,count=count)
        #hgt2=o._get_tie('Height',stride=stride,offset=offset,count=count)
        #hgt1=o.get_height(stride=stride,offset=offset,count=count)
        #print (hgt1-hgt2).min(),(hgt1-hgt2).max()
        
        
        print(bla.shape,blo.shape)#,lat.shape,lon.shape
 #       return
        
#        print lon.min(),lon.max()
#        print lat.min(),lat.max()
      
        #print bla[123,32]
        #pylab.plot(bla)
        
#        print ttt.shape
        #o.get_band('a')
        #pylab.plot(ttt[:30,3])
        #print ttt[:10,:10]
        pylab.imshow(bla)
        #pylab.colorbar(format="%.2f")
        pylab.show()
        #klon=(lon-lon.min())/(lon.max()-lon.min())*360.-180.
        #fub_image.fub_image(blo,lon,lat,method="mesh",mini=0.,maxi=0.25,colorbar=False,fac=1.1 )
        #fub_image.fub_image(hgt,lon,lat,method="mesh",figsize=(20,20),colorbar=False,polar=True,mini=-1000.)

        


if __name__ == "__main__": test()



