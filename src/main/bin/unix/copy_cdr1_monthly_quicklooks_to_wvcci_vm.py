#!/usr/bin/env python

from os import listdir, system
from os.path import exists

srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/'
dstBaseDir = '/home/olafd/cci_wv/data/dataset3/CDR-1/QL/monthly/full_period/'

excludes = ['_temporary', '_SUCCESS', '_processing_metadata', 'product-sets.csv']
#excludes = []
exclStr = ''
for e in excludes:
    exclStr += ' --exclude=' + e + ' '

#sensor = 'meris'
sensor = 'meris-modis_terra'
#sensor = 'modis_terra'
#sensor = 'olci-modis_terra'
#sensor = 'olci'

#years = [ '2002', '2003', '2004', '2005', '2006', '2007', '2008', '2009', '2010' ]
#years = [ '2011', '2012' ]
years = [ '2012' ]
#years = [ '2012', '2013', '2014', '2015', '2016' ]
#years = [ '2016' ]
#years = [ '2017' ]

for year in years:
    srcQLDir = srcBaseDir + sensor + '/l3-monthly-final-nc-ql/005/' + year + '/' 
    #dstQLDir = dstBaseDir + sensor + '/' + year + '/' 
    dstQLDir = dstBaseDir 

    if exists(srcQLDir):
        cmd = "rsync -avOP " + exclStr + srcQLDir + " olafd@wvcci-vm:" + dstQLDir
        print 'cmd: ', cmd
        system(cmd)
    else:
        print(srcQLDir, " does not exist!")

