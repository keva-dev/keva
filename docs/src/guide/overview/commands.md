# Commands

Follows [Redis's commands](https://redis.io/commands).

Implemented commands:

<details>
    <summary>Server</summary>

- INFO
- FLUSHDB
- TIME

</details>

<details>
    <summary>Connection</summary>

- AUTH
- ECHO
- PING
- QUIT
- CLIENT ID
- CLIENT INFO

</details>

<details>
    <summary>Key</summary>

- DEL
- EXISTS
- RENAME
- EXPIRE
- EXPIREAT
- DUMP
- RESTORE
- TYPE

</details>

<details>
    <summary>String</summary>

- APPEND
- GET
- INCRBY
- INCR
- SET
- GETSET
- MGET
- STRLEN
- SETRANGE

</details>

<details>
    <summary>Hash</summary>

- HGET
- HGETALL
- HKEYS
- HVALS
- HSET
- HDEL
- HEXISTS
- HLEN
- HSTRLEN

</details>

<details>
    <summary>List</summary>

- LPUSH
- RPUSH
- LPOP
- RPOP
- LLEN
- LRANGE
- LINDEX
- LSET
- LREM

</details>

<details>
    <summary>Set</summary>

- SADD
- SMEMBERS
- SISMEMBER
- SCARD
- SDIFF
- SINTER
- SUNION
- SMOVE
- SREM

</details>

<details>
    <summary>SortedSet</summary>

- ZADD
- ZSCORE

</details>

<details>
    <summary>Pub/Sub</summary>

- SUBSCRIBE
- UNSUBSCRIBE
- PUBLISH

</details>

<details>
    <summary>Transactions</summary>

- MULTI
- EXEC
- DISCARD
- WATCH

</details>
