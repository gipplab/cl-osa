#!/bin/bash
# Simple bash script to run the PAN-PC evaluation script to a already
# generated output of xml-type results from the PAN-PC-11 CL-OSA detailed evaluation.
# This can save time in changing evaluation parameters without having to do
# the scores calculation again.
# @author Johannes Stegmüller

PLAGPATH="/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison_salvador/evalPAN2011En-es_2021_03_13_11_08_51/file_selection_cache"
DETPATH="/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison_salvador/evalPAN2011En-es_2021_03_13_11_08_51"

python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyShortCases"
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyMediumCases"
python  ./src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py -p $PLAGPATH --det-path=$DETPATH --filter="onlyLongCases"