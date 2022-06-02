---
sidebar_position: 4
---

# Security

By default, Keva binds to all the interfaces and has no authentication at all.
Similar to Redis, Keva is designed to be accessed by trusted clients inside trusted environments.
This means that usually it is not a good idea to expose the Keva instance directly to the internet or, in general,
to an environment where untrusted clients can directly access the Keva TCP port or UNIX socket.

Access to the Keva port should be denied to everybody but trusted clients in the network, so the servers running Redis
should be directly accessible only by the computers implementing the application using Redis.

## Enable authentication

Use the `--requirepass yourpassword` argument to enable authentication, so that clients
will require authenticating using the AUTH command.

```shell
keva-server --requirepass yourpassword
```

If you enable the `--requirepass` argument, Keva will deny any command executed by the just connected clients,
unless the connection gets authenticated via `AUTH` command.

If the password provided via AUTH matches the password in the configuration file, the server replies with the OK status
code and starts accepting commands. Otherwise, an error is returned and the clients needs to try a new password.

## Security notice

Similar to Redis, because of the high performance nature, it is possible to try a lot of passwords in parallel
in very short time, so make sure to generate a strong and very long password so that this attack is infeasible.
