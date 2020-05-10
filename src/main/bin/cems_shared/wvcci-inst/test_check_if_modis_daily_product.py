import os, sys

from pyhdf.SD import SD, SDC


def check_if_day(hdf_file):

    hdf_filename = sys.argv[1]
    hdf_file = SD(hdf_filename, SDC.READ)

    core_metadata0_string = hdf_file.attributes()['CoreMetadata.0']
    daynight_start_index = core_metadata0_string.find('DAYNIGHT')
    #print('test: |' + core_metadata0_string[daynight_start_index:daynight_start_index+120] + '|')
    daynight_info_block = core_metadata0_string[daynight_start_index:daynight_start_index+120]

    is_day = (daynight_info_block.find('\"Day\"') != -1)
    #print(is_day)
    return is_day


if __name__ == '__main__':
    infilename = sys.argv[1]
    #print (check_if_day(infilename))
    if check_if_day(infilename):
        exit(0)
    else:
        exit(1)

    #pass

