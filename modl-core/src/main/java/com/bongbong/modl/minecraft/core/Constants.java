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

    String API_KEY = "";
    String API_URL = "";
    String PANEL_URL = "";
}
