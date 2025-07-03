package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import dev.simplix.cirrus.spigot.CirrusSpigot;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlugin extends JavaPlugin {

    @Override
    public synchronized void onEnable() {
        saveDefaultConfig();
        HttpManager httpManager = new HttpManager(
                getConfig().getString("api.key"),
                getConfig().getString("api.url")
        );

        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        new CirrusSpigot(this).init();

        SpigotPlatform platform = new SpigotPlatform(commandManager, getLogger());
        ChatMessageCache chatMessageCache = new ChatMessageCache();
        PluginLoader loader = new PluginLoader(platform, new SpigotCommandRegister(commandManager), getDataFolder().toPath(), chatMessageCache);
        getServer().getPluginManager().registerEvents(new SpigotListener(platform, loader.getCache(), loader.getHttpClient(), chatMessageCache), this);
    }

    @Override
    public synchronized void onDisable() {
    }
}