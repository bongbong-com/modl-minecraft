package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
import com.bongbong.modl.minecraft.core.plugin.PluginInfo;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.simplix.cirrus.velocity.CirrusVelocity;
import lombok.Getter;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

@Plugin(id = PluginInfo.ID,
        name = PluginInfo.NAME,
        version = PluginInfo.VERSION,
        authors = { PluginInfo.AUTHOR },
        description = PluginInfo.DESCRIPTION,
        url = PluginInfo.URL,
        dependencies = @Dependency(id = "protocolize"))
@Getter
public final class VelocityPlugin {

    private final PluginContainer plugin;
    private final ProxyServer server;
    private final Path folder;
    private final Logger logger;
    private Map<String, Object> configuration;

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
        HttpManager httpManager = new HttpManager(
                (String) ((Map<String, Object>) configuration.get("api")).get("key"),
                (String) ((Map<String, Object>) configuration.get("api")).get("url")
        );

        VelocityCommandManager commandManager = new VelocityCommandManager(this.server, this);
        new CirrusVelocity(this, server, server.getCommandManager()).init();

        VelocityPlatform platform = new VelocityPlatform(httpManager, commandManager);
        PluginLoader loader = new PluginLoader(platform, new VelocityCommandRegister(commandManager), folder);
        
        JoinListener joinListener = new JoinListener(platform);
        server.getEventManager().register(this, joinListener);
        server.getEventManager().register(this, new ChatListener(platform, joinListener.getPunishmentCache()));
    }

    @Subscribe
    public synchronized void onProxyShutdown(ProxyShutdownEvent evt) {

    }

    private void loadConfig() {
        File file = new File(folder.toFile(), "config.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (InputStream in = new FileInputStream(file)) {
            configuration = new Yaml().load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
