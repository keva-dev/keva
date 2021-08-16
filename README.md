<p align="center">
  <img width="150" src="https://i.imgur.com/OpG00Ct.png">
</p>

Keva is an open source (Apache 2.0 licensed), off-heap in-memory data structure, used as a database or cache. Keva
provides value types such as strings, object, integer, long, double, float.

Keva Server provides access to mutable data structures via a set of commands, which are sent using a server-client model
with TCP sockets and a simple protocol. So different processes/clients can query and modify the same data structures in
a shared way.

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/tuhuynh27/keva/Build/master?label=build&style=flat-square)
![Lines of code](https://img.shields.io/tokei/lines/github/tuhuynh27/keva?style=flat-square)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/tuhuynh27/keva?style=flat-square)
![GitHub](https://img.shields.io/github/license/tuhuynh27/keva?style=flat-square)
![Maven Central](https://img.shields.io/maven-central/v/com.jinyframework/core?style=flat-square)

## Features

- High performance and low latency in-memory key-value store (JVM GC independent)
- The basic operations are PUT(key,value), GET(key), DEL(key)
- Persistence in-memory data to disk (very fast thanks to disk-memory mapping at OS level)

## Install

Visit [Binaries Builds](https://github.com/tuhuynh27/keva/tree/master/binaries)

## Usage

### PING

- Returns "PONG". Often used to check connection to server.

```command
localhost/127.0.0.1:6767> ping
PONG
```

### GET key

- Returns value of key. If key or value doesn't exist, returns "null".

```command
localhost/127.0.0.1:6767> get mykey
null
localhost/127.0.0.1:6767> set mykey myvalue
1
localhost/127.0.0.1:6767> get mykey
myvalue
```

### SET key value

- Set value for key. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> set mykey myvalue
1
```

### DEL key

- Remove the key value pair. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> set a b
1
localhost/127.0.0.1:6767> get a
b
localhost/127.0.0.1:6767> del a
1
localhost/127.0.0.1:6767> get a
null
```

### EXPIRE key expireTimeInMilliSecond

- Set expire time for key. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> expire mykey 1000
1
```

### INFO

- Returns information about the server.

```command
localhost/127.0.0.1:6767> info
{threads:=6, clients:=1}
```

## Development

Want to file a bug, contribute some code, or improve documentation? Excellent!

First, [see Developer Guide](https://jinyframework.com/guide/developer-guide.html).

Pull requests are encouraged and always welcome. [Pick an issue](https://github.com/tuhuynh27/keva/issues) and help
us out!

To install and work on Keva locally:

```command
git clone git@github.com:tuhuynh27/keva.git
cd keva
./gradlew dependencies
```

Run server:

```command
./gradlew --no-daemon --quiet --console plain :server:run
```

Run CLI client:

```command
./gradlew --no-daemon --quiet --console plain :cli-client:run
```

Build server:

```command
./gradlew :server:shadowJar
```

Build CLI client:

```command
./gradlew :cli-client:shadowJar
```

## License

[Apache License 2.0](https://github.com/tuhuynh27/keva/blob/master/LICENSE)
