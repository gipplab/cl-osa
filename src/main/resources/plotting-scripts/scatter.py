from matplotlib import pyplot as plt
import seaborn as sns
import pandas as pd
import os
import argparse

# handle input
parser = argparse.ArgumentParser()
parser.add_argument("-f", "--file", type=str,
                    help="File location of CSV file")
args = parser.parse_args()

# load data
data = pd.read_csv(args.file);
sns.lmplot(x="PC1", y="PC2", data, hue='class', fit_reg=False, markers=["o", "x"], palette="Set1")

# plot data with corresponding labels
new_title = 'Scatter Plot ofi ' + os.path.basename(args.file)
g._legend.set_title(new_title)
new_labels = ['no Plagiarism', 'Plagiarism']
for t, l in zip(g._legend.texts, new_labels): t.set_text(l)

fig = plt.gcf()
fig.set_size_inches(15, 10)
plt.show()
