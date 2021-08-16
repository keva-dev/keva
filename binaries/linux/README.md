# Keva Binary Builds (for Linux/macOS)

## Requirements

- Java 11 (JDK 11) or higher

## Install

- Server

```command
curl -o /usr/local/bin/keva-server https://rawcdn.githack.com/tuhuynh27/keva/607bfaf8bd5b87a7b58b192769ae08b4bd135580/binaries/linux/keva-server
chmod +x /usr/local/bin/keva-server
```

- CLI Client

```command
curl -o /usr/local/bin/keva-cli https://rawcdn.githack.com/tuhuynh27/keva/607bfaf8bd5b87a7b58b192769ae08b4bd135580/binaries/linux/keva-cli
chmod +x /usr/local/bin/keva-cli
```

## Run

Options:
- ```-h```: hostname
- ```-p```: port

### Server

```command
keva-server
```

### Client

```command
keva-cli
```
