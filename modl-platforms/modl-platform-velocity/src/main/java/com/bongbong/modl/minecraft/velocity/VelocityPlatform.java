package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.util.WebPlayer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.simplix.cirrus.player.CirrusPlayerWrapper;
//import dev.simplix.cirrus.velocity.wrapper.VelocityPlayerWrapper;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private Player getOnlinePlayer(String username) {
        return server.getPlayer(username).isPresent() ? server.getPlayer(username).get() : null;
    }

    private Player getOnlinePlayer(UUID uuid) {
        return server.getPlayer(uuid).isPresent() ? server.getPlayer(uuid).get() : null;
    }

    @Override
    public AbstractPlayer getAbstractPlayer(UUID uuid, boolean queryMojang) {
        Player player = getOnlinePlayer(uuid);

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
        Player player = getOnlinePlayer(username);

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

    @Override
    public Collection<AbstractPlayer> getOnlinePlayers() {
        return server.getAllPlayers().stream()
                .map(player -> new AbstractPlayer(
                        player.getUniqueId(),
                        player.getUsername(),
                        true,
                        player.getRemoteAddress().getAddress().getHostAddress()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public AbstractPlayer getPlayer(UUID uuid) {
        Player player = server.getPlayer(uuid).orElse(null);
        if (player == null) return null;
        
        return new AbstractPlayer(
                player.getUniqueId(),
                player.getUsername(),
                true,
                player.getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Override
    public int getMaxPlayers() {
        return server.getConfiguration().getShowMaxPlayers();
    }

    @Override
    public String getServerVersion() {
        return server.getVersion().getVersion();
    }

    @Override
    public void runOnMainThread(Runnable task) {
        // Velocity doesn't have a main thread concept like Bukkit
        // Execute immediately since we're already on the main event thread
        task.run();
    }

    @Override
    public void kickPlayer(AbstractPlayer player, String reason) {
        Player velocityPlayer = server.getPlayer(player.getUuid()).orElse(null);
        if (velocityPlayer != null) {
            velocityPlayer.disconnect(get(reason));
        }
    }
}
