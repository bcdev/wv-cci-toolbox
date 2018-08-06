import numpy as np
from numpy.linalg import inv as npinv


def inverse(inn):
    try:
        out=npinv(inn)
    except np.linalg.LinAlgError:
        # TODO
        # Is this always smart?
        #or better to flag ..
        out=np.zeros_like(inn)
    return out

def right_inverse(inn):
    return np.dot(inn.T, inverse(np.dot(inn, inn.T)))

def left_inverse(inn):
    return np.dot(inverse(np.dot(inn.T, inn)), inn.T)

#bit useless
#Todo refactor
def clipper(a, b, x):
    return np.clip(x, a, b)



def norm_error_weighted_x(ix, sri):
    # see Rodgers for details
    # ix : increment of x = x_i -x_i+1
    # sri: inverse of retrieval error co-variance
    return np.dot(ix.T, np.dot(sri, ix))

def norm_y(inn):
    return (inn * inn).mean()





#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Gauss Newton Operator
def gauss_newton_operator(a, b, x, y, k):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :return: cnx (clipped) root of fnc for the linear case, last y=fnc(x), last increment of x
    '''
    ki = left_inverse(k)
    incr_x = np.dot(ki, y)
    cnx = clipper(a, b, x - incr_x)
    return cnx, incr_x, None, None
def gauss_newton_cost(y):
    '''
    L2 norm
    '''
    cost = np.dot(y.T, y)
    return cost
def gauss_newton_ret_err_cov_i(x,dfnc):
    return None
def gauss_newton_ret_err_cov(x,dfnc):
    return None

def gauss_newton_gain_aver_cost(x, y, k):
    '''
    Calculates Gain, averagiong kernel matrix and cost
    :param y:
    :param x:
    :param k:
    :return:
    '''
    # gain matrix
    gain = left_inverse(k)
    # averaging kernel
    aver = np.identity(x.size)
    # cost function
    cost = np.dot(y.T, y)
    return gain, aver, cost

#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Levenberg Marquardt Operator
def leve_marq_operator(a, b, x, y, k, l):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :param l: lambda damping of levenberg dumping
    :return: cnx (clipped) root of fnc for the linear case,  last increment of x
    '''
    ktk=np.dot(k.T, k)
    lvma=l*np.diag(ktk.diagonal())
    ki = np.dot(inverse(ktk + lvma), k.T)
    incr_x = np.dot(ki, y)
    cnx = clipper(a, b, x - incr_x)
    return cnx, incr_x, None, None

def leve_marq_cost(y):
    '''
    L2 norm
    '''
    cost = np.dot(y.T, y)
    return cost
def leve_marq_ret_err_cov_i(x,dfnc):
    return None
def leve_marq_ret_err_cov(x,dfnc):
    return None

def leve_marq_gain_aver_cost(x, y, k):
    '''
    Calculates Gain, averaging kernel matrix and cost
    :param y:
    :param x:
    :param k:
    :return:
    '''
    # gain matrix
    gain = left_inverse(k)
    # averaging kernel
    aver = np.identity(x.size)
    # cost function
    cost = np.dot(y.T, y)
    return gain, aver, cost


#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Gauss Newton with measurement error
def gauss_newton_operator_with_se(a, b, x, y, k, sei):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :param sei: inverse of measurement error co-variance
    :return: cnx (clipped) root of fnc for the linear case, last y=fnc(x), last increment of x, last
            retrieval error co.-variance
    '''
    #print 'sei',sei
    kt_sei = np.dot(k.T, sei)
    #print 'kt_sei',kt_sei
    ret_err_cov_i = (np.dot(kt_sei, k))
    #print 'ret_err_cov_i',ret_err_cov_i
    ret_err_cov = inverse(ret_err_cov_i)
    #print 'ret_err_cov',ret_err_cov
    kt_sei_y = np.dot(kt_sei, y)
    #print 'kt_sei_y',kt_sei_y
    incr_x = np.dot(ret_err_cov, kt_sei_y)
    #print 'nx',x - incr_x
    cnx = clipper(a, b, x - incr_x)
    #print 'cnx',cnx
    return cnx, incr_x, ret_err_cov_i, ret_err_cov
def gauss_newton_se_ret_err_cov_i(k,sei):
    kt_sei = np.dot(k.T, sei)
    # inverse retrieval error co-variance
    kt_sei_k = np.dot(kt_sei, k)
    return kt_sei_k
def gauss_newton_se_ret_err_cov(k,sei):
    return inverse(gauss_newton_se_ret_err_cov_i(k,sei))
def gauss_newton_se_cost(y,sei):
    cost = np.dot(y.T, np.dot(sei, y))
    return cost
def gauss_newton_se_gain_aver_cost(x, y, k, sei,ret_err_cov):
    #retrieval error co-varince
    gain = np.dot(ret_err_cov, np.dot(k.T, sei))
    # averaging kernel
    # aver=np.dot(gain,k)
    aver = np.identity(x.size)
    # cost function
    cost = np.dot(y.T, np.dot(sei, y))
    return gain, aver, cost

#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Levenberg Marquardt with measurement error
def leve_marq_operator_with_se(a, b, x, y, k, sei, l):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :param sei: inverse of measurement error co-variance
    :param l: lambda damping of levenberg dumping
    :return: cnx (clipped) root of fnc for the linear case, last y=fnc(x), last increment of x, last
            retrieval error co.-variance
    '''
    #print 'sei',sei
    kt_sei = np.dot(k.T, sei)
    #print 'kt_sei',kt_sei
    ret_err_cov_i = (np.dot(kt_sei, k))
    #print 'ret_err_cov_i',ret_err_cov_i
    lvma=l*np.diag(ret_err_cov_i.diagonal())
    # this is not the real retrieval error covar, but it converges, if l is getting small
    ret_err_cov = inverse(ret_err_cov_i + lvma)
    #print 'ret_err_cov',ret_err_cov
    kt_sei_y = np.dot(kt_sei, y)
    #print 'kt_sei_y',kt_sei_y
    incr_x = np.dot(ret_err_cov, kt_sei_y)
    #print 'nx',x - incr_x
    cnx = clipper(a, b, x - incr_x)
    #print 'cnx',cnx
    return cnx, incr_x, ret_err_cov_i, ret_err_cov
def leve_marq_se_ret_err_cov_i(k,sei):
    kt_sei = np.dot(k.T, sei)
    # inverse retrieval error co-variance
    kt_sei_k = np.dot(kt_sei, k)
    return kt_sei_k
def leve_marq_se_ret_err_cov(k,sei):
    return inverse(leve_marq_se_ret_err_cov_i(k,sei))
def leve_marq_se_cost(y,sei):
    cost = np.dot(y.T, np.dot(sei, y))
    return cost
def leve_marq_se_gain_aver_cost(x, y, k, sei,ret_err_cov):
    #retrieval error co-varince
    gain = np.dot(ret_err_cov, np.dot(k.T, sei))
    # averaging kernel
    # aver=np.dot(gain,k)
    aver = np.identity(x.size)
    # cost function
    cost = np.dot(y.T, np.dot(sei, y))
    return gain, aver, cost

#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Optimal estmation with Gauss Newton
def optimal_estimation_gauss_newton_operator(a, b, x, y, k, sei, sai, xa):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :param sei: inverse of measurement error co-variance
    :param sai: inverse of prior error co-variance
    :param xa: prior
    :return: cnx (clipped) optimal solution for  fnc-1 for the linear case, last y=fnc(x), last increment of x, last
            retrieval error co.-variance
    '''
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = (np.dot(kt_sei, k))
    # inverse retrieval error co-variance
    ret_err_cov_i = sai + kt_sei_k
    # retrieval error co-variance
    ret_err_cov = inverse(ret_err_cov_i)
    kt_sei_y = np.dot(kt_sei, y)
    sai_dx = np.dot(sai, xa - x)
    incr_x = np.dot(ret_err_cov, kt_sei_y - sai_dx)

    #print 'x',x
    #print 'incr_x',incr_x
    #print 'ret_err_cov_i',ret_err_cov_i
    #print 'kt_sei_k',kt_sei_k
    #print 'kt_sei',kt_sei
    #print 'sei',sei
    cnx = clipper(a, b, x - incr_x)
    return cnx, incr_x, ret_err_cov_i, ret_err_cov
def oe_ret_err_cov_i(k,sei,sai):
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = np.dot(kt_sei, k)
    # inverse retrieval error co-variance
    ret_err_cov_i = sai + kt_sei_k
    return ret_err_cov_i
def oe_ret_err_cov(k,sei,sai):
    return inverse(oe_ret_err_cov_i(k,sei,sai))
def oe_cost(x,xa,y,sei,sai):
    cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + \
        np.dot(y.T, np.dot(sei, y))
    return cost
def oe_gain_aver_cost(x, y, k, xa, sei, sai,ret_err_cov):
    # gain matrix
    gain = np.dot(ret_err_cov, np.dot(k.T, sei))
    # averaging kernel
    aver = np.dot(gain, k)
    # cost function
    cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + \
        np.dot(y.T, np.dot(sei, y))
    return gain, aver, cost


#######################################################################
#######################################################################
#######################################################################
#######################################################################
###Optimal estmation with Levenberg Marquardt
def oe_leve_marq_operator(a, b, x, y, k, sei, sai, xa, l):
    '''

    :param a: lower limit of x np.array with 1 dimension
    :param b: upper limit of x np.array with same length  as a
    :param x: state vector
    :param y: fnc(x)
    :param k: dfnc(x)
    :param sei: inverse of measurement error co-variance
    :param sai: inverse of prior error co-variance
    :param xa: prior
    :param l: lambda damping of levenberg dumping
    :return: cnx (clipped) optimal solution for  fnc-1 for the linear case, last y=fnc(x), last increment of x, last
            retrieval error co.-variance
    '''
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = (np.dot(kt_sei, k))
    # inverse retrieval error co-variance
    lvma=l*np.diag(kt_sei_k.diagonal())
    ret_err_cov_i = sai + kt_sei_k + lvma
    # this is *not* the real retrieval error covar, but it converges, if l is getting small
    ret_err_cov = inverse(ret_err_cov_i )
    kt_sei_y = np.dot(kt_sei, y)
    sai_dx = np.dot(sai, xa - x)
    incr_x = np.dot(ret_err_cov, kt_sei_y - sai_dx)
    cnx = clipper(a, b, x - incr_x)
    return cnx, incr_x, ret_err_cov_i, ret_err_cov
def oe_leve_marq_ret_err_cov_i(k,sei,sai):
    kt_sei = np.dot(k.T, sei)
    kt_sei_k = np.dot(kt_sei, k)
    # inverse retrieval error co-variance
    ret_err_cov_i = sai + kt_sei_k
    return ret_err_cov_i
def oe_leve_marq_ret_err_cov(k,sei,sai):
    return inverse(oe_ret_err_cov_i(k,sei,sai))
def oe_leve_marq_cost(x,xa,y,sei,sai):
    cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + \
        np.dot(y.T, np.dot(sei, y))
    return cost
def oe_leve_marq_gain_aver_cost(x, y, k, xa, sei, sai,ret_err_cov):
    # gain matrix
    gain = np.dot(ret_err_cov, np.dot(k.T, sei))
    # averaging kernel
    aver = np.dot(gain, k)
    # cost function
    cost = np.dot((xa - x).T, np.dot(sai, xa - x)) + \
        np.dot(y.T, np.dot(sei, y))
    return gain, aver, cost
