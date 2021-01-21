Use `gradle` if you have it installed, or `gradlew` from root project
## Quickstart
### Build jar files:
- build server:
```
./gradlew :keva:server:shadowJar
```
- build client:
```
./gradlew :keva:client:shadowJar
```

### Run jar files:
- run server: replace `server.jar` with correct jar name
```
java -jar server.jar

```
- run client: replace `client.jar` with correct jar name
```
java -jar client.jar
```

### Run directly (for development):
- run server:
```
./gradlew --no-daemon --quiet --console plain :keva:server:run
```
- run client:
```
./gradlew --no-daemon --quiet --console plain :keva:client:run
```

### Run options:
- ```-h```: hostname
- ```-p```: port

## Available commands:
### PING
- Returns "PONG". Often used to check connection to server.
```
localhost/127.0.0.1:6767> ping
PONG
```
### GET key 
- Returns value of key. If key or value doesn't exist, returns "null". 
```
localhost/127.0.0.1:6767> get mykey
null
localhost/127.0.0.1:6767> set mykey myvalue
1
localhost/127.0.0.1:6767> get mykey
myvalue
```
### SET key value
- Set value for key. Returns 1 if successful, 0 otherwise.
```
localhost/127.0.0.1:6767> set mykey myvalue
1
```
### INFO
- Returns information about the server.
```
localhost/127.0.0.1:6767> info
{threads:=6, clients:=1}
```
