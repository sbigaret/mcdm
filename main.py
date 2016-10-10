from scipy.sparse import csgraph
import numpy as np

from django.forms.widgets import FileInput

K = 4
A = ['a', 'b', 'c', 'd']
Pi = [[0 for i in range(K)] for j in range(K)]

Pi[0][1] = 1
Pi[0][2] = 1
Pi[0][3] = 1
Pi[1][2] = 0.5
Pi[1][3] = 1
Pi[2][1] = 0.5
Pi[2][3] = 1


def OrderedClustering(index, pi, size):
    if len(index) != size:
        return []
    if len(pi) != size:
        return []
    for p in pi:
        if len(p) != size:
            return []

    M = [[0 for i in range(size)] for j in range(size)]
    I = [[0 for i in range(size)] for j in range(size)]

    max = FindMax(pi)
    while max[0] > 0:
        M[max[1]][max[2]] = 1
        if Cycle(M) or 2 >= size:
            M[max[1]][max[2]] = 0
            I[max[1]][max[2]] = pi[max[1]][max[2]]
        pi[max[1]][max[2]] = 0
        max = FindMax(pi)


def FindMax(tab):
    max = 0
    max_a = -1
    max_b = -1
    size = len(tab)
    for a in range(0, size):
        for b in range(0, size):
            if tab[a][b] > max:
                max = tab[a][b]
                max_a = a
                max_b = b
    return max, max_a, max_b



arr = [[0 for i in range(K)] for j in range(K)]

arr[0][1] = 1
arr[1][2] = 1
arr[2][3] = 1
arr[3][0] = 1

def Cycle(G):
    visited = { u : 0 for u in G  }
    found_cycle = [False]
    for u in G:
        if visited[u] == 0:
            dfs_visit(G, u, visited, found_cycle)
        if found_cycle[0]:
            break
    return found_cycle[0]


def dfs_visit(G, u, visited, found_cycle):
    if found_cycle[0]:
        return
    visited[u] = 1
    for v in G[u]:
        if visited[v] == 1:
            found_cycle[0] = True
            return
        if visited[v] == 0:                 
            dfs_visit(G, v, visited, found_cycle)
    visited[u] = 2


print Cycle(arr)


print OrderedClustering(A, Pi, K)
print FindMax(Pi)
