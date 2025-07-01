package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.util.WebPlayer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.simplix.cirrus.player.CirrusPlayerWrapper;
//import dev.simplix.cirrus.velocity.wrapper.VelocityPlayerWrapper;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@RequiredArgsConstructor
public class VelocityPlatform implements Platform {
    private final ProxyServer server;
    private final VelocityCommandManager commandManager;

    private static Component get(String string) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(string);
    }

    @Override
    public void broadcast(String string) {
        server.getAllPlayers().forEach(player -> player.sendMessage(get(string)));
    }

    @Override
    public void staffBroadcast(String string) {
        server.getAllPlayers().forEach(player -> {
            if (player.hasPermission(Constants.PERMISSION)) player.sendMessage(get(string));
        });
    }

    @Override
    public void disconnect(UUID uuid, String message) {
        server.getPlayer(uuid).orElseThrow().disconnect(get(message));
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        server.getPlayer(uuid).orElseThrow().sendMessage(get(message));
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return server.getPlayer(uuid).isPresent();
    }

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }

    private Player getPlayer(String username) {
        return server.getPlayer(username).isPresent() ? server.getPlayer(username).get() : null;
    }

    private Player getPlayer(UUID uuid) {
        return server.getPlayer(uuid).isPresent() ? server.getPlayer(uuid).get() : null;
    }

    @Override
    public AbstractPlayer getAbstractPlayer(UUID uuid, boolean queryMojang) {
        Player player = getPlayer(uuid);

        if (player != null) return new AbstractPlayer(player.getUniqueId(), player.getUsername(), true);
        if (!queryMojang) return null;

        WebPlayer webPlayer;
        try {
            webPlayer = WebPlayer.get(uuid);
        } catch (IOException ignored) {
            return null;
        }

        if (webPlayer == null) return null;
        return new AbstractPlayer(webPlayer.uuid(), webPlayer.name(), false);
    }

    @Override
    public AbstractPlayer getAbstractPlayer(String username, boolean queryMojang) {
        Player player = getPlayer(username);

        if (player != null) return new AbstractPlayer(player.getUniqueId(), player.getUsername(), true);
        if (!queryMojang) return null;

        WebPlayer webPlayer;
        try {
            webPlayer = WebPlayer.get(username);
        } catch (IOException ignored) {
            return null;
        }

        if (webPlayer == null) return null;
        return new AbstractPlayer(webPlayer.uuid(), webPlayer.name(), false);
    }

    @Override
    public CirrusPlayerWrapper getPlayerWrapper(UUID uuid) {
//        return new VelocityPlayerWrapper(getPlayer(uuid));
        return null;
    }
}
