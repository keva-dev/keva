---
sidebar_position: 2
---

# Redis Compatibility

Keva supports Redis client protocol up to version 6.2. Following table shows the list of supported [Redis commands](https://redis.io/commands):

This is a list of commands that are available in the current latest version of Keva:

| Feature      | Supported? | Supported Commands                                                                                                                                                                               |
|--------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| String       | ✅          | APPEND - GET - INCRBY - INCR - SET - GETSET - GETDEL - MGET - STRLEN - SETRANGE - DECR - DECRBY - GETRANGE - MSET - INCRBYFLOAT - SUBSTR - STRALGO LCS - GETEX - MSETNX - PSETEX - SETEX - SETNX |
| Hash         | ✅          | HGET - HGETALL - HKEYS - HVALS - HSET - HDEL - HEXISTS - HLEN - HSTRLEN                                                                                                                          |
| List         | ✅          | LPUSH - RPUSH - LPOP - RPOP - LLEN - LRANGE - LINDEX - LSET - LREM                                                                                                                               |
| Set          | ✅          | SADD - SMEMBERS - SISMEMBER - SCARD - SDIFF - SINTER - SUNION - SMOVE - SREM                                                                                                                     |
| SortedSet    | ✅          | ZADD - ZSCORE                                                                                                                                                                                    |
| Transactions | ✅          | MULTI - EXEC - DISCARD - WATCH                                                                                                                                                                   |
| Generic      | ✅          | DEL - EXISTS - RENAME - EXPIRE - EXPIREAT - DUMP - RESTORE - TYPE                                                                                                                                |
| Connection   | ✅          | AUTH - ECHO - PING - QUIT - CLIENT ID - CLIENT INFO                                                                                                                                              |
| Server       | ✅          | INFO - FLUSHDB - TIME - CONFIG SET - CONFIG GET                                                                                                                                                  |
| Scripting    | ❌          |                                                                                                                                                                                                  |
| Pub/Sub      | ✅          | SUBSCRIBE - UNSUBSCRIBE - PUBLISH                                                                                                                                                                |
| Cluster      | ❌          |                                                                                                                                                                                                  |
| Geo          | ❌          |                                                                                                                                                                                                  |
| HyperLogLog  | ❌          |                                                                                                                                                                                                  |
| Streams      | ❌          |                                                                                                                                                                                                  |
