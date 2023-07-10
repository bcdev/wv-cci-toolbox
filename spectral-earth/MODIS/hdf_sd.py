from pyhdf import SD
import numpy as np
import os

class hdf_sd:
        '''
        A simple wrapper for pyhdf
        '''
        def __init__(self,hdffile):
                if not os.path.exists(hdffile):
                        print(('No such file: '+ hdffile))
                        return None
                self.hdffile=hdffile
                try:
                       self.fh=SD.SD(hdffile,SD.SDC.READ)
                except Exception as  inst:
                        print(("Unexpected error opening %s: %s" % (hdffile, inst))) 
                        return None
                self.sds=self.fh.datasets()
                self.atr=self.fh.attributes(full=1)
                self.sdsdims={}
                for name in list(self.sds.keys()):
                        self.sdsdims[name]={'dimnames':self.sds[name][0],'dims':self.sds[name][1]}
                self.sdsatr={}
                for name in list(self.sds.keys()):
                        self.sdsatr[name]=self.fh.select(name).attributes(full=1)
                        

        def get_sds(self,sds_name,count=None,offset=None,stride=None):
                
                #Achtung stride is das "hdf stride" und nicht das idl/python stride
                # also hier erst stride dann count
                # (in idl/python: erst count dann stride)
                # 
                # Weiterhin ist count bei hdf wirklich die Anzahl der Elemente
                # in der python notation ist es dagegen das letzte element+1
                
                if sds_name not in self.sds: return None
                if count == None  : count =list((self.sds[sds_name][1]))   # dimensions
                if offset == None : offset=list((i*0    for i in count))   # same size as dimension
                if stride == None : stride=list((i*0+1  for i in count))   # same size as dimension
                # clip count to max elements
                for i in range(len(count)):
                        maxi=(self.sds[sds_name][1][i]-offset[i]-1)/stride[i]+1
                        #print('maxi',maxi,count[i]
                        count[i] = min(maxi,count[i])

                #print("hh",count,offset,stride
                # everything has to be an int 
                for i in range(len(count)):
                        count[i]=int(count[i])
                        stride[i]=int(stride[i])
                        offset[i]=int(offset[i])
                #print('hhh',count,stride,offset        
                return self.fh.select(sds_name).get(count=count,start=offset,stride=stride)


def test():
        import hdf_sd
        o=hdf_sd.hdf_sd('./MYD06.hdf')

        #global attributes and sds names
        print(("Global Attrinutes",list(o.atr.keys())))
        print(("Dataset names",list(o.sds.keys())))
        print(('Cloud_Effective_Radius' in o.sds))

        #sds attributes
        print(('scale_factor' in o.sdsatr['Cloud_Effective_Radius']))
        #sds attribute o.sdsatr[sdsname][atrname][0]
        print((o.sdsatr['Cloud_Effective_Radius']['scale_factor'][0]))
        print((list(o.sdsatr['Cloud_Effective_Radius'].keys())))
        
        ddd=o.get_sds('Cloud_Effective_Radius',count=[10,10],offset=[100,100],stride=[2,2])
        print((type(ddd)))
        print((ddd.shape))
        ddd=o.get_sds('Cloud_Effective_Radius',stride=[2,2])
        print((type(ddd)))
        print((ddd.shape))
        print((o.sdsdims['Longitude']['dims']))
        print((o.sdsdims['Longitude']['dimnames']))
        
        
        o=hdf_sd.hdf_sd('./MOD11A1.A2009010.h13v03.005.2009012113003.hdf')
        print(("Global Attrinutes",list(o.atr.keys())))
        print(("Dataset names",list(o.sds.keys())))
        ddd=o.get_sds('LST_Day_1km')
        print((type(ddd)))
        print((ddd.shape))
        print((ddd.min()))
        print((ddd.max()))
        print((o.sdsatr['LST_Day_1km']['scale_factor'][0]))
        print((ddd.min()*o.sdsatr['LST_Day_1km']['scale_factor'][0]))
        print((ddd.max()*o.sdsatr['LST_Day_1km']['scale_factor'][0]))
       
        
        

if __name__ == "__main__": test()


