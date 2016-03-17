baseDir=$(cd "$(dirname "$0")"; pwd)
className=com.zhuhuibao.fsearch.Main

java -cp "$baseDir/target/lib/*" $className &
