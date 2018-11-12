import glob
import os
import calendar
import datetime

from calendar import monthrange
from pmonitor import PMonitor

__author__ = 'olafd'

sensor = 'MODIS_TERRA'

years = ['2011']    #test  
#allMonths = ['01']
#allMonths = ['02','03']
allMonths = ['08','09','10','11']
#allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def getMonth(year):
    #if year == '2002':
    #    return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    #if year == '2012':
    #    return ['01', '02', '03', '04']
    return allMonths

######################## L1b --> Idepix --> IdepixEraInterim --> TCWV: ###########################

wvcciRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/WvcciTest'
eraInterimRootDir = wvcciRootDir + '/auxiliary/era-interim-t2m-mslp-tcwv-u10-v10'
snapDir = '/group_workspaces/cems2/qa4ecv/vol4/software/snap'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='wvcci-l2-tcwv-modis-chain',
             logdir='log', 
             hosts=[('localhost',128)],
             types=[('wvcci-l2-idepix-modis-step.sh',32), 
                    ('wvcci-l2-tcwv-modis-step.sh', 96)])

for year in years:
    l1bRootDir = wvcciRootDir + '/L1b/' + sensor
    modisLandMaskRootDir = wvcciRootDir + '/LandMask/MOD03'

    for month in getMonth(year):

        if os.path.exists(l1bRootDir + '/' + year + '/' + month):

            numMonthDays = monthrange(int(year), int(month))[1]
            #numMonthDays = 3  # test
            for iday in range(1, numMonthDays+1):
                day = str(iday).zfill(2)

                idepixDir = wvcciRootDir + '/Idepix/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)
                idepixEraDir = wvcciRootDir + '/Idepix-Erainterim/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)
                tcwvDir = wvcciRootDir + '/Tcwv/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)

                if os.path.exists(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                    l1bFiles = os.listdir(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))

                    if len(l1bFiles) > 0:
                        for index in range(0, len(l1bFiles)):
                            l1bPath = l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]

                            if l1bFiles[index].endswith(".hdf"):
                                # MODIS only

                                if os.path.exists(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                                    # we have a MOD03 land mask, should be the normal case!  
                                    modisLandMaskFiles = os.listdir(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))
                                    modisLandMaskPath = modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisLandMaskFiles[index]

                                    if os.path.exists(modisLandMaskPath):
                                        l1bFileBase = os.path.splitext(l1bFiles[index])[0]

                                        # =============== Idepix  =======================

                                        # Idepix:
                                        idepixFile = l1bFileBase + '_idepix.nc'
                                        m.execute('wvcci-l2-idepix-modis-step.sh', 
                                                   ['dummy'], 
                                                   [idepixFile], 
                                                   parameters=[l1bPath,modisLandMaskPath,l1bFiles[index],modisLandMaskFiles[index],idepixDir,sensor,year,month,wvcciRootDir,snapDir])

                                        idepixPath = idepixDir + '/' + idepixFile

                                        # =============== Merge Idepix with ERA-INTERIM, then TCWV from Idepix-ERA-INTERIM merge product  =======================

                                        idepixEraFile = l1bFileBase + '_idepix-era-interim.nc'
                                        m.execute('wvcci-l2-tcwv-modis-step.sh',
                                                   [idepixFile],
                                                   [idepixEraFile],
                                                   parameters=[idepixPath,idepixFile,year,month,day,wvcciRootDir,snapDir])

m.wait_for_completion()

