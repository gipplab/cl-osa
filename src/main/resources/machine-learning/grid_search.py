import pandas as pd
import numpy as np
import argparse
from joblib import dump

from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.model_selection import GridSearchCV
from sklearn.svm import SVC
from sklearn.neighbors import KNeighborsClassifier

class GridSearchTrainer:
"""
Provide methods to train base machine learning algorithms including a grid search.

The __init__ method is used to set flags, which were set by the user arguments.

Attributs:
  save [bool]    : saves models as .joblib file
  verbose [bool] : verbose mode
  rfc [bool]     : train random forest model
  svm [bool]     : train support vector machine
  lr [bool]      : train logistic regression
  knn [bool]     : train nearest neighbours
  TODO: nb [bool]: train naive baies

"""

  def __init__(self, args, data, labels):
    self.save     = args.save
    self.verbose  = args.verbose
    self.rfc      = args.rfm || args.all
    self.svm      = args.svm || args.all
    self.lr       = args.lr  || args.all
    self.knn      = args.knn || args.all
    base=os.path.basename(args.file)
    self.filename = os.path.splitext(base)[0]
    self.data = data
    self.labels = labels

  def __call__(self, args, data, labels):
    self.__init__(args)
    if(self.rfc):
      self.train_rfc()
    if(self.svm):
      self.train_svm()
    if(self.lr):
      self.train_lr()
    if(self.knn):
      self.train_knn()

  def split_data(self):

  def evaluate(self, y_test, y_pred):
    print(confusion_matrix(y_test,y_pred))
    print(classification_report(y_test,y_pred))
    print(accuracy_score(y_test, y_pred))

  def execute_gridsearch(self, model, param_grid):
    CV_models = GridSearchCV(estimator=rfc_grid_search, param_grid=param_grid, cv=5)
    CV_models.fit(x_train, y_train)
    return CV_models.best_params_

  def save_model(self, model, filename):
    dump(model, filename+'joblib')

  def train_rfc(self):
    rfc_param_grid = { 
      'n_estimators': [1, 100],
      'max_features': ['auto', 'sqrt', 'log2'],
      'max_depth' : [6,7],
      'criterion' :['gini', 'entropy']}
    rfc_grid_search = RandomForestClassifier(random_state=42)
    self.execute_gridsearch(model, param_grid)
    if save_model:
      self.save_model(model, base_file_name+'rfc')

  def train_svm(self):
    svm_param_grid = {
      'kernel' : ['linear', 'poly', 'rbf'],
      'degree' : [1,2,3,4,5,6,7,8,9],
      'gamma': [1e-3, 1e-4],
      'C': [1, 10, 100]}
    svc_grid_search = SVC(random_state=42)
    self.execute_gridsearch(model, param_grid)
    if save_model:
      self.save_model(model, base_file_name+'svm')

  def train_lr(self):
    lr_param_grid = {
      'kernel' : ['newton-cg', 'lbfgs', 'sag', 'saga'],
      'maximum_iter' : [500, 1000, 1500],
      'multi_class' : ['auto', 'ovr', 'multinominal'],
      'tol' : [0.0001, 0.00001]}
    lr_grid_search = LogisticRegression(random_state=42)
    self.execute_gridsearch(model, param_grid)
    if save_model:
      self.save_model(model, base_file_name+'lr')

  def train_knn(self):
    knn_param_grid = {'n_neighbors' : k_range}
    knn_grid_search = KNeighborsClassifier(random_state = 42)
    self.execute_gridsearch(model, param_grid)
    if save_model:
      self.save_model(model, base_file_name+'knn')

if __name__ == ”__main__”:
  parser = argparse.ArgumentParser()
  parser.add_argument('-f', '--file', type=str, required=True, help="Path to transformed data")
  parser.add_argument('-a', '--all', action='store_true', help='Train all models')
  parser.add_argument('-r', '--rfm', action='store_true', help='Train random forest model')
  parser.add_argument('-m', '--svm', action='store_true', help='Train support vector machine')
  parser.add_argument('-l', '--lr', action='store_true', help='Train logistic regression')
  parser.add_argument('-k', '--nn', action='store_true', help='Train nearest neighbours')
  parser.add_argument('-s', '--save', action='store_true', help='Save models as pickles')
  parser.add_argument('-v', '--verbose', action='store_true', help='an optional argument')
  args = parser.parse_args()
  print(args)

  train = pd.read_csv(args.file)
  feature_cols = ['x', 'y']
  X = train.loc[:, feature_cols].values
  y = train.loc[: ,'class'].values
  y = y.astype(np.int)

  x_train, x_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=0)

  sc = StandardScaler()
  x_train = sc.fit_transform(x_train)
  x_test = sc.transform(x_test)

  best_param = execute_gridsearch(rfc_grid_search, rfc_param_grid)

  rfc_best=RandomForestClassifier(
        random_state=42, 
        max_features=best_param['max_features'], 
        n_estimators=best_param['n_estimators'], 
        max_depth=best_param['max_depth'], 
        criterion=best_param['criterion'])

  rfc_best.fit(x_train, y_train)
  pred=rfc_best.predict(x_test)
  evaluate(y_test, pred)
  dump(rfc_best, 'rfc_best.joblib')
