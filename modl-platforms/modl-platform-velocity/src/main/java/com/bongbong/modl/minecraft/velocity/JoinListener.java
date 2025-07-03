package com.bongbong.modl.minecraft.velocity;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.request.PunishmentAcknowledgeRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.bongbong.modl.minecraft.core.util.PunishmentMessages;
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
    private final ChatMessageCache chatMessageCache;

    @Subscribe
    public void onLogin(LoginEvent event) {
        PlayerLoginRequest request = new PlayerLoginRequest(
                event.getPlayer().getUniqueId().toString(),
                event.getPlayer().getUsername(),
                event.getPlayer().getRemoteAddress().getAddress().getHostAddress(),
                null,
                null
        );

        try {
            // Check for active punishments and prevent login if banned (synchronous)
            CompletableFuture<PlayerLoginResponse> loginFuture = httpClient.playerLogin(request);
            PlayerLoginResponse response = loginFuture.join(); // Block until response
            
            logger.info(String.format("Login response for %s: status=%d, punishments=%s", 
                    event.getPlayer().getUsername(),
                    response.getStatus(),
                    response.getActivePunishments()));
            
            if (response.getActivePunishments() != null) {
                for (SimplePunishment p : response.getActivePunishments()) {
                    logger.info(String.format("Punishment: type='%s', isBan=%s, isMute=%s, started=%s, id=%s", 
                            p.getType(), p.isBan(), p.isMute(), p.isStarted(), p.getId()));
                }
            }
            
            logger.info(String.format("Login response for %s: hasBan=%s, hasMute=%s", 
                    event.getPlayer().getUsername(),
                    response.hasActiveBan(),
                    response.hasActiveMute()));
            
            if (response.hasActiveBan()) {
                SimplePunishment ban = response.getActiveBan();
                String banText = PunishmentMessages.formatBanMessage(ban);
                Component kickMessage = Colors.get(banText);
                event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
                
                logger.info(String.format("Denied login for %s due to active ban: %s", 
                        event.getPlayer().getUsername(), ban.getDescription()));
                
                // Acknowledge ban enforcement if it wasn't started yet
                if (!ban.isStarted()) {
                    acknowledgeBanEnforcement(ban, event.getPlayer().getUniqueId().toString());
                }
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
        } catch (Exception e) {
            // On error, allow login but log warning
            logger.error("Failed to check punishments for " + event.getPlayer().getUsername() + 
                        " - allowing login as fallback", e);
            event.setResult(ResultedEvent.ComponentResult.allowed());
        }
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
        
        // Remove player from chat message cache
        chatMessageCache.removePlayer(event.getPlayer().getUniqueId().toString());
    }
    
    
    public Cache getPunishmentCache() {
        return cache;
    }
    
    public ChatMessageCache getChatMessageCache() {
        return chatMessageCache;
    }
    
    /**
     * Acknowledge that a ban was successfully enforced (player denied login)
     */
    private void acknowledgeBanEnforcement(SimplePunishment ban, String playerUuid) {
        try {
            PunishmentAcknowledgeRequest request = new PunishmentAcknowledgeRequest(
                    ban.getId(),
                    playerUuid,
                    java.time.Instant.now().toString(),
                    true, // success
                    null // no error message
            );
            
            httpClient.acknowledgePunishment(request).thenAccept(response -> {
                logger.info("Successfully acknowledged ban enforcement for punishment " + ban.getId());
            }).exceptionally(throwable -> {
                logger.error("Failed to acknowledge ban enforcement for punishment " + ban.getId(), throwable);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error acknowledging ban enforcement for punishment " + ban.getId(), e);
        }
    }
}
