import sys
import os
import numpy as np

from  .libs import  cowa_core
from  .libs import  cowa_olci_io
from  .libs import  cowa_olci_io_for_cci

about_me = cowa_core.about_me()
about_me['scriptname'] = __file__
__author__ = 'Rene Preusker'
__version__ = about_me['version']
__version_info__ = tuple([int(num) for num in __version__.split('.')])



class cowa_land(cowa_core.cowa_core):
    '''
    '''
    pass

class cowa_ocean(cowa_core.cowa_core):
    '''
    '''
    pass
    #def estimator(self,datain):
        #'''
        #adds few specific fields to data.
        #'''
        ##TODO transform retrieval_error_covar with **2 
        #data['prior_wvc'] = np.sqrt(data['prior_iwv'])
        #return self.generic_estimator(data)
