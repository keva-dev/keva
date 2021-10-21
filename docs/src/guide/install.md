# Install

## Requirements

- Linux/macOS environment
- Java 11 (JDK 11) or higher

## Download

```command
curl -o /usr/local/bin/keva-server https://rawcdn.githack.com/tuhuynh27/keva/607bfaf8bd5b87a7b58b192769ae08b4bd135580/binaries/linux/keva-server
chmod +x /usr/local/bin/keva-server
```

## Run commands

Options:
- ```-h```: hostname (default: localhost)
- ```-p```: port (default: 6767)
- ```-sl```: snapshot location (default: ./)
- ```-ro```: replicate of (default: none)

```command
$ keva-server -p 6767
```
