package com.bongbong.modl.minecraft.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

// include leniency and level in notes, don't store.
public record PunishmentData(
        @Nullable String blockedName,
        @Nullable String blockedSkin,
        @Nullable String linkedBanId,
        @NotNull AtomicReference<Date> linkedBanExpiry,
        @Nullable List<String> chatLog,
        long duration,
        boolean altBlocking,
        boolean wipeAfterExpiry
        )
{

    public Map<String, Object> export() {
       Map<String, Object> map = new HashMap<>();

       if (blockedName != null) map.put("blockedName", blockedName);
       if (blockedSkin != null) map.put("blockedSkin", blockedSkin);
       if (linkedBanId != null) map.put("linkedBanId", linkedBanId);
       if (linkedBanExpiry.get() != null) map.put("linkedBanExpiry", linkedBanExpiry.get().getTime());
       if (chatLog != null) map.put("chatLog", chatLog);
       if (duration != -1L) map.put("duration", duration);
       if (altBlocking) map.put("altBlocking", true);
       if (wipeAfterExpiry) map.put("wipeAfterExpiry", true);

       return map;
    }

    @SuppressWarnings("unchecked")
    public static PunishmentData fromMap(Map<String, Object> map) {
        return new PunishmentData(
                map.containsKey("blockedName") ? (String) map.get("blockedName") : null,
                map.containsKey("blockedSkin") ? (String) map.get("blockedSkin") : null,
                map.containsKey("linkedBanId") ? (String) map.get("linkedBanId") : null,
                map.containsKey("linkedBanExpiry") ? new AtomicReference<>(new Date((long) map.get("linkedBanExpiry"))) : new AtomicReference<>(null),
                map.containsKey("chatLog") ? new ArrayList<>((List<String>) map.get("chatLog")) : null,
                map.containsKey("duration") ? (long) map.get("duration") : -1L,
                map.containsKey("altBlocking"),
                map.containsKey("wipeAfterExpiry")
        );
    }

}
