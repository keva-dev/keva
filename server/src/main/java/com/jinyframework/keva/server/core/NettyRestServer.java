package com.jinyframework.keva.server.core;

import com.google.gson.Gson;
import com.jinyframework.keva.server.command.CommandService;

import lombok.AllArgsConstructor;
import reactor.netty.http.server.HttpServer;

public class NettyRestServer implements IServer {
    private final Gson gson = new Gson();
    private final CommandService commandService;

    public NettyRestServer(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void shutdown() {
        // Reactor Netty doesn't have Graceful shutdown
    }

    @Override
    public void run() {
        HttpServer.create().port(6768).route(routes -> {
            routes.post("/command/{command}", (req, res) ->
                res.sendString(req.receive().asString().map(body -> {
                    String command = req.param("command");
                    SimpleRequestJSON jsonReq = gson.fromJson(body, SimpleRequestJSON.class);
                    String commandStr = command + " " + jsonReq.value;
                    Object responseStr = commandService.handleCommand(null, commandStr);
                    SimpleResponseJSON responseJSON = new SimpleResponseJSON(responseStr != null ? responseStr : "null");
                    return gson.toJson(responseJSON);
                }))
            );
        }).bindNow();
    }

    public static class SimpleRequestJSON {
        private String value;
    }

    @AllArgsConstructor
    public static class SimpleResponseJSON {
        private Object result;
    }
}
