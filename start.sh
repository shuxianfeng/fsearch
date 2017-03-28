#!/bin/bash
#prepare
#baseDir=$(cd "$(dirname "$0")"; pwd)
#baseDir=/home/app/projects/fsearch
#className=com.movision.fsearch.Main
#java -cp "$baseDir/target/lib/*" $className &

#Java程序所在的目录
APP_HOME=/home/app/projects/fsearch

#需要启动的Java主程序（main方法类）
APP_MAINCLASS=com.movision.fsearch.Main

#拼凑完整的classpath参数，包括指定lib目录下所有的jar
CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

java -classpath $CLASSPATH $APP_MAINCLASS &

