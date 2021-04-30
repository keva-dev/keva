# First, use graalvm
# jabba use graalvm@20.2.0

# Server
./gradlew :server:shadowJar
native-image --no-server --no-fallback -H:ReflectionConfigurationFiles=native-config.json --allow-incomplete-classpath -jar server/build/libs/server-1.0-SNAPSHOT-all.jar keva-server
mv keva-server builds/macOS_x86/keva-server

# Client
./gradlew :client:shadowJar
native-image --no-server --no-fallback -H:ReflectionConfigurationFiles=native-config.json --allow-incomplete-classpath -jar client/build/libs/client-1.0-SNAPSHOT-all.jar keva-client
mv keva-client builds/macOS_x86/keva-client
