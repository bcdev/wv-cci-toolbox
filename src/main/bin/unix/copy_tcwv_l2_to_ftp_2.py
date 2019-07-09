#!/usr/bin/env python

from os import listdir, system
from os.path import exists

import datetime
import calendar

from datetime import date
from calendar import monthrange, isleap


##########################################################################

def getMonths(year, sensor):
    if year == '2002':
        months  = [ '04', '05', '06', '07', '08', '09', '10', '11', '12' ]
    elif year == '2012':
        months  = [ '01', '02', '03', '04' ]
    else:
        months  = [ '01' ]
        #months  = [ '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12' ]

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

sensor = 'modis_terra'
#sensor = 'meris'
years = ['2011']

if sensor == 'meris':
    srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/' + sensor + '/l2/nc/'
else:
    srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/' + sensor + '/l2/'

dstBaseDir = '/data/ftp/cciwv/data/tcwv/' + sensor + '/l2/'

excludes = ['_temporary', '_SUCCESS', '_processing_metadata', 'part-r-00000']
#excludes = []
exclStr = ''
for e in excludes:
    exclStr += ' --exclude=' + e + ' '

#for year in years:
#    for month in getMonths(year, sensor):
#        for iday in range(1, getNumDaysInMonth(year, month)+1):
#            day = str(iday).zfill(2)
#            srcTcwvDir = srcBaseDir + year + '/' + month + '/' + day + '/' 
#            dstTcwvDir = dstBaseDir + year + '/' + month + '/' + day + '/' 
#            if exists(srcTcwvDir):
#                cmd = "rsync -avOP " + exclStr + srcTcwvDir + " olafd@bcserver8:" + dstTcwvDir
#                #print("cmd: ", cmd)
#                print(cmd)
#                system(cmd)
#            else:
#                print(srcTcwvDir, " does not exist!")

# keep number of rsync call small, as they ask for pwd every time
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

