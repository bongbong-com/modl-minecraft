package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerGetRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerNameRequest;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import lombok.Getter;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.bongbong.modl.minecraft.core.Constants.QUERY_MOJANG;

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

        commandManager.getCommandContexts().registerContext(AbstractPlayer.class, (c)
                -> fetchPlayer(c.popFirstArg(), platform, httpClient));

        commandManager.getCommandContexts().registerContext(Account.class, (c) -> fetchPlayer(c.popFirstArg(), httpClient));
//
//        commandRegister.register(new BanCommand(httpManager.getHttpClient()));
    }

    public static AbstractPlayer fetchPlayer(String target, Platform platform, ModlHttpClient httpClient) {
        AbstractPlayer player = platform.getAbstractPlayer(target, false);
        if (player != null) return player;

        Account account = httpClient.getPlayer(new PlayerNameRequest(target)).join().getPlayer();

        if (account != null)
            return new AbstractPlayer(account.getMinecraftUuid(), "test", false);

        if (account == null && QUERY_MOJANG)
            return platform.getAbstractPlayer(target, true);

        return null;
    }

    public static Account fetchPlayer(String target, ModlHttpClient httpClient) {
        return httpClient.getPlayer(new PlayerNameRequest(target)).join().getPlayer();
    }
}