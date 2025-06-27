package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.core.cache.PunishmentCache;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Date;

public class ChatListener {
    
    private final VelocityPlatform platform;
    private final PunishmentCache punishmentCache;
    
    public ChatListener(VelocityPlatform platform, PunishmentCache punishmentCache) {
        this.platform = platform;
        this.punishmentCache = punishmentCache;
    }
    
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (punishmentCache.isMuted(event.getPlayer().getUniqueId())) {
            Punishment mute = punishmentCache.getMute(event.getPlayer().getUniqueId());
            
            if (mute != null) {
                // Cancel the chat event
                event.setResult(PlayerChatEvent.ChatResult.denied());
                
                // Send mute message to player
                Component muteMessage = formatMuteMessage(mute);
                event.getPlayer().sendMessage(muteMessage);
            }
        }
    }
    
    private Component formatMuteMessage(Punishment mute) {
        String reason = mute.getReason() != null ? mute.getReason() : "No reason provided";
        
        Component message = Component.text("You are muted!", NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.GRAY))
                .append(Component.text(reason, NamedTextColor.WHITE));
        
        if (mute.getExpires() != null) {
            long timeLeft = mute.getExpires().getTime() - System.currentTimeMillis();
            if (timeLeft > 0) {
                String timeString = formatTime(timeLeft);
                message = message.append(Component.newline())
                        .append(Component.text("Time remaining: ", NamedTextColor.GRAY))
                        .append(Component.text(timeString, NamedTextColor.WHITE));
            }
        } else {
            message = message.append(Component.newline())
                    .append(Component.text("This mute is permanent.", NamedTextColor.DARK_RED));
        }
        
        return message;
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
}