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
- ```-h {host}```: hostname (default: localhost)
- ```-p {port}```: port (default: 6767)
- ```-dir {directory}```: working directory (default: ./)
- ```-ps {true/false}```: persistence (default: 'true' if flag not set or provide value when set). 

```command
$ keva-server -p 6767
```
