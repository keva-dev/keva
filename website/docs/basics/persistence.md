---
sidebar_position: 3
---

# Persistence

:::warning WIP
This feature is under development
:::

Currently, Keva supports two persistence mechanisms:

- KDB (Keva Database): KDB persistence performs point-in-time snapshotting of the
  state of the system. This means that the state of the system is stored in a
  database, and the database is periodically backed up.
- AOF (Append-Only File): The AOF persistence logs every write operation to a
  file, that will be played again at server startup, reconstructing the original dataset.

KDB is the default persistence mechanism, and is the recommended persistence. Use AOF if you need
the database more durable, it tradeoffs performance for durability.
