---
slug: keva-proxy-approaches
title: Keva Proxy approaches
authors: [tuan]
tags: [proxy]
---

Some Proxy-based Clustering solution for Keva right now:

## Twemproxy

[Twemproxy (nutcracker)](https://github.com/twitter/twemproxy) is a fast and lightweight proxy for [memcached](http://www.memcached.org/) and [redis](http://redis.io/) protocol developed by **Twitter**. It was built primarily to reduce the number of connections to the caching servers on the backend. This, together with protocol pipelining and sharding enables you to horizontally scale your distributed caching architecture

### Features
- Pipelining:
  Twemproxy enables proxying multiple client connections onto one or few server connections. This architectural setup makes it ideal for pipelining requests and responses and hence saving on the round trip time.
- Zero copy:
  All the memory for incoming requests and outgoing responses is allocated in mbuf. Mbuf enables zero-copy because the same buffer on which a request was received from the client is used for forwarding it to the server
- Sharding:
  Data are shared automatically across multiple servers

### Disadvantage

- No automatically resharding when add/remove node

## Dynomite

**[Dynomite](https://github.com/Netflix/dynomite)**, inspired by [Dynamo whitepaper](http://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf), is a thin, distributed dynamo layer for different storage engines and protocols. Currently these include [Redis](http://redis.io/) and [Memcached](http://www.memcached.org/). Dynomite supports multi-datacenter replication and is designed for high availability. Dynomite was originally a fork of Twemproxy.

### Features
- Everything Twemproxy offers
- Replication:
  Dynomite offers replication between multiple cluster.
- High availability:
  With replication feature, Dynomite can handle different failure scenarios

### Disadvantage
- No automatically resharding when add/remove node

## Summary

Twemproxy is good as a pure proxy that serve read/write request to multiple server while Dynomite can also do that plus replication + HA but it add a bit more complexity since it is more than just a mere proxy. For now, Keva can go with Twemproxy because of it simplicity.
