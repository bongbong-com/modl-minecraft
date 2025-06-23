package com.bongbong.modl.minecraft.bungee;

import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeListener implements Listener {

    private final BungeePlatform platform;

    public BungeeListener(BungeePlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getPlayer().getSocketAddress().toString(),
                null,
                null
        );

        httpClient.playerLogin(request);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
    }
}