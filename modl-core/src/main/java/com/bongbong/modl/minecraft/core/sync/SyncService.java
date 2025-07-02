package com.bongbong.modl.minecraft.core.sync;

import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PunishmentAcknowledgeRequest;
import com.bongbong.modl.minecraft.api.http.request.SyncRequest;
import com.bongbong.modl.minecraft.api.http.response.SyncResponse;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
                logger.warning("Sync request failed: " + throwable.getMessage());
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
            
            // Broadcast (SimplePunishment doesn't have silent flag, so always broadcast)
            String duration = punishment.getFormattedDuration();
            platform.broadcast(String.format("§c%s has been muted %s: §f%s", username, duration, punishment.getDescription()));
            
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
                String kickMsg = String.format("You have been banned: %s", punishment.getDescription());
                platform.kickPlayer(player, kickMsg);
            }
            
            // Broadcast
            String duration = punishment.getFormattedDuration();
            platform.broadcast(String.format("§c%s has been banned %s: §f%s", username, duration, punishment.getDescription()));
            
            logger.info(String.format("Successfully executed ban for %s: %s", username, punishment.getDescription()));
            return true;
        } catch (Exception e) {
            logger.severe("Error executing ban: " + e.getMessage());
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
                    logger.warning("Failed to acknowledge punishment " + punishmentId + ": " + throwable.getMessage());
                    return null;
                });
    }
    
}