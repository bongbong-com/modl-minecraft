package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
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
            logger.info(String.format("Login response for %s: hasBan=%s, hasMute=%s", 
                    event.getPlayer().getUsername(),
                    response.hasActiveBan(),
                    response.hasActiveMute()));
            
            if (response.hasActiveBan()) {
                SimplePunishment ban = response.getActiveBan();
                Component kickMessage = formatBanMessage(ban);
                event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
                
                logger.info(String.format("Denied login for %s due to active ban: %s", 
                        event.getPlayer().getUsername(), ban.getDescription()));
            } else {
                // Cache active mute if present
                if (response.hasActiveMute()) {
                    SimplePunishment mute = response.getActiveMute();
                    cache.cacheMute(event.getPlayer().getUniqueId(), mute);
                    logger.info(String.format("Cached active mute for %s: %s", 
                            event.getPlayer().getUsername(), mute.getDescription()));
                }
                event.setResult(ResultedEvent.ComponentResult.allowed());
                
                logger.info(String.format("Allowed login for %s", event.getPlayer().getUsername()));
            }
        }).exceptionally(throwable -> {
            // On error, allow login but log warning
            logger.error("Failed to check punishments for " + event.getPlayer().getUsername() + 
                        " - allowing login as fallback", throwable);
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
    
    private Component formatBanMessage(SimplePunishment ban) {
        Component message = Component.text("You are banned from this server!", NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.GRAY))
                .append(Component.text(ban.getDescription(), NamedTextColor.WHITE));
        
        // Add punishment ID for reference
        message = message.append(Component.newline())
                .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                .append(Component.text(ban.getId(), NamedTextColor.YELLOW));
        
        // Add expiration information with formatted duration
        if (ban.isPermanent()) {
            message = message.append(Component.newline())
                    .append(Component.text("This ban is permanent.", NamedTextColor.DARK_RED));
        } else if (ban.isExpired()) {
            message = message.append(Component.newline())
                    .append(Component.text("This ban has expired (contact staff).", NamedTextColor.GREEN));
        } else {
            message = message.append(Component.newline())
                    .append(Component.text("Expires in: ", NamedTextColor.GRAY))
                    .append(Component.text(ban.getFormattedDuration(), NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text("Expires: ", NamedTextColor.GRAY))
                    .append(Component.text(ban.getExpiration().toString(), NamedTextColor.WHITE));
        }
        
        // Add appeal information
        message = message.append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Appeal at: ", NamedTextColor.GRAY))
                .append(Component.text("https://123.cobl.gg/appeal", NamedTextColor.BLUE));
        
        return message;
    }
    
    public Cache getPunishmentCache() {
        return cache;
    }
}
