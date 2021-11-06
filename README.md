<p align="center">
  <img width="150" src="https://i.imgur.com/z0c9bV7.png">
</p>

Keva is an open source (Apache 2.0 licensed), off-heap in-memory data structure, used as a database or cache,
can be a drop-in replacement for Redis.

Keva Server provides access to mutable data structures via a set of commands, which are sent using a server-client model
with TCP sockets and a simple protocol. So different processes/clients can query and modify the same data structures in
a shared way.

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/keva-dev/keva/Build/master?label=build&style=flat-square)
![Lines of code](https://img.shields.io/tokei/lines/github/keva-dev/keva?style=flat-square)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/keva-dev/keva?style=flat-square)
![GitHub](https://img.shields.io/github/license/keva-dev/keva?style=flat-square)

## Quick Start

[Get started in 5 minutes.](https://keva.dev)

## Changelogs

[Learn about the latest improvements.](https://keva.dev/changelogs.html)

## Development

Want to file a bug, contribute some code, or improve documentation? Excellent!

First, [see Developer Guide](https://keva.dev/developer-guide.html).

Pull requests are encouraged and always welcome. [Pick an issue](https://github.com/keva-dev/keva/issues) and help
us out!

To install and work on Keva locally:

```command
git clone git@github.com:keva-dev/keva.git
cd keva
./gradlew dependencies
```

Run:

```command
./gradlew :server:run
```

Build:

```command
./gradlew :build:server
```

## License

[Apache License 2.0](https://github.com/keva-dev/keva/blob/master/LICENSE)
