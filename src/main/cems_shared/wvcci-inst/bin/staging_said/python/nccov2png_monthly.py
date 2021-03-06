
#!/usr/bin/env python

import netCDF4 as nc

import matplotlib as mpl
mpl.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np



import os
import sys
import copy
from datetime import datetime,date,time
#from enthought.pyface.ui.wx.grid.grid import Grid
#from matplotlib.colors import NP_CLIP_OUT
#from scikits.statsmodels.sandbox.regression.kernridgeregress_class import plt_closeall

dpi = 80.0
margin = 0.00

default_cmap='gist_stern'
#default_cmap='gnuplot2'
creationYear='2014'

def readcdict(lutfile, minV, maxV):
    if (lutfile!=None):
        red=[]
        green=[]
        blue=[]
        lut=open(lutfile, 'r')
        red.insert(0,(0,0,0))
        green.insert(0, (0,0,0))
        blue.insert(0, (0,0,0))
        i=1
        for line in lut:
                tab=line.split(',')
                tab[0]=tab[0].strip()
                tab[1]=tab[1].strip()
                tab[2]=tab[2].strip()
                val= (i/256.0)
                red.insert(i,(val , float(tab[0])/255.0, float(tab[0])/255.0))
                green.insert(i,(val, float(tab[1])/255.0, float(tab[1])/255.0))
                blue.insert(i,(val, float(tab[2])/255.0, float(tab[2])/255.0))

                i+=1
        return {'red':red, 'green':green, 'blue':blue}
    else:
        return None



def toDateStr(dateIn):
	
	year=date[0:4]
	
	month=-1
	doy=-1
	if(len(dateIn)==8):
		doy=dateIn[5:8]
	else:
		month=dateIn[5:7]
	
	out=year
		
	if(doy>-1):
		dt=datetime.strptime(dateIn, "%Y.%j")
                out+=" - "+dt.strftime('%d')+' '+dt.strftime('%B')+" (doy:"+doy+")"
	if(month>-1):
                dt=datetime.strptime(dateIn, "%Y.%m")
                out+=" - "+dt.strftime('%B')

	return out
    
def plotImag(array, outPNG, band, date, tickets, cdict):
    #matplotlib.rcParams.update({'font.size': 8})

    #imgplot = plt.imshow(array)
    #imgplot.set_cmap('Set1')
    #array=array.T
    fig = plt.figure(figsize=figsize, dpi=dpi)
    ax1 = fig.add_axes([margin, margin, 1 - 2*margin, 1 - 2*margin])
    
    my_cmap=default_cmap
    if(cdict != None):
    	my_cmap = mpl.colors.LinearSegmentedColormap('my_colormap',cdict,256)
    	
    im=ax1.imshow(array, cmap=my_cmap)
    
    plt.xticks([])
    plt.yticks([])
    #plt.title(date)
    res=360.0/float(len(array[0]))  
    cbaxes = fig.add_axes([0.05, 0.1, 0.05, 0.6]) 
    cbar=plt.colorbar(im, ticks=tickets, cax=cbaxes)
    cbar.ax.yaxis.set_tick_params(color='black')
    for label in cbar.ax.get_yticklabels():  
            label.set_color(colorTxt)
            label.set_size(int((4/res)))
    

    #print 'RESOLUTION: ', res
    ax1.text(0.67, 0.2, date,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(7/res),fontstyle='oblique')
    
    ax1.text(0.12, 0.72, band,
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color=colorTxt, fontsize=int(5/res), fontstyle='oblique')
  
    ax1.text(0.2, 0.02, "ImagingGroup.MSSL.UCL("+creationYear+")",
        verticalalignment='bottom', horizontalalignment='right',
        transform=ax1.transAxes,
        color='black', fontsize=int(2.5/res), fontstyle='oblique')

    #plt.show()                
    plt.savefig(outPNG)
                    
    plt.clf()
    plt.close()
    

inFile = sys.argv[1]
outDir = sys.argv[2]
#bands = sys.argv[3].split(',') 
bands = ['BHR_sigma_SW', 'BHR_SW'] 
min_max = sys.argv[3].split(',');
lutColor = sys.argv[4];
size = sys.argv[5];
idxyear = int(sys.argv[6])
idxmonth = int(sys.argv[7])
colorTxt = sys.argv[8]

#bandDisplay=copy.copy(bands)
bandDisplay = ['BHR_SW_CoV']

numTicks=4;


if(lutColor=='None' or lutColor=='none'):
	lutColor=None


w, h = int(size.split("x")[0]), int(size.split("x")[1])
figsize =  (1 + margin) * w / dpi, (1 + margin) * h / dpi

max=[]
min=[]

i=0
for val in min_max:
    min.insert(i, float(val.split(':')[0]));
    max.insert(i, float(val.split(':')[1]));
    #print bands[i], 'min, max', min[i],max[i]  
    i+=1

#name=inFile.split('/')[-1].replace('.nc','')    
#date=name.split('.')[idxdate];    
#date=name.split('.')[idxyear] + '_' + name.split('.')[idxmonth];    

name0=inFile.split('/')[-1].replace('.nc','')    # Qa4ecv.albedo.avh_geo.Merge.05.monthly.1982.9.PC
#date=name.split('.')[idxdate];
year = name0.split('.')[idxyear]
month = name0.split('.')[idxmonth]
if len(month) == 1:
    name = name0.replace(year + '.' + month, year + '.0' + month)
else:
    name = name0

date=name.split('.')[idxyear] + '_' + name.split('.')[idxmonth];

print('inFile: ', inFile) 
print('name: ', name) 
ncfile = nc.Dataset(inFile,'r')
i=0

data0 = ncfile.variables[bands[0]][:]
w = np.where(np.isnan(data0))
data0[w] = 0.0
data0 = np.where(data0<=max[0], data0, max[0])
data0 = np.where(data0>min[0], data0, min[0])
data0[0,0] = min[0]
data0[-1,-1] = max[0]

data1 = ncfile.variables[bands[1]][:]
w = np.where(np.isnan(data1))
data1[w] = 0.001
data1 = np.where(data1<=max[0], data1, max[0])
data1 = np.where(data1>min[0], data1, 0.001)
data1[0,0] = 0.001
data1[-1,-1] = max[0]

bin=(min[0]+max[0])/float(numTicks);
tickets=[min[0]+(j*bin) for j in range(numTicks+1)]
#date0=date[0:4]+'.'+date[4:]
cdict=readcdict(lutColor, min[0], max[0])
outdir=outDir+'/'+bandDisplay[0]
os.system('mkdir -p '+outdir)

#dateStr=toDateStr(date0)
dateStr=date
#dataCoV = data0/data1
dataCoV = np.divide(data0, data1)
w = np.where(np.isnan(dataCoV))
dataCoV[w] = 0.0
dataCoV = np.where(dataCoV<=max[0], dataCoV, max[0])
dataCoV = np.where(dataCoV>min[0], dataCoV, min[0])
dataCoV[0,0] = min[0]
dataCoV[-1,-1] = max[0]

plotImag(dataCoV,outdir+'/'+name+"_"+bandDisplay[0]+".png", bandDisplay[i], dateStr, tickets, cdict)

ncfile.close()
exit(0)

