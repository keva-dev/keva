# Introduction

Keva is an open source (Apache 2.0 licensed), off-heap in-memory data structure, used as a database or cache,
can be a drop-in replacement for Redis.

Keva Server provides access to mutable data structures via a set of commands, which are sent using a server-client model
with TCP sockets and a simple protocol. So different processes/clients can query and modify the same data structures in
a shared way.

## Features

- High performance and low latency in-memory key-value database
- Compatible with Redis protocol
- Multithreading engine helps to maximize the use of system's resources
- Persistence in-memory data to disk

## Major dependencies

- Run on JVM
- Netty for handling TCP messages
- ChronicleMap for storage engine
