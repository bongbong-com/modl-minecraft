package com.bongbong.modl.minecraft.bungee;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import com.bongbong.modl.minecraft.core.cache.PunishmentCache;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.concurrent.CompletableFuture;

public class BungeeListener implements Listener {

    private final BungeePlatform platform;
    private final PunishmentCache punishmentCache;

    public BungeeListener(BungeePlatform platform) {
        this.platform = platform;
        this.punishmentCache = new PunishmentCache();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getConnection().getUniqueId().toString(),
                event.getConnection().getName(),
                event.getConnection().getSocketAddress().toString(),
                null,
                null
        );

        // Check for active punishments and prevent login if banned
        CompletableFuture<PlayerLoginResponse> loginFuture = httpClient.playerLogin(request);
        
        loginFuture.thenAccept(response -> {
            if (response.hasActiveBan()) {
                Punishment ban = response.getActiveBan();
                TextComponent kickMessage = new TextComponent(formatBanMessage(ban));
                event.setCancelReason(kickMessage);
                event.setCancelled(true);
            }
        }).exceptionally(throwable -> {
            // On error, allow login but log warning
            platform.getLogger().severe("Failed to check punishments for " + event.getConnection().getName() + ": " + throwable.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // Cache mute status after successful join
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getPlayer().getSocketAddress().toString(),
                null,
                null
        );
        
        httpClient.playerLogin(request).thenAccept(response -> {
            if (response.hasActiveMute()) {
                punishmentCache.cacheMute(event.getPlayer().getUniqueId(), response.getActiveMute());
            }
        }).exceptionally(throwable -> {
            platform.getLogger().severe("Failed to cache mute for " + event.getPlayer().getName() + ": " + throwable.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
        
        // Remove player from punishment cache
        punishmentCache.removePlayer(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent event) {
        if (event.isCommand() || event.getSender() == null) {
            return; // Ignore commands and non-player senders
        }

        ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
        
        if (punishmentCache.isMuted(sender.getUniqueId())) {
            Punishment mute = punishmentCache.getMute(sender.getUniqueId());
            
            if (mute != null) {
                // Cancel the chat event
                event.setCancelled(true);
                
                // Send mute message to player
                String muteMessage = formatMuteMessage(mute);
                sender.sendMessage(new TextComponent(muteMessage));
            }
        }
    }
    
    private String formatBanMessage(Punishment ban) {
        String reason = ban.getReason() != null ? ban.getReason() : "No reason provided";
        
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("You are banned from this server!\n");
        message.append(ChatColor.GRAY).append("Reason: ").append(ChatColor.WHITE).append(reason);
        
        if (ban.getExpires() != null) {
            message.append("\n").append(ChatColor.GRAY).append("Expires: ")
                   .append(ChatColor.WHITE).append(ban.getExpires().toString());
        } else {
            message.append("\n").append(ChatColor.DARK_RED).append("This ban is permanent.");
        }
        
        return message.toString();
    }
    
    private String formatMuteMessage(Punishment mute) {
        String reason = mute.getReason() != null ? mute.getReason() : "No reason provided";
        
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("You are muted!\n");
        message.append(ChatColor.GRAY).append("Reason: ").append(ChatColor.WHITE).append(reason);
        
        if (mute.getExpires() != null) {
            long timeLeft = mute.getExpires().getTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                String timeString = formatTime(timeLeft);
                message.append("\n").append(ChatColor.GRAY).append("Time remaining: ")
                       .append(ChatColor.WHITE).append(timeString);
            }
        } else {
            message.append("\n").append(ChatColor.DARK_RED).append("This mute is permanent.");
        }
        
        return message.toString();
    }
    
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public PunishmentCache getPunishmentCache() {
        return punishmentCache;
    }
}