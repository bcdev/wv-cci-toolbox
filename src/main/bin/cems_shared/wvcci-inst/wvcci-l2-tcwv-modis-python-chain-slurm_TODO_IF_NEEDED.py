import glob
import os
import calendar
import datetime
import fnmatch

from calendar import monthrange
from pmonitor import PMonitor
from monitor import Monitor
from job import Job

from pyhdf.SD import SD, SDC

__author__ = 'olafd'

sensor = 'MODIS_TERRA'
#sensor = 'MODIS_AQUA'
platformId = 'MOD'
#platformId = 'MYD'  # note that on neodc MYD03 and MYD35_L2 data start in May 2013 (as of 20200519) !!

years = ['2010']
#years = ['2016']
#years = ['2011']
#years = ['2011','2016']
#years = ['2013','2014']

allMonths = ['07']
#allMonths = ['12']
#allMonths = ['01','02','03']
#allMonths = ['04','05','06']
#allMonths = ['07','08','09']
#allMonths = ['10','11','12']
#allMonths = ['08','09']
#allMonths = ['10']
#allMonths = ['11','12']

#allMonths = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#days = ['07', '16', ,'17', '21', '23', '24', '25', '26', '28', '30', '31']
#days = ['17']

#################################################################

def getMonth(year):
    return allMonths

def is_daily_product(hdf_filename):

    hdf_file = SD(hdf_filename, SDC.READ)

    # get CoreMetadata.0 attribute (which is a very long string) and just extract and analyze the substring
    # with the DAYNIGHT info:
    core_metadata0_string = hdf_file.attributes()['CoreMetadata.0']
    daynight_start_index = core_metadata0_string.find('DAYNIGHT')

    if daynight_start_index != -1:
        #print('test: |' + core_metadata0_string[daynight_start_index:daynight_start_index+120] + '|')
        daynight_info_block = core_metadata0_string[daynight_start_index:daynight_start_index+120]

        is_daily = (daynight_info_block.find('\"Day\"') != -1)
        #print(is_daily)
        return is_daily
    else:
        return false


######################## L1b --> Idepix --> IdepixEraInterim --> TCWV: ###########################

wvcciRootDir = '/gws/nopw/j04/esacci_wv/odanne/WvcciRoot'
eraInterimRootDir = wvcciRootDir + '/auxiliary/era-interim-t2m-mslp-tcwv-u10-v10'

inputs = ['dummy']

# NEW PMonitor version for SLURM, MB/TB Nov 2018:
mon = Monitor(inputs,
             'wvcci-l2-tcwv-modis-python-chain-slurm',
             [('localhost',64)],
             [('wvcci-l2-tcwv-modis-python-step-slurm.sh', 64)],
             'log',
             False)

print('start...')
for year in years:
    l1bRootDir = wvcciRootDir + '/L1b/' + sensor
    modisLandMaskRootDir = wvcciRootDir + '/ModisLandMask/' + platformId + '03'
    modisCloudMaskRootDir = wvcciRootDir + '/ModisCloudMask/' + platformId + '35_L2'
    print('year: ' + year)

    for month in getMonth(year):
        print('month: ' + month)

        if os.path.exists(l1bRootDir + '/' + year + '/' + month):

            numMonthDays = monthrange(int(year), int(month))[1]
            print('numMonthDays: ' + str(numMonthDays))

            #for day in days:
            #for iday in range(15, 16):
            for iday in range(28, 29):
            #for iday in range(1, numMonthDays+1):
                day = str(iday).zfill(2)
                print('day: ' + day)

                tcwvDir = wvcciRootDir + '/Tcwv_python/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)

                if os.path.exists(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                    l1bFiles = os.listdir(l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2))

                    if len(l1bFiles) > 0:
                        for index in range(0, len(l1bFiles)):
                            l1bPath = l1bRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1bFiles[index]

			    # TEST: do only 1 Terra product, DOY 209, 10:50
                            if l1bFiles[index].endswith(".hdf") and l1bFiles[index].startswith("MOD021KM.A2010209.1050"):
                            # END TEST 
                            #if l1bFiles[index].endswith(".hdf"):
                                # MODIS only
                                # MOD021KM or MOD021KM product e.g. MOD021KM.A2015196.1855.061.2017321064215.hdf
                                dateTimeString = l1bFiles[index][9:22]  # A2015196.1855
                                hhmm = dateTimeString[9:]   # 1855

                                fileFilter = '*' + dateTimeString + '*.hdf'
                                l1bFileBase = os.path.splitext(l1bFiles[index])[0]

                                # cloud product e.g. MOD35_L2.A2015196.1855.061.2017321064215.hdf
                                modisCloudMaskFiles = fnmatch.filter(os.listdir(modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)), fileFilter)
                                if len(modisCloudMaskFiles) > 0:
                                    modisCloudMaskPath = modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisCloudMaskFiles[0]

                                    if os.path.exists(modisCloudMaskPath):

                                        # =============== Merge MODIS L1b with ERA-INTERIM, then TCWV from Idepix-ERA-INTERIM merge product  =======================

                                        l1bEraFile = l1bFileBase + '_l1b-era-interim.nc'

                                        job = Job('test-' + dateTimeString, 'wvcci-l2-tcwv-modis-python-step-slurm.sh', 
                                                 ['dummy'], [l1bEraFile], 
                                                 [l1bPath,l1bFiles[index],modisCloudMaskPath,sensor,year,month,day,hhmm,wvcciRootDir])
                                        #print('call mon.execute...')
                                        mon.execute(job)

mon.wait_for_completion()

