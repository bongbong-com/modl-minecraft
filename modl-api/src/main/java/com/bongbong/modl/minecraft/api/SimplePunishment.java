package com.bongbong.modl.minecraft.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * Simplified punishment object for API responses with only essential fields
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplePunishment {
    @NotNull
    private String type; // "BAN" or "MUTE"
    
    private boolean started;
    
    @Nullable
    private Date expiration;
    
    @NotNull
    private String description;
    
    @NotNull
    private String id;
    
    /**
     * Check if this is a ban punishment
     */
    public boolean isBan() {
        return "BAN".equalsIgnoreCase(type);
    }
    
    /**
     * Check if this is a mute punishment
     */
    public boolean isMute() {
        return "MUTE".equalsIgnoreCase(type);
    }
    
    /**
     * Check if the punishment is permanent (no expiration)
     */
    public boolean isPermanent() {
        return expiration == null;
    }
    
    /**
     * Check if the punishment has expired
     */
    public boolean isExpired() {
        return expiration != null && expiration.before(new Date());
    }
    
    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        if (isPermanent()) {
            return "permanently";
        }
        
        if (isExpired()) {
            return "expired";
        }
        
        long timeLeft = expiration.getTime() - System.currentTimeMillis();
        return formatDuration(timeLeft);
    }
    
    private String formatDuration(long durationMs) {
        if (durationMs <= 0) return "expired";
        
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
}