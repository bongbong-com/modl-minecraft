package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Account;
import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class JoinListener {

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID minecraftUuid = player.getUniqueId();
        String username = player.getUsername();
        String ipAddress = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://modl.bongbong.com/api/players/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"minecraftUuid\": \"" + minecraftUuid + "\",\"username\": \"" + username + "\",\"ipAddress\": \"" + ipAddress + "\"}"
                ))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                Account account = new Gson().fromJson(response.body(), Account.class);
                System.out.println(account.export());
            } else {
                System.out.println("Error 1");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
