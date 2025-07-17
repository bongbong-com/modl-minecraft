package com.bongbong.modl.minecraft.core;


import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerGetRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerNameRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerNameResponse;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.impl.commands.TicketCommands;
import com.bongbong.modl.minecraft.core.impl.commands.PlayerLookupCommand;
import com.bongbong.modl.minecraft.core.impl.commands.punishments.PunishCommand;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.bongbong.modl.minecraft.core.sync.SyncService;
import lombok.Getter;

import java.nio.file.Files;
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
        this(platform, commandRegister, dataDirectory, chatMessageCache, new HttpManager(Constants.API_KEY, Constants.API_URL));
    }
    
    public PluginLoader(Platform platform, PlatformCommandRegister commandRegister, Path dataDirectory, ChatMessageCache chatMessageCache, HttpManager httpManager) {
        this.chatMessageCache = chatMessageCache;
        cache = new Cache();

        this.httpClient = httpManager.getHttpClient();

        // Initialize locale manager with support for external locale files
        LocaleManager localeManager = new LocaleManager();
        Logger logger = Logger.getLogger("MODL-" + platform.getClass().getSimpleName());
        
        // Try to load locale from external file if it exists
        Path localeFile = dataDirectory.resolve("locale").resolve("en_US.yml");
        if (Files.exists(localeFile)) {
            logger.info("Loading locale from external file: " + localeFile);
            localeManager.loadFromFile(localeFile);
        }

        // Initialize sync service

        this.syncService = new SyncService(platform, httpClient, cache, logger, localeManager);
        
        // Log configuration details
        logger.info("MODL Configuration:");
        logger.info("  API URL: " + Constants.API_URL);
        logger.info("  API Key: " + (Constants.API_KEY.length() > 8 ? 
            Constants.API_KEY.substring(0, 8) + "..." : "***"));
        logger.info("  Debug Mode: " + Constants.DEBUG_HTTP);
        
        // Start sync service
        syncService.start();

        CommandManager<?, ?, ?, ?, ?, ?> commandManager = platform.getCommandManager();
        commandManager.enableUnstableAPI("help");

        commandManager.getCommandContexts().registerContext(AbstractPlayer.class, (c)
                -> fetchPlayer(c.popFirstArg(), platform, httpClient));

        commandManager.getCommandContexts().registerContext(Account.class, (c) -> fetchPlayer(c.popFirstArg(), httpClient));
//
        commandManager.registerCommand(new TicketCommands(platform, httpManager.getHttpClient(), Constants.PANEL_URL, localeManager, chatMessageCache));
        
        // Register player lookup command
        commandManager.registerCommand(new PlayerLookupCommand(httpManager.getHttpClient(), platform, cache, localeManager));
        
        // Register punishment command with tab completion
        PunishCommand punishCommand = new PunishCommand(httpManager.getHttpClient(), platform, cache, localeManager);
        commandManager.registerCommand(punishCommand);
        
        // Set up punishment types tab completion
        commandManager.getCommandCompletions().registerCompletion("punishment-types", c -> 
            punishCommand.getPunishmentTypeNames()
        );
        
        // Initialize punishment types cache
        punishCommand.initializePunishmentTypes();
        
        // Register manual punishment commands
        commandManager.registerCommand(new com.bongbong.modl.minecraft.core.impl.commands.punishments.BanCommand(httpManager.getHttpClient(), platform, cache, localeManager));
        commandManager.registerCommand(new com.bongbong.modl.minecraft.core.impl.commands.punishments.MuteCommand(httpManager.getHttpClient(), platform, cache, localeManager));
        commandManager.registerCommand(new com.bongbong.modl.minecraft.core.impl.commands.punishments.KickCommand(httpManager.getHttpClient(), platform, cache, localeManager));
        commandManager.registerCommand(new com.bongbong.modl.minecraft.core.impl.commands.punishments.BlacklistCommand(httpManager.getHttpClient(), platform, cache, localeManager));
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
        try {
            PlayerNameResponse response = httpClient.getPlayer(new PlayerNameRequest(target)).join();
            if (response != null && response.isSuccess()) {
                return response.getPlayer();
            }
        } catch (Exception e) {
            // Log error but don't crash - return null to indicate player not found
            System.err.println("[MODL] Error fetching player by name '" + target + "': " + e.getMessage());
        }
        return null;
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