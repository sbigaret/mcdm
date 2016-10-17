""" EXAMPLE OF USE ILP

from cvxopt.glpk import ilp
from cvxopt import matrix
import numpy as np

cc = np.ones(7, 'd')
temp_g = np.array([[20, 17, 15, 15, 10, 8, 5], [145, 92, 70, 70, 84, 14, 47]], 'd')
temp_h = np.array([100, 250], 'd')

c = matrix(cc)
g = matrix(temp_g)
h = matrix(temp_h)

(ans, res) = ilp(-c, g, h, B=set(range(0, 7)))

print ans
print res

print -c
print temp_g
print temp_h
"""

