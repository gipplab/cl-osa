#!/bin/bash

if [ -z "$1" ]
  then
    echo "Please give Purpose as filename and retry..."
    exit 1
fi

mkdir -p ./mylogs
cd /home/johannes/Repositories/cl-osa-tng && mvn package -DskipTests -Dmaven.javadoc.skip=true

if [ -z "$2" ]
  then
    NAME_SUFFIX=""
  else
    NAME_SUFFIX=_$2_$3_$4_$5
fi
# ATM Not fetching the errors  to logs (2>&1 for that)
java -Xmx100G -cp /home/johannes/Repositories/cl-osa-tng/target/closa-1.4.jar com.iandadesign.closa.SalvadorFragmentLevelEval $2 $3 $4 $5 1>&1 | tee ./mylogs/$1$NAME_SUFFIX
