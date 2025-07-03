package com.bongbong.modl.minecraft.api.http.response;

import com.bongbong.modl.minecraft.api.SimplePunishment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerLoginResponse {
    private int status;
    private List<SimplePunishment> activePunishments;
    private List<String> pendingNotifications;
    
    public boolean hasActiveBan() {
        return activePunishments != null && activePunishments.stream().anyMatch(SimplePunishment::isBan);
    }
    
    public boolean hasActiveMute() {
        return activePunishments != null && activePunishments.stream().anyMatch(SimplePunishment::isMute);
    }
    
    public SimplePunishment getActiveBan() {
        return activePunishments != null ? activePunishments.stream()
            .filter(SimplePunishment::isBan)
            .findFirst()
            .orElse(null) : null;
    }
    
    public SimplePunishment getActiveMute() {
        return activePunishments != null ? activePunishments.stream()
            .filter(SimplePunishment::isMute)
            .findFirst()
            .orElse(null) : null;
    }
    
    public boolean hasNotifications() {
        return pendingNotifications != null && !pendingNotifications.isEmpty();
    }
}