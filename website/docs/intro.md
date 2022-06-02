---
sidebar_position: 1
---

# Introduction

Keva is an open source (Apache 2.0 licensed), JVM off-heap in-memory data store, used as a database or cache,
can be a drop-in replacement for Redis.

Keva provides access to mutable data structures (String, Set, Sorted Set, List, Hash) via a set of commands, which are sent using a server-client model
with TCP sockets and a [RESP](https://redis.io/topics/protocol) protocol.

## Features

- Low latency in-memory key-value database
- Compatible with Redis protocol
- Multithreading engine helps to maximize the use of system's resources
- Persistence in-memory data to disk
- Various backend databases to choose: LevelDB, LMDB, RocksDB, BoltDB, etc. (WIP)

## Major dependencies

- Run on JVM
- [Netty](https://github.com/netty/netty) for network layer
- [ChronicleMap](https://github.com/OpenHFT/Chronicle-Map) for storage layer
