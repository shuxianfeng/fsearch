#!/bin/bash
#prepare
#baseDir=$(cd "$(dirname "$0")"; pwd)
baseDir=/home/app/projects/fsearch
className=Main

java -cp "$baseDir/target/lib/*" $className &
