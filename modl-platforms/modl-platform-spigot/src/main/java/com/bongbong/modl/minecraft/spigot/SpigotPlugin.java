package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import dev.simplix.cirrus.spigot.CirrusSpigot;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPlugin extends JavaPlugin {

    @Override
    public synchronized void onEnable() {
        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        new CirrusSpigot(this).init();

        new PluginLoader(new SpigotPlatform(commandManager), new SpigotCommandRegister(commandManager), getDataFolder().toPath());
    }

    @Override
    public synchronized void onDisable() {
    }
}