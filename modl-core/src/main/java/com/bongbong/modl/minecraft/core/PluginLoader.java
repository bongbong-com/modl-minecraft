package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.impl.commands.BanCommand;
import com.bongbong.modl.minecraft.core.impl.commands.KickCommand;
import com.bongbong.modl.minecraft.core.impl.commands.MuteCommand;
import lombok.Getter;

import java.nio.file.Path;

@Getter
public class PluginLoader {
    private final ModlHttpClient httpClient;
    private final Cache cache;

    public PluginLoader(Platform platform, PlatformCommandRegister commandRegister, Path dataDirectory) {
        cache = new Cache();

        HttpManager httpManager = new HttpManager(
                Constants.API_KEY,
                Constants.API_URL
        );

        this.httpClient = httpManager.getHttpClient();

        CommandManager<?, ?, ?, ?, ?, ?> commandManager = platform.getCommandManager();
        commandManager.enableUnstableAPI("help");

        commandRegister.register(new BanCommand(httpManager.getHttpClient()));
        commandRegister.register(new MuteCommand(httpManager.getHttpClient()));
        commandRegister.register(new KickCommand(httpManager.getHttpClient()));
    }


}