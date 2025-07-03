package com.bongbong.modl.minecraft.core;

import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import dev.simplix.cirrus.player.CirrusPlayerWrapper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

public interface Platform {
    void broadcast(String string);
    void staffBroadcast(String string);
    void disconnect(UUID uuid, String message);
    void sendMessage(UUID uuid, String message);
    boolean isOnline(UUID uuid);
    CommandManager<?,?,?,?,?,?> getCommandManager();
    AbstractPlayer getAbstractPlayer(UUID uuid, boolean queryMojang);
    AbstractPlayer getAbstractPlayer(String username, boolean queryMojang);
    CirrusPlayerWrapper getPlayerWrapper(UUID uuid);
    
    // New methods for sync service
    Collection<AbstractPlayer> getOnlinePlayers();
    AbstractPlayer getPlayer(UUID uuid);
    int getMaxPlayers();
    String getServerVersion();
    void runOnMainThread(Runnable task);
    void kickPlayer(AbstractPlayer player, String reason);
    String getServerName();
}

