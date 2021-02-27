#!/bin/bash
# find best THRESH1 with 6-3: Seems to be roughly 1400, but not much difference
# find best mergemode + THRESH2 with 6-3 (seems to be 1400, simpleAdd)
# plausibility check small fragment small thresh, big big thresh etc once or twice
# compare this with absolute scoring once
# compare with other fragmentations 6-3 8-4? 10-5 14-7
# maybe do EN-DE comparison check with best config



# batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batchTestAbsolute
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 10  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 14  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 16  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 18  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 20  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 1400 24  simpleAdd


./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 14  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 18  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 20  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME false 2400 24  simpleAdd
