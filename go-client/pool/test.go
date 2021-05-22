package pool

import (
	"context"
	"fmt"
	"time"

	"github.com/go-redis/redis/v8"
)

//
func st() {
	c := redis.NewClient(&redis.Options{})
	rs, _ := c.BRPop(context.Background(), time.Second, "hello").Result()
	fmt.Println(rs)
	r := redis.NewRing(&redis.RingOptions{})

	_ = r.BRPop(context.Background(), time.Second, "hello")
	cl := redis.NewClusterClient(&redis.ClusterOptions{})
	_ = cl.BRPop(context.Background(), time.Second, "hello")
}
