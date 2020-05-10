import glob
import os
import calendar
import datetime
import fnmatch

from calendar import monthrange
from pmonitor import PMonitor

from pyhdf.SD import SD, SDC

__author__ = 'olafd'

sensor = 'MODIS_TERRA'

#years = ['2017']
#years = ['2016']
#years = ['2011']
years = ['2011','2016']

#allMonths = ['01']
allMonths = ['11']
#allMonths = ['01','02','03']
#allMonths = ['04','05','06']
#allMonths = ['07','08','09']
#allMonths = ['08','09','10']
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

# NEW PMonitor version, MB/TB Nov 2018:
m = PMonitor(inputs,
             request='wvcci-l2-tcwv-modis-chain_3',
             logdir='log',
             hosts=[('localhost',256)],
             types=[('wvcci-l2-tcwv-modis-step_2.sh', 256)],
             polling="job_status_callback.sh")

for year in years:
    l1bRootDir = wvcciRootDir + '/L1b/' + sensor
    modisLandMaskRootDir = wvcciRootDir + '/ModisLandMask/MOD03'
    modisCloudMaskRootDir = wvcciRootDir + '/ModisCloudMask/MOD35_L2'
    print('year: ' + year)

    for month in getMonth(year):
        print('month: ' + month)

        if os.path.exists(l1bRootDir + '/' + year + '/' + month):

            numMonthDays = monthrange(int(year), int(month))[1]
            print('numMonthDays: ' + str(numMonthDays))

            #for day in days:
            #for iday in range(15, 16):
            for iday in range(2, 3):
            #for iday in range(1, numMonthDays+1):
                day = str(iday).zfill(2)
                print('day: ' + day)

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

                                        # cloud product e.g. MOD35_L2.A2015196.1855.061.2017321064215.hdf
                                        modisCloudMaskFiles = fnmatch.filter(os.listdir(modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2)), fileFilter)
                                        modisCloudMaskPath = modisCloudMaskRootDir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modisCloudMaskFiles[0]

                                        if os.path.exists(modisCloudMaskPath):
                                            #print( 'Daily product: ' + str(is_daily_product(modisCloudMaskPath)) + ' --> ' + modisCloudMaskPath)

                                            if is_daily_product(modisCloudMaskPath):
                                                # exclude non-daily products from the beginning:
                                                print( 'Daily product: ' + str(is_daily_product(modisCloudMaskPath)) + ' --> ' + modisCloudMaskPath)

  
                                                # =============== Merge MODIS L1b with ERA-INTERIM, then TCWV from Idepix-ERA-INTERIM merge product  =======================

                                                l1bEraFile = l1bFileBase + '_l1b-era-interim.nc'
                                                m.execute('wvcci-l2-tcwv-modis-step_2.sh',
                                                           ['dummy'],
                                                           [l1bEraFile],
                                                           parameters=[l1bPath,l1bFiles[index],modisCloudMaskPath,year,month,day,hhmm,wvcciRootDir])

m.wait_for_completion()

