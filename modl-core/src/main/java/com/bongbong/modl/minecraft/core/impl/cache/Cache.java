package com.bongbong.modl.minecraft.core.impl.cache;

import com.bongbong.modl.minecraft.api.Punishment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    
    private final Map<UUID, CachedPlayerData> cache = new ConcurrentHashMap<>();
    
    public void cacheMute(UUID playerUuid, Punishment mute) {
        cache.computeIfAbsent(playerUuid, k -> new CachedPlayerData()).setMute(mute);
    }

    public boolean isMuted(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        if (data == null || data.getMute() == null) {
            return false;
        }
        
        Punishment mute = data.getMute();
        
        // Check if mute is expired
        if (mute.getExpires() != null && mute.getExpires().before(new java.util.Date())) {
            removeMute(playerUuid);
            return false;
        }
        
        return true;
    }
    
    public Punishment getMute(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        return data != null ? data.getMute() : null;
    }
    
    public void removeMute(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        if (data != null) {
            data.setMute(null);
            if (data.isEmpty()) {
                cache.remove(playerUuid);
            }
        }
    }
    
    public void removePlayer(UUID playerUuid) {
        cache.remove(playerUuid);
    }
    
    public void clear() {
        cache.clear();
    }
    
    public int size() {
        return cache.size();
    }
    
    @Getter
    @Setter
    public static class CachedPlayerData {
        private Punishment mute;
        public boolean isEmpty() {
            return mute == null;
        }
    }
}