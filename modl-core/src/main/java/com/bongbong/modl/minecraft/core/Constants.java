package com.bongbong.modl.minecraft.core;


public interface Constants {
    String PERMISSION = "hammer.mod";

    String DATE_FORMAT = "MM/dd/yy hh:mm:ss aa";

    // enable this if you want to ban/report players that have never joined (not in db)
    // could lead to rate limit errors so not recommended
    boolean QUERY_MOJANG = false;
    
    // Note: Player not found messages are now handled through the locale system
    // Use localeManager.getMessage("constants.mojang_api_no_player") or 
    // localeManager.getMessage("constants.never_joined_server") instead

    String API_KEY = "fiE_fmowW-qth2IQNGlK0deuQLpl-gI043SPGy--xYc";
    String API_URL = "https://001.cobl.gg/api";
    String PANEL_URL = "https://001.cobl.gg";
    
    // Debug mode for HTTP requests - disabled by default
    boolean DEBUG_HTTP = Boolean.parseBoolean(System.getProperty("modl.debug.http", "false"));
}
