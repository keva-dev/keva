#!/bin/sh
./gradlew clean :server:build -x test
cat ./compile.sh ./server/build/libs/server-1.0-SNAPSHOT-all.jar > ./binaries/unix/keva-server
echo "Done"
