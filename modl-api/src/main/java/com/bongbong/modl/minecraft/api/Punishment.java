package com.bongbong.modl.minecraft.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class Punishment {
    @NotNull
    @SerializedName("id")
    private final String id;

    @NotNull
    @SerializedName("issuerName")
    private final String issuerName;

    @NotNull
    @SerializedName("issued")
    private final Date issued;

    @SerializedName("started")
    private final Date started;

    @SerializedName("type")
    private final Type type;

    @NotNull
    @SerializedName("modifications")
    private final List<Modification> modifications;

    @NotNull
    @SerializedName("notes")
    private final List<Note> notes;

    @NotNull
    @SerializedName("attachedTicketIds")
    private final List<String> attachedTicketIds;

    @NotNull
    @SerializedName("data")
    private final Map<String, Object> dataMap;
    
    // Lazy-initialized structured data
    private transient PunishmentData data;

    public PunishmentData getData() {
        if (data == null) {
            data = PunishmentData.fromMap(dataMap);
        }
        return data;
    }
    
    public String getReason() {
        Object reason = dataMap.get("reason");
        return reason instanceof String ? (String) reason : null;
    }
    
    public Date getExpires() {
        Object expires = dataMap.get("expires");
        if (expires instanceof Date) {
            return (Date) expires;
        } else if (expires instanceof Long) {
            return new Date((Long) expires);
        }
        return null;
    }
    
    public boolean isActive() {
        // Check manual active flag
        Object activeFlag = dataMap.get("active");
        if (activeFlag instanceof Boolean && !((Boolean) activeFlag)) {
            return false;
        }
        
        // Check expiry date
        Date expiry = getExpires();
        if (expiry != null && expiry.before(new Date())) {
            return false;
        }
        
        // Bans and mutes must be started to be active
        if (type == Type.BAN || type == Type.MUTE) {
            return started != null;
        }
        
        return true;
    }

    @RequiredArgsConstructor
    public enum Type {
        BAN,
        MUTE
    }
}
