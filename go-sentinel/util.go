package sentinel

import "sync"

func locked(mu *sync.Mutex, f func()) {
	mu.Lock()
	defer mu.Unlock()
	f()
}
