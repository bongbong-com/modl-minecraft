package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.util.PunishmentMessages;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChatListener {
    
    private final VelocityPlatform platform;
    private final Cache cache;
    
    public ChatListener(VelocityPlatform platform, Cache cache) {
        this.platform = platform;
        this.cache = cache;
    }
    
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (cache.isMuted(event.getPlayer().getUniqueId())) {
            // Cancel the chat event
            event.setResult(PlayerChatEvent.ChatResult.denied());
            
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
                    muteMessage = "§cYou are muted!";
                }
                Component muteComponent = Colors.get(muteMessage);
                event.getPlayer().sendMessage(muteComponent);
            }
        }
    }
    
    private String formatMuteMessage(Punishment mute) {
        String reason = mute.getReason() != null ? mute.getReason() : "No reason provided";
        
        StringBuilder message = new StringBuilder();
        message.append("§cYou are muted!\n");
        message.append("§7Reason: §f").append(reason).append("\n");
        
        if (mute.getExpires() != null) {
            long timeLeft = mute.getExpires().getTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                String timeString = PunishmentMessages.formatDuration(timeLeft);
                message.append("§7Time remaining: §f").append(timeString).append("\n");
            }
        } else {
            message.append("§4This mute is permanent.\n");
        }
        
        return message.toString();
    }
}