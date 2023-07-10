import os
import sys
import inspect
import time
import typing

import numpy as np
from netCDF4 import Dataset
import xarray as xr
# todo use my own simple colored
#from termcolor import colored
from . import lut2oe as loe
from .config_tools import read_and_analyze_ini 


COWA_INI_RULE = {
    'GENERAL':
        {
            'lut_file': 's',
        },
    'INTERNAL':
        {
            'absorption_band': 's',
            'state_index': 'i',
            'maxiter': 'i',
            'debug': 'b',
            'use_absorption_correction': 'b',
            'parallel': 'b',
            'batch': 'b',
        },
}


COWADIR = os.path.split(os.path.dirname(os.path.abspath(inspect.stack()[0].filename)))[0]

def about_me() -> dict:
    dct = {}
    dct['scriptname'] = __file__
    dct['cowadir'] = COWADIR
    dct['libdir'] = os.path.join(COWADIR,'libs')
    versfile = os.path.join(dct['libdir'],'..','..','current_version.txt')
    dct['version'] = open(versfile).readlines()[0][:-1]
    dct['cwd'] = os.path.abspath(os.getcwd())
    dct['date']=time.ctime()
    return dct

class CowaCoreError(Exception):
    def __init__(self, message=''):
        self.message = message
class InputError(CowaCoreError):
    pass

def snr_to_pseudo_absoprtion_measurement_variance(snr=500,interpolation_error=0.01,amf=2 ):
    '''
    Within the OE, the pseudo measurement in the absorption band (pab) is:

    pab = -a - ( log(ab) -log(wb) ) * b / samf

    Here: a,b:  irrelevant calibration factors (almost 0 and 1)
          ab:   absorption band
          wb:   reference band, as the result of the extra/interpolation of the window bands the SNR
          samf: square root of amf
    
    -->
    
    uncert = 1/samf**2 * [ (1/ab)**2 * sgma(ab)**2 + (1/wb)**2 * sgma(wb)**2 ]
    
    using: sgma(ab) = ab/SNR
           sgma(wb) = wb/SNR  + wb * interpolation_error 
                    = wb * (1/SNR + interpolation_error)
    --> 
    
    uncert = 1/amf * [(1/SNR)**2 + (1/SNR + interpolation_error)**2] 
    
    I assume:
        * the SNR is approximatly the same for both bands
        * no covariance between the window-band-uncertainty 
          and the  pseudo absorption band measurement (this is 
          obviously not true, but a conservative estimate)
        * amf is always larger then 2 
    '''
    
    uncert = ((1./snr**2)  + (1./snr**2 +interpolation_error )) / amf   # rough estimate
    
    return uncert



class cowa_core:
    '''
    '''
    def __init__(self,inifile):
        '''
        TODO: This init is a bit long, 
        '''
        self.about_me = about_me()
        self.config = read_and_analyze_ini(inifile,COWA_INI_RULE)
        self.si = self.config['INTERNAL']['state_index']
        
        if self.config['INTERNAL']['absorption_band'] == 'all':
            with Dataset(self.config['GENERAL']['lut_file'],'r') as ncds:
                self.wb = [int(i) for i in ncds.getncattr('win_bnd').split(',')]
                self.ab = [int(i) for i in ncds.getncattr('abs_bnd').split(',')]
                self.cor = {bnd:ncds.groups['cor'].variables['%i'%bnd][:] for bnd in self.ab}
                self.cwvl={bnd:float(ncds.groups['cha'].groups['%i'%bnd].cwvl) for bnd in self.wb+self.ab}
            self.ds = xr.load_dataset(self.config['GENERAL']['lut_file'])
        else:
            with Dataset(self.config['GENERAL']['lut_file'],'r') as ncds:
                self.wb = [int(i) for i in ncds.getncattr('win_bnd').split(',')]
                self.ab = [int(self.config['INTERNAL']['absorption_band'])]
                self._ab = [int(i) for i in ncds.getncattr('abs_bnd').split(',')]
                self.cor = {bnd:ncds.groups['cor'].variables['%i'%bnd][:] for bnd in self.ab}
                self.cwvl={bnd:float(ncds.groups['cha'].groups['%i'%bnd].cwvl) for bnd in self.wb+self.ab}
            ab_idx = self._ab.index(self.ab[0])
            wo_idx = list(range(len(self.wb)))+[len(self.wb)+ab_idx]
            #print(wo_idx)
            self.ds = xr.load_dataset(self.config['GENERAL']['lut_file']).isel(bands=wo_idx)
            
        self.parameter_names = self.ds['lut'].dims[self.si:-1]
        self.state_names = self.ds['lut'].dims[:self.si]
        self.n_meas = len(self.ds['lut'].bands)
        self.n_parameter = len(self.ds['lut'].dims)-1-self.si
        self.n_state = self.si

        ifunc,func = loe.xarray2oe(self.ds,'lut',self.si
                                ,batch=self.config['INTERNAL']['batch']
                                ,parallel=self.config['INTERNAL']['parallel']
                                )
        self.inverse = ifunc
        self.forward = func
        
    def stpamv(self,snr,itp_error, amf):
        return snr_to_pseudo_absoprtion_measurement_variance(snr,itp_error, amf) 
    
    def rectify_and_correct_apparent_transmission(self,ch, meas, samf):
        '''
        ch : channel identifier of the absoprtion band 
        This is currently based on nominal cwl, real should be 
        no problem
        '''
        ab_idx = len(self.wb)+ self.ab.index(ch)      
        if self.config['INTERNAL']['use_absorption_correction'] is True:
            a, b = self.cor[ch]
        else:
            a, b = 0., 1.
        if len(self.wb) == 1:
            ref = meas[0]+0
        else:
            dwvl = (self.cwvl[self.wb[1]]-self.cwvl[self.wb[0]])
            drho = (meas[1]-meas[0])
            if abs(dwvl) >= 1.e-2:
                drho_dwvl = drho/dwvl
                ref = meas[0] + drho_dwvl*(self.cwvl[ch]-self.cwvl[self.wb[0]]) 
            else:
                ref = meas[0]+0
        return -a -np.log( meas[ab_idx] / ref )/samf*b

    def prepare_processing(self,data,config,target='land'):
        
        if target == 'ocean':
            what = 'dfo'
            snr = config['PROCESSING']['ocean_snr']
            ite = config['PROCESSING']['ocean_interpolation_error']
            sa = np.diag([e for e in config['PROCESSING']['ocean_apriori_error_covariance_diagonals']])
        else:
            what = 'dfl'
            snr = config['PROCESSING']['land_snr']
            ite = config['PROCESSING']['land_interpolation_error']
            sa = np.diag([e for e in config['PROCESSING']['land_apriori_error_covariance_diagonals']])
            
        meas = np.array([data['rad'][int(k)][data[what]].flat[:] for k in self.wb+self.ab])
        amf = data['amf'][data[what]].flat[:]
        samf = np.sqrt(amf)
        nnn = len(amf)    
        
        for ich,ch in enumerate(self.ab):
            meas[len(self.wb)+ich] = self.rectify_and_correct_apparent_transmission(ch, meas, samf)
            
        bla = {  'suz':         data['geo']['SZA'][data[what]].flat[:]
                ,'vie':         data['geo']['OZA'][data[what]].flat[:]
                ,'azi': 180. -  data['geo']['ADA'][data[what]].flat[:]
                ,'prs': np.log( data['prs'][data[what]].flat[:])
                ,'tmp':         data['tem'][data[what]].flat[:]
                ,'wvc': np.sqrt(data['tcw'][data[what]].flat[:])
                ,'wsp':         data['wsp'][data[what]].flat[:]
                ,'aot':         data['aot'][data[what]].flat[:]
            }
        if target == 'land':
            bla['al0'] = meas[0]*np.pi/np.cos(bla['suz']*np.pi/180.)
            bla['al1'] = meas[1]*np.pi/np.cos(bla['suz']*np.pi/180.)
        
        para = np.array([bla[k] for k in self.parameter_names])
        prio = np.array([bla[k] for k in self.state_names])
        
        
        se = np.zeros((nnn,self.n_meas,self.n_meas))
        for ich,ch in enumerate(self.ab):
            se[:,len(self.wb)+ich,len(self.wb)+ich] = self.stpamv(snr,ite[ich],amf)
        for ich,ch in enumerate(self.wb):
            se[:,ich,ich] = 1./snr**2

        sa = np.tile(sa,(nnn,1,1))
        aca = np.ascontiguousarray    
        return {'yy':aca(meas.T),'pa':aca(para.T),'xa':aca(prio.T),'se':se,'sa':sa}

    


