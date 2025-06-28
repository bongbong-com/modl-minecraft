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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ModlHttpClientImpl implements ModlHttpClient {
    @NotNull
    private final String baseUrl;
    @NotNull
    private final String apiKey;
    @NotNull
    private final HttpClient httpClient;
    @NotNull
    private final Gson gson;

    public ModlHttpClientImpl(@NotNull String baseUrl, @NotNull String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerProfileResponse> getPlayerProfile(@NotNull UUID uuid) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players/" + uuid))
                .header("X-API-Key", apiKey)
                .build(), PlayerProfileResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<LinkedAccountsResponse> getLinkedAccounts(@NotNull UUID uuid) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players/" + uuid + "/linked-accounts"))
                .header("X-API-Key", apiKey)
                .build(), LinkedAccountsResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<PlayerLoginResponse> playerLogin(@NotNull PlayerLoginRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players/login"))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build(), PlayerLoginResponse.class);
    }

    @NotNull
    @Override
    public CompletableFuture<Void> playerDisconnect(@NotNull PlayerDisconnectRequest request) {
        return sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/players/disconnect"))
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
                .uri(URI.create(baseUrl + "/players/" + request.getTargetUuid() + "/notes"))
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

    private <T> CompletableFuture<T> sendAsync(HttpRequest request, Class<T> responseType) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        if (responseType == Void.class) {
                            return null;
                        }
                        return gson.fromJson(response.body(), responseType);
                    } else {
                        throw new RuntimeException("Request failed with status code " + response.statusCode() + ": " + response.body());
                    }
                });
    }
}