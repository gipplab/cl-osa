#!/bin/bash
# Simple bash script to run  multiple detailed evaluations for PAN-PC-11 detailed analysis.
# This is especially handy if running the evaluation on shell-only-computers.
# See Readme.md for usage.
# @author Johannes Stegmüller

# Parameter expanation:  batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batchTestAbsolute
./runDetailedEval.sh $BATCH_BASE_NAME true 1400 9  simpleAdd
./runDetailedEval.sh $BATCH_BASE_NAME true 1400 8  simpleAdd
./runDetailedEval.sh $BATCH_BASE_NAME true 1400 7  simpleAdd

./runDetailedEval.sh $BATCH_BASE_NAME true 2400 13  simpleAdd
./runDetailedEval.sh $BATCH_BASE_NAME true 2400 12  simpleAdd
./runDetailedEval.sh $BATCH_BASE_NAME true 2400 10  simpleAdd