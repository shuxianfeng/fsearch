projectDir=$(cd "$(dirname "$0")"; pwd)
mvn install:install-file -Dfile="$projectDir/src/main/lib/petkit-base.jar" -DgroupId=com.petkit -DartifactId=zhb-petkit-base -Dversion=4.0 -Dpackaging=jar