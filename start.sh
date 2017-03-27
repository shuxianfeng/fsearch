#!/bin/bash
#prepare
code_path="/home/app/projects/fsearch"
version_name=$1
baseDir=$(cd "$(dirname "$0")"; pwd)
className=Main

cd "$code_path"
git checkout master
git pull

if [ -n $version_name ];
then
  git checkout $version_name
  git pull
fi

java -cp "$baseDir/target/lib/*" $className &
