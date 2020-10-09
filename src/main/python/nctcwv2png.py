#!/usr/bin/env python

import netCDF4 as nc

import matplotlib as mpl
# mpl.use('Agg')

import matplotlib.pyplot as plt
import numpy as np

import os
import sys
import copy
from datetime import datetime, date, time
# from enthought.pyface.ui.wx.grid.grid import Grid
# from matplotlib.colors import NP_CLIP_OUT
# from scikits.statsmodels.sandbox.regression.kernridgeregress_class import plt_closeall
from matplotlib import cm

dpi = 80.0
margin = 0.00

default_cmap = 'gist_stern'
default_cmap = 'gnuplot2'
default_cmap = 'jet'
creationYear = '2020'


# def readcdict(lutfile, minV, maxV):
#     if (lutfile!=None):
#         red=[]
#         green=[]
#         blue=[]
#         lut=open(lutfile, 'r')
#         red.insert(0,(0,0,0))
#         green.insert(0, (0,0,0))
#         blue.insert(0, (0,0,0))
#         i=1
#         for line in lut:
#                 tab=line.split(',')
#                 tab[0]=tab[0].strip()
#                 tab[1]=tab[1].strip()
#                 tab[2]=tab[2].strip()
#                 val= (i/256.0)
#                 red.insert(i,(val , float(tab[0])/255.0, float(tab[0])/255.0))
#                 green.insert(i,(val, float(tab[1])/255.0, float(tab[1])/255.0))
#                 blue.insert(i,(val, float(tab[2])/255.0, float(tab[2])/255.0))
#
#                 i+=1
#         return {'red':red, 'green':green, 'blue':blue}
#     else:
#         return None


# def toDateStr(dateIn):
#
# 	year=date[0:4]
#
# 	month=-1
# 	doy=-1
# 	if(len(dateIn)==8):
# 		doy=dateIn[5:8]
# 	else:
# 		month=dateIn[5:7]
#
# 	out=year
#
# 	if(doy>-1):
# 		dt=datetime.strptime(dateIn, "%Y.%j")
#                 out+=" - "+dt.strftime('%d')+' '+dt.strftime('%B')+" (doy:"+doy+")"
# 	if(month>-1):
#                 dt=datetime.strptime(dateIn, "%Y.%m")
#                 out+=" - "+dt.strftime('%B')
#
# 	return out

def plotImag(array, outPNG, band, date, tickets, cdict):
    # matplotlib.rcParams.update({'font.size': 8})

    # imgplot = plt.imshow(array)
    # imgplot.set_cmap('Set1')
    # array=array.T
    # fig = plt.figure(figsize=figsize, dpi=dpi)
    fig = plt.figure(figsize=figsize)
    ax1 = fig.add_axes([margin, margin, 1 - 2 * margin, 1 - 2 * margin])

    my_cmap = default_cmap
    # bla = cm.get_cmap(my_cmap, 12)
    # print(bla(range(100)))
    if (cdict != None):
        my_cmap = mpl.colors.LinearSegmentedColormap('my_colormap', cdict, 256)

    im = ax1.imshow(array, cmap=my_cmap)

    plt.xticks([])
    plt.yticks([])
    # plt.title(date)
    res = 360.0 / float(len(array[0]))
    cbaxes = fig.add_axes([0.05, 0.1, 0.05, 0.6])
    cbar = plt.colorbar(im, ticks=tickets, cax=cbaxes)
    cbar.ax.yaxis.set_tick_params(color='black')
    for label in cbar.ax.get_yticklabels():
        label.set_color(colorTxt)
        label.set_size(int((4 / res)))

    # print 'RESOLUTION: ', res
    ax1.text(0.95, 0.05, 'TCWV - ' + date,
             verticalalignment='bottom', horizontalalignment='right',
             transform=ax1.transAxes,
             color=colorTxt, fontsize=int(7 / res), fontstyle='oblique')

    ax1.text(0.098, 0.71, band.upper(),
             verticalalignment='bottom', horizontalalignment='right',
             transform=ax1.transAxes,
             color=colorTxt, fontsize=int(5 / res), fontstyle='oblique')

    ax1.text(0.95, 0.01, "ESA/CM-SAF (" + creationYear + ")",
             verticalalignment='bottom', horizontalalignment='right',
             transform=ax1.transAxes,
             color=colorTxt, fontsize=int(5 / res), fontstyle='oblique')

    # plt.show()
    plt.savefig(outPNG)

    # plt.clf()
    # plt.close()


if __name__ == "__main__":

    inFile = sys.argv[1]
    # outDir = sys.argv[2]
    outDir = os.getcwd()
    band = 'tcwv'
    min_max = '0.0:70.0'
    # lutColor = sys.argv[5]
    lutColor = None
    # size = sys.argv[6]
    size = '7200x3600'
    # colorTxt = sys.argv[8]
    colorTxt = '#224466'
    colorTxt = '#ffffff'
    # bandsDis = sys.argv[9]
    bandsDis = 'tcwv'

    numTicks = 4

    w, h = int(size.split("x")[0]), int(size.split("x")[1])
    figsize = (1 + margin) * w / dpi, (1 + margin) * h / dpi

    max = []
    min = []

    i = 0
    for val in min_max:
        # min.insert(i, float(val.split(':')[0]));
        min.insert(i, 0.0)
        # max.insert(i, float(val.split(':')[1]));
        max.insert(i, 70.0)
        # print bands[i], 'min, max', min[i],max[i]
        i += 1
    # name=inFile.split('/')[-1].replace('.nc','')
    name = os.path.splitext(os.path.basename(inFile))[0]
    print('inFile: ', inFile)
    print('name: ', name)
    ncfile = nc.Dataset(inFile, 'r')
    # data= ncfile.variables[band][0,:]
    # NOTE: Alex Loew requested a 3rd dimension (time). For old mosaic netcdf (2D) use this:
    # data= ncfile.variables[band][:]
    # w=np.where(np.isnan(data))
    # data[w]=0.0
    # data=np.where(data<=max[i], data, max[i])
    # data=np.where(data>min[i], data, 0.0)
    # print np.min(data), np.max(data)
    # data[0,0]=min[i]
    # data[-1,-1]=max[i]
    bin = (min[0] + max[0]) / float(numTicks)
    tickets = [min[0] + (j * bin) for j in range(numTicks + 1)]
    # cdict=readcdict(lutColor, min[i], max[i])
    cdict = None
    outdir = outDir
    # os.system('mkdir -p '+outdir)
    # dateStr=toDateStr(date0)
    date_start_index = name.find('20')
    date_end_index = name.find('fv') - 1
    date_str = name[date_start_index:date_end_index]

    tcwv_arr = np.array(ncfile.variables['tcwv'])
    # tcwv_arr[np.where(np.isnan(tcwv_arr))] = min[0]
    tcwv_arr[np.where(tcwv_arr > 70.0)] = -15.0
    tcwv_arr[np.where(np.isnan(tcwv_arr))] = -15.0

    # plotImag(data,outdir+'/'+name+"_tcwv.png", 'tcwv', dateStr, tickets, cdict)
    plotImag(tcwv_arr, outdir + '/' + name + "_tcwv.png", 'tcwv', date_str, tickets, cdict)
    i += 1

    ncfile.close()
    exit(0)
