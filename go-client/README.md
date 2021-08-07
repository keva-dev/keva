# KEVAGO - A dead simple Go client for Keva

## Pooled client

### Default options

```go
client,_ := NewDefaultClient()
ret, _ = cl.Set("key1", "value1")
fmt.Println(ret)
ret, _ := cl.Get("key1")
fmt.Println(ret)
```

### More options

Create a client to Keva server, which under the hood holds a connection pool. Each time a command is called, client gets a connecton from pool and use it.

```go
poolOpts := Options{
    PoolTimeout: time.Second, // max time to wait to get new connection from pool
    PoolSize:    20, // max number of connection can get from the pool
    MinIdleConn: 5,
    Address:     "localhost:6767",
    Dialer: func(ctx context.Context,addr string) (net.Conn, error) { //Must define dialer func
        conn, err := net.Dial("tcp", addr)
        if err != nil {
            return nil, err
        }
        return conn, err
    },
    IdleTimeout:        time.Minute * 5, // if connection lives longer than 5 minutes, it is removable
    MaxConnAge:         time.Minute * 10, // all connections cannot live longer than this
    IdleCheckFrequency: time.Minute * 5, // reap staled connections after 5 minutes
}
client, _ := NewClient(Config{
    Pool: poolOpts,
})
ret, _ = cl.Set("key1", "value1")
fmt.Println(ret)
ret, _ := cl.Get("key1")
fmt.Println(ret)
```

## Ring Client

Connect to multiple Keva nodes and load balance request by consistent hashing the first key of a command.
If one of the node goes down, the ring ignore that instance and rebalance using the remaining nodes, but it won't guarantee consistency. This means 
the datas allocated to the previous set of nodes are no longer belong to the same instance.

```go
client,_ := NewDefaultRing([]string{"localhost:6767","localhost:6768"})
ret, _ = cl.Set("key1", "value1")
fmt.Println(ret)
ret, _ := cl.Get("key1")
fmt.Println(ret)
```

## Credit

- [goredis](https://github.com/go-redis/redis)