baseDir=$(cd "$(dirname "$0")"; pwd)
className=Main

java -cp "$baseDir/target/lib/*" $className &
