<p align="center">
  <img width="150" src="https://i.imgur.com/z0c9bV7.png">
</p>

Keva is an open source (Apache 2.0 licensed), JVM off-heap in-memory data store, used as a database or cache,
can be a drop-in replacement for Redis.

Keva provides access to mutable data structures (String, Set, Sorted Set, List, Hash) via a set of commands, which are sent using a server-client model
with TCP sockets and a [RESP](https://redis.io/topics/protocol) protocol.

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/keva-dev/keva/Build/master?label=build&style=flat-square)
![Lines of code](https://img.shields.io/tokei/lines/github/keva-dev/keva?style=flat-square)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/keva-dev/keva?style=flat-square)
![GitHub](https://img.shields.io/github/license/keva-dev/keva?style=flat-square)

Major dependencies: [Netty](https://github.com/netty/netty), [ChronicleMap](https://github.com/OpenHFT/Chronicle-Map)

## Quick Start

[Get started in 5 minutes.](https://keva.dev)

## Changelogs

[Learn about the latest improvements.](https://keva.dev/changelogs.html)

## Development

Want to file a bug, contribute some code, or improve documentation? Excellent!

First, [see Developer Guide](https://keva.dev/guide/developer-guide.html).

Join [our Slack workspace](https://join.slack.com/t/kevadev/shared_invite/zt-103vkwyki-pwum_qmcJgaOq6FIy3k2GQ) to chat with us.

Pull requests are encouraged and always welcome. [Pick an issue](https://github.com/keva-dev/keva/issues) and help
us out!

To install and work on Keva locally:

```
git clone git@github.com:keva-dev/keva.git
cd keva
./gradlew dependencies
```

Run:

```
./gradlew :app:run
```

Build:

```
./gradlew :app:build
```

## License

[Apache License 2.0](https://github.com/keva-dev/keva/blob/master/LICENSE)
