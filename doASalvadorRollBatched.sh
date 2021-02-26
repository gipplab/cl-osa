#!/bin/bash

# batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batchTest
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1200 0.8 simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1200 0.5 keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1200 0.7 simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2000 0.8 keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2000 0.5 simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME
