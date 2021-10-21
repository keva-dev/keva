package com.jinyframework.keva.server.factory;

import com.jinyframework.keva.server.config.ConfigHolder;
import lombok.Getter;
import lombok.Setter;

public class AppFactory {
    @Setter
    @Getter
    private static ConfigHolder configHolder;
}
