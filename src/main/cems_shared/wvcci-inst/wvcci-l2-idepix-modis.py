import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

sensors = ['MODIS_TERRA']
years = ['2011']    #test  
allMonths = ['12']
#allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def getMonth(year):
    #if year == '2002':
    #    return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    #if year == '2012':
    #    return ['01', '02', '03', '04']
    return allMonths

def getNumMonthDays(year, month_index):
    if month_index == 2:
        if calendar.isleap(int(year)):      
            return 29 
        else:
            return 28
    elif month_index == 4 or month_index == 6 or month_index == 9 or month_index == 11:
        #return 1  # test!!
        return 30
    else:
        #return 2  # test!!
        return 31

######################## L1b --> Idepix: ###########################

wvcciRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/WvcciTest'
snapDir = '/group_workspaces/cems2/qa4ecv/vol4/software/snap'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='wvcci-l2-idepix',
             logdir='log', 
             hosts=[('localhost',128)],
             types=[('wvcci-l2-idepix-step.sh',128)])

for year in years:

    eraInterimRootDir = wvcciRootDir + '/auxiliary/era-interim-t2m-mslp-tcwv-u10-v10'

    for sensor in sensors:
        l1bRootDir = wvcciRootDir + '/L1b/' + sensor
        modisLandMaskRootDir = wvcciRootDir + '/LandMask/MOD03'

        for month in getMonth(year):

            if os.path.exists(l1bRootDir + '/' + year + '/' + month):
                for iday in range(1, getNumMonthDays(year, int(month))+1):
                    day = str(iday).zfill(2)

                    # =============== l2 day =======================

                    # Idepix

                    idepixL2Dir = wvcciRootDir + '/Idepix/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)
                    idepixErainterimL2Dir = wvcciRootDir + '/Idepix-Erainterim/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)
                    #print 'idepixL2Dir: ', idepixL2Dir

                    if os.path.exists(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                        l1bFiles = os.listdir(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))
                        if len(l1bFiles) > 0:
                            for index in range(0, len(l1bFiles)):
                                l1bPath = l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]
                                if l1bFiles[index].endswith(".hdf"):
                                    # MODIS only
                                    # print 'modisLandMaskRootDir: ', modisLandMaskRootDir
                                    if os.path.exists(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                                        # we have a MOD03 land mask, should be the normal case!  
                                        modisLandMaskFiles = os.listdir(modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))
                                        modisLandMaskPath = modisLandMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisLandMaskFiles[index]
                                        if os.path.exists(modisLandMaskPath):

                                            # Idepix:
                                            m.execute('wvcci-l2-idepix-step.sh', 
                                                       ['dummy'], 
                                                       [idepixL2Dir], 
                                                       parameters=[l1bPath,modisLandMaskPath,l1bFiles[index],modisLandMaskFiles[index],idepixL2Dir,sensor,year,month,wvcciRootDir,snapDir])

                                            # Merge Idepix with ERA-INTERIM:
                                            eraInterimDir = eraInterimRootDir + '/' + year
                                            idepixFiles = os.listdir(idepixL2Dir)

                                            m.execute('wvcci-l2-idepix-modis-erainterim-step.sh',
                                                       [idepixL2Dir],
                                                       [idepixErainterimL2Dir],
                                                       parameters=[l1bPath,l1bFiles[index],idepixErainterimL2Dir,year,month,day,wvcciRootDir,snapDir])

m.wait_for_completion()

