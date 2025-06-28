package com.bongbong.modl.minecraft.core.util;

public class Colors {
    public static final String RED = "§c";
    public static final String GREEN = "§a";
    public static final String YELLOW = "§e";
    public static final String BLUE = "§9";
    public static final String DARK_RED = "§4";
    public static final String GRAY = "§7";
    public static final String WHITE = "§f";
    public static final String BOLD = "§l";
    public static final String ITALIC = "§o";
    public static final String RESET = "§r";
    
    public static String translate(String message) {
        return message.replace('&', '§');
    }
}