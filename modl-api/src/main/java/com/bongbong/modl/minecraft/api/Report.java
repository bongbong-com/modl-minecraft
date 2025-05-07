package com.bongbong.modl.minecraft.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record Report(
        @NotNull UUID id,
        @NotNull String witness,
        @Nullable UUID witnessId,
        @NotNull UUID subject,
        @NotNull Type type,
        @NotNull String data,
        @NotNull Date date
) {
    public enum Type {
        CHAT,
        USERNAME,
        SKIN,
        CONTENT,
        TEAM,
        GAME,
        CHEATING,
        OTHER
    }

    public Map<String, Object> export() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("witness", witness);
        if (witnessId != null) map.put("witnessId", witnessId);
        map.put("subject", subject);
        map.put("type", type.name());
        map.put("data", data);
        map.put("date", date.getTime());
        return map;
    }

    public static Report fromMap(Map<String, Object> map) {
        return new Report(
                (UUID) map.get("id"),
                (String) map.get("witness"),
                map.get("witnessId") == null ? null : (UUID) map.get("witnessId"),
                (UUID) map.get("subject"),
                Type.valueOf((String) map.get("type")),
                (String) map.get("data"),
                new Date((Long) map.get("date"))
        );
    }
}
