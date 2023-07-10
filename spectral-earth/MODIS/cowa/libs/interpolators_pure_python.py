# Copyright (c) 2018
# Author(s): 
#   Rene Preusker <rene.preusker@gmail.com>


'''
This is a pure python (ok, the data must be numpy array...) implementation 
of an n-dimensional linear interpolator for:

a)  R^N => R^1 :  generate_itp 
b)  R^N => R^1, ignoring NaNs: generate_nan_itp 
c)  R^N => R^M: generate_itp_pn 

(currently N <= 10)

As you may expect, its not the fastet (numba and
fortran versions are available) but a straight 
forward implementation, which should be fast enough
if coded in a resp. language

Actually generate_*** curries the corresponding interpolator 
functions (which of_course need the LUT and the place 
where to interpolate) into a LUT specific function 
with only one argument: the place
'''

import numpy as np
import math

def en_cl(xx,ll):
    '''
    Returns lower/upper index
    and lower/upper weight 
    for a float index xx. 
    Further it clips xx between 
    0 and ll
    '''
    low=max(int(math.floor(xx)) ,0)
    low=min(low   ,ll-1)
    upp=min(low+1, ll-1)
    wlo=1.-(xx-float(low))
    wup=1.-wlo
    return (low,wlo),(upp,wup)

def interpol_1(wo,lut):
    '''
    Test:
    >>> lut = np.array(np.arange(12,dtype=np.float).reshape(12)) 
    >>> wo = np.array([1.6])
    >>> interpol_1(wo,lut)
    1.6
    '''
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0] * wgt
    return out

def interpol_nan_1(wo,lut):
    '''
    Test:
    >>> lut = np.array(np.arange(12,dtype=np.float).reshape(12))
    >>> wo = np.array([1.6])
    >>> interpol_nan_1(wo,lut)
    1.6
    >>> lut[1:3]=np.nan
    >>> interpol_nan_1(wo,lut)
    nan
    '''
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        if np.isnan(lut[i0]):
            pass
        else:
            wgt = w0
            out += lut[i0] * wgt
            swg += wgt
            cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_1pn(wo,lut):
    out = lut[0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0,:] * wgt
    return out

def interpol_2(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1] * wgt
    return out

def interpol_nan_2(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            if np.isnan(lut[i0,i1]):
                pass
            else:
                wgt = w0*w1
                out += lut[i0,i1] * wgt
                swg += wgt
                cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_2pn(wo,lut):
    out = lut[0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1,:] * wgt
    return out

def interpol_3(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2] * wgt
    return out

def interpol_nan_3(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                if np.isnan(lut[i0,i1,i2]):
                    pass
                else:
                    wgt = w0*w1*w2
                    out += lut[i0,i1,i2] * wgt
                    swg += wgt
                    cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_3pn(wo,lut):
    out = lut[0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2,:] * wgt
    return out

def interpol_4(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3] * wgt
    return out

def interpol_nan_4(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    if np.isnan(lut[i0,i1,i2,i3]):
                        pass
                    else:
                        wgt = w0*w1*w2*w3
                        out += lut[i0,i1,i2,i3] * wgt
                        swg += wgt
                        cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_4pn(wo,lut):
    out = lut[0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3,:] * wgt
    return out

def interpol_5(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        wgt = w0*w1*w2*w3*w4
                        out += lut[i0,i1,i2,i3,i4] * wgt
    return out

def interpol_nan_5(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        if np.isnan(lut[i0,i1,i2,i3,i4]):
                            pass
                        else:
                            wgt = w0*w1*w2*w3*w4
                            out += lut[i0,i1,i2,i3,i4] * wgt
                            swg += wgt
                            cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_5pn(wo,lut):
    out = lut[0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        wgt = w0*w1*w2*w3*w4
                        out += lut[i0,i1,i2,i3,i4,:] * wgt
    return out

def interpol_6(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            wgt = w0*w1*w2*w3*w4*w5
                            out += lut[i0,i1,i2,i3,i4,i5] * wgt
    return out

def interpol_nan_6(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            if np.isnan(lut[i0,i1,i2,i3,i4,i5]):
                                pass
                            else:
                                wgt = w0*w1*w2*w3*w4*w5
                                out += lut[i0,i1,i2,i3,i4,i5] * wgt
                                swg += wgt
                                cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_6pn(wo,lut):
    out = lut[0,0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            wgt = w0*w1*w2*w3*w4*w5
                            out += lut[i0,i1,i2,i3,i4,i5,:] * wgt
    return out

def interpol_7(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                wgt = w0*w1*w2*w3*w4*w5*w6
                                out += lut[i0,i1,i2,i3,i4,i5,i6] * wgt
    return out

def interpol_nan_7(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                if np.isnan(lut[i0,i1,i2,i3,i4,i5,i6]):
                                    pass
                                else:
                                    wgt = w0*w1*w2*w3*w4*w5*w6
                                    out += lut[i0,i1,i2,i3,i4,i5,i6] * wgt
                                    swg += wgt
                                    cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_7pn(wo,lut):
    out = lut[0,0,0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                wgt = w0*w1*w2*w3*w4*w5*w6
                                out += lut[i0,i1,i2,i3,i4,i5,i6,:] * wgt
    return out

def interpol_8(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    wgt = w0*w1*w2*w3*w4*w5*w6*w7
                                    out += lut[i0,i1,i2,i3,i4,i5,i6,i7] * wgt
    return out

def interpol_nan_8(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    if np.isnan(lut[i0,i1,i2,i3,i4,i5,i6,i7]):
                                        pass
                                    else:
                                        wgt = w0*w1*w2*w3*w4*w5*w6*w7
                                        out += lut[i0,i1,i2,i3,i4,i5,i6,i7] * wgt
                                        swg += wgt
                                        cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_8pn(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    wgt = w0*w1*w2*w3*w4*w5*w6*w7
                                    out += lut[i0,i1,i2,i3,i4,i5,i6,i7,:] * wgt
    return out

def interpol_9(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8
                                        out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8] * wgt
    return out

def interpol_nan_9(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        if np.isnan(lut[i0,i1,i2,i3,i4,i5,i6,i7,i8]):
                                            pass
                                        else:
                                            wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8
                                            out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8] * wgt
                                            swg += wgt
                                            cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_9pn(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8
                                        out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8,:] * wgt
    return out

def interpol_10(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        for i9,w9 in en_cl(wo[9],sha[9]):
                                            wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8*w9
                                            out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8,i9] * wgt
    return out

def interpol_nan_10(wo,lut):
    out = 0.
    sha = lut.shape
    swg = 0.
    cnt = 0
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        for i9,w9 in en_cl(wo[9],sha[9]):
                                            if np.isnan(lut[i0,i1,i2,i3,i4,i5,i6,i7,i8,i9]):
                                                pass
                                            else:
                                                wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8*w9
                                                out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8,i9] * wgt
                                                swg += wgt
                                                cnt += 1
    if cnt>0: return out/swg
    else: return np.nan

def interpol_10pn(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        for i5,w5 in en_cl(wo[5],sha[5]):
                            for i6,w6 in en_cl(wo[6],sha[6]):
                                for i7,w7 in en_cl(wo[7],sha[7]):
                                    for i8,w8 in en_cl(wo[8],sha[8]):
                                        for i9,w9 in en_cl(wo[9],sha[9]):
                                            wgt = w0*w1*w2*w3*w4*w5*w6*w7*w8*w9
                                            out += lut[i0,i1,i2,i3,i4,i5,i6,i7,i8,i9,:] * wgt
    return out

def generate_itp(lut):
    ndim=lut.ndim
    if ndim == 1: interpolator = interpol_1
    elif ndim == 2: interpolator = interpol_2
    elif ndim == 3: interpolator = interpol_3
    elif ndim == 4: interpolator = interpol_4
    elif ndim == 5: interpolator = interpol_5
    elif ndim == 6: interpolator = interpol_6
    elif ndim == 7: interpolator = interpol_7
    elif ndim == 8: interpolator = interpol_8
    elif ndim == 9: interpolator = interpol_9
    elif ndim == 10: interpolator = interpol_10
    else:
        print("Not implemented")
        return
    def function(wo): return interpolator(wo,lut)
    return function

def generate_nan_itp(lut):
    ndim=lut.ndim
    if ndim == 1: interpolator = interpol_nan_1
    elif ndim == 2: interpolator = interpol_nan_2
    elif ndim == 3: interpolator = interpol_nan_3
    elif ndim == 4: interpolator = interpol_nan_4
    elif ndim == 5: interpolator = interpol_nan_5
    elif ndim == 6: interpolator = interpol_nan_6
    elif ndim == 7: interpolator = interpol_nan_7
    elif ndim == 8: interpolator = interpol_nan_8
    elif ndim == 9: interpolator = interpol_nan_9
    elif ndim == 10: interpolator = interpol_nan_10
    else:
        print("Not implemented")
        return
    def function(wo): return interpolator(wo,lut)
    return function

def generate_itp_pn(lut):
    ndim=lut.ndim-1
    if ndim == 1: interpolator = interpol_1pn
    elif ndim == 2: interpolator = interpol_2pn
    elif ndim == 3: interpolator = interpol_3pn
    elif ndim == 4: interpolator = interpol_4pn
    elif ndim == 5: interpolator = interpol_5pn
    elif ndim == 6: interpolator = interpol_6pn
    elif ndim == 7: interpolator = interpol_7pn
    elif ndim == 8: interpolator = interpol_8pn
    elif ndim == 9: interpolator = interpol_9pn
    elif ndim == 10: interpolator = interpol_10pn
    else:
        print("Not implemented")
        return
    def function(wo): return interpolator(wo,lut)
    return function

def linint2index(x,xtab):
    '''
    returns a float index of the place 
    of x within xtab. 
    xtab must be ascendig (its not tested!!)
    >>> linint2index(28.5, [0,1,10,12,45])
    3.6
    '''
    if x <= xtab[0]:
        return 0.
    elif x >=  xtab[-1]:
        return xtab.size -1. # implicit float
    else:
        xf = xtab[0]
        ii = 0.
        for xt in xtab[1:]:
            if x < xt: 
                break
            xf = xt
            ii += 1.
        return ii +(x-xf)/(xt-xf)

if __name__ == "__main__":
    import doctest
    for f in (linint2index,interpol_1,interpol_nan_1):
        doctest.run_docstring_examples(f,globals(),name=f.__name__,summary=True)

