#!/bin/bash


PLAGPATH="/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison_salvador/evalPAN2011En-es_2021_03_13_19_43_51/file_selection_cache"
DETPATH="/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison_salvador/evalPAN2011En-es_2021_03_13_19_43_51"


python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyShortCases"
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyMediumCases"
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyLongCases"

