package com.bongbong.modl.minecraft.core.util;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.core.Constants;

/**
 * Utility class for formatting punishment messages across all platforms
 */
public class PunishmentMessages {
    
    /**
     * Format a ban message with all available punishment data
     * Platform-specific components should be created from this text
     */
    public static String formatBanMessage(SimplePunishment ban) {
        StringBuilder message = new StringBuilder();

        String temp = ban.getExpiration() != null ? "temporarily" : "permanently";
        
        message.append("&c&lYou are " + temp +  " banned for: \n&f \n");
        message.append("&f").append(ban.getDescription()).append("\n&f \n");
        
        // Add expiration information with formatted duration
        if (!ban.isPermanent()) {
            assert ban.getExpiration() != null;
            message.append("&7This ban will expire in ").append(formatDuration(ban.getExpiration() - System.currentTimeMillis())).append("\n");
        }
        
        // Add appeal information
        message.append("&7Appeal at ").append(Constants.PANEL_URL).append("/appeal using punishment ID #" + ban.getId() + "");
        
        return message.toString();
    }

    public static String getFormattedDuration(SimplePunishment punishment) {
        if (punishment.isPermanent()) {
            return "permanently";
        }

        if (punishment.isExpired()) {
            return "expired";
        }

        assert punishment.getExpiration() != null;
        long timeLeft = punishment.getExpiration() - System.currentTimeMillis();
        // Import the utility method dynamically to avoid circular imports
        return formatDuration(timeLeft);
    }
    /**
     * Format a mute message with all available punishment data
     */
    public static String formatMuteMessage(SimplePunishment mute) {
        StringBuilder message = new StringBuilder();
        
        message.append("§cYou have been muted!\n");
        message.append("§7Reason: §f").append(mute.getDescription()).append("\n");
        message.append("§7Mute ID: §e").append(mute.getId()).append("\n");
        
        // Add expiration information
        if (mute.isPermanent()) {
            message.append("§4This mute is permanent.\n");
        } else if (mute.isExpired()) {
            message.append("§aThis mute has expired.\n");
        } else {
            message.append("§7Expires in: §f").append(formatDuration(mute.getExpiration() - System.currentTimeMillis())).append("\n");
            message.append("§7Expires: §f").append(formatTime(mute.getExpirationAsDate())).append("\n");
        }
        
        return message.toString();
    }
    
    /**
     * Format a punishment for broadcast messages
     */
    public static String formatPunishmentBroadcast(String username, SimplePunishment punishment, String action) {
        String duration;
        if (punishment.isPermanent()) {
            duration = "permanently";
        } else {
            assert punishment.getExpiration() != null;
            duration = formatDuration(punishment.getExpiration() - System.currentTimeMillis());
        }
        return String.format("§c%s has been %s %s: §f%s", username, action, duration, punishment.getDescription());
    }
    
    /**
     * Format time in a user-friendly way
     */
    public static String formatTime(java.util.Date date) {
        if (date == null) return "Never";
        
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(Constants.DATE_FORMAT);
        return formatter.format(date);
    }
    
    /**
     * Format duration in milliseconds to human readable string
     */
    public static String formatDuration(long millis) {
        long seconds = (millis / 1000L) + 1;

        if (seconds < 1) return "0 seconds";

        long minutes = seconds / 60;
        seconds = seconds % 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        long day = hours / 24;
        hours = hours % 24;
        long years = day / 365;
        day = day % 365;

        StringBuilder time = new StringBuilder();

        if (years != 0) time.append(years).append("y ");
        if (day != 0) time.append(day).append("d ");
        if (hours != 0) time.append(hours).append("h ");
        if (minutes != 0) time.append(minutes).append("m ");
        if (seconds != 0) time.append(seconds).append("s ");

        return time.toString().trim();
    }
    
    /**
     * Create a clean text version without color codes (for console/logs)
     */
    public static String stripColors(String message) {
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * Convert legacy color codes (&) to modern format (§)
     */
    public static String translateColors(String message) {
        return message.replace('&', '§');
    }
}