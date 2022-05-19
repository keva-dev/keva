#!/bin/sh
./gradlew clean :app:fatJar -x test
cat ./compile.sh ./app/build/libs/*-all.jar > ./dist/unix/keva-server
echo "Done"
