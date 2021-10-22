# Install

## Requirements

- Linux/macOS environment
- Java 11 (JDK 11) or higher

## Download

```command
curl -L -o /usr/local/bin/keva-server https://download.keva.dev
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
