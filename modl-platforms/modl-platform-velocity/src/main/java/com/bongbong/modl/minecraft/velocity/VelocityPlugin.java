package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.PluginLoader;
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

import javax.inject.Inject;
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
        VelocityCommandManager commandManager = new VelocityCommandManager(this.server, this);
        new CirrusVelocity(this, server, server.getCommandManager()).init();

        VelocityPlatform platform = new VelocityPlatform(this.server, commandManager);
        PluginLoader loader = new PluginLoader(platform, new VelocityCommandRegister(commandManager), folder);

        server.getEventManager().register(this, new JoinListener(loader.getHttpClient(), loader.getCache(), getLogger()));
        server.getEventManager().register(this, new ChatListener(platform, loader.getCache()));
    }

    @Subscribe
    public synchronized void onProxyShutdown(ProxyShutdownEvent evt) {

    }

}
