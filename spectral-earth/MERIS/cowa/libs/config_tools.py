import os
import configparser
import typing
import numpy as np

class ConfigError(Exception):
    def __init__(self, message=''):
        self.message = message
class InputError(ConfigError):
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
        raise ConfigError(eee)
    try:
        config = config2dict(input_file)
    except Exception as eee:
        raise ConfigError(eee)

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
