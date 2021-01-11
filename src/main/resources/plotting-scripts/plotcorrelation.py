import pandas as pd
from matplotlib import pyplot as plt
from pylab import rcParams

import seaborn as sns
import numpy as np

from heatmap import heatmap, corrplot

import argparse

def printverbose(msg):
  if args.verbosity:
    print(msg)

parser = argparse.ArgumentParser()
parser.add_argument("--verbosity", help="increase output verbosity", action="store_true")
parser.add_argument("-f", "--file", type=str,
                    help="File location of CSV file")
args = parser.parse_args()

printverbose("verbosity turned on")

data = pd.read_csv(args.file)
if(data.empty):
  print("Error while reading file {}!".format(args.file))
  return -1

printverbose("Read CSV from {}".format(args.file))

try:
  corr = data.corr()
  break
except ValueError as err:
  print("error while calculating the correlation.")
  print(err)
  return -1

printverbose("Calculated correlation")

plt.figure(figsize=(8, 8))
corrplot(data.corr(), size_scale=300);

plt.show()
