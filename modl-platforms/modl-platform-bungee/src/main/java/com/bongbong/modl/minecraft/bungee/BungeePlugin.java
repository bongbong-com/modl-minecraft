package com.bongbong.modl.minecraft.bungee;

import co.aikar.commands.BungeeCommandManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import dev.simplix.cirrus.bungee.CirrusBungee;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin {

    @Override
    public synchronized void onEnable() {
        BungeeCommandManager commandManager = new BungeeCommandManager(this);
        new CirrusBungee(this).init();

        new PluginLoader(new BungeePlatform(commandManager), new BungeeCommandRegister(commandManager), getDataFolder().toPath());
    }

    @Override
    public synchronized void onDisable() {
    }
}
