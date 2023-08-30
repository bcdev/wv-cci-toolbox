# This program plots the completeness of MODIS Aqua/Terra (MOD021KM/MOD021KM) at CEDA NEODC,
# which is in line with original data availability at NASA:
# https://ladsweb.modaps.eosdis.nasa.gov/missions-and-measurements/products/MOD021KM
# https://ladsweb.modaps.eosdis.nasa.gov/missions-and-measurements/products/MYD021KM

# Script on JASMIN in WVCCI_INST/bin computing the numbers: check_l1b_neodc_completeness.sh

# OD, 20230830

from datetime import date
from matplotlib import pyplot
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import colors as mcolors

colors = dict(mcolors.BASE_COLORS, **mcolors.CSS4_COLORS)

years = np.arange(2000, 2023, 1.)

yearly_completeness_percent_terra = np.array(
    [79.1, 94.2, 96.7, 96.7, 99.2, 99.6, 99.0, 99.6, 98.7, 99.5, 99.6,
     99.7, 99.5, 99.6, 99.3, 99.6, 97.2, 99.6, 99.9, 99.9, 99.9, 99.9, 96.5])

yearly_completeness_percent_aqua = np.array(
    [0., 0., 46., 99.1, 99.9, 99.9, 99.8, 99.8, 99.9, 99.9, 99.9,
     99.8, 99.8, 99.9, 99.8, 99.9, 99.8, 99.9, 99.9, 99.9, 97.5, 99.9, 99.9])

line1, = plt.plot(years, yearly_completeness_percent_terra, color=colors['limegreen'], label='MODIS TERRA',
                  linestyle='-', marker="^")
line2, = plt.plot(years, yearly_completeness_percent_aqua, color=colors['darkblue'], label='MODIS AQUA',
                  linestyle='-', marker="s")

data_legend1 = plt.legend(handles=[line1, line2], loc=4, prop={'size': 8})
data_legend1.legendHandles[0].set_linewidth(2.0)
frame = data_legend1.get_frame()
frame.set_color('lightgray')
pyplot.gca().add_artist(data_legend1)

#average_text_terra = 'Average MODIS TERRA: ' + str("{:.2f}".format(np.mean(yearly_completeness_percent_terra))) + ' %'
#average_text_aqua  = 'Average MODIS AQUA: ' + str("{:.2f}".format(np.mean(yearly_completeness_percent_aqua))) + ' %'
#
#plt.text(2017., 78., average_text_terra + '\n' +
#         average_text_aqua,
#         style='italic', bbox={'facecolor': 'green', 'alpha': 0.5, 'pad': 5})

plt.ylim([75.0, 100.0])
plt.xlabel('Year')
plt.ylabel('Percent')
plt.title('MODIS MOD/MYD021KM L1b Coverage 2000 - 2022')
plt.grid(True)
today = date.today()
plt.savefig("plot_modis_l1b_completeness_" + today.strftime("%Y-%d-%m") + ".png")
plt.show()

