package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import com.bongbong.modl.minecraft.core.plugin.PluginInfo;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
//import dev.simplix.cirrus.velocity.CirrusVelocity;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Plugin(id = PluginInfo.ID,
        name = PluginInfo.NAME,
        version = PluginInfo.VERSION,
        authors = { PluginInfo.AUTHOR },
        description = PluginInfo.DESCRIPTION,
        url = PluginInfo.URL,
        dependencies = @Dependency(id = "protocolize"))
public final class VelocityPlugin {

    private final PluginContainer plugin;
    private final ProxyServer server;
    private final Path folder;
    private final Logger logger;
    private Map<String, Object> configuration;
    private PluginLoader pluginLoader;

    @Inject
    public VelocityPlugin(PluginContainer plugin, ProxyServer server, @DataDirectory Path folder, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.folder = folder;
        this.logger = logger;
    }

    @Subscribe
    public synchronized void onProxyInitialize(ProxyInitializeEvent evt) {
        loadConfig();
        createLocaleFiles();
        
        // Get configuration values or use defaults
        String apiKey = getConfigString("api.key", "your-api-key-here");
        String apiUrl = getConfigString("api.url", "https://your-panel-url.com/api");
        
        HttpManager httpManager = new HttpManager(apiKey, apiUrl);
        
        VelocityCommandManager commandManager = new VelocityCommandManager(this.server, this);
//        new CirrusVelocity(this, server, server.getCommandManager()).init();

        VelocityPlatform platform = new VelocityPlatform(this.server, commandManager);
        ChatMessageCache chatMessageCache = new ChatMessageCache();
        this.pluginLoader = new PluginLoader(platform, new VelocityCommandRegister(commandManager), folder, chatMessageCache, httpManager);

        server.getEventManager().register(this, new JoinListener(pluginLoader.getHttpClient(), pluginLoader.getCache(), logger, chatMessageCache, platform, pluginLoader.getSyncService()));
        server.getEventManager().register(this, new ChatListener(platform, pluginLoader.getCache(), chatMessageCache));
    }

    @Subscribe
    public synchronized void onProxyShutdown(ProxyShutdownEvent evt) {
        if (pluginLoader != null) {
            pluginLoader.shutdown();
        }
    }
    
    private void loadConfig() {
        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
            
            Path configFile = folder.resolve("config.yml");
            
            // Create default config if it doesn't exist
            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
            }
            
            // Load configuration
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                Yaml yaml = new Yaml();
                this.configuration = yaml.load(inputStream);
                
                if (this.configuration == null) {
                    logger.warn("Configuration file is empty, using defaults");
                    this.configuration = Map.of();
                }
            }
            
            logger.info("Configuration loaded successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            this.configuration = Map.of(); // Use empty config as fallback
        }
    }
    
    private void createDefaultConfig(Path configFile) throws IOException {
        String defaultConfig = """
                # MODL Minecraft Plugin Configuration
                # 
                # API Configuration - Get these values from your MODL panel
                api:
                  # Your API key from the panel
                  key: "your-api-key-here"
                  # Your panel's API URL (usually https://yourserver.modl.gg/api)
                  url: "https://your-panel-url.com/api"
                  
                # Panel Configuration
                panel:
                  # Your panel's base URL (usually https://yourserver.modl.gg)
                  url: "https://your-panel-url.com"
                  
                # Server Configuration
                server:
                  # Name of this server (used for identification in the panel)
                  name: "Velocity-Proxy"
                """;
        
        Files.writeString(configFile, defaultConfig);
        logger.info("Created default configuration file at: " + configFile);
    }
    
    @SuppressWarnings("unchecked")
    private String getConfigString(String path, String defaultValue) {
        if (configuration == null) {
            return defaultValue;
        }
        
        String[] keys = path.split("\\.");
        Object current = configuration;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return defaultValue;
            }
        }
        
        return current instanceof String ? (String) current : defaultValue;
    }
    
    private void createLocaleFiles() {
        try {
            // Create locale directory
            Path localeDir = folder.resolve("locale");
            if (!Files.exists(localeDir)) {
                Files.createDirectories(localeDir);
                logger.info("Created locale directory at: " + localeDir);
            }
            
            // Create default en_US.yml if it doesn't exist
            Path enUsFile = localeDir.resolve("en_US.yml");
            if (!Files.exists(enUsFile)) {
                // Copy the default locale from resources
                try (InputStream defaultLocale = getClass().getResourceAsStream("/locale/en_US.yml")) {
                    if (defaultLocale != null) {
                        Files.copy(defaultLocale, enUsFile);
                        logger.info("Created default locale file at: " + enUsFile);
                    } else {
                        logger.warn("Default locale resource not found in JAR");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to create locale files", e);
        }
    }

}
