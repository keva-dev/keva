# Replication

:::warning WIP
This feature is in experimental
:::

## How to run in replication mode

- Start one master instance (eg: localhost:6767)
```command
keva-server -p 6767
```

- Start replicator instances with args (`-ro` mean replicationOf):

```command
keva-server -p 6768 -ro localhost:6767
keva-server -p 6769 -ro localhost:6767
keva-server -p 6770 -ro localhost:6767
```

## Ideas

- Replica can be configure if option "replica_of" set to valid host and port, default is "NO:ONE" meaning it's a master.
- A new replica will initialize connection with master by calling "FSYNC host:port" to register itself to master and receive a snapshot file to perform full sync.
- Master will forward commands for all registered replicas, master acts like a client of replica.
- When slave calls "FSYNC" on master, it will be registered as replica on master and have it's own buffer for new commands.
- On master, for each slave there will be a separate thread running, it will consume from the blocking queue of commands and send them to client in a blocking manner to ensure ordering (since there is no order mechanics in command yet).

## Incoming improvements

- Add mechanics to ensure order of commands from master to slave.
- Use async when communicating between master and slave.
- Add a partial sync and improve data consistency in case of slow network, lost connection, reconnection.
