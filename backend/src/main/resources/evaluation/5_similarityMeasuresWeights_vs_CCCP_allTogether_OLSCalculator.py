from os import walk

import pandas as pd
import statsmodels.api as sm
import sys


# OLS Regression Model calculator
# Estimates the complexity values based on the N value and
# the weights given to each similarity measure

files = [
# "45_11275.42_bw-simulation.csv",
# "49_81574.52_bw-maven.csv",
"58_3968.0_LdoD-test.csv",
# "57_43122.39_LdoD-simulation-5fragments.csv",
# "69_99631.32_ldod-maven.csv",
]
# for (dirpath, dirnames, filenames) in walk("./data/"):
#     files.extend(filenames)
#     print(filenames)
#     break

df = {
    'n': [],
    'A': [],
    'W': [],
    'R': [],
    'S': [],
    'complexity': [],
    'coupling': [],
    'cohesion': [],
    'performance': [],
}

for file in files:
    print(file)
    data = pd.read_csv("./data/" + file)
    for entry in data.values:
        df['n'].append(entry[0])
        df['A'].append(entry[1])
        df['W'].append(entry[2])
        df['R'].append(entry[3])
        df['S'].append(entry[4])
        df['cohesion'].append(entry[5])
        df['coupling'].append(entry[6])
        df['complexity'].append(entry[8]) # pComplexity
        df['performance'].append(entry[10]) # pPerformance

df = pd.DataFrame(df)

X = df.loc[:, ['n', 'A', 'W', 'R', 'S']]
y = df.loc[:, 'complexity']
X = sm.add_constant(X)
model = sm.OLS(y, X)
results = model.fit()
print(results.summary())
print()

X = df.loc[:, ['n', 'A', 'W', 'R', 'S']]
y = df.loc[:, 'coupling']
X = sm.add_constant(X)
model = sm.OLS(y, X)
results = model.fit()
print(results.summary())
print()

X = df.loc[:, ['n', 'A', 'W', 'R', 'S']]
y = df.loc[:, 'cohesion']
X = sm.add_constant(X)
model = sm.OLS(y, X)
results = model.fit()
print(results.summary())
print()

X = df.loc[:, ['n', 'A', 'W', 'R', 'S']]
y = df.loc[:, 'performance']
X = sm.add_constant(X)
model = sm.OLS(y, X)
results = model.fit()
print(results.summary())
print()
