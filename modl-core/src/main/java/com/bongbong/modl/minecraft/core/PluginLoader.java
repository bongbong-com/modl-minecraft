package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.core.commands.BanCommand;
import com.bongbong.modl.minecraft.core.commands.KickCommand;
import com.bongbong.modl.minecraft.core.commands.MuteCommand;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class PluginLoader {
    private final Cache cache;

    public PluginLoader(Platform platform, PlatformCommandRegister commandRegister, Path dataDirectory) {
        cache = new Cache();

        CommandManager<?, ?, ?, ?, ?, ?> commandManager = platform.getCommandManager();
        commandManager.enableUnstableAPI("help");

        // Register punishment commands using the provided PlatformCommandRegister
        registerPunishmentCommands(commandRegister, platform);
    }
    
    private void registerPunishmentCommands(PlatformCommandRegister commandRegister, Platform platform) {
        commandRegister.register(new BanCommand(platform.getHttpClient()));
        commandRegister.register(new MuteCommand(platform.getHttpClient()));
        commandRegister.register(new KickCommand(platform.getHttpClient()));
    }
}