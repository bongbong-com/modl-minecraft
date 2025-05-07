package com.bongbong.modl.minecraft.api;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public record Note(
        @NotNull Date issued,
        @NotNull String issuer,
        @NotNull String note
) {

    public Map<String, Object> export() {
        return Map.of(
                "issued", issued.getTime(),
                "issuer", issuer,
                "note", note
        );
    }

    public static Note fromMap(Map<String, Object> map) {
        return new Note(
                new Date((long) map.get("issued")),
                (String) map.get("issuer"),
                (String) map.get("note")
        );
    }
}
