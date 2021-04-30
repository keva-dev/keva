# Jiny Keva Binary Builds (for macOS x86)

(binaries produced by GraalVM)

## Install

- Server

```shell
$ curl -o /usr/local/bin/keva-server https://raw.githubusercontent.com/tuhuynh27/jiny/master/keva/builds/macOS_x86/keva-server
$ chmod +x /usr/local/bin/keva-server
```

- CLI Client

```shell
$ curl -o /usr/local/bin/keva-client https://raw.githubusercontent.com/tuhuynh27/jiny/master/keva/builds/macOS_x86/keva-client
$ chmod +x /usr/local/bin/keva-client
```

## Run

Options:
- ```-h```: hostname
- ```-p```: port

### Server

```shell
$ keva-server
```

### Client

```shell
$ keva-client
```
