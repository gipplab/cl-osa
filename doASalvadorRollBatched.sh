#!/bin/bash
# find best THRESH1 with 6-3: Seems to be roughly 1400, but not much difference
# find best mergemode + THRESH2 with 6-3:
# plausibility check small fragment small thresh, big big tresh etc once or twice
# compare this with absolute scoring once
# compare with other fragmentations 6-3 8-4? 10-5 14-7
# maybe do EN-DE comparison check with best config



# batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batchTest
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.65  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 1.65  simpleAdd

./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.05  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.08  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.10  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.12  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.20  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.30  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.40  keepingMax
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 0.50  keepingMax
