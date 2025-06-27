package com.bongbong.modl.minecraft.spigot;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import com.bongbong.modl.minecraft.core.cache.PunishmentCache;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

public class SpigotListener implements Listener {

    private final SpigotPlatform platform;
    private final PunishmentCache punishmentCache;

    public SpigotListener(SpigotPlatform platform) {
        this.platform = platform;
        this.punishmentCache = new PunishmentCache();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getAddress().getHostAddress(),
                null,
                null
        );

        try {
            // Synchronous check for active punishments
            CompletableFuture<PlayerLoginResponse> loginFuture = httpClient.playerLogin(request);
            PlayerLoginResponse response = loginFuture.join(); // Blocks until response
            
            if (response.hasActiveBan()) {
                Punishment ban = response.getActiveBan();
                String banMessage = formatBanMessage(ban);
                event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
                event.setKickMessage(banMessage);
            }
        } catch (Exception e) {
            platform.getLogger().severe("Failed to check punishments for " + event.getPlayer().getName() + ": " + e.getMessage());
            // Allow login on error to prevent false kicks
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Cache mute status after successful join
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getPlayer().getAddress().getHostAddress(),
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
        
        // Remove player from punishment cache
        punishmentCache.removePlayer(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (punishmentCache.isMuted(event.getPlayer().getUniqueId())) {
            Punishment mute = punishmentCache.getMute(event.getPlayer().getUniqueId());
            
            if (mute != null) {
                // Cancel the chat event
                event.setCancelled(true);
                
                // Send mute message to player
                String muteMessage = formatMuteMessage(mute);
                event.getPlayer().sendMessage(muteMessage);
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