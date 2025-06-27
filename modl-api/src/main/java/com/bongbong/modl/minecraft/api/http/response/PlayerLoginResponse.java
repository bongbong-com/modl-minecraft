package com.bongbong.modl.minecraft.api.http.response;

import com.bongbong.modl.minecraft.api.Punishment;
import lombok.Data;

import java.util.List;

@Data
public class PlayerLoginResponse {
    private final boolean success;
    private final String message;
    private final List<Punishment> activePunishments;
    
    public PlayerLoginResponse(boolean success, String message, List<Punishment> activePunishments) {
        this.success = success;
        this.message = message;
        this.activePunishments = activePunishments != null ? activePunishments : List.of();
    }
    
    public boolean hasActiveBan() {
        return activePunishments.stream().anyMatch(p -> p.getType() == Punishment.Type.BAN);
    }
    
    public boolean hasActiveMute() {
        return activePunishments.stream().anyMatch(p -> p.getType() == Punishment.Type.MUTE);
    }
    
    public Punishment getActiveBan() {
        return activePunishments.stream()
            .filter(p -> p.getType() == Punishment.Type.BAN)
            .findFirst()
            .orElse(null);
    }
    
    public Punishment getActiveMute() {
        return activePunishments.stream()
            .filter(p -> p.getType() == Punishment.Type.MUTE)
            .findFirst()
            .orElse(null);
    }
}