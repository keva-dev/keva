# KEVAGO - A dead simple Go client for Keva

## Usage

Create a client to Keva server, which under the hood holds a connection pool.
Each time a command is called, client gets a connecton from pool and use it.

```go
poolOpts := pool.Options{
    PoolTimeout: time.Second, // max time to wait to get new connection from pool
    PoolSize:    20, // max number of connection can get from the pool
    MinIdleConn: 5,
    Dialer: func(ctx context.Context) (net.Conn, error) { //Must define dialer func
        conn, err := net.Dial("tcp", "localhost:6767")
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
