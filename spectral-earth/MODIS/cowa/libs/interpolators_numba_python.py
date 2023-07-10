import numpy as np
import math
import numba
@numba.jit( nopython=True, cache=True )
def en_cl(xx,ll):
    low=max(int(math.floor(xx)) ,0)
    low=min(low   ,ll-1)
    upp=min(low+1, ll-1)
    wlo=1.-(xx-float(low))
    wup=1.-wlo
    return (low,wlo),(upp,wup)

def linint2index(x,xtab):
    return np.interp(x,xtab,np.arange(len(xtab)))


@numba.jit("f8(f8[:],f8[:])", nopython=True, cache=True )
def interpol_1_f8(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0] * wgt
    return out

@numba.jit("f8(f8[:],f8[:])", nopython=True, cache=True )
def interpol_nan_1_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:])", nopython=True, cache=True )
def interpol_1pn_f8(wo,lut):
    out = lut[0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0,:] * wgt
    return out

@numba.jit("f4(f4[:],f4[:])", nopython=True, cache=True )
def interpol_1_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0] * wgt
    return out

@numba.jit("f4(f4[:],f4[:])", nopython=True, cache=True )
def interpol_nan_1_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:])", nopython=True, cache=True )
def interpol_1pn_f4(wo,lut):
    out = lut[0,:]*np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        wgt = w0
        out += lut[i0,:] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:])", nopython=True, cache=True )
def interpol_2_f8(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:])", nopython=True, cache=True )
def interpol_nan_2_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:])", nopython=True, cache=True )
def interpol_2pn_f8(wo,lut):
    out = lut[0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1,:] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:])", nopython=True, cache=True )
def interpol_2_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:])", nopython=True, cache=True )
def interpol_nan_2_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:])", nopython=True, cache=True )
def interpol_2pn_f4(wo,lut):
    out = lut[0,0,:]*np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            wgt = w0*w1
            out += lut[i0,i1,:] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:])", nopython=True, cache=True )
def interpol_3_f8(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:])", nopython=True, cache=True )
def interpol_nan_3_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:])", nopython=True, cache=True )
def interpol_3pn_f8(wo,lut):
    out = lut[0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2,:] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:,:])", nopython=True, cache=True )
def interpol_3_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:,:])", nopython=True, cache=True )
def interpol_nan_3_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:])", nopython=True, cache=True )
def interpol_3pn_f4(wo,lut):
    out = lut[0,0,0,:]*np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                wgt = w0*w1*w2
                out += lut[i0,i1,i2,:] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:,:])", nopython=True, cache=True )
def interpol_4_f8(wo,lut):
    out = 0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:,:])", nopython=True, cache=True )
def interpol_nan_4_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:])", nopython=True, cache=True )
def interpol_4pn_f8(wo,lut):
    out = lut[0,0,0,0,:]*0.
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3,:] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:,:,:])", nopython=True, cache=True )
def interpol_4_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:,:,:])", nopython=True, cache=True )
def interpol_nan_4_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:])", nopython=True, cache=True )
def interpol_4pn_f4(wo,lut):
    out = lut[0,0,0,0,:]*np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    wgt = w0*w1*w2*w3
                    out += lut[i0,i1,i2,i3,:] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:,:,:])", nopython=True, cache=True )
def interpol_5_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_5_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_5pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:])", nopython=True, cache=True )
def interpol_5_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        wgt = w0*w1*w2*w3*w4
                        out += lut[i0,i1,i2,i3,i4] * wgt
    return out

@numba.jit("f4(f4[:],f4[:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_5_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_5pn_f4(wo,lut):
    out = lut[0,0,0,0,0,:]*np.float32(0.)
    sha = lut.shape
    for i0,w0 in en_cl(wo[0],sha[0]):
        for i1,w1 in en_cl(wo[1],sha[1]):
            for i2,w2 in en_cl(wo[2],sha[2]):
                for i3,w3 in en_cl(wo[3],sha[3]):
                    for i4,w4 in en_cl(wo[4],sha[4]):
                        wgt = w0*w1*w2*w3*w4
                        out += lut[i0,i1,i2,i3,i4,:] * wgt
    return out

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_6_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_6_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_6pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_6_f4(wo,lut):
    out = np.float32(0.)
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_6_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_6pn_f4(wo,lut):
    out = lut[0,0,0,0,0,0,:]*np.float32(0.)
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_7_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_7_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_7pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_7_f4(wo,lut):
    out = np.float32(0.)
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_7_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_7pn_f4(wo,lut):
    out = lut[0,0,0,0,0,0,0,:]*np.float32(0.)
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_8_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_8_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_8pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_8_f4(wo,lut):
    out = np.float32(0.)
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_8_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_8pn_f4(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,:]*np.float32(0.)
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_9_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_9_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_9pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_9_f4(wo,lut):
    out = np.float32(0.)
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_9_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_9pn_f4(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,0,:]*np.float32(0.)
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_10_f8(wo,lut):
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

@numba.jit("f8(f8[:],f8[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_10_f8(wo,lut):
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

@numba.jit("f8[:](f8[:],f8[:,:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_10pn_f8(wo,lut):
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_10_f4(wo,lut):
    out = np.float32(0.)
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

@numba.jit("f4(f4[:],f4[:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_nan_10_f4(wo,lut):
    out = np.float32(0.)
    sha = lut.shape
    swg = np.float32(0.)
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

@numba.jit("f4[:](f4[:],f4[:,:,:,:,:,:,:,:,:,:,:])", nopython=True, cache=True )
def interpol_10pn_f4(wo,lut):
    out = lut[0,0,0,0,0,0,0,0,0,0,:]*np.float32(0.)
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
    mytype=lut.dtype
    if mytype==np.float64:
        if ndim == 1: interpolator = interpol_1_f8
        elif ndim == 2: interpolator = interpol_2_f8
        elif ndim == 3: interpolator = interpol_3_f8
        elif ndim == 4: interpolator = interpol_4_f8
        elif ndim == 5: interpolator = interpol_5_f8
        elif ndim == 6: interpolator = interpol_6_f8
        elif ndim == 7: interpolator = interpol_7_f8
        elif ndim == 8: interpolator = interpol_8_f8
        elif ndim == 9: interpolator = interpol_9_f8
        elif ndim == 10: interpolator = interpol_10_f8
        else:
            print("Not implemented")
            return
    elif mytype==np.float32:
        if ndim == 1: interpolator = interpol_1_f4
        elif ndim == 2: interpolator = interpol_2_f4
        elif ndim == 3: interpolator = interpol_3_f4
        elif ndim == 4: interpolator = interpol_4_f4
        elif ndim == 5: interpolator = interpol_5_f4
        elif ndim == 6: interpolator = interpol_6_f4
        elif ndim == 7: interpolator = interpol_7_f4
        elif ndim == 8: interpolator = interpol_8_f4
        elif ndim == 9: interpolator = interpol_9_f4
        elif ndim == 10: interpolator = interpol_10_f4
        else:
            print("Not implemented")
            return
    else:
        print("No float")
        return
    def function(wo): return interpolator(wo,lut)
    return function

def generate_nan_itp(lut):
    ndim=lut.ndim
    mytype=lut.dtype
    if mytype==np.float64:
        if ndim == 1: interpolator = interpol_nan_1_f8
        elif ndim == 2: interpolator = interpol_nan_2_f8
        elif ndim == 3: interpolator = interpol_nan_3_f8
        elif ndim == 4: interpolator = interpol_nan_4_f8
        elif ndim == 5: interpolator = interpol_nan_5_f8
        elif ndim == 6: interpolator = interpol_nan_6_f8
        elif ndim == 7: interpolator = interpol_nan_7_f8
        elif ndim == 8: interpolator = interpol_nan_8_f8
        elif ndim == 9: interpolator = interpol_nan_9_f8
        elif ndim == 10: interpolator = interpol_nan_10_f8
        else:
            print("Not implemented")
            return
    elif mytype==np.float32:
        if ndim == 1: interpolator = interpol_nan_1_f4
        elif ndim == 2: interpolator = interpol_nan_2_f4
        elif ndim == 3: interpolator = interpol_nan_3_f4
        elif ndim == 4: interpolator = interpol_nan_4_f4
        elif ndim == 5: interpolator = interpol_nan_5_f4
        elif ndim == 6: interpolator = interpol_nan_6_f4
        elif ndim == 7: interpolator = interpol_nan_7_f4
        elif ndim == 8: interpolator = interpol_nan_8_f4
        elif ndim == 9: interpolator = interpol_nan_9_f4
        elif ndim == 10: interpolator = interpol_nan_10_f4
        else:
            print("Not implemented")
            return
    else:
        print("No float")
        return
    def function(wo): return interpolator(wo,lut)
    return function

def generate_itp_pn(lut):
    ndim=lut.ndim-1
    mytype=lut.dtype
    if mytype==np.float64:
        if ndim == 1: interpolator = interpol_1pn_f8
        elif ndim == 2: interpolator = interpol_2pn_f8
        elif ndim == 3: interpolator = interpol_3pn_f8
        elif ndim == 4: interpolator = interpol_4pn_f8
        elif ndim == 5: interpolator = interpol_5pn_f8
        elif ndim == 6: interpolator = interpol_6pn_f8
        elif ndim == 7: interpolator = interpol_7pn_f8
        elif ndim == 8: interpolator = interpol_8pn_f8
        elif ndim == 9: interpolator = interpol_9pn_f8
        elif ndim == 10: interpolator = interpol_10pn_f8
        else:
            print("Not implemented")
            return
    elif mytype==np.float32:
        if ndim == 1: interpolator = interpol_1pn_f4
        elif ndim == 2: interpolator = interpol_2pn_f4
        elif ndim == 3: interpolator = interpol_3pn_f4
        elif ndim == 4: interpolator = interpol_4pn_f4
        elif ndim == 5: interpolator = interpol_5pn_f4
        elif ndim == 6: interpolator = interpol_6pn_f4
        elif ndim == 7: interpolator = interpol_7pn_f4
        elif ndim == 8: interpolator = interpol_8pn_f4
        elif ndim == 9: interpolator = interpol_9pn_f4
        elif ndim == 10: interpolator = interpol_10pn_f4
        else:
            print("Not implemented")
            return
    else:
        print("No float")
        return
    def function(wo): return interpolator(wo,lut)
    return function

