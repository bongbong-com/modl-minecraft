package com.bongbong.modl.minecraft.api.http.request;

import com.google.gson.JsonObject;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class CreateTicketRequest {
    @NotNull
    private final String creatorUuid;
    @NotNull
    private final String creatorUsername;
    @NotNull
    private final String type;
    @NotNull
    private final String subject;
    @NotNull
    private final String reportedPlayerUuid;
    @NotNull
    private final String reportedPlayerUsername;
    @Nullable
    private final List<String> chatMessages;
    @Nullable
    private final JsonObject formData;
}