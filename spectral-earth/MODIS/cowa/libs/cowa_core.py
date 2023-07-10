# Copyright (c) 2018
# Author(s): 
#   Rene Preusker <rene.preusker@gmail.com>

'''
This provides the basic functionality for cowa water vapor retrieval
'''
import os
import sys
import inspect
import time
import configparser
import typing 


import numpy as np
from netCDF4 import Dataset
#from termcolor import colored
from . import soe_core as oec

#try:
    #sys.path.append('/home/rene/python/interpolation')
    #import lut2func as l2f
    #print('FAST')
#except ModuleNotFoundError :
    #from . import lut2func as l2f
    #print('SLOW')
from . import lut2func as l2f

COWA_INI_RULE = {
    'GENERAL':
        {
            'lut_file': 's',
        },
    'INTERNAL':
        {
            'absorption_band': 's',
            'state_index': 'i',
            'use_precalculated_jacobian': 'b',
            'eps': 'f',
            'maxiter': 'i',
            'debug': 'b',
            'use_absorption_correction': 'b',
        },
}


COWADIR = os.path.split(os.path.dirname(os.path.abspath(inspect.stack()[0].filename)))[0]


#LRU_FOR_PRECALC_JACO = True
LRU_FOR_PRECALC_JACO = False


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


def read_and_analyze_input(input_file: str, rule: dict):
    '''
    input file must be a valid cowa input/steering
          file in 'ini' style
    output is a dict containing filename numbers etc 
          to control the cowa object
    '''
    if not os.path.isfile(input_file):
        eee = "Problem: '%s' is not found" % ( input_file)
        raise CowaCoreError(eee)
    try:
        config = config2dict(input_file)
    except Exception as eee:
        raise CowaCoreError(eee)

    for section in rule:
        if section not in config:
            eee = "Problem: Section '%s' is missing in inputfile '%s'" % (section, input_file)
            raise InputError(eee)
        for element in rule[section]:
            if element not in config[section]:
                eee = ("Problem: Section '%s' misses element '%s' in inputfile '%s'"
                       % (section, element, input_file))
                raise InputError(eee)
            else:
                config[section][element] = convert_type(config[section][element],
                                                        rule[section][element])
    return config

#TODO finalize refactoring
def read_and_analyze_ini(*a,**b):
    return read_and_analyze_input(*a,**b)

def is_float(f: str) -> bool:
    try: _ = float(f)
    except ValueError:
        return False
    return True

def string2np(stg: str) -> np.ndarray:
    dum = [float(_) for _ in stg.split(',') if is_float(_) ]
    return np.array(dum)

def convert_type(inn: str, typ:str) -> typing.Union[float, str, int, bool, list]:
    '''
    tiny comodity function for 
    input parsing and converting
    '''
    if typ == 'f':
        out = float(inn)
    elif typ == 's':
        out = inn
    elif typ == 'i':
        out = int(inn)
    elif typ == 'b':
        if inn.lower() in ("yes", "true", "t", "1"):
            out = True
        else:
            out = False
    elif typ == 'lf':
        out = string2np(inn)
    elif typ == 'ls':
        out = [i.strip() for i in inn.split(',')]
    elif typ == 'li':
        out = [int(i.strip()) for i in inn.split(',')]
    else:
        out = None
        print('Problem, ask Rene')
    return out


def config2dict(configfile: str) -> dict:
    '''
    Small simple wrapper for Configparser. 
    makes a dict form the *.ini file.
    Eventually I re-invent a functionality 
    which Configparser allready has.  
    '''
    models = configparser.ConfigParser()
    models.optionxform = str
    models.read(configfile)
    d = dict(models._sections)
    for k in d:
        d[k] = dict(d[k])
        d[k].pop('__name__', None)
    return d

def pos_in_list(l: list,e: typing.Any) -> typing.List[int]:
    '''
    returns positions of e in l
    '''
    return [i for i,x in enumerate(l) if x == e]

def jlut2func(lut: np.ndarray ,axes: typing.Tuple[np.ndarray,...],
              ny: int,nx: int) -> typing.Callable:
    func=l2f.lut2func(lut,axes)
    def jfunc(woo):
        dum = func(woo)
        dum.shape=(ny,nx)
        return dum
    #return l2f.simple_restricted_memoizer(jfunc,1,30)
    return jfunc

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
        self.config = read_and_analyze_input(inifile,COWA_INI_RULE)
        self.si = self.config['INTERNAL']['state_index']
        self.read_lut()

        generic_forward = l2f.lut2func(self.lut,self.axes)

        #This predefinitions safe some time 
        #because the np arrays have to initialized only once
        self.inp = np.zeros(len(self.axes))
        self.xaa = np.zeros(self.si)
        self.par = np.zeros(len(self.axes)-self.si)
        self.par_unc = np.zeros(len(self.axes)-self.si)

        def internal_forward(s,p):
            self.inp[:self.si] = s
            self.inp[self.si:] = p
            return generic_forward(self.inp)

        def internal_forward_switched(p,s):
            # needed for paramter jacobian
            self.inp[:self.si] = s
            self.inp[self.si:] = p
            return generic_forward(self.inp)

        if self.config['INTERNAL']['absorption_band'].lower() == 'all':
            self.ab_idx = None
            self.wo_idx = None
            self.mes = np.zeros(len(self.wb)+len(self.ab))
            self._forward = internal_forward  
            self._forward_switched = internal_forward_switched  
        else:
            self.ab_idx = pos_in_list(self.ab,self.config['INTERNAL']['absorption_band'])[0]
            self.wo_idx = tuple(list(range(len(self.wb)))+[len(self.wb)+self.ab_idx])
            self.mes = np.zeros(len(self.wb)+1)
            self._forward = lambda s,p: internal_forward(s,p)[(self.wo_idx,)]
            self._forward_switched = lambda p,s: internal_forward_switched(p,s)[(self.wo_idx,)]

        if self.config['INTERNAL']['use_precalculated_jacobian']:
            interpolated_jacobian = jlut2func(lut=self.jlut,axes=self.jaxes,ny=self.ny_nx[0],nx=self.ny_nx[1])
            def internal_jacobian(s,p, dx=None):
                self.inp[:self.si] = s
                self.inp[self.si:] = p
                return interpolated_jacobian(self.inp)[:,:self.si]
            def internal_jacobian_param(s,p, dx=None):
                self.inp[:self.si] = s
                self.inp[self.si:] = p
                return interpolated_jacobian(self.inp)[:,self.si:]
            if self.config['INTERNAL']['absorption_band'].lower() == 'all':
                self._jforward = internal_jacobian  
                self._jforward_param = internal_jacobian_param  
            else:
                self._jforward = lambda s, p, dx=None: internal_jacobian(s, p)[self.wo_idx,:]
                self._jforward_param = lambda s, p, dx=None: internal_jacobian_param(s, p)[self.wo_idx,:]
        else:
            generic_jacobian = oec.approximate_jacobian_function(self._forward)
            self._jforward = lambda s, p, dx=None: generic_jacobian(s, p, None)
            generic_jacobian_switched = oec.approximate_jacobian_function(self._forward_switched)
            self._jforward_param = lambda s, p, dx=None: generic_jacobian_switched(p, s, None)

        # make upper and lower limits of state for the 
        # oe solver consistent with LUT
        self.ll = np.array([self.axes[i].min() for i in range(self.si)])
        self.ul = np.array([self.axes[i].max() for i in range(self.si)])
        self._solver = oec.invert_function(func=self._forward
                                 ,clip=True,ll=self.ll,ul=self.ul
                                 ,jaco=self._jforward
                                 ,eps=self.config['INTERNAL']['eps']
                                 ,mi=self.config['INTERNAL']['maxiter'])

    def read_lut(self):
        with Dataset(self.config['GENERAL']['lut_file'],'r') as ncds:
            #get the full lut
            self.lut = np.array(ncds.variables['lut'][:])
            self.jlut = np.array(ncds.variables['jlut'][:])
            self.axes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['lut'].dimensions[:-1]])
            self.axes_names = (ncds.variables['lut'].dimensions[:-1])
            self.jaxes = tuple([np.array(ncds.variables[a][:]) for a in ncds.variables['jlut'].dimensions[:-1]])
            self.ny_nx = np.array(ncds.variables['jaco'][:])
            self.wb = ncds.getncattr('win_bnd').split(',')
            self.ab = ncds.getncattr('abs_bnd').split(',')
            self.cor = {bnd:ncds.groups['cor'].variables[bnd][:] for bnd in self.ab}
            self.cwvl={k:float(ncds.groups['cha'].groups[k].cwvl) for k in self.wb+self.ab}

    def rectify_and_correct_apparent_transmission(self, ch, data, samf):
        '''
        ch : channel identifier of the absoprtion band 
        '''
        if self.config['INTERNAL']['use_absorption_correction'] is True:
            a, b = self.cor[ch]
        else:
            a, b = 0., 1.
        if 'abs_cor' in data:
            a, b = data['abs_cor'][ch]
        if len(self.wb) == 1:
            ref = data['rtoa'][self.wb[0]]
        else:
            dwvl = (self.cwvl[self.wb[1]]-self.cwvl[self.wb[0]])
            drho = (data['rtoa'][self.wb[1]]-data['rtoa'][self.wb[0]])
            if abs(dwvl) >= oec.HH:
                drho_dwvl = drho/dwvl
                ref = data['rtoa'][self.wb[0]] + drho_dwvl*(self.cwvl[ch]-self.cwvl[self.wb[0]]) 
            else:
                ref = data['rtoa'][self.wb[0]]
        #print(a,b)
        return -a -np.log( data['rtoa'][ch] / ref )/samf*b

    def prepare_data(self,data):
        '''
        put the data fields into prepared 
        parameter, prior and mess np.arrays
        of self
        '''
        #TODO: get the jacobians to transform SE_data into SE_mess ...
        for ipn,pn in enumerate(self.axes_names[self.si:]):
            self.par[ipn] = data[pn]
            if pn+'_unc' in data:
                self.par_unc[ipn] = data[pn+'_unc']
            else:
                self.par_unc[ipn] = 0.
        for isn,sn in enumerate(self.axes_names[:self.si]):
            #self.xaa[isn] = data['prior_%s'%sn]
            # for numerical stability, should be clipped (infact 
            # only important for windspeed)
            self.xaa[isn] = np.clip(data['prior_%s'%sn],self.ll[isn],self.ul[isn])
           
        for ich,ch in enumerate(self.wb):
            self.mes[ich]=data['rtoa'][ch]

        amf = 1./np.cos(data['suz']*np.pi/180.)+1./np.cos(data['vie']*np.pi/180.)
        samf = np.sqrt(amf)
        if self.config['INTERNAL']['absorption_band'].lower() == 'all':
            for ich,ch in enumerate(self.ab):
                self.mes[len(self.wb)+ich] = (
                    self.rectify_and_correct_apparent_transmission(ch, data, samf))
        else:
            self.mes[len(self.wb)] = (
                self.rectify_and_correct_apparent_transmission(
                self.config['INTERNAL']['absorption_band'], data, samf))

    def generic_estimator(self,data):
        '''
        '''
        self.prepare_data(data)
        result = self._solver(self.mes, self.par,xa=self.xaa, 
                              se=data['se'],
                              sa=data['sa'])
        #print(result)
        result.state.clip(self.ll,self.ul,out=result.state)
        debug = {}
        if self.config['INTERNAL']['debug']:
            debug['simulated_meas_from_apriori'] = self._forward(self.xaa,self.par)
            debug['simulated_meas_from_state'] = self._forward(result.state, self.par)
            debug['transformed meas'] = self.mes
            debug['transformed prior'] = self.xaa
 
        #TODO: transform SR_(sqrt(iwv)) into SR_(iwv) ...
        return {'iwv':result.state[0]**2 , 'result':result, 'debug':debug}

