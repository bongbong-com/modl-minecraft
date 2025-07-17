package com.bongbong.modl.minecraft.bungee;

import co.aikar.commands.BungeeCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import dev.simplix.cirrus.bungee.CirrusBungee;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Getter
public class BungeePlugin extends Plugin {
    private Configuration configuration;
    private PluginLoader loader;

    @Override
    public synchronized void onEnable() {
        loadConfig();
        createLocaleFiles();
        
        // Get configuration values or use defaults
        String apiKey = configuration.getString("api.key", "your-api-key-here");
        String apiUrl = configuration.getString("api.url", "https://your-panel-url.com/api");
        
        HttpManager httpManager = new HttpManager(apiKey, apiUrl);

        BungeeCommandManager commandManager = new BungeeCommandManager(this);
        new CirrusBungee(this).init();

        BungeePlatform platform = new BungeePlatform(commandManager, getLogger());
        ChatMessageCache chatMessageCache = new ChatMessageCache();
        this.loader = new PluginLoader(platform, new BungeeCommandRegister(commandManager), getDataFolder().toPath(), chatMessageCache, httpManager);
        getProxy().getPluginManager().registerListener(this, new BungeeListener(platform, loader.getCache(), loader.getHttpClient(), chatMessageCache, loader.getSyncService()));
    }

    @Override
    public synchronized void onDisable() {
        if (loader != null) {
            loader.shutdown();
        }
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
                // Create default configuration
                createDefaultConfig(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig(File file) {
        try {
            Configuration defaultConfig = new Configuration();
            Configuration apiSection = new Configuration();
            apiSection.set("key", "your-api-key-here");
            apiSection.set("url", "https://your-panel-url.com/api");
            
            Configuration panelSection = new Configuration();
            panelSection.set("url", "https://your-panel-url.com");
            
            Configuration serverSection = new Configuration();
            serverSection.set("name", "Bungee-Proxy");
            
            defaultConfig.set("api", apiSection);
            defaultConfig.set("panel", panelSection);
            defaultConfig.set("server", serverSection);
            
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(defaultConfig, file);
            getLogger().info("Created default configuration file");
        } catch (IOException e) {
            getLogger().severe("Failed to create default configuration: " + e.getMessage());
        }
    }
    
    private void createLocaleFiles() {
        try {
            // Create locale directory
            File localeDir = new File(getDataFolder(), "locale");
            if (!localeDir.exists()) {
                localeDir.mkdirs();
                getLogger().info("Created locale directory at: " + localeDir.getPath());
            }
            
            // Create default en_US.yml if it doesn't exist
            File enUsFile = new File(localeDir, "en_US.yml");
            if (!enUsFile.exists()) {
                // Copy the default locale from resources
                try (InputStream defaultLocale = getResourceAsStream("locale/en_US.yml")) {
                    if (defaultLocale != null) {
                        Files.copy(defaultLocale, enUsFile.toPath());
                        getLogger().info("Created default locale file at: " + enUsFile.getPath());
                    } else {
                        getLogger().warning("Default locale resource not found in JAR");
                    }
                }
            }
        } catch (IOException e) {
            getLogger().severe("Failed to create locale files: " + e.getMessage());
        }
    }
}
