from scipy.sparse import csgraph
import numpy as np

from django.forms.widgets import FileInput

K = 4
A = ['a', 'b', 'c', 'd']
Pi = [[0 for i in range(K)] for j in range(K)]

Pi[0][1] = 1.05
Pi[0][2] = 1.04
Pi[0][3] = 1.03
Pi[1][2] = 0.51
Pi[1][3] = 1.02
Pi[2][1] = 0.5
Pi[2][3] = 1.01


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
        if Cycle(M) or not DFS(M, 0, 0, size-1):
                M[max[1]][max[2]] = 0
                I[max[1]][max[2]] = pi[max[1]][max[2]]
        pi[max[1]][max[2]] = 0
        max = FindMax(pi)

    return M


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
arr[0][3] = 1

def Cycle(G):
    visited = [ 0 for g in range(len(G))]
    found_cycle = False
    for u in range(len(G)):
        if visited[u] == 0:
            found_cycle, visited = dfs_visit(G, u, visited, found_cycle)
        if found_cycle:
            break
    return found_cycle


def dfs_visit(G, u, visited, found_cycle):
    if visited[u] == 1 or found_cycle:
        return True, visited
    visited[u] = 1
    for g in range(len(G)):
        if G[u][g] == 1:
            result, visited = dfs_visit(G, g, visited, found_cycle)
            if result:
                return True, visited
    visited[u] = 0
    return False, visited


def DFS(G, n, length, max):
    result = True
    if length >= max:
        return False
    for k in range(len(G)):
        if G[n][k] == 1:
            length += 1
            result = DFS(G, k, length, max)
        if not result:
            break
    return result



#print Cycle(arr)
#print DFS(arr, 0, 0, 2)

print OrderedClustering(A, Pi, K)

print not DFS([[0, 1, 1, 1], [0, 0, 1, 1], [0, 0, 0, 1], [0, 0, 0, 0]], 0, 0, 3)
