#!/usr/bin/env bash
#prepare
#baseDir=$(cd "$(dirname "$0")"; pwd)
#baseDir=/home/app/projects/fsearch
#className=com.movision.fsearch.Main
#java -cp "$baseDir/target/lib/*" $className &

#JDK所在路径
JAVA_HOME="/usr/java/jdk1.8.0_111"

#执行程序启动所使用的系统用户，考虑到安全，推荐不使用root帐号
RUNNING_USER=root

#Java程序所在的目录
APP_HOME=/home/app/projects/fsearch

#需要启动的Java主程序（main方法类）
APP_MAINCLASS=com.movision.fsearch.Main

#java虚拟机启动参数
JAVA_OPTS="-ms512m -mx512m -Xmn256m -Djava.awt.headless=true -XX:MaxPermSize=128m -Ddefault.client.encoding=UTF-8 -Dfile.encoding=UTF-8"

#拼凑完整的classpath参数，包括指定lib目录下所有的jar
CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

################################################################
start() {
    echo -n "Starting $APP_MAINCLASS ..."
    JAVA_CMD="nohup $JAVA_HOME/bin/java $JAVA_OPTS -classpath $CLASSPATH $APP_MAINCLASS >/dev/null 2>&1 &"
    su - $RUNNING_USER -c "$JAVA_CMD"
}

