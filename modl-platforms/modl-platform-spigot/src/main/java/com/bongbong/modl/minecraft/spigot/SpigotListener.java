package com.bongbong.modl.minecraft.spigot;

import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SpigotListener implements Listener {

    private final SpigotPlatform platform;

    public SpigotListener(SpigotPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getName(),
                event.getAddress().getHostAddress(),
                null,
                null
        );

        httpClient.playerLogin(request);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ModlHttpClient httpClient = platform.getHttpClient();
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
    }
}