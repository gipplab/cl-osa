#!/bin/bash
# find best THRESH1 with 6-3
# find best mergemode + THRESH2 with 6-3
# compare this with absolute scoring once
# compare with other fragmentations 6-3 8-4? 10-5 14-7
# maybe do de comparison check with best config
# maybe do de


# batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batchTest
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.15  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.20  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.25  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.30  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.35  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.40  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.50  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.55  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.60  simpleAdd
