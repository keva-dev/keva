---
slug: implement-nio-server-in-java
title: Implement NIO server in Java
authors: [blu]
tags: [network]
---

When developing the server for a learning project database [Keva](https://github.com/tuhuynh27/keva) I got the chance to learn a bit more about non-blocking I/O (NIO) and their libraries in Java. [Netty](https://github.com/netty/netty) implementations, I finally was able to implement a working prototype by myself. You can check out the [source code here](https://github.com/axblueblader/nio-server-demo) (it's only a few short files).

## The basics

There are two basic parts for this problem, first the NIO part, and then the server part.

Fundamentally, NIO from the application level just means not waiting around. For example, when we call a "read" method on a socket, the results are returned immediately whether we can read it or not, the process continues to work on the next line of code instead of waiting for data. We can pass in a callback function to handle the results whenever it's ready.

The server's primary logic is to take in messages from clients, process them, and return the results to those clients, all via the network.

In a traditional blocking server. When we read bytes from a connection, the server will have to wait for the whole message to arrive before processing, since we can only read a limited amount of data in the buffer. To handle multiple clients, we spawn multiple threads.

For the NIO server, a thread doesn't need to stop and wait for the whole message, so we can read what we can, then continue to do other stuff, and come back to read again when there is new data. The main problem is how to we manage bytes being read asynchronously to construct correct messages. This is the problem I struggled with and finally managed to solve (but probably not in the optimal way though).

## The idea

So my idea to this problem is using the event-driven architecture. Specifically, we can have 2 thread groups, the main thread group, which is responsible for accepting connection (this can just be 1 thread), and the worker thread group, which is responsible for reading, parsing, and writing the results to the socket. The worker group is very importantly since I'm using it for executing read writes but it's also used by Java's NIO2 library to invoke completion handlers.

For example purposes, this will be a TCP echo server, and messages will use the \n line ending character as delimiter between them.

So what happens when data arrives? Well it could be in any of these forms below:

1. part\n : It could be a full message or the last part of a message.
2. a partial mess : A partial message, we need a way to store it while waiting for the rest of the message to arrive.
3. last part\n mess 2\n mess 3\n start new : We can expect to receive many messages or portion of them in a single socket read as well.

## The flow

So the process will look like this:

### Bootstraping the server

- We start the server by initiating threads used as worker for the socket channels as well as our own processing.

```java
private final ExecutorService worker = Executors.newFixedThreadPool(4);
private final ExecutorService main = Executors.newFixedThreadPool(1);
```

Then bind the socket to the port and start accepting connections. Also we need to make the server run forever, here I just used a System.in.read to achieve that.

```java
group = AsynchronousChannelGroup.withThreadPool(worker);
server = AsynchronousServerSocketChannel.open(group);
final int port = 8989;
server.bind(new InetSocketAddress(port));
main.submit(() -> server.accept(null, new AcceptHandler(server, main, worker)));
System.out.println("Server started at: " + port);
System.in.read();
```

### When client is connected

- The thread that invokes the handler will submit a task for accepting connection again.
- The accept completion handler will also initialize a byte buffer, a queue for storing completed message to write and submit a task for reading the socket.
- The messBuf will be used to store the current unfinished message.
- The writeQueue need to be thread-safe for use in both the thread reading as well as the thread trying to write.

```java
public void completed(AsynchronousSocketChannel channel, Object attachment) {
    main.submit(() -> server.accept(null, this));

    final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
    final StringBuffer messBuf = new StringBuffer();
    final Queue<String> writeQueue = new ConcurrentLinkedQueue<>();
    worker.submit(() -> channel.read(buffer, null,
                                     new ReadHandler(worker, channel, buffer, messBuf, writeQueue)));
}
```

### When a read finishes

- We process the data based on the cases described above. We loop through the buffer, when the delimiter('\n') is reached, the previous characters are put into the current buffer and that buffer is considered a finished message so we put it in the writeQueue for the writer worker to consume later on. After processing is finished, we clear the buffer and submit a new read task. Here's the snippet:

```java
int startIdx = 0;
int endIdx;
while (frame.indexOf(DELIM, startIdx) != -1) {
    endIdx = frame.indexOf(DELIM, startIdx) + 1;
    messBuf.append(frame, startIdx, endIdx);
    writeQueue.add(messBuf.toString());
    this.messBuf = new StringBuffer();
    startIdx = endIdx;
}
messBuf.append(frame, startIdx, frame.length());
channel.read(buffer, null, this);
```

- Everytime we finish a read, we can check to see if the `writeQueue` has any finished messages for the writer consume. If it does, submit a task to consume it.

### When a write finishes

- The response is not guaranteed to be written completely in one write, so if there is still something to write, we to continue write it.

If the current message is really finished, we still need to check the `writeQueue` again since on the read side, 1 read will trigger at most only 1 write task. However, the stream is continuous and 1 read might contain multiple messages. Therefore the check after finished writing is necessary. We could maybe count the number messages in the read handler then submit as much write task.

```java
public void completed(Integer bytesWritten, Object attachment) {
    if (bytesWritten > 0 && writeBuf.hasRemaining()) {// write not finished, continue writing this buffer
        worker.submit(() -> channel.write(writeBuf, null, this));
    } else {
        // Continue to write from the queue
        String message = writeQueue.peek();
        if (message != null) {
            writeQueue.remove();
            ByteBuffer writeBuf = ByteBuffer.wrap(message.getBytes());
            channel.write(writeBuf, null, new WriteHandler(worker, channel, writeBuf, writeQueue));
        }
    }
}
```

## The result

Well the implementation worked (as least it for the test suite I wrote for it):

- I tried sending messages smaller and bigger than the buffer size (which is 8 bytes by default):

```java
@Test
void buf8_echo1Less8_success() throws Exception {
    final SocketClient client = startClient();
    final String abcde = client.exchange("abcde");
    client.disconnect();

    assertEquals("abcde", abcde);
}
```

- Tested it with multiple messages:

```java
final SocketClient client = startClient();
final List<String> abcd = client.exchange("12345678\n987654321\nabc\nd", 4);
client.disconnect();

assertEquals("12345678", abcd.get(0));
assertEquals("987654321", abcd.get(1));
assertEquals("abc", abcd.get(2));
assertEquals("d", abcd.get(3));
```

- Tested with many clients:

```java
final ExecutorService executor = Executors.newFixedThreadPool(3);
final int taskNum = 10000;
for (int i = 0; i < taskNum; i++) {
  tasks.add(() -> {
      final SocketClient client = startClient();
      final String res = client.exchange(mess16);
      client.disconnect();
      return res;
  });
}
```

Maybe the way I test is kinda wrong, if you notice a mistake, I'm open to feedbacks. This is just a way to implement it, and it's actually a very naive, slow one. I used string mainly in my code so I had to convert the buffer to string. A better approach would be to deal with the bytes directly. Also the way I implemented the `writeQueue` required bytes being copied from buffers to the string holders. Modern NIO servers are implemented with zero-copy techniques for dealing with the buffers, for example Netty have their own type of buffers that stores pointers to the original buffers used to read. That could be topic for more research however I'm quite satisfied with these results for now, hope this was useful to you.
