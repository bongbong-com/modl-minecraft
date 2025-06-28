package com.bongbong.modl.minecraft.core.util;

public class TimeUtil {
    
    public static String formatTimeMillis(long milliseconds) {
        if (milliseconds <= 0) {
            return "permanent";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;
        
        if (years > 0) {
            return years + "y " + (months % 12) + "M " + (days % 30) + "d";
        } else if (months > 0) {
            return months + "M " + (days % 30) + "d " + (hours % 24) + "h";
        } else if (weeks > 0) {
            return weeks + "w " + (days % 7) + "d " + (hours % 24) + "h";
        } else if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}