package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class JoinListener {

    private final ModlHttpClient httpClient;
    private final Cache cache;
    private final Logger logger;

    @Subscribe
    public void onLogin(LoginEvent event) {
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getUsername(),
                event.getPlayer().getRemoteAddress().getAddress().getHostAddress(),
                null,
                null
        );

        // Check for active punishments and prevent login if banned
        CompletableFuture<PlayerLoginResponse> loginFuture = httpClient.playerLogin(request);
        
        loginFuture.thenAccept(response -> {
            if (response.hasActiveBan()) {
                Punishment ban = response.getActiveBan();
                Component kickMessage = formatBanMessage(ban);
                event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
            } else {
                // Cache active mute if present
                if (response.hasActiveMute()) {
                    cache.cacheMute(event.getPlayer().getUniqueId(), response.getActiveMute());
                }
                event.setResult(ResultedEvent.ComponentResult.allowed());
            }
        }).exceptionally(throwable -> {
            // On error, allow login but log warning
            logger.error("Failed to check punishments for " + event.getPlayer().getUsername(), throwable);
            event.setResult(ResultedEvent.ComponentResult.allowed());
            return null;
        });
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        // Additional processing after successful login if needed
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(event.getPlayer().getUniqueId().toString());

        httpClient.playerDisconnect(request);
        
        // Remove player from punishment cache
        cache.removePlayer(event.getPlayer().getUniqueId());
    }
    
    private Component formatBanMessage(Punishment ban) {
        String reason = ban.getReason() != null ? ban.getReason() : "No reason provided";
        
        Component message = Component.text("You are banned from this server!", NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.GRAY))
                .append(Component.text(reason, NamedTextColor.WHITE));
        
        if (ban.getExpires() != null) {
            message = message.append(Component.newline())
                    .append(Component.text("Expires: ", NamedTextColor.GRAY))
                    .append(Component.text(ban.getExpires().toString(), NamedTextColor.WHITE));
        } else {
            message = message.append(Component.newline())
                    .append(Component.text("This ban is permanent.", NamedTextColor.DARK_RED));
        }
        
        return message;
    }
    
    public Cache getPunishmentCache() {
        return cache;
    }
}
