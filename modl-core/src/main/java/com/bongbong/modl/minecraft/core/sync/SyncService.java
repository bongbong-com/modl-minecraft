package com.bongbong.modl.minecraft.core.sync;

import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.PanelUnavailableException;
import com.bongbong.modl.minecraft.api.http.request.NotificationAcknowledgeRequest;
import com.bongbong.modl.minecraft.api.http.request.PunishmentAcknowledgeRequest;
import com.bongbong.modl.minecraft.api.http.request.SyncRequest;
import com.bongbong.modl.minecraft.api.http.response.SyncResponse;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.util.PunishmentMessages;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SyncService {
    @NotNull
    private final Platform platform;
    @NotNull
    private final ModlHttpClient httpClient;
    @NotNull
    private final Cache cache;
    @NotNull
    private final Logger logger;
    @NotNull
    private final LocaleManager localeManager;
    
    private String lastSyncTimestamp;
    private ScheduledExecutorService syncExecutor;
    private boolean isRunning = false;
    
    /**
     * Start the sync service with 5-second intervals
     */
    public void start() {
        if (isRunning) {
            logger.warning("Sync service is already running");
            return;
        }
        
        this.lastSyncTimestamp = Instant.now().toString();
        this.syncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "modl-sync");
            t.setDaemon(true);
            return t;
        });
        
        // Perform initial diagnostic check
        logger.info("MODL Sync service starting - performing initial diagnostic check...");
        performDiagnosticCheck();
        
        // Start sync task every 5 seconds (delayed by 10 seconds to allow diagnostic check)
        syncExecutor.scheduleAtFixedRate(this::performSync, 10, 5, TimeUnit.SECONDS);
        isRunning = true;
        
        logger.info("MODL Sync service started - syncing every 5 seconds");
    }
    
    /**
     * Stop the sync service
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        if (syncExecutor != null) {
            syncExecutor.shutdown();
            try {
                if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    syncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                syncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        isRunning = false;
        logger.info("MODL Sync service stopped");
    }
    
    /**
     * Perform diagnostic check to validate API connectivity
     */
    private void performDiagnosticCheck() {
        try {
            logger.info("Diagnostic: Testing API connectivity...");
            logger.info("Diagnostic: API Base URL: " + Constants.API_URL);
            logger.info("Diagnostic: API Key: " + (Constants.API_KEY.length() > 8 ? 
                Constants.API_KEY.substring(0, 8) + "..." : "***"));
            
            // First, test basic connectivity to the panel URL
            testBasicConnectivity();
            
            // Test with a minimal sync request
            SyncRequest testRequest = new SyncRequest();
            testRequest.setLastSyncTimestamp(Instant.now().toString());
            testRequest.setOnlinePlayers(List.of()); // Empty list
            testRequest.setServerStatus(new SyncRequest.ServerStatus(
                0, 
                platform.getMaxPlayers(),
                platform.getServerVersion(),
                Instant.now().toString()
            ));
            
            CompletableFuture<SyncResponse> testFuture = httpClient.sync(testRequest);
            testFuture.thenAccept(response -> {
                logger.info("Diagnostic: API connectivity test PASSED");
                logger.info("Diagnostic: Server response timestamp: " + response.getTimestamp());
            }).exceptionally(throwable -> {
                if (throwable.getCause() instanceof PanelUnavailableException) {
                    logger.warning("Diagnostic: Panel is temporarily unavailable (502 error) - likely restarting");
                } else {
                    logger.severe("Diagnostic: API connectivity test FAILED: " + throwable.getMessage());
                    if (throwable.getMessage().contains("502")) {
                        logger.severe("Diagnostic: 502 Bad Gateway indicates the API server is unreachable");
                        logger.severe("Diagnostic: This usually means:");
                        logger.severe("Diagnostic:   1. The panel server (123.cobl.gg) is down");
                        logger.severe("Diagnostic:   2. The reverse proxy/load balancer is misconfigured");
                        logger.severe("Diagnostic:   3. The API endpoint URL is incorrect");
                        logger.severe("Diagnostic: Expected sync endpoint: " + Constants.API_URL + "/sync");
                        logger.severe("Diagnostic: Check if you can access " + Constants.PANEL_URL + " in a web browser");
                    } else if (throwable.getMessage().contains("401") || throwable.getMessage().contains("403")) {
                        logger.severe("Diagnostic: Authentication failed - API key may be invalid");
                    } else if (throwable.getMessage().contains("404")) {
                        logger.severe("Diagnostic: Endpoint not found - the sync endpoint may not exist");
                    } else if (throwable.getMessage().contains("ConnectException") || throwable.getMessage().contains("UnknownHostException")) {
                        logger.severe("Diagnostic: Network connectivity issue - cannot reach " + Constants.PANEL_URL);
                    }
                }
                return null;
            });
            
        } catch (Exception e) {
            logger.severe("Diagnostic: Failed to perform connectivity test: " + e.getMessage());
        }
    }
    
    /**
     * Test basic connectivity to the panel server
     */
    private void testBasicConnectivity() {
        try {
            logger.info("Diagnostic: Testing basic connectivity to " + Constants.PANEL_URL);
            URL url = new URL(Constants.PANEL_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            logger.info("Diagnostic: Basic connectivity test - Response code: " + responseCode);
            
            if (responseCode >= 200 && responseCode < 400) {
                logger.info("Diagnostic: Panel server is reachable");
            } else if (responseCode == 502) {
                logger.warning("Diagnostic: Panel server returned 502 - server may be misconfigured");
            } else {
                logger.warning("Diagnostic: Panel server returned unexpected code: " + responseCode);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            logger.severe("Diagnostic: Failed to connect to panel server: " + e.getMessage());
            if (e.getMessage().contains("UnknownHostException")) {
                logger.severe("Diagnostic: Domain name '123.cobl.gg' cannot be resolved - check DNS");
            } else if (e.getMessage().contains("ConnectException")) {
                logger.severe("Diagnostic: Connection refused - server may be down");
            } else if (e.getMessage().contains("SocketTimeoutException")) {
                logger.severe("Diagnostic: Connection timeout - server may be slow or unreachable");
            }
        }
    }
    
    /**
     * Perform a sync operation
     */
    private void performSync() {
        try {
            // Get online players
            Collection<AbstractPlayer> onlinePlayers = platform.getOnlinePlayers();
            
            // Build sync request
            SyncRequest request = new SyncRequest();
            request.setLastSyncTimestamp(lastSyncTimestamp);
            request.setOnlinePlayers(buildOnlinePlayersList(onlinePlayers));
            request.setServerStatus(buildServerStatus(onlinePlayers));
            
            // Send sync request
            CompletableFuture<SyncResponse> syncFuture = httpClient.sync(request);
            
            syncFuture.thenAccept(response -> {
                try {
                    handleSyncResponse(response);
                } catch (Exception e) {
                    logger.severe("Error handling sync response: " + e.getMessage());
                    e.printStackTrace();
                }
            }).exceptionally(throwable -> {
                if (throwable.getCause() instanceof PanelUnavailableException) {
                    logger.warning("Sync request failed: Panel temporarily unavailable (502 error)");
                } else {
                    logger.warning("Sync request failed: " + throwable.getMessage());
                }
                return null;
            });
            
        } catch (Exception e) {
            logger.severe("Error during sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build online players list for sync request
     */
    private List<SyncRequest.OnlinePlayer> buildOnlinePlayersList(Collection<AbstractPlayer> onlinePlayers) {
        return onlinePlayers.stream()
                .map(player -> new SyncRequest.OnlinePlayer(
                        player.getUuid().toString(),
                        player.getName(),
                        player.getIpAddress()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Build server status for sync request
     */
    private SyncRequest.ServerStatus buildServerStatus(Collection<AbstractPlayer> onlinePlayers) {
        return new SyncRequest.ServerStatus(
                onlinePlayers.size(),
                platform.getMaxPlayers(),
                platform.getServerVersion(),
                Instant.now().toString()
        );
    }
    
    /**
     * Handle sync response from the panel
     */
    private void handleSyncResponse(SyncResponse response) {
        // Update last sync timestamp
        this.lastSyncTimestamp = response.getTimestamp();
        
        SyncResponse.SyncData data = response.getData();
        
        // Process pending punishments
        for (SyncResponse.PendingPunishment pending : data.getPendingPunishments()) {
            processPendingPunishment(pending);
        }
        
        // Process recently started punishments
        for (SyncResponse.PendingPunishment started : data.getRecentlyStartedPunishments()) {
            processStartedPunishment(started);
        }
        
        // Process modified punishments (pardons, duration changes)
        for (SyncResponse.ModifiedPunishment modified : data.getRecentlyModifiedPunishments()) {
            processModifiedPunishment(modified);
        }
        
        // Process active staff members (only if present - removed from sync in favor of startup loading)
        if (data.getActiveStaffMembers() != null) {
            for (SyncResponse.ActiveStaffMember staffMember : data.getActiveStaffMembers()) {
                processActiveStaffMember(staffMember);
            }
        }
        
        // Process player notifications
        for (SyncResponse.PlayerNotification notification : data.getPlayerNotifications()) {
            processPlayerNotification(notification);
        }
    }
    
    /**
     * Process a pending punishment that needs to be executed
     */
    private void processPendingPunishment(SyncResponse.PendingPunishment pending) {
        String playerUuid = pending.getMinecraftUuid();
        String username = pending.getUsername();
        SimplePunishment punishment = pending.getPunishment();
        
        logger.info(String.format("Processing pending punishment %s for %s (%s)", 
                punishment.getId(), username, punishment.getType()));
        
        // Execute on main thread for platform-specific operations
        platform.runOnMainThread(() -> {
            boolean success = executePunishment(playerUuid, username, punishment);
            
            // Acknowledge the punishment execution (this will start the punishment)
            acknowledgePunishment(punishment.getId(), playerUuid, success, null);
        });
    }
    
    /**
     * Process a recently started punishment
     */
    private void processStartedPunishment(SyncResponse.PendingPunishment started) {
        // Same as pending punishment processing
        processPendingPunishment(started);
    }
    
    /**
     * Process a modified punishment (pardon, duration change)
     */
    private void processModifiedPunishment(SyncResponse.ModifiedPunishment modified) {
        String playerUuid = modified.getMinecraftUuid();
        String username = modified.getUsername();
        SyncResponse.PunishmentWithModifications punishment = modified.getPunishment();
        
        platform.runOnMainThread(() -> {
            for (SyncResponse.PunishmentModification modification : punishment.getModifications()) {
                handlePunishmentModification(playerUuid, username, punishment.getId(), modification);
            }
        });
    }
    
    /**
     * Execute a punishment on the server
     */
    private boolean executePunishment(String playerUuid, String username, SimplePunishment punishment) {
        try {
            UUID uuid = UUID.fromString(playerUuid);
            AbstractPlayer player = platform.getPlayer(uuid);
            
            if (punishment.isBan()) {
                return executeBan(uuid, username, punishment);
            } else if (punishment.isMute()) {
                return executeMute(uuid, username, punishment);
            } else if (punishment.isKick()) {
                return executeKick(uuid, username, punishment);
            } else {
                logger.warning("Unknown punishment type: " + punishment.getType());
                return false;
            }
        } catch (Exception e) {
            logger.severe("Error executing punishment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Execute a mute punishment
     */
    private boolean executeMute(UUID uuid, String username, SimplePunishment punishment) {
        try {
            // Add to cache for immediate effect
            cache.cacheMute(uuid, punishment);
            
            // Broadcast punishment
            String broadcastMessage = PunishmentMessages.formatPunishmentBroadcast(username, punishment, "muted", localeManager);
            platform.broadcast(broadcastMessage);
            
            logger.info(String.format("Successfully executed mute for %s: %s", username, punishment.getDescription()));
            return true;
        } catch (Exception e) {
            logger.severe("Error executing mute: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a ban punishment
     */
    private boolean executeBan(UUID uuid, String username, SimplePunishment punishment) {
        try {
            // Kick player if online
            AbstractPlayer player = platform.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Use proper ban message from punishment types (ordinal 2)
                Map<String, String> variables = new HashMap<>();
                variables.put("target", "You");
                variables.put("reason", punishment.getDescription() != null ? punishment.getDescription() : "No reason specified");
                variables.put("duration", punishment.isPermanent() ? "permanent" : PunishmentMessages.formatDuration(punishment.getExpiration() - System.currentTimeMillis()));
                variables.put("appeal_url", localeManager.getMessage("config.appeal_url"));
                
                String kickMsg = localeManager.getPlayerNotificationMessage(2, variables);
                platform.kickPlayer(player, kickMsg);
            }
            
            // Broadcast punishment
            String broadcastMessage = PunishmentMessages.formatPunishmentBroadcast(username, punishment, "banned", localeManager);
            platform.broadcast(broadcastMessage);
            
            logger.info(String.format("Successfully executed ban for %s: %s", username, punishment.getDescription()));
            return true;
        } catch (Exception e) {
            logger.severe("Error executing ban: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a kick punishment
     */
    private boolean executeKick(UUID uuid, String username, SimplePunishment punishment) {
        try {
            // Kick player if online
            AbstractPlayer player = platform.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Use proper kick message formatting
                String kickMsg = PunishmentMessages.formatKickMessage(punishment, localeManager);
                platform.kickPlayer(player, kickMsg);
                
                logger.info(String.format("Successfully executed kick for %s: %s", username, punishment.getDescription()));
                return true;
            } else {
                logger.info(String.format("Player %s is not online, kick punishment ignored", username));
                return true; // Still considered successful since player is offline
            }
        } catch (Exception e) {
            logger.severe("Error executing kick: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle punishment modifications (pardons, duration changes)
     */
    private void handlePunishmentModification(String playerUuid, String username, String punishmentId, 
                                               SyncResponse.PunishmentModification modification) {
        try {
            UUID uuid = UUID.fromString(playerUuid);
            
            switch (modification.getType()) {
                case "MANUAL_PARDON":
                case "APPEAL_ACCEPT":
                    handlePardon(uuid, username, punishmentId);
                    break;
                case "MANUAL_DURATION_CHANGE":
                case "APPEAL_DURATION_CHANGE":
                    handleDurationChange(uuid, username, punishmentId, modification.getEffectiveDuration());
                    break;
                default:
                    logger.info("Unknown modification type: " + modification.getType());
            }
        } catch (Exception e) {
            logger.severe("Error handling punishment modification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle punishment pardon
     */
    private void handlePardon(UUID uuid, String username, String punishmentId) {
        // Remove from cache
        cache.removePlayer(uuid);
        
        logger.info(String.format("Pardoned punishment %s for %s", punishmentId, username));
        platform.broadcast(String.format("§a%s has been pardoned", username));
    }
    
    /**
     * Handle punishment duration change
     */
    private void handleDurationChange(UUID uuid, String username, String punishmentId, Long newDuration) {
        // Update cache if needed
        // Implementation depends on specific cache structure
        
        logger.info(String.format("Updated punishment %s duration for %s to %d ms", 
                punishmentId, username, newDuration));
    }
    
    /**
     * Acknowledge punishment execution to the panel
     */
    private void acknowledgePunishment(String punishmentId, String playerUuid, boolean success, String errorMessage) {
        PunishmentAcknowledgeRequest request = new PunishmentAcknowledgeRequest(
                punishmentId,
                playerUuid,
                Instant.now().toString(),
                success,
                errorMessage
        );
        
        httpClient.acknowledgePunishment(request)
                .thenAccept(response -> {
                    logger.info(String.format("Acknowledged punishment %s execution: %s", 
                            punishmentId, success ? "SUCCESS" : "FAILED"));
                })
                .exceptionally(throwable -> {
                    if (throwable.getCause() instanceof PanelUnavailableException) {
                        logger.warning("Failed to acknowledge punishment " + punishmentId + ": Panel temporarily unavailable");
                    } else {
                        logger.warning("Failed to acknowledge punishment " + punishmentId + ": " + throwable.getMessage());
                    }
                    return null;
                });
    }
    
    /**
     * Process active staff member information
     */
    private void processActiveStaffMember(SyncResponse.ActiveStaffMember staffMember) {
        try {
            UUID uuid = UUID.fromString(staffMember.getMinecraftUuid());
            AbstractPlayer player = platform.getPlayer(uuid);
            
            if (player != null && player.isOnline()) {
                // Cache staff member information for easy access
                cache.cacheStaffMember(uuid, staffMember);
                
                logger.info(String.format("Updated staff member data for %s (%s) - Role: %s, Permissions: %d", 
                        staffMember.getMinecraftUsername(), 
                        staffMember.getStaffUsername(),
                        staffMember.getStaffRole(),
                        staffMember.getPermissions().size()));


            }
        } catch (Exception e) {
            logger.warning("Error processing staff member data: " + e.getMessage());
        }
    }
    
    /**
     * Process a player notification
     */
    private void processPlayerNotification(SyncResponse.PlayerNotification notification) {
        try {
            logger.info(String.format("Processing notification %s (type: %s): %s", 
                    notification.getId(), notification.getType(), notification.getMessage()));
            
            // Check if this notification has a target player UUID
            String targetPlayerUuid = notification.getTargetPlayerUuid();
            if (targetPlayerUuid != null && !targetPlayerUuid.isEmpty()) {
                try {
                    UUID playerUuid = UUID.fromString(targetPlayerUuid);
                    deliverNotificationToPlayer(playerUuid, notification);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID format in notification: " + targetPlayerUuid);
                }
            } else {
                // Fallback for old format - deliver to all online players (deprecated)
                handleNotificationForAllOnlinePlayers(notification);
            }
            
        } catch (Exception e) {
            logger.severe("Error processing player notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Deliver a notification to a specific player
     */
    private void deliverNotificationToPlayer(UUID playerUuid, SyncResponse.PlayerNotification notification) {
        AbstractPlayer player = platform.getPlayer(playerUuid);
        
        if (player != null && player.isOnline()) {
            // Player is online - deliver immediately
            String message;
            Map<String, Object> data = notification.getData();
            
            // Check if we have a ticket URL for clickable messages
            if (data != null && data.containsKey("ticketUrl")) {
                String ticketUrl = (String) data.get("ticketUrl");
                String ticketId = (String) data.get("ticketId");
                
                // Create a simpler clickable message format that works across platforms
                message = String.format(
                    "{\"text\":\"\",\"extra\":[" +
                    "{\"text\":\"[Ticket] \",\"color\":\"gold\"}," +
                    "{\"text\":\"%s \",\"color\":\"white\"}," +
                    "{\"text\":\"[Click to view]\",\"color\":\"aqua\",\"underlined\":true," +
                    "\"clickEvent\":{\"action\":\"open_url\",\"value\":\"%s\"}," +
                    "\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click to view ticket %s\"}}]}",
                    notification.getMessage().replace("\"", "\\\""), ticketUrl, ticketId
                );
                
                logger.info("Sending clickable notification JSON: " + message);
                
                platform.runOnMainThread(() -> {
                    platform.sendJsonMessage(playerUuid, message);
                });
            } else {
                // Fallback to regular message format
                message = localeManager.getMessage("notification.ticket_reply", Map.of(
                    "message", notification.getMessage()
                ));
                
                platform.runOnMainThread(() -> {
                    platform.sendMessage(playerUuid, message);
                });
            }
            
            logger.info(String.format("Delivered notification %s to online player %s", 
                    notification.getId(), player.getName()));
        } else {
            // Player is offline - cache for later delivery
            cache.cacheNotification(playerUuid, notification);
            logger.info(String.format("Cached notification %s for offline player %s", 
                    notification.getId(), playerUuid));
        }
    }
    
    /**
     * Deliver a notification immediately for a player who just logged in
     */
    public void deliverLoginNotification(UUID playerUuid, SyncResponse.PlayerNotification notification) {
        logger.info(String.format("Delivering login notification %s to player %s", 
                notification.getId(), playerUuid));
        deliverNotificationToPlayer(playerUuid, notification);
    }
    
    /**
     * Handle notification delivery for online players
     * This is a temporary solution until we have proper player UUID targeting
     */
    private void handleNotificationForAllOnlinePlayers(SyncResponse.PlayerNotification notification) {
        Collection<AbstractPlayer> onlinePlayers = platform.getOnlinePlayers();
        
        for (AbstractPlayer player : onlinePlayers) {
            try {
                UUID playerUuid = player.getUuid();
                
                // Check if this notification is relevant to this player
                // For ticket notifications, we might check if they created recent tickets
                // For now, we'll cache it for all players and let them see it on login
                
                // Cache the notification
                cache.cacheNotification(playerUuid, notification);
                
                // Deliver immediately if player is online
                deliverNotificationToPlayer(playerUuid, notification);
                
            } catch (Exception e) {
                logger.warning("Error handling notification for player " + player.getName() + ": " + e.getMessage());
            }
        }
    }
    
    
    /**
     * Deliver all pending notifications to a player (called on login)
     */
    public void deliverPendingNotifications(UUID playerUuid) {
        try {
            List<Cache.PendingNotification> pendingNotifications = cache.getPendingNotifications(playerUuid);
            
            if (pendingNotifications.isEmpty()) {
                return;
            }
            
            // Create a copy of the list to avoid ConcurrentModificationException
            List<Cache.PendingNotification> notificationsToProcess = new ArrayList<>(pendingNotifications);
            
            logger.info(String.format("Delivering %d pending notifications to player %s", 
                    notificationsToProcess.size(), playerUuid));
            
            List<String> deliveredNotificationIds = new ArrayList<>();
            List<String> expiredNotificationIds = new ArrayList<>();
            
            // Deliver notifications with delays and sound effects
            deliverNotificationsWithDelay(playerUuid, notificationsToProcess, deliveredNotificationIds, expiredNotificationIds);
            
            // Remove all expired notifications in batch
            for (String expiredId : expiredNotificationIds) {
                cache.removeNotification(playerUuid, expiredId);
            }
            
            // Remove all delivered notifications in batch
            for (String deliveredId : deliveredNotificationIds) {
                cache.removeNotification(playerUuid, deliveredId);
            }
            
            // Acknowledge all delivered notifications
            if (!deliveredNotificationIds.isEmpty()) {
                acknowledgeNotifications(playerUuid, deliveredNotificationIds);
            }
            
        } catch (Exception e) {
            logger.severe("Error delivering pending notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Format notification message for display
     */
    private String formatNotificationMessage(SyncResponse.PlayerNotification notification) {
        return localeManager.getMessage("notification.modl_prefix", Map.of("message", notification.getMessage()));
    }
    
    /**
     * Format pending notification message for display
     */
    private String formatNotificationMessage(Cache.PendingNotification notification) {
        return localeManager.getMessage("notification.ticket_prefix", Map.of("message", notification.getMessage()));
    }
    
    /**
     * Deliver a pending notification to a player with clickable links for ticket notifications
     */
    private void deliverPendingNotificationToPlayer(UUID playerUuid, Cache.PendingNotification pending) {
        AbstractPlayer player = platform.getPlayer(playerUuid);
        
        if (player != null && player.isOnline()) {
            // Check if we have ticket data for clickable messages
            Map<String, Object> data = pending.getData();
            if (data != null && data.containsKey("ticketUrl")) {
                String ticketUrl = (String) data.get("ticketUrl");
                String ticketId = (String) data.get("ticketId");
                
                // Create a clickable message format similar to sync notifications
                String message = String.format(
                    "{\"text\":\"\",\"extra\":[" +
                    "{\"text\":\"[Ticket] \",\"color\":\"gold\"}," +
                    "{\"text\":\"%s \",\"color\":\"white\"}," +
                    "{\"text\":\"[Click to view]\",\"color\":\"aqua\",\"underlined\":true," +
                    "\"clickEvent\":{\"action\":\"open_url\",\"value\":\"%s\"}," +
                    "\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click to view ticket %s\"}}]}",
                    pending.getMessage().replace("\"", "\\\""), ticketUrl, ticketId
                );
                
                platform.runOnMainThread(() -> {
                    platform.sendJsonMessage(playerUuid, message);
                });
            } else {
                // Fallback to regular message format
                String message = formatNotificationMessage(pending);
                platform.runOnMainThread(() -> {
                    platform.sendMessage(playerUuid, message);
                });
            }
        }
    }
    
    /**
     * Deliver notifications with delays and sound effects for login
     */
    private void deliverNotificationsWithDelay(UUID playerUuid, List<Cache.PendingNotification> notifications, 
                                             List<String> deliveredIds, List<String> expiredIds) {
        if (notifications.isEmpty()) {
            return;
        }
        
        // Start delivery after initial delay (2 seconds after login)
        syncExecutor.schedule(() -> {
            platform.runOnMainThread(() -> {
                deliverNotificationAtIndex(playerUuid, notifications, 0, deliveredIds, expiredIds);
            });
        }, 2000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Recursively deliver notifications with delays between each one
     */
    private void deliverNotificationAtIndex(UUID playerUuid, List<Cache.PendingNotification> notifications, 
                                          int index, List<String> deliveredIds, List<String> expiredIds) {
        if (index >= notifications.size()) {
            // All notifications processed, clean up
            finalizePendingNotificationDelivery(playerUuid, deliveredIds, expiredIds);
            return;
        }
        
        Cache.PendingNotification pending = notifications.get(index);
        
        try {
            // Skip expired notifications
            if (pending.isExpired()) {
                expiredIds.add(pending.getId());
            } else {
                // Check if player is still online before delivering
                AbstractPlayer player = platform.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    // Play notification sound effect (placeholder for now)
                    playNotificationSound(playerUuid);
                    
                    // Format and send the notification (with clickable links for ticket notifications)
                    deliverPendingNotificationToPlayer(playerUuid, pending);
                    
                    // Track for acknowledgment and removal
                    deliveredIds.add(pending.getId());
                    
                    logger.info(String.format("Delivered pending notification %s to player %s", 
                            pending.getId(), playerUuid));
                } else {
                    // Player disconnected, stop delivery
                    logger.info(String.format("Player %s disconnected during notification delivery", playerUuid));
                    return;
                }
            }
        } catch (Exception e) {
            logger.warning("Error delivering pending notification " + pending.getId() + ": " + e.getMessage());
        }
        
        // Schedule next notification delivery with delay (1.5 seconds between notifications)
        syncExecutor.schedule(() -> {
            platform.runOnMainThread(() -> {
                deliverNotificationAtIndex(playerUuid, notifications, index + 1, deliveredIds, expiredIds);
            });
        }, 1500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Finalize the pending notification delivery by cleaning up and acknowledging
     */
    private void finalizePendingNotificationDelivery(UUID playerUuid, List<String> deliveredIds, List<String> expiredIds) {
        try {
            // Remove all expired notifications in batch
            for (String expiredId : expiredIds) {
                cache.removeNotification(playerUuid, expiredId);
            }
            
            // Remove all delivered notifications in batch
            for (String deliveredId : deliveredIds) {
                cache.removeNotification(playerUuid, deliveredId);
            }
            
            // Acknowledge all delivered notifications
            if (!deliveredIds.isEmpty()) {
                acknowledgeNotifications(playerUuid, deliveredIds);
            }
            
            logger.info(String.format("Completed pending notification delivery for player %s. " +
                    "Delivered: %d, Expired: %d", playerUuid, deliveredIds.size(), expiredIds.size()));
                    
        } catch (Exception e) {
            logger.severe("Error finalizing pending notification delivery: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Play notification sound effect (placeholder - to be implemented per platform)
     */
    private void playNotificationSound(UUID playerUuid) {
        // TODO: Implement sound playing per platform
        // For now, this is a placeholder that can be extended in platform implementations
        // Example sounds: "entity.experience_orb.pickup", "block.note_block.pling", "entity.player.levelup"
        
        // This could be implemented by:
        // 1. Adding a playSound method to the Platform interface
        // 2. Implementing it in each platform (Spigot, Velocity, BungeeCord)
        // 3. Using the appropriate platform-specific sound API
        
        logger.fine("Playing notification sound for player " + playerUuid + " (placeholder)");
    }
    
    /**
     * Acknowledge a single notification to the panel
     */
    private void acknowledgeNotification(UUID playerUuid, String notificationId) {
        acknowledgeNotifications(playerUuid, List.of(notificationId));
    }
    
    /**
     * Acknowledge multiple notifications to the panel
     */
    private void acknowledgeNotifications(UUID playerUuid, List<String> notificationIds) {
        try {
            NotificationAcknowledgeRequest request = new NotificationAcknowledgeRequest(
                    playerUuid.toString(),
                    notificationIds,
                    Instant.now().toString()
            );
            
            httpClient.acknowledgeNotifications(request)
                    .thenAccept(response -> {
                        logger.info(String.format("Acknowledged %d notifications for player %s", 
                                notificationIds.size(), playerUuid));
                    })
                    .exceptionally(throwable -> {
                        if (throwable.getCause() instanceof PanelUnavailableException) {
                            logger.warning("Failed to acknowledge notifications for player " + playerUuid + ": Panel temporarily unavailable");
                        } else {
                            logger.warning("Failed to acknowledge notifications for player " + playerUuid + ": " + throwable.getMessage());
                        }
                        return null;
                    });
                    
        } catch (Exception e) {
            logger.severe("Error acknowledging notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}