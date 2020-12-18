#!/bin/bash
cd /cl-osa-tng && mvn package -DskipTests -Dmaven.javadoc.skip=true
java -cp /cl-osa-tng/target/closa-1.4.jar com.iandadesign.closa.SalvadorFragmentLevelEval
