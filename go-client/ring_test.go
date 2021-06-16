package kevago

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func setupDefaultRing(t *testing.T) *Ring {
	r, err := NewDefaultRing([]string{"localhost:6767", "localhost:6768"})
	assert.NoError(t, err)
	return r
}

func TestRingCRUD(t *testing.T) {
	cl := setupDefaultRing(t)
	defer cl.Close()

	ret, err := cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

	ret, err = cl.Set("key1", "value1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "value1", ret)

	ret, err = cl.Delete("key1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key1")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)
}

func TestRingExpire(t *testing.T) {
	cl := setupDefaultRing(t)
	defer cl.Close()

	ret, err := cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

	ret, err = cl.Set("key2", "value1")
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)

	ret, err = cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "value1", ret)

	ret, err = cl.Expire("key2", 2000*time.Millisecond)
	assert.NoError(t, err)
	assert.Equal(t, "1", ret)
	time.Sleep(2000 * time.Millisecond)

	ret, err = cl.Get("key2")
	assert.NoError(t, err)
	assert.Equal(t, "null", ret)

}

// func TestRingPing(t *testing.T) {
// 	cl := setupDefaultRing(t)
// 	defer cl.Close()
// 	err := cl.Ping()
// 	assert.NoError(t, err)

// }

// func TestRingInfo(t *testing.T) {
// 	cl := setupDefaultRing(t)
// 	defer cl.Close()
// 	_, err := cl.Info()
// 	assert.NoError(t, err)
// }
