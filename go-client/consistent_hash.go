package kevago

import (
	"github.com/cespare/xxhash/v2"
	"github.com/dgryski/go-rendezvous"
)

type RendezvousHasher struct {
	*rendezvous.Rendezvous
	// shards []string
}

func (s RendezvousHasher) Get(input string) string {
	return s.Rendezvous.Lookup(input)
}

func newRendezvous(shards []string) RendezvousHasher {
	return RendezvousHasher{
		rendezvous.New(shards, xxhash.Sum64String),
	}
}
