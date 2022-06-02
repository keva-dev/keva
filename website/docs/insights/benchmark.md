---
sidebar_position: 2
---

# Benchmark

:::warning WIP
Because we're in early development stage, a more detailed benchmark will be provided later (when Keva is more mature and stable)
:::

Keva's throughput and latency (`GET`/`SET`) is on par with the Redis 6.2.6

Redis 6.2.6:

```
$ redis-benchmark -t set,get -h localhost -p 6379 -n 1000000 -q
SET: 66128.82 requests per second, p50=0.351 msec
GET: 68662.45 requests per second, p50=0.367 msec
```

Keva:
```
$ redis-benchmark -t set,get -h localhost -p 6767 -n 1000000 -q
SET: 65312.52 requests per second, p50=0.391 msec
GET: 65703.02 requests per second, p50=0.391 msec
```

Machine:
```
Macbook Pro 16-inch 2019 (macOS Big Sur)
Intel i7-9750H (12) @ 2.60GHz, 16GB memory
```
