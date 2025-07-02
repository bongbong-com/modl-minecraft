package com.bongbong.modl.minecraft.core.impl.cache;

import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.SimplePunishment;
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
    
    public void cacheMute(UUID playerUuid, SimplePunishment mute) {
        cache.computeIfAbsent(playerUuid, k -> new CachedPlayerData()).setSimpleMute(mute);
    }

    public boolean isMuted(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        if (data == null) {
            return false;
        }
        
        // Check SimplePunishment first (new format)
        if (data.getSimpleMute() != null) {
            SimplePunishment mute = data.getSimpleMute();
            if (mute.isExpired()) {
                removeMute(playerUuid);
                return false;
            }
            return true;
        }
        
        // Fallback to old Punishment format
        if (data.getMute() != null) {
            Punishment mute = data.getMute();
            if (mute.getExpires() != null && mute.getExpires().before(new java.util.Date())) {
                removeMute(playerUuid);
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    public Punishment getMute(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        return data != null ? data.getMute() : null;
    }
    
    public void removeMute(UUID playerUuid) {
        CachedPlayerData data = cache.get(playerUuid);
        if (data != null) {
            data.setMute(null);
            data.setSimpleMute(null);
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
        private SimplePunishment simpleMute;
        
        public boolean isEmpty() {
            return mute == null && simpleMute == null;
        }
    }
}