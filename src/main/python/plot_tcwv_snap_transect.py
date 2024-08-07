# This program plots the money balance from 2005 to given year as taken from 'Finanzstatus' and listed
# manually per year and month.  
# - Versmold heritage (+80K) and Landrover (-35K) are excluded here.
# - years 2009-2011: increases due to KK, JG.
#
# in Idea just run as it is with Python 3.x
# OD, 2020-2024

from datetime import date
from matplotlib import pyplot
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import colors as mcolors
import unicodedata

colors = dict(mcolors.BASE_COLORS, **mcolors.CSS4_COLORS)

pixels = np.arange(1, 129, 1.)
tcwv = np.array(
    [20.42,20.34,20.21,20.09,20.25,19.87,19.97,20.19,20.2,20.43,19.93,
     19.53,19.29,20.25,19.46,19.33,19.57,19.41,19.41,19.53,19.62,19.68,19.35,
     19.09,19.86,19.75,19.75,19.85,19.69,19.58,19.25,19.69,19.15,19.35,19.42,
     19.62,20.19,19.95,19.96,20.45,19.95,19.86,19.89,19.36,19.57,20.05,
     20.28,20.16,20.24,20.10,20.19,19.94,20.18,19.62,20.14,20.27,20.17,
     20.22,19.83,20.06,20.1,20.11,20.45,19.86,20.61,19.8,20.69,19.63,
     20.59,20.02,20.65,19.46,20.59,19.59,20.57,19.85,20.23,19.79,20.33,21.39,
     20.43,19.92,20.87,20.11,21.16,20.98,20.15,20.94,20.16,21.09,19.8,
     21.12,20.56,19.72,21.15,20.93,20.29,20.94,20.45,20.6,20.75,20.75,
     20.74,21.47,21.27,21.05,21.9,21.51,21.33,21.54,21.68,21.59,21.17,21.61,21.61,20.67,21.54,
     21.54,21.54,20.71,21.75,21.49,21.19,21.48,21.64,21.42,21.07,21.48,
     21.47,21.33,21.33,21.44,21.94,21.96,21.68,21.99,21.99,21.46,21.39,
     21.39,21.44,21.44,21.3,21.41,21.44,21.66,21.41,21.27,21.44,20.93,
     20.93,21.46,21.55,21.28,21.79,21.65,21.62,21.71,21.44,21.42,21.15,
     21.22,21.38,21.45,21.59,21.52,21.49,21.53,21.31,21.38,21.06,20.87,
     21.26,21.24,21.91,21.66,21.42,21.61,21.36,21.37,21.28,21.45,21.44,
     21.55,21.48,21.7,21.41,21.37,21.91,20.96,20.96,21.38,21.39,21.83,
     22.18,22.07,22.52,22.45,22.38,22.2,22.24,22.21,21.72,22.16,22.35,
     22.15,22.37,22.35,22.3,22.21,22.29,21.91,22.42,22.31,22.17,22.37,
     22.24,21.9,21.8,22.2,21.67,22.06,22.19,21.99,21.99,21.68,21.62,21.33,
     21.8,21.36,21.46,21.63,21.67,21.97,21.99,22.0,21.83,21.49,21.74,
     22.02,22.12,22.14,22.4,22.27,21.98,22.23,21.74,21.65])

tcwv_fft = np.fft.fft(tcwv[0:128]).real
wvl = np.fft.fftfreq(tcwv.shape[-1])

# line1, = plt.plot(pixels, tcwv, color=colors['red'], label='transect', linestyle='-', marker="")
line2, = plt.plot(pixels, tcwv_fft, color=colors['limegreen'], label='income', linestyle='-', marker="")
# line3, = plt.plot(years, dividends_monthly_ave, color=colors['gold'], label='interests/dividends', linestyle='-',
#                   marker="P")
# line4, = plt.plot(years, income_plus_dividends, color=colors['darkgreen'], label='income + interests/dividends',
#                   linestyle='-', marker="s")
# data_legend1 = plt.legend(handles=[line1, line2, line3, line4], loc=0, prop={'size': 8})
# data_legend1 = plt.legend(handles=[line1], loc=0, prop={'size': 8})
data_legend1 = plt.legend(handles=[line2], loc=0, prop={'size': 8})
data_legend1.legendHandles[0].set_linewidth(1.0)
frame = data_legend1.get_frame()
frame.set_color('gray')
pyplot.gca().add_artist(data_legend1)

# plt.xlim([0.0, 250.0])
plt.xlim([0.0, 25.])
plt.xticks(np.arange(0, 25, step=5))
# plt.ylim([15., 25.0])
plt.ylim([0., 10.0])
plt.xlabel('Transect Pixel')
plt.ylabel('TCWV')
plt.title('MODIS stripes pattern')
plt.grid(True)
plt.savefig("modis_stripes_pattern_" + date.today().strftime("%Y-%m-%d") + ".png")
plt.show()
