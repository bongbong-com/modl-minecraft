package com.bongbong.modl.minecraft.api.http;

import com.bongbong.modl.minecraft.api.http.request.*;
import com.bongbong.modl.minecraft.api.http.response.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ModlHttpClient {
    @NotNull
    CompletableFuture<PlayerProfileResponse> getPlayerProfile(@NotNull UUID uuid);

    @NotNull
    CompletableFuture<LinkedAccountsResponse> getLinkedAccounts(@NotNull UUID uuid);

    @NotNull
    CompletableFuture<PlayerLoginResponse> playerLogin(@NotNull PlayerLoginRequest request);

    @NotNull
    CompletableFuture<Void> playerDisconnect(@NotNull PlayerDisconnectRequest request);

    @NotNull
    CompletableFuture<CreateTicketResponse> createTicket(@NotNull CreateTicketRequest request);

    @NotNull
    CompletableFuture<CreateTicketResponse> createUnfinishedTicket(@NotNull CreateTicketRequest request);

    // Legacy methods for backward compatibility
    @NotNull
    CompletableFuture<Void> createPunishment(@NotNull CreatePunishmentRequest request);

    @NotNull
    CompletableFuture<Void> createPlayerNote(@NotNull CreatePlayerNoteRequest request);

    // New methods with proper return types
    @NotNull
    CompletableFuture<PunishmentCreateResponse> createPunishmentWithResponse(@NotNull PunishmentCreateRequest request);

    @NotNull
    CompletableFuture<PlayerGetResponse> getPlayer(@NotNull PlayerGetRequest request);

    @NotNull
    CompletableFuture<PlayerNoteCreateResponse> createPlayerNoteWithResponse(@NotNull PlayerNoteCreateRequest request);
}