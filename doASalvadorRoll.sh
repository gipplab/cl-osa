#!/bin/bash

if [ -z "$1" ]
  then
    echo "Please give Purpose as filename and retry..."
    exit 1
fi

mkdir -p ./mylogs
cd /cl-osa-tng && mvn package -DskipTests -Dmaven.javadoc.skip=true
# ATM Not fetching the errors  to logs (2>&1 for that)
java -Xmx100G -cp /cl-osa-tng/target/closa-1.4.jar com.iandadesign.closa.SalvadorFragmentLevelEval 1>&1 | tee ./mylogs/$1
