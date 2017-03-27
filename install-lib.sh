#!/bin/bash
#prepare
version_name=$1
projectDir=$(cd "$(dirname "$0")"; pwd)

if [ -n $version_name ];
then
  git checkout $version_name
  git pull
fi

mvn install:install-file -Dfile="$projectDir/src/main/lib/petkit-base.jar" -DgroupId=com.petkit -DartifactId=petkit-base -Dversion=4.0 -Dpackaging=jar