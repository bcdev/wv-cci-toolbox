import sys
import os
import numpy as np

from  .libs import  cowa_core

about_me = cowa_core.about_me()
about_me['scriptname'] = __file__
__author__ = 'Rene Preusker'
__version__ = about_me['version']
__version_info__ = tuple([int(num) for num in __version__.split('.')])



class cowa_land(cowa_core.cowa_core):
    '''
    '''
    def estimator(self,datain,frec=False):
        '''
        adds few specific fields to data.
        '''
        #TODO transform retrieval_error_covar with **2 
        data = dict(datain)  # datain remains unchanged. That's better!
        data['prs'] = np.log(data['press'])
        data['prior_wvc'] = np.sqrt(data['prior_iwv'])
        ret = self.generic_estimator(data)
            
        return ret

class cowa_ocean(cowa_core.cowa_core):
    '''
    '''
    def estimator(self,datain,frec=False):
        '''
        adds few specific fields to data.
        '''
        #TODO transform retrieval_error_covar with **2 
        data = dict(datain)  # datain remains unchanged. That's better!
        data['prior_wvc'] = np.sqrt(data['prior_iwv'])
        return self.generic_estimator(data)

