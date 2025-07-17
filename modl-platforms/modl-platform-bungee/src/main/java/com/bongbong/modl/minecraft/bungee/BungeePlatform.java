package com.bongbong.modl.minecraft.bungee;

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import dev.simplix.cirrus.player.CirrusPlayerWrapper;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class BungeePlatform implements Platform {
    private final BungeeCommandManager commandManager;
    private final Logger logger;



    @Override
    public void broadcast(String string) {

    }

    @Override
    public void staffBroadcast(String string) {

    }

    @Override
    public void disconnect(UUID uuid, String message) {

    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null && player.isConnected()) {
            player.sendMessage(message);
        }
    }
    
    @Override
    public void sendJsonMessage(UUID uuid, String jsonMessage) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null && player.isConnected()) {
            // Parse and send JSON message using BungeeCord's ComponentSerializer
            player.sendMessage(net.md_5.bungee.chat.ComponentSerializer.parse(jsonMessage));
        }
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return false;
    }

    @NotNull
    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }

    @Override
    public AbstractPlayer getAbstractPlayer(UUID uuid, boolean queryMojang) {
        return null;
    }

    @Override
    public AbstractPlayer getAbstractPlayer(String username, boolean queryMojang) {
        return null;
    }

    @Override
    public CirrusPlayerWrapper getPlayerWrapper(UUID uuid) {
        return null;
    }

    @Override
    public Collection<AbstractPlayer> getOnlinePlayers() {
        return List.of();
    }

    @Override
    public AbstractPlayer getPlayer(UUID uuid) {
        return null;
    }

    @Override
    public int getMaxPlayers() {
        return 0;
    }

    @Override
    public String getServerVersion() {
        return "";
    }

    @Override
    public void runOnMainThread(Runnable task) {

    }

    @Override
    public void kickPlayer(AbstractPlayer player, String reason) {
        ProxiedPlayer bungeePlayer = ProxyServer.getInstance().getPlayer(player.getUuid());
        if (bungeePlayer != null && bungeePlayer.isConnected()) {
            bungeePlayer.disconnect(new TextComponent(reason));
        }
    }

    @Override
    public String getServerName() {
        return "bungee-proxy"; // Default server name, can be made configurable
    }

    public Logger getLogger() {
        return logger;
    }
}
