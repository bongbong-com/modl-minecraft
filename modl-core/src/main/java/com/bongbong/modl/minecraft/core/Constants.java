package com.bongbong.modl.minecraft.core;


import com.bongbong.modl.minecraft.core.util.Colors;

public interface Constants {
    String PERMISSION = "hammer.mod";

    String DATE_FORMAT = "MM/dd/yy hh:mm:ss aa";

    // enable this if you want to ban/report players that have never joined (not in db)
    // could lead to rate limit errors so not recommended
    boolean QUERY_MOJANG = false;
    String NOT_FOUND = QUERY_MOJANG ?
            Colors.translate("&cNo player with that username exists (queried Mojang API)") :
            Colors.translate("&cNo player with that username has never joined the server.");

    String API_KEY = "fiE_fmowW-qth2IQNGlK0deuQLpl-gI043SPGy--xYc";
    String API_URL = "https://001.cobl.gg/api";
    String PANEL_URL = "https://001.cobl.gg";
    
    // Debug mode for HTTP requests - disabled by default
    boolean DEBUG_HTTP = Boolean.parseBoolean(System.getProperty("modl.debug.http", "false"));
}
