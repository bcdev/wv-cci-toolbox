#!/usr/bin/env python

from os import listdir, system
from os.path import exists

srcBaseDir = '/mnt/hdfs/calvalus/projects/wvcci/tcwv/'
dstBaseDir = '/home/olafd/cci_wv/data/dataset3/CDR-2/QL/daily/'

excludes = ['_temporary', '_SUCCESS', '_processing_metadata', 'product-sets.csv']
#excludes = []
exclStr = ''
for e in excludes:
    exclStr += ' --exclude=' + e + ' '

#sensor = 'meris-modis_terra-cmsaf_hoaps'
sensor = 'meris-modis_terra'
year = '2011'

srcQLDir = srcBaseDir + sensor + '/l3-daily-final-nc-ql/005/' + year + '/' 
dstQLDir = dstBaseDir + sensor + '/' + year + '/' 

if exists(srcQLDir):
    cmd = "rsync -avOP " + exclStr + srcQLDir + " olafd@wvcci-vm:" + dstQLDir
    print 'cmd: ', cmd
    system(cmd)
else:
    print(srcQLDir, " does not exist!")

