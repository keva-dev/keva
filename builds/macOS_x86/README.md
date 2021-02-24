# Jiny Keva Binary Builds (for macOS x86)

<p align="center">
  <img width="150" src="https://i.imgur.com/OpG00Ct.png">
</p>

Jiny Keva is an open source (Apache 2.0 licensed), in-memory data structure store, used as a database or cache. Keva provides data structures such as strings, hashes, lists, sets, sorted sets.

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
