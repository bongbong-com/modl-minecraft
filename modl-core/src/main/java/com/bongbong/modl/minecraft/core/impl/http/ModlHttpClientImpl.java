package com.bongbong.modl.minecraft.core.impl.http;

import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.*;
import com.bongbong.modl.minecraft.api.http.response.*;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ModlHttpClientImpl implements ModlHttpClient {
    @NotNull
    private final String baseUrl;
    @NotNull
    private final String apiKey;
    @NotNull
    private final HttpClient httpClient;
    @NotNull
    private final Gson gson;
    @NotNull
    private final Logger logger;
    private final boolean debugMode;

    public ModlHttpClientImpl(@NotNull String baseUrl, @NotNull String apiKey) {
        this(baseUrl, apiKey, false);
    }

    public ModlHttpClientImpl(@NotNull String baseUrl, @NotNull String apiKey, boolean debugMode) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.debugMode = debugMode;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        this.logger = Logger.getLogger(ModlHttpClientImpl.class.getName());
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerProfileResponse> getPlayerProfile(@NotNull UUID uuid) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/player/" + uuid))
                .header("X-API-Key", apiKey)
                .build(), PlayerProfileResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<LinkedAccountsResponse> getLinkedAccounts(@NotNull UUID uuid) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/player/" + uuid + "/linked-accounts"))
                .header("X-API-Key", apiKey)
                .build(), LinkedAccountsResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerLoginResponse> playerLogin(@NotNull PlayerLoginRequest request) {
        String requestBody = gson.toJson(request);
        if (debugMode) {
            logger.info(String.format("Player login request body: %s", requestBody));
        }
        
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/player/login"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build(), PlayerLoginResponse.class, "LOGIN");
    }

    @NotNull
    @Override
    public CompletableFuture<Void> playerDisconnect(@NotNull PlayerDisconnectRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/player/disconnect"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), Void.class);
    }

    @NotNull
    @Override
    public CompletableFuture<CreateTicketResponse> createTicket(@NotNull CreateTicketRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/public/tickets"))
                .header("X-Ticket-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), CreateTicketResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<CreateTicketResponse> createUnfinishedTicket(@NotNull CreateTicketRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/public/tickets/unfinished"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), CreateTicketResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> createPunishment(@NotNull CreatePunishmentRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/punishments"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), Void.class);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> createPlayerNote(@NotNull CreatePlayerNoteRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/player/" + request.getTargetUuid() + "/notes"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), Void.class);
    }

    @NotNull
    @Override
    public CompletableFuture<PunishmentCreateResponse> createPunishmentWithResponse(@NotNull PunishmentCreateRequest request) {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerGetResponse> getPlayer(@NotNull PlayerGetRequest request) {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerNameResponse> getPlayer(@NotNull PlayerNameRequest request) {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerNoteCreateResponse> createPlayerNoteWithResponse(@NotNull PlayerNoteCreateRequest request) {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<SyncResponse> sync(@NotNull SyncRequest request) {
        String requestBody = gson.toJson(request);
        if (debugMode) {
            logger.info(String.format("Sync request body: %s", requestBody));
        }
        
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sync"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build(), SyncResponse.class, "SYNC");
    }

    @NotNull
    @Override
    public CompletableFuture<Void> acknowledgePunishment(@NotNull PunishmentAcknowledgeRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/punishment/acknowledge"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), Void.class);
    }

    private <T> CompletableFuture<T> sendAsync(HttpRequest request, Class<T> responseType) {
        return sendAsync(request, responseType, null);
    }
    
    private <T> CompletableFuture<T> sendAsync(HttpRequest request, Class<T> responseType, String operation) {
        final Instant startTime = Instant.now();
        final String requestId = generateRequestId();
        
        if (debugMode) {
            logger.info(String.format("[REQ-%s] %s %s", requestId, request.method(), request.uri()));
            logger.info(String.format("[REQ-%s] Headers: %s", requestId, request.headers().map()));
            
            // Log request body if present (for POST/PUT requests)
            request.bodyPublisher().ifPresent(body -> {
                logger.info(String.format("[REQ-%s] Body present: %s", requestId, body.getClass().getSimpleName()));
            });
        }
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    final Duration duration = Duration.between(startTime, Instant.now());
                    
                    if (debugMode) {
                        logger.info(String.format("[RES-%s] Status: %d (took %dms)", 
                                requestId, response.statusCode(), duration.toMillis()));
                        logger.info(String.format("[RES-%s] Headers: %s", requestId, response.headers().map()));
                        
                        String body = response.body();
                        if (body != null && !body.isEmpty()) {
                            // Always show full JSON for LOGIN operations, truncate others
                            if ("LOGIN".equals(operation) || body.length() <= 1000) {
                                logger.info(String.format("[RES-%s] Body: %s", requestId, body));
                            } else {
                                logger.info(String.format("[RES-%s] Body: %s... (truncated, %d chars total)", 
                                        requestId, body.substring(0, 1000), body.length()));
                            }
                        }
                    }
                    
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        if (responseType == Void.class) {
                            return null;
                        }
                        
                        try {
                            T result = gson.fromJson(response.body(), responseType);
                            if (debugMode) {
                                logger.info(String.format("[REQ-%s] Successfully parsed response to %s", 
                                        requestId, responseType.getSimpleName()));
                            }
                            return result;
                        } catch (Exception e) {
                            logger.severe(String.format("[REQ-%s] Failed to parse response: %s", requestId, e.getMessage()));
                            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
                        }
                    } else {
                        String errorMsg = String.format("Request failed with status code %d: %s", 
                                response.statusCode(), response.body());
                        
                        // Log additional details for common errors
                        if (response.statusCode() == 502) {
                            logger.severe(String.format("[REQ-%s] Bad Gateway (502) - API server may be down or unreachable", requestId));
                            logger.severe(String.format("[REQ-%s] Request URL: %s", requestId, request.uri()));
                            logger.severe(String.format("[REQ-%s] Request method: %s", requestId, request.method()));
                        } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                            logger.severe(String.format("[REQ-%s] Authentication failed - check API key", requestId));
                        } else if (response.statusCode() == 404) {
                            logger.severe(String.format("[REQ-%s] Endpoint not found - check API URL", requestId));
                        }
                        
                        logger.warning(String.format("[REQ-%s] %s", requestId, errorMsg));
                        throw new RuntimeException(errorMsg);
                    }
                })
                .exceptionally(throwable -> {
                    final Duration duration = Duration.between(startTime, Instant.now());
                    logger.severe(String.format("[REQ-%s] Request failed after %dms: %s", 
                            requestId, duration.toMillis(), throwable.getMessage()));
                    
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw new RuntimeException("HTTP request failed", throwable);
                });
    }
    
    private String generateRequestId() {
        return String.valueOf(System.nanoTime() % 1000000);
    }
}