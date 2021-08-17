# Usage

## PING

- Returns "PONG". Often used to check connection to server.

```command
localhost/127.0.0.1:6767> ping
PONG
```

## GET key

- Returns value of key. If key or value doesn't exist, returns "null".

```command
localhost/127.0.0.1:6767> get mykey
null
localhost/127.0.0.1:6767> set mykey myvalue
1
localhost/127.0.0.1:6767> get mykey
myvalue
```

## SET key value

- Set value for key. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> set mykey myvalue
1
```

## DEL key

- Remove the key value pair. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> set a b
1
localhost/127.0.0.1:6767> get a
b
localhost/127.0.0.1:6767> del a
1
localhost/127.0.0.1:6767> get a
null
```

## EXPIRE key expireTimeInMilliSecond

- Set expire time for key. Returns 1 if successful, 0 otherwise.

```command
localhost/127.0.0.1:6767> expire mykey 1000
1
```

## INFO

- Returns information about the server.

```command
localhost/127.0.0.1:6767> info
{threads:=6, clients:=1}
```
