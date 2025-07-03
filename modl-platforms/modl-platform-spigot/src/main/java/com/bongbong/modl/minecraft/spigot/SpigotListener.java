package com.bongbong.modl.minecraft.spigot;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.request.PunishmentAcknowledgeRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.bongbong.modl.minecraft.core.util.PunishmentMessages;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class SpigotListener implements Listener {

    private final SpigotPlatform platform;
    private final Cache cache;
    private final ModlHttpClient httpClient;
    private final ChatMessageCache chatMessageCache;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
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
                SimplePunishment ban = response.getActiveBan();
                String banMessage = PunishmentMessages.formatBanMessage(ban);
                event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
                event.setKickMessage(banMessage);
                
                // Acknowledge ban enforcement if it wasn't started yet
                if (!ban.isStarted()) {
                    acknowledgeBanEnforcement(ban, event.getPlayer().getUniqueId().toString());
                }
            }
        } catch (Exception e) {
            platform.getLogger().severe("Failed to check punishments for " + event.getPlayer().getName() + ": " + e.getMessage());
            // Allow login on error to prevent false kicks
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Cache mute status after successful join
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getPlayer().getAddress().getHostName(),
                null,
                null
        );
        
        httpClient.playerLogin(request).thenAccept(response -> {
            if (response.hasActiveMute()) {
                cache.cacheMute(event.getPlayer().getUniqueId(), response.getActiveMute());
            }
        }).exceptionally(throwable -> {
            platform.getLogger().severe("Failed to cache mute for " + event.getPlayer().getName() + ": " + throwable.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
        
        // Remove player from punishment cache
        cache.removePlayer(event.getPlayer().getUniqueId());
        
        // Remove player from chat message cache
        chatMessageCache.removePlayer(event.getPlayer().getUniqueId().toString());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Cache the chat message first (before potentially cancelling)
        chatMessageCache.addMessage(
            platform.getServerName(), // Server name for cross-platform compatibility
            event.getPlayer().getUniqueId().toString(),
            event.getPlayer().getName(),
            event.getMessage()
        );
        
        if (cache.isMuted(event.getPlayer().getUniqueId())) {
            // Cancel the chat event
            event.setCancelled(true);
            
            // Get cached mute and send message to player
            Cache.CachedPlayerData data = cache.getCache().get(event.getPlayer().getUniqueId());
            if (data != null) {
                String muteMessage;
                if (data.getSimpleMute() != null) {
                    muteMessage = PunishmentMessages.formatMuteMessage(data.getSimpleMute());
                } else if (data.getMute() != null) {
                    // Fallback to old punishment format
                    muteMessage = formatMuteMessage(data.getMute());
                } else {
                    muteMessage = ChatColor.RED + "You are muted!";
                }
                event.getPlayer().sendMessage(muteMessage);
            }
        }
    }
    
    /**
     * Format mute message for old punishment format (fallback)
     */
    private String formatMuteMessage(Punishment mute) {
        String reason = mute.getReason() != null ? mute.getReason() : "No reason provided";
        
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED).append("You are muted!\n");
        message.append(ChatColor.GRAY).append("Reason: ").append(ChatColor.WHITE).append(reason);
        
        if (mute.getExpires() != null) {
            long timeLeft = mute.getExpires().getTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                String timeString = PunishmentMessages.formatDuration(timeLeft);
                message.append("\n").append(ChatColor.GRAY).append("Time remaining: ")
                       .append(ChatColor.WHITE).append(timeString);
            }
        } else {
            message.append("\n").append(ChatColor.DARK_RED).append("This mute is permanent.");
        }
        
        return message.toString();
    }
    
    public Cache getPunishmentCache() {
        return cache;
    }
    
    public ChatMessageCache getChatMessageCache() {
        return chatMessageCache;
    }
    
    /**
     * Acknowledge that a ban was successfully enforced (player denied login)
     */
    private void acknowledgeBanEnforcement(SimplePunishment ban, String playerUuid) {
        try {
            PunishmentAcknowledgeRequest request = new PunishmentAcknowledgeRequest(
                    ban.getId(),
                    playerUuid,
                    java.time.Instant.now().toString(),
                    true, // success
                    null // no error message
            );
            
            httpClient.acknowledgePunishment(request).thenAccept(response -> {
                platform.getLogger().info("Successfully acknowledged ban enforcement for punishment " + ban.getId());
            }).exceptionally(throwable -> {
                platform.getLogger().severe("Failed to acknowledge ban enforcement for punishment " + ban.getId() + ": " + throwable.getMessage());
                return null;
            });
        } catch (Exception e) {
            platform.getLogger().severe("Error acknowledging ban enforcement for punishment " + ban.getId() + ": " + e.getMessage());
        }
    }
}