#!/bin/bash
#prepare
#baseDir=$(cd "$(dirname "$0")"; pwd)
baseDir=/home/app/projects/fsearch
className=com.movision.fsearch.Main

java -cp "$baseDir/target/lib/*" $className &
