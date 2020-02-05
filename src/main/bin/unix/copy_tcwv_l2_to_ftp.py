#!/usr/bin/env python

from os import listdir, system
from os.path import exists

import datetime
import calendar

from datetime import date
from calendar import monthrange, isleap

#############################################################
# Script to rsync TCWV L2 data with BC WV_cci ftp (bcserver8)
# O. Danne, July 2019
############################################################# 

##########################################################################

def getMonths(year, sensor):
    #months  = [ '01', '04', '07', '10' ]
    #months  = [ '02', '05', '08', '11' ]
    #months  = [ '03', '06', '09', '12' ]
    #months  = [ '08', '09', '11', '12' ]
    #months  = [ '08', '09' ]
    months  = [ '12' ]
    return months

##########################################################################

def getNumDaysInMonth(year, month):
    return calendar.monthrange(int(year), int(month))[1]

##########################################################################

def getMinMaxDate(year, month):
    numDaysInMonth = getNumDaysInMonth(year, month)
    minDate = datetime.date(int(year), int(month), 1)
    maxDate = datetime.date(int(year), int(month), int(numDaysInMonth))
    return (minDate, maxDate)

##########################################################################

#sensor = 'modis_terra'
sensor = 'olci'
#sensor = 'meris'
years = ['2017']

srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/' + sensor + '/l2/nc-final/'
#if sensor == 'meris':
#    srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/' + sensor + '/l2/nc/'
#else:
#    srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/' + sensor + '/l2/'

#dstBaseDir = '/data/ftp/cciwv/data/tcwv/' + sensor + '/l2/'
dstBaseDir = '/data/ftp/cciwv/data/tcwv/for_tim/l2/dataset2/' + sensor + '/'

excludes = ['_temporary', '_SUCCESS', '_processing_metadata', 'part-r-00000']
#excludes = []
excludes = ['product-sets.csv', '_temporary', '_SUCCESS']
exclStr = ''
for e in excludes:
    exclStr += ' --exclude=' + e + ' '

# keep number of rsync calls smaller (i.e. per month), as these calls ask for pwd every time
for year in years:
    for month in getMonths(year, sensor):
        srcTcwvDir = srcBaseDir + year + '/' + month
        dstTcwvDir = dstBaseDir + year
        if exists(srcTcwvDir):
            cmd = "rsync -avOP " + exclStr + srcTcwvDir + " olafd@bcserver8:" + dstTcwvDir
            #print("cmd: ", cmd)
            print(cmd)
            system(cmd)
        else:
            print(srcTcwvDir, " does not exist!")

