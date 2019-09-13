import glob
import os
import calendar
import datetime
import fnmatch

from calendar import monthrange
from pmonitor import PMonitor

__author__ = 'olafd'

sensor = 'MODIS_TERRA'

years = ['2011']    #test  
#years = ['2012']    #test  
#years = ['2018']    #test  
#allMonths = ['01','02','03']
#allMonths = ['03','04','05']
#allMonths = ['06','07','08','09']
#allMonths = ['04','05','06']
#allMonths = ['07','08','09']
#allMonths = ['10','11','12']
#allMonths = ['01', '07']
allMonths = ['05']
##allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#days = ['07', '16', ,'17', '21', '23', '24', '25', '26', '28', '30', '31']
#days = ['17']

#################################################################

def getMonth(year):
    return allMonths

######################## L1b --> Idepix --> IdepixEraInterim --> TCWV: ###########################

wvcciRootDir = '/gws/nopw/j04/esacci_wv/odanne/WvcciRoot'
eraInterimRootDir = wvcciRootDir + '/auxiliary/era-interim-t2m-mslp-tcwv-u10-v10'
snapDir = '/gws/nopw/j04/esacci_wv/software/snap'

inputs = ['dummy']

# NEW PMonitor version, MB/TB Nov 2018:
m = PMonitor(inputs,
             request='wvcci-l2-tcwv-modis-chain',
             logdir='log',
             hosts=[('localhost',384)],
             types=[('wvcci-l2-idepix-modis-step.sh',128),
                    ('wvcci-l2-tcwv-modis-step.sh', 256)],
             polling="job_status_callback.sh")


for year in years:
    l1bRootDir = wvcciRootDir + '/L1b/' + sensor
    modisLandMaskRootDir = wvcciRootDir + '/ModisLandMask/MOD03'
    modisCloudMaskRootDir = wvcciRootDir + '/ModisCloudMask/MOD35_L2'

    for month in getMonth(year):

        if os.path.exists(l1bRootDir + '/' + year + '/' + month):

            numMonthDays = monthrange(int(year), int(month))[1]
            #for day in days:
            #for iday in range(15, 16):
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
                                # MOD021KM product e.g. MOD021KM.A2015196.1855.061.2017321064215.hdf
                                dateTimeString = l1bFiles[index][9:22]  # A2015196.1855
                                hhmm = dateTimeString[9:]   # 1855

                                if os.path.exists(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                                    # we have a MOD03 land mask, should be the normal case!  
                                    # MOD03 product e.g. MOD03.A2015196.1855.061.2017321064215.hdf
                                    fileFilter = '*' + dateTimeString + '*.hdf'
                                    modisLandMaskFiles = fnmatch.filter(os.listdir(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)), fileFilter) 
                                    modisLandMaskPath  = modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisLandMaskFiles[0]

                                    if os.path.exists(modisLandMaskPath):
                                        l1bFileBase = os.path.splitext(l1bFiles[index])[0]

                                        # =============== Idepix  =======================

                                        # Idepix:
                                        idepixFile = l1bFileBase + '_idepix.nc'
                                        m.execute('wvcci-l2-idepix-modis-step.sh', 
                                                   ['dummy'], 
                                                   [idepixFile], 
                                                   parameters=[l1bPath,modisLandMaskPath,l1bFiles[index],modisLandMaskFiles[0],idepixDir,sensor,year,month,wvcciRootDir,snapDir])

                                        idepixPath = idepixDir + '/' + idepixFile

                                        # cloud product e.g. MOD35_L2.A2015196.1855.061.2017321064215.hdf
                                        modisCloudMaskFiles = fnmatch.filter(os.listdir(modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)), fileFilter)
                                        modisCloudMaskPath = modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisCloudMaskFiles[0]

                                        # =============== Merge Idepix with ERA-INTERIM, then TCWV from Idepix-ERA-INTERIM merge product  =======================

                                        # !!! TODO: make sure that l1bFiles[index], modisLandMaskFiles[index] and modisCloudMaskFiles[0] refer to the same swath/time !!!
                                        # should be OK?! (20190716)

                                        idepixEraFile = l1bFileBase + '_idepix-era-interim.nc'
                                        m.execute('wvcci-l2-tcwv-modis-step.sh',
                                                   [idepixFile],
                                                   [idepixEraFile],
                                                   parameters=[idepixPath,idepixFile,modisCloudMaskPath,year,month,day,hhmm,wvcciRootDir,snapDir])

m.wait_for_completion()

