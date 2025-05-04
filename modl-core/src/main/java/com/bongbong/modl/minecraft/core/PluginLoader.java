package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class PluginLoader {
    private final Cache cache;

    public PluginLoader(Platform platform, PlatformCommandRegister commandRegister, Path dataDirectory) {
        cache = new Cache();

        CommandManager<?, ?, ?, ?, ?, ?> commandManager = platform.getCommandManager();
        commandManager.enableUnstableAPI("help");

    }
}