package com.bongbong.modl.minecraft.api;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public record Modification(
        @NotNull Type type,
        @NotNull String issuer, // username or a discord name
        @NotNull Date issued,
        long effectiveDuration
){

    // Pardons/accepted appeals are wrongful, if you want to unban someone without reversing the points, change the duration 0 minutes.
    @RequiredArgsConstructor
    public enum Type {
        MANUAL_DURATION_CHANGE,
        MANUAL_PARDON,
        APPEAL_REJECT,
        APPEAL_DURATION_CHANGE,
        APPEAL_ACCEPT,
        SET_ALT_BLOCKING_TRUE,
        SET_WIPING_TRUE,
        SET_ALT_BLOCKING_FALSE,
        SET_WIPING_FALSE,
    }

    public Map<String, Object> export() {
        return Map.of(
                "type", type.ordinal(),
                "issuer", issuer,
                "issued", issued.getTime(),
                "effectiveDuration", effectiveDuration
        );
    }

    public static Modification fromMap(Map<String, Object> map) {
        return new Modification(
                Type.values()[(int) map.get("type")],
                (String) map.get("issuer"),
                new Date((long) map.get("issued")),
                ((long) map.get("effectiveDuration"))
        );
    }
}
