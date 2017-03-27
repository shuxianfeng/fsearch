#!/bin/bash
#prepare
projectDir=$(cd "$(dirname "$0")"; pwd)
code_path="/home/app/projects/fsearch"
version_name=$1

cd "$code_path"
git checkout master
git pull

if [ -n $version_name ];
then
  git checkout $version_name
  git pull
fi

mvn install:install-file -Dfile="$projectDir/src/main/lib/petkit-base.jar" -DgroupId=com.petkit -DartifactId=petkit-base -Dversion=4.0 -Dpackaging=jar