package com.bongbong.modl.minecraft.bungee;

import co.aikar.commands.BungeeCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import dev.simplix.cirrus.bungee.CirrusBungee;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

@Getter
public class BungeePlugin extends Plugin {
    private Configuration configuration;

    @Override
    public synchronized void onEnable() {
        loadConfig();
        HttpManager httpManager = new HttpManager(
                configuration.getString("api.key"),
                configuration.getString("api.url")
        );

        BungeeCommandManager commandManager = new BungeeCommandManager(this);
        new CirrusBungee(this).init();

        BungeePlatform platform = new BungeePlatform(commandManager, getLogger());
        new PluginLoader(platform, new BungeeCommandRegister(commandManager), getDataFolder().toPath());
        getProxy().getPluginManager().registerListener(this, new BungeeListener(platform));
    }

    @Override
    public synchronized void onDisable() {
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
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
}
