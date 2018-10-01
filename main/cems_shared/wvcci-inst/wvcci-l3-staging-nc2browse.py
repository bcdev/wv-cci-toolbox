import glob
import os
import datetime
from pmonitor import PMonitor

import calendar
from calendar import monthrange, isleap

__author__ = 'olafd'

##################################################################################################
### Provides one step:
###    - staging 'nc2browse' --> png files for band tcwv_mean_mean from WV-CCI L3 netcdf files
##################################################################################################


def getNumDaysInMonth(year, month):
    return calendar.monthrange(int(year), int(month))[1]

def getMonths(year):
    if year == '2002':
        months  = [ '04', '05', '06', '07', '08', '09', '10', '11', '12' ]
    elif year == '2012':
        months  = [ '01', '02', '03', '04' ]
    else:
        #months  = [ '01' , '07']
        months  = [ '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12' ]

    return months

#################################################################################################

years=['2010']

sensor = 'meris'

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='wvcci-l3-staging-nc2browse', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('wvcci-l3-staging-nc2browse-step.sh',64)])

for year in years:
    tcwvMosaicDir = gaRootDir + '/Mosaic/TCWV/' + sensor + '/' + year
    stagingNc2browseResultDir = gaRootDir + '/staging/QL/tcwv/' + sensor + '/' + year
    for month in getMonths(year):
        # l3_tcwv_meris_2008-01-01_2008-01-31.nc
        stagingNc2browseFile = tcwvMosaicDir + '/l3_tcwv_' + sensor + '_' + year + '-' + month + '-01_' + year + '-' + month + '-' + str(getNumDaysInMonth(year, month)) + '.nc' 

        m.execute('wvcci-l3-staging-nc2browse-step.sh', ['dummy'], [stagingNc2browseResultDir], 
                   parameters=[year,month,sensor.upper(),stagingNc2browseFile, stagingNc2browseResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
