package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import dev.simplix.cirrus.spigot.CirrusSpigot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class SpigotPlugin extends JavaPlugin {
    
    private PluginLoader loader;

    @Override
    public synchronized void onEnable() {
        saveDefaultConfig();
        createLocaleFiles();
        
        HttpManager httpManager = new HttpManager(
                getConfig().getString("api.key"),
                getConfig().getString("api.url")
        );

        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        new CirrusSpigot(this).init();

        SpigotPlatform platform = new SpigotPlatform(commandManager, getLogger());
        ChatMessageCache chatMessageCache = new ChatMessageCache();
        this.loader = new PluginLoader(platform, new SpigotCommandRegister(commandManager), getDataFolder().toPath(), chatMessageCache, httpManager);
        getServer().getPluginManager().registerEvents(new SpigotListener(platform, loader.getCache(), loader.getHttpClient(), chatMessageCache, loader.getSyncService()), this);
    }

    @Override
    public synchronized void onDisable() {
        if (loader != null) {
            loader.shutdown();
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
                try (InputStream defaultLocale = getResource("locale/en_US.yml")) {
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