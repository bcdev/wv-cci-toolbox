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

#sensor = 'MODIS_TERRA'
sensor = 'MODIS_AQUA'
#platform_id = 'MOD'
platform_id = 'MYD'  # note that on neodc MYD03 and MYD35_L2 data start in May 2013 (as of 20200519) !!

#years = ['2017']
years = ['2022']
#years = ['2011']
#years = ['2011','2016']
#years = ['2013','2014']

all_months = ['08']
#all_months = ['12']
#all_months = ['01','02','03']
#all_months = ['04','05','06']
#all_months = ['07','08','09']
#all_months = ['10','11','12']
#all_months = ['08','09']
#all_months = ['10']
#all_months = ['11','12']

#all_months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

#days = ['07', '16', ,'17', '21', '23', '24', '25', '26', '28', '30', '31']
#days = ['17']

#################################################################

def get_month(year):
    return all_months

def get_era5_timestamp(hhmm):
    # first test:
    # hhmm <= 0900: 060000 
    # 0900 < hhmm <= 1500: 120000 
    # hhmm > 1500: 180000 

    ihhmm = int(hhmm)
    if ihhmm <= 900:
        return '060000'
    elif ihhmm > 900 and ihhmm <= 1500:
        return '120000'
    else:
        return '180000'

    # TODO: better provide Era5 for 03, 09, 15, 21 and use:
    # hhmm <= 0600: 030000 
    # 0600 < hhmm <= 1200: 090000 
    # 1200 < hhmm <= 1800: 150000 
    # hhmm > 1800: 210000 

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

wvcci_root_dir = '/gws/nopw/j04/esacci_wv/odanne/WvcciRoot'
era5_root_dir = wvcci_root_dir + '/auxiliary/era5-t2m-mslp-tcwv-u10-v10'

inputs = ['dummy']

# NEW PMonitor version for SLURM, MB/TB Nov 2018:
mon = Monitor(inputs,
             'wvcci-l2-tcwv-modis-era5-chain-slurm',
             [('localhost',64)],
             [('wvcci-l2-tcwv-modis-era5-step-slurm.sh', 64)],
             'log',
             False)

print('start...')
for year in years:
    l1b_root_dir = wvcci_root_dir + '/L1b/' + sensor
    modis_land_mask_root_dir = wvcci_root_dir + '/ModisLandMask/' + platform_id + '03'
    modis_cloud_mask_root_dir = wvcci_root_dir + '/ModisCloudMask/' + platform_id + '35_L2'
    print('year: ' + year)

    for month in get_month(year):
        print('month: ' + month)

        if os.path.exists(l1b_root_dir + '/' + year + '/' + month):

            num_month_days = monthrange(int(year), int(month))[1]
            print('num_month_days: ' + str(num_month_days))

            #for day in days:
            for iday in range(16, 17):
            #for iday in range(1, 2):
            #for iday in range(1, num_month_days+1):
                day = str(iday).zfill(2)
                print('day: ' + day)

                tcwv_dir = wvcci_root_dir + '/Tcwv/' + sensor + '/' + year + '/' + month + '/' + str(day).zfill(2)

                if os.path.exists(l1b_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2)):
                    l1b_files = os.listdir(l1b_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2))

                    if len(l1b_files) > 0:
                        for index in range(0, len(l1b_files)):
                            lab_path = l1b_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + l1b_files[index]

			    # TEST: do only 1 product, 1030
                            #if l1b_files[index].endswith(".hdf") and l1b_files[index].startswith("MYD021KM.A2022213.0410"):
                            if l1b_files[index].endswith(".hdf"):
                                # MODIS only
                                # MOD021KM or MOD021KM product e.g. MOD021KM.A2015196.1855.061.2017321064215.hdf
                                date_time_string = l1b_files[index][9:22]  # A2015196.1855
                                hhmm = date_time_string[9:]   # 1855

                                file_filter = '*' + date_time_string + '*.hdf'
                                l1b_file_base = os.path.splitext(l1b_files[index])[0]

                                # cloud product e.g. MOD35_L2.A2015196.1855.061.2017321064215.hdf
                                modis_cloud_mask_files = fnmatch.filter(os.listdir(modis_cloud_mask_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2)), file_filter)
                                if len(modis_cloud_mask_files) > 0:
                                    modis_cloud_mask_path = modis_cloud_mask_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/' + modis_cloud_mask_files[0]

                                    if os.path.exists(modis_cloud_mask_path):

                                        # =============== Merge MODIS L1b with ERA-INTERIM, then TCWV from Idepix-ERA-INTERIM merge product  =======================

                                        hhmm_era5 = get_era5_timestamp(hhmm)
                                        yyyymmdd = year + month + str(day).zfill(2) 
                                        era5_path = era5_root_dir + '/' + year + '/' + month + '/' + str(day).zfill(2) + '/era5_' + yyyymmdd + '_' + hhmm_era5 + '.nc'

                                        l1b_era_file = l1b_file_base + '_l1b-era-interim.nc'

                                        job = Job('test-' + date_time_string, 'wvcci-l2-tcwv-modis-era5-step-slurm.sh', 
                                                 ['dummy'], [l1b_era_file], 
                                                 [lab_path,l1b_files[index],modis_cloud_mask_path,era5_path,sensor,year,month,day,hhmm,wvcci_root_dir])
                                        #print('call mon.execute...')
                                        mon.execute(job)

mon.wait_for_completion()

