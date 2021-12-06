# Security

:::warning WIP
This feature is under development
:::

Keva is designed to be accessed by trusted clients inside trusted environments.
This means that usually it is not a good idea to expose the Keva instance directly to the internet or, in general,
to an environment where untrusted clients can directly access the Keva TCP port or UNIX socket.

Access to the Keva port should be denied to everybody but trusted clients in the network, so the servers running Redis
should be directly accessible only by the computers implementing the application using Redis.
