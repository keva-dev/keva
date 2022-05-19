# Install

## Requirements

- Linux/macOS environment
- Java 8 (JDK/JRE 8) or higher

## Download

```
curl -L -o /usr/local/bin/keva-server https://github.com/keva-dev/keva/releases/download/v1.0.0-rc1/keva-server
chmod +x /usr/local/bin/keva-server
```

## Run commands

Run:

```
$ keva-server
```

Parameters:

    Option                          Description
    ------                          -----------
    --p, --port <Integer>           Accept connections on the specified port (default: 6379)
    --save <Boolean>                Enable save the DB to disk (default: true)
    --appendonly <Boolean>          Enable append-only-file (default: false)
    --appendfsync <Integer>         Define append-only fsync in ms (default: 1000)
    --dir <String>                  Define working directory (default: './')
    --requirepass <String>          Authenticate password (default: '' - no password)

## Docker

Available at [Docker Hub](https://hub.docker.com/r/kevadev/keva-server)

Pull image:

```
docker pull kevadev/keva-server
```

Run container:

```
docker run --name some-keva -d -p 6379:6379 kevadev/keva-server 
```
