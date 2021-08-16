# Introduction

Keva is an open source (Apache 2.0 licensed), off-heap in-memory data structure, used as a database or cache. Keva
provides value types such as strings, object, integer, long, double, float.

Keva Server provides access to mutable data structures via a set of commands, which are sent using a server-client model
with TCP sockets and a simple protocol. So different processes/clients can query and modify the same data structures in
a shared way.

## Features

- High performance and low latency in-memory key-value store (JVM GC independent)
- The basic operations are PUT(key,value), GET(key), DEL(key)
- Persistence in-memory data to disk (very fast thanks to disk-memory mapping at OS level)
