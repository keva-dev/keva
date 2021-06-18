package main

import (
	"strings"
	// . "github.com/go-redis/redis/v8"
)

func main() {

	// fmt.Println(Key("{get}"))
	// NewCmd(context.Background(), "")
}
func Key(key string) string {
	if s := strings.IndexByte(key, '{'); s > -1 {
		if e := strings.IndexByte(key[s+1:], '}'); e > 0 {
			return key[s+1 : s+e+1]
		}
	}
	return key
}
