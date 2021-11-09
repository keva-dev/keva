# Install

## Requirements

- Linux/macOS environment
- Java 11 (JDK/JRE 11) or higher

## Download

```
curl -L -o /usr/local/bin/keva-server https://download.keva.dev
chmod +x /usr/local/bin/keva-server
```

## Run commands

Options:
- ```-h {host}```: hostname (default: localhost)
- ```-p {port}```: port (default: 6379)
- ```-dir {directory}```: working directory (default: ./)
- ```-ps {true/false}```: persistence (default: 'true' if flag not set or provide value when set). 

```
$ keva-server
```

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
