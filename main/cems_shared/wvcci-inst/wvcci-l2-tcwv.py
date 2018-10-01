import glob
import os
import calendar
import datetime
from pmonitor import PMonitor

__author__ = 'olafd'

sensors = ['MERIS']
years = ['2011']    #test  
#allMonths = ['05']
allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#################################################################

def getMonth(year):
    if year == '2002':
        return ['04', '05', '06', '07', '08', '09', '10', '11', '12']
    if year == '2012':
        return ['01', '02', '03', '04']
    return allMonths

def getNumMonthDays(year, month_index):
    if month_index == 2:
        if calendar.isleap(int(year)):      
            return 29 
        else:
            return 28
    elif month_index == 4 or month_index == 6 or month_index == 9 or month_index == 11:
        return 30
    else:
        #return 1  # test!!
        return 31

######################## Idepix --> TCWV: ###########################

wvcciRootDir = '/group_workspaces/cems2/qa4ecv/vol4/olafd/WvcciTest'
idepixRootDir = wvcciRootDir + '/Idepix'

snapDir = '/group_workspaces/cems2/qa4ecv/vol4/software/snap'

inputs = ['dummy']
m = PMonitor(inputs, 
             request='wvcci-l2-tcwv',
             logdir='log', 
             hosts=[('localhost',128)],
             types=[('wvcci-l2-tcwv-step.sh',128)])

for year in years:
    for sensor in sensors:
        for month in getMonth(year):
            tcwvL2Dir = wvcciRootDir + '/Tcwv/' + sensor + '/' + year + '/' + month 

            if os.path.exists(idepixRootDir + '/' + sensor + '/' + year + '/' + month):
                for day in range(1, getNumMonthDays(year, int(month))+1):
                    if os.path.exists(idepixRootDir + '/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                        idepixFiles = os.listdir(idepixRootDir + '/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2))
                        if len(idepixFiles) > 0:
                            for index in range(0, len(idepixFiles)):
                                if idepixFiles[index].endswith(".nc") or idepixFiles[index].endswith(".nc.gz"):
                                    idepixPath = idepixRootDir + '/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + idepixFiles[index]
                                    m.execute('wvcci-l2-tcwv-step.sh', ['dummy'], [tcwvL2Dir], parameters=[idepixPath,idepixFiles[index],tcwvL2Dir,sensor,year,month,wvcciRootDir,snapDir])

m.wait_for_completion()

