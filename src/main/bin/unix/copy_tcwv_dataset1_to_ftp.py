#!/usr/bin/env python

from os import listdir, system
from os.path import exists

import datetime
import calendar

from datetime import date
from calendar import monthrange, isleap

#############################################################
# Script to rsync 'Dataset1' TCWV L3 data with BC WV_cci ftp (bcserver8)
# O. Danne, August 2019
############################################################# 

##########################################################################

#years = ['2010', '2011','2012']
years = ['2011','2012']

srcBaseDir = '/home/olaf/wvcci/dataset1/zip/'
dstBaseDir = '/data/ftp/cciwv/data/tcwv/dataset1/'

#excludes = ['_temporary', '_SUCCESS', '_processing_metadata', 'part-r-00000']
excludes = []
exclStr = ''
for e in excludes:
    exclStr += ' --exclude=' + e + ' '

for year in years:
    srcTcwvDir = srcBaseDir + year
    dstTcwvDir = dstBaseDir
    if exists(srcTcwvDir):
        cmd = "rsync -avOP " + exclStr + srcTcwvDir + " olafd@bcserver8:" + dstTcwvDir
        print(cmd)
        system(cmd)
    else:
        print(srcTcwvDir, " does not exist!")

