import sys
import netCDF4

from netCDF4 import Dataset

nc_infile = sys.argv[1]
print ('nc_infile: ' + nc_infile)

try:
    ds = Dataset(nc_file)
except:
    print ('Cannot read nc_infile')

