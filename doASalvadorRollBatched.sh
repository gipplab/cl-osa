#!/bin/bash
# find best THRESH1 with 6-3: Seems to be roughly 1400, but not much difference
# find best mergemode + THRESH2 with 6-3 (seems to be 1400, simpleAdd)
# compare this with absolute scoring once = absolute scoring has much higher scoring
# find best THRESH2 for absolute scoring - ok
# plausibility check small fragment small thresh, big big thresh etc once or twice
# compare with other fragmentations 6-3 8-4? 10-5 14-7
# maybe do EN-DE comparison check with best config



# batchBaseName / USE_ABSOLUTE_SCORES / THRESH1 / THRESH2 / FRAGMENT_MERGE_MODE
BATCH_BASE_NAME=batch5-2-SP-Rel-



./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 15  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 16  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 17  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 18  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 19  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 2400 20  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 3200 15  simpleAdd
./doASalvadorRoll.sh $BATCH_BASE_NAME true 3200 18  simpleAdd


# Best Thresh2
#
# 10-5:  9 or 10
# 14-7:  not clear with absolute scoring, T1 was 2400 T2 0