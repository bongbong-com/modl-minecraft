package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import dev.simplix.cirrus.player.CirrusPlayerWrapper;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SpigotPlatform implements Platform {
    private final BukkitCommandManager commandManager;
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
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            // Handle both escaped newlines and literal \n sequences
            String formattedMessage = message.replace("\\n", "\n").replace("\\\\n", "\n");
            player.sendMessage(formattedMessage);
        }
    }
    
    @Override
    public void sendJsonMessage(UUID uuid, String jsonMessage) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            // Use Spigot's JSON message API
            player.spigot().sendMessage(ComponentSerializer.parse(jsonMessage));
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
        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
            // Handle both escaped newlines and literal \n sequences
            String formattedReason = reason.replace("\\n", "\n").replace("\\\\n", "\n");
            bukkitPlayer.kickPlayer(formattedReason);
        }
    }

    @Override
    public String getServerName() {
        return "spigot-server"; // Default server name, can be made configurable
    }

    public Logger getLogger() {
        return logger;
    }
}
