import glob
import os
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#############################################################################################################################
### Provides one step:
###    - staging 'browse2mov' --> 1-year movies from png browse files for each band + BHR RGB from Albedo mosaic netcdf files
#############################################################################################################################

years=['2010']

sensors=['meris']
bands=['tcwv_mean_mean']

gaRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest'

inputs = ['dummy']

m = PMonitor(inputs, 
             request='wvcci-l3-staging-browse2mov', 
             logdir='log',
             hosts=[('localhost',64)],
	     types=[('wvcci-l3-staging-browse2mov-step.sh',64)])

### matching for all years:
for year in years:
    for sensor in sensors:
        for band in bands:
            stagingMoviesInputDir = gaRootDir + '/staging/QL/tcwv/' + sensor + '/' + year + '/' + band
            stagingMoviesResultDir = gaRootDir + '/staging/Movies/tcwv/' + sensor + '/' + year
            m.execute('wvcci-l3-staging-browse2mov-step.sh', ['dummy'], [stagingMoviesResultDir], 
                              parameters=[year,band,stagingMoviesInputDir, stagingMoviesResultDir])

# wait for processing to complete
m.wait_for_completion()

#####################################################################################################
