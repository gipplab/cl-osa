#!/bin/bash
# Simple bash script to run the detailed evaluation of CL-OSA for PAN-PC-11
# This is especially handy if running the evaluation on shell-only-computers.
# See Readme.md for usage.
# @author Johannes Stegmüller

if [ -z "$1" ]
  then
    echo "Please give Purpose as filename and retry..."
    exit 1
fi

mkdir -p ./mylogs
mvn package -DskipTests -Dmaven.javadoc.skip=true

if [ -z "$2" ]
  then
    NAME_SUFFIX=""
  else
    NAME_SUFFIX=_$2_$3_$4_$5
fi


# ATM Not fetching the errors  to logs (2>&1 for that)
java -Xmx100G -cp ./target/closa-1.4.jar com.iandadesign.closa.PAN11CharacterLevelEval $2 $3 $4 $5 1>&1 | tee ./mylogs/$1$NAME_SUFFIX
