package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerGetRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerNameRequest;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.impl.commands.TicketCommands;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.bongbong.modl.minecraft.core.sync.SyncService;
import lombok.Getter;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static com.bongbong.modl.minecraft.core.Constants.QUERY_MOJANG;

@Getter
public class PluginLoader {
    private final ModlHttpClient httpClient;
    private final Cache cache;
    private final SyncService syncService;
    private final ChatMessageCache chatMessageCache;

    public PluginLoader(Platform platform, PlatformCommandRegister commandRegister, Path dataDirectory, ChatMessageCache chatMessageCache) {
        this.chatMessageCache = chatMessageCache;
        cache = new Cache();

        HttpManager httpManager = new HttpManager(
                Constants.API_KEY,
                Constants.API_URL
        );

        this.httpClient = httpManager.getHttpClient();

        // Initialize sync service
        Logger logger = Logger.getLogger("MODL-" + platform.getClass().getSimpleName());
        this.syncService = new SyncService(platform, httpClient, cache, logger);
        
        // Log configuration details
        logger.info("MODL Configuration:");
        logger.info("  API URL: " + Constants.API_URL);
        logger.info("  API Key: " + (Constants.API_KEY.length() > 8 ? 
            Constants.API_KEY.substring(0, 8) + "..." : "***"));
        logger.info("  Debug Mode: " + Constants.DEBUG_HTTP);
        
        // Start sync service
        syncService.start();

        LocaleManager localeManager = new LocaleManager();

        CommandManager<?, ?, ?, ?, ?, ?> commandManager = platform.getCommandManager();
        commandManager.enableUnstableAPI("help");

        commandManager.getCommandContexts().registerContext(AbstractPlayer.class, (c)
                -> fetchPlayer(c.popFirstArg(), platform, httpClient));

        commandManager.getCommandContexts().registerContext(Account.class, (c) -> fetchPlayer(c.popFirstArg(), httpClient));
//
        commandManager.registerCommand(new TicketCommands(platform, httpManager.getHttpClient(), Constants.PANEL_URL, localeManager, chatMessageCache));
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
    
    /**
     * Stop all services (should be called on plugin disable)
     */
    public void shutdown() {
        if (syncService != null) {
            syncService.stop();
        }
    }
}