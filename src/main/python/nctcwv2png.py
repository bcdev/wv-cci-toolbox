#!/usr/bin/env python

import netCDF4

import matplotlib.pyplot as plt
from mpl_toolkits.axes_grid1 import make_axes_locatable
import numpy as np

import os
import sys

default_cmap = 'gist_stern'
creation_year = '2020'


def plot_image(array, out_png, date):
    '''

    :param array:
    :param out_png:
    :param date:
    :return:
    '''

    # draw image
    fig, ax1 = plt.subplots(1, 1, figsize=(72, 36),
                            subplot_kw={'xticks': [-180, -90, 0, 90, 180],
                                        'yticks': [-60, -30, 0, 30, 60, 90]})
    im = ax1.imshow(array, cmap=default_cmap, extent=[-180,180,-90,90])
    for tick in ax1.xaxis.get_major_ticks():
        tick.label.set_fontsize(30)
    for tick in ax1.yaxis.get_major_ticks():
        tick.label.set_fontsize(30)

    # set up vertical color bar right to image
    divider = make_axes_locatable(ax1)
    cax = divider.append_axes("right", size="3%", pad=0.5)
    wv_ticks = [0, 10, 20, 30, 40, 50, 60, 70]
    cbar = plt.colorbar(im, ticks=wv_ticks, cax=cax)
    cbar.ax.yaxis.set_tick_params(color='black')
    for label in cbar.ax.get_yticklabels():
        label.set_color('black')
        label.set_size(50)

    # draw labels into image
    ax1.text(0.25, 0.05, 'TCWV (kg/m2) - ' + date,
             verticalalignment='bottom', horizontalalignment='right',
             transform=ax1.transAxes,
             color='white', fontsize=60, fontstyle='oblique')

    ax1.text(0.98, 0.025, "ESA/CM-SAF (C)" + creation_year,
             verticalalignment='bottom', horizontalalignment='right',
             transform=ax1.transAxes,
             color='white', fontsize=60, fontstyle='oblique')

    plt.savefig(out_png)


if __name__ == "__main__":

    # get TCWV input data
    input_file = sys.argv[1]
    input_filename = os.path.splitext(os.path.basename(input_file))[0]
    ncfile = netCDF4.Dataset(input_file, 'r')

    # extract date from input filename
    date_start_index = input_filename.find('20')
    date_end_index = input_filename.find('fv') - 1
    date_str = input_filename[date_start_index:date_end_index]

    # change values outside [0, 70] to black color (negative values for given 'gist_stern' color map)
    tcwv_arr = np.array(ncfile.variables['tcwv'])
    tcwv_arr[np.where(tcwv_arr > 70.0)] = -3.0
    tcwv_arr[np.where(np.isnan(tcwv_arr))] = -3.0

    # draw white grid lines every 30deg by changing tcwv values to 70.0 (white for given 'gist_stern' color map)
    dx, dy = int(tcwv_arr.shape[0]/6), int(tcwv_arr.shape[0]/6)
    tcwv_arr[:,::dy] = 70.0
    tcwv_arr[::dx,:] = 70.0

    # create the plot and save to png
    output_dir = os.getcwd()
    output_path = output_dir + '/' + input_filename + '_tcwv.png'
    plot_image(tcwv_arr, output_path, date_str)

    # close input netcdf
    ncfile.close()

    exit(0)
