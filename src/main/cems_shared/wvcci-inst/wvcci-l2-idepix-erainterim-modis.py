import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

#######################################################################################################
# Script for MODIS EraInterim L2 generation. Using Idepix products as reference dataset instead of L1b.
#######################################################################################################

sensors = ['MODIS_TERRA']
years = ['2011']    #test  
#allMonths = ['08']
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

######################## EraInterim from Idepix: ###########################

wvcciRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/WvcciTest'
snapDir = '/group_workspaces/cems2/qa4ecv/vol4/software/snap'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='wvcci-l2-idepix-erainterim-modis',
             logdir='log', 
             hosts=[('localhost',128)],
             types=[('wvcci-l2-idepix-erainterim-modis-step.sh',128)])

for year in years:

    for sensor in sensors:
        idepixRootDir = wvcciRootDir + '/Idepix/' + sensor

        for month in getMonth(year):

            if os.path.exists(idepixRootDir + '/' + year + '/' + month):
                #for iday in range(1, getNumMonthDays(year, int(month))+1):
                for iday in range(2, 3):
                    day = str(iday).zfill(2)

                    erainterimL2Dir = wvcciRootDir + '/Erainterim/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)

                    if os.path.exists(idepixRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                        idepixFiles = os.listdir(idepixRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))
                        if len(idepixFiles) > 0:
                            for index in range(0, len(idepixFiles)):
                                idepixPath = idepixRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + idepixFiles[index]
                                if idepixFiles[index].endswith(".nc"):
                                    m.execute('wvcci-l2-idepix-modis-erainterim-step.sh', 
                                               ['dummy'], 
                                               [idepixFiles[index] + '_erainterim'], 
                                               parameters=[idepixPath,idepixFiles[index],year,month,day,wvcciRootDir])

m.wait_for_completion()

