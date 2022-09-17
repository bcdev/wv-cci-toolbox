# -*- coding: utf-8 -*-
# ! /usr/bin/env python
from __future__ import print_function

# Preprocessing steps for merge of sensors:
#   - conversion from nc4 to nc3 (only step so far)
#
__author__ = 'olafd'

import os
import sys

from netCDF4 import Dataset

#############################################################################

def is_py3():
    """
    Checks if current Python major version is 3.
    :return: boolean
    """

    return sys.version_info.major == 3

def get_iteritems(iterable_obj):
    """
    Wraps Python2/3 difference for iterable objects
    :param iterable_obj:
    :return: corresponding items
    """

    if is_py3():
        return iterable_obj.items()
    else:
        return iterable_obj.iteritems()

def nc4to3(src, dst):
    """
    Copies dimensions, variables and attributes from nc4 source to target dataset
    :param src: source dataset
    :param dst: taret dataset
    """

    # Copy dimensions
    for dname, the_dim in get_iteritems(src.dimensions):
        dst.createDimension(dname, len(the_dim) if not the_dim.isunlimited() else None)

    # Copy variables
    for src_var_name, src_var in get_iteritems(src.variables):
        dst_var = dst.createVariable(src_var_name, src_var.datatype, src_var.dimensions)

        # Copy variable attributes
        dst_var.setncatts({k: src_var.getncattr(k) for k in src_var.ncattrs()})

        # Copy data
        dst_var[:] = src_var[:]

    # copy global attributes all at once via dictionary
    dst.setncatts(src.__dict__)

def run(args):
    """
    Does the conversion from nc4 input to nc3 output product.
    :param args: program arguments
    """

    # Evaluate program arguments...
    nc4_infile = args[1]
    nc3_outfile = args[2]

    # Source dataset...
    src = Dataset(nc4_infile)

    # Target dataset
    dst = Dataset(nc3_outfile, "w", format="NETCDF3_CLASSIC")

    # get an 'nc3 copy' of the source:
    nc4to3(src, dst)

    # close the output file
    dst.close()

    print("FINISHED tcwv-merge-sensors-process.py.", file=sys.stderr)


if __name__ == "__main__":

    print("STARTING tcwv-merge-sensors-process.py...", file=sys.stderr)
    print('Working dir: ', os.getcwd())

    if len(sys.argv) != 3:
        print(
            'Usage:  python tcwv-merge-sensors-process.py <nc4_infile> <nc3_outfile>')
        sys.exit(-1)

    run(sys.argv)
