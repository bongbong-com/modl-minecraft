package com.bongbong.modl.minecraft.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Account(
        @NotNull UUID uuid,
        @NotNull String username,
        @NotNull List<Note> notes,
        @NotNull List<IPAddress> ipList,
        @NotNull List<Punishment> punishments,
        @NotNull List<String> pendingNotifications
        ) {

        public Map<String, Object> export() {
                return Map.of(
                        "username", username,
                        "notes", notes.stream().map(Note::export).toList(),
                        "ipList", ipList.stream().map(IPAddress::export).toList(),
                        "punishments", punishments.stream().map(Punishment::export).toList(),
                        "pendingNotifications", pendingNotifications
                );
        }

        @SuppressWarnings("unchecked")
        public static Account fromMap(Map<String, Object> map) {
                return new Account(
                        (UUID) map.get("_id"),
                        (String) map.get("username"),
                        new ArrayList<>(((List<Map<String, Object>>) map.get("notes")).stream().map(Note::fromMap).toList()),
                        new ArrayList<>(((List<Map<String, Object>>) map.get("ipList")).stream().map(IPAddress::fromMap).toList()),
                        new ArrayList<>(((List<Map<String, Object>>) map.get("punishments")).stream().map(Punishment::fromMap).toList()),
                        new ArrayList<>((List<String>) map.get("pendingNotifications"))
                );
        }
}
