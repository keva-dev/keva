# Install

## Requirements

- Linux/macOS environment
- Java 11 (JDK/JRE 11) or higher

## Download

```
curl -L -o /usr/local/bin/keva-server https://github.com/keva-dev/keva/releases/download/v0.1.0-alpha/keva-server
chmod +x /usr/local/bin/keva-server
```

## Run commands

Run:

```
$ keva-server
```

Parameters:

    Option          Description
    ------          -----------
    -h <String>     host (default: localhost)
    -p <Integer>    port (default: 6379)
    -ps <Boolean>   enable persistence (default: true)
    -aof <Boolean>  enable append-only-file (default: false)
    -ai <Integer>   define append-only interval in ms (default: 1000)
    -dir <String>   working directory (default: ./)
    -pw <String>    authenticate password (default: none - no password)

## Docker

Available in [Docker Hub](https://hub.docker.com/r/kevadev/keva-server)

Pull image:

```
docker pull kevadev/keva-server
```

Run container:

```
docker run --name some-keva -d -p 6379:6379 keva-server 
```
