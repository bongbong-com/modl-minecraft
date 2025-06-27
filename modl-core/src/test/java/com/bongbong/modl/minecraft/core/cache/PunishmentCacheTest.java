package com.bongbong.modl.minecraft.core.cache;

import com.bongbong.modl.minecraft.api.Punishment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PunishmentCacheTest {
    
    private PunishmentCache cache;
    private UUID testPlayerId;
    private Punishment mockMute;
    
    @BeforeEach
    void setUp() {
        cache = new PunishmentCache();
        testPlayerId = UUID.randomUUID();
        mockMute = createMockMute();
    }
    
    private Punishment createMockMute() {
        Punishment mute = mock(Punishment.class);
        when(mute.getType()).thenReturn(Punishment.Type.MUTE);
        when(mute.getReason()).thenReturn("Test mute reason");
        
        // Mock active mute (not expired)
        Date futureDate = new Date(System.currentTimeMillis() + 86400000L); // 1 day from now
        when(mute.getExpires()).thenReturn(futureDate);
        
        return mute;
    }
    
    private Punishment createExpiredMute() {
        Punishment expiredMute = mock(Punishment.class);
        when(expiredMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(expiredMute.getReason()).thenReturn("Expired mute");
        
        // Mock expired mute
        Date pastDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        when(expiredMute.getExpires()).thenReturn(pastDate);
        
        return expiredMute;
    }
    
    @Test
    void testCacheMute() {
        cache.cacheMute(testPlayerId, mockMute);
        
        assertTrue(cache.isMuted(testPlayerId));
        assertEquals(mockMute, cache.getMute(testPlayerId));
    }
    
    @Test
    void testIsMutedReturnsFalseForNonExistentPlayer() {
        UUID nonExistentPlayer = UUID.randomUUID();
        
        assertFalse(cache.isMuted(nonExistentPlayer));
        assertNull(cache.getMute(nonExistentPlayer));
    }
    
    @Test
    void testCacheActivePunishments() {
        List<Punishment> punishments = Arrays.asList(mockMute);
        
        cache.cacheActivePunishments(testPlayerId, punishments);
        
        assertTrue(cache.isMuted(testPlayerId));
        assertEquals(mockMute, cache.getMute(testPlayerId));
    }
    
    @Test
    void testCacheActivePunishmentsWithoutMute() {
        Punishment mockBan = mock(Punishment.class);
        when(mockBan.getType()).thenReturn(Punishment.Type.BAN);
        
        List<Punishment> punishments = Arrays.asList(mockBan);
        
        cache.cacheActivePunishments(testPlayerId, punishments);
        
        assertFalse(cache.isMuted(testPlayerId));
        assertNull(cache.getMute(testPlayerId));
    }
    
    @Test
    void testCacheActivePunishmentsWithMixedTypes() {
        Punishment mockBan = mock(Punishment.class);
        when(mockBan.getType()).thenReturn(Punishment.Type.BAN);
        
        List<Punishment> punishments = Arrays.asList(mockBan, mockMute);
        
        cache.cacheActivePunishments(testPlayerId, punishments);
        
        assertTrue(cache.isMuted(testPlayerId));
        assertEquals(mockMute, cache.getMute(testPlayerId));
    }
    
    @Test
    void testExpiredMuteIsAutomaticallyRemoved() {
        Punishment expiredMute = createExpiredMute();
        
        cache.cacheMute(testPlayerId, expiredMute);
        
        // Should return false because mute is expired
        assertFalse(cache.isMuted(testPlayerId));
        
        // The expired mute should be removed from cache
        assertNull(cache.getMute(testPlayerId));
    }
    
    @Test
    void testRemoveMute() {
        cache.cacheMute(testPlayerId, mockMute);
        assertTrue(cache.isMuted(testPlayerId));
        
        cache.removeMute(testPlayerId);
        
        assertFalse(cache.isMuted(testPlayerId));
        assertNull(cache.getMute(testPlayerId));
    }
    
    @Test
    void testRemovePlayer() {
        cache.cacheMute(testPlayerId, mockMute);
        assertTrue(cache.isMuted(testPlayerId));
        
        cache.removePlayer(testPlayerId);
        
        assertFalse(cache.isMuted(testPlayerId));
        assertNull(cache.getMute(testPlayerId));
    }
    
    @Test
    void testClearCache() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        cache.cacheMute(player1, mockMute);
        cache.cacheMute(player2, mockMute);
        
        assertTrue(cache.isMuted(player1));
        assertTrue(cache.isMuted(player2));
        
        cache.clear();
        
        assertFalse(cache.isMuted(player1));
        assertFalse(cache.isMuted(player2));
        assertEquals(0, cache.size());
    }
    
    @Test
    void testCacheSize() {
        assertEquals(0, cache.size());
        
        cache.cacheMute(UUID.randomUUID(), mockMute);
        assertEquals(1, cache.size());
        
        cache.cacheMute(UUID.randomUUID(), mockMute);
        assertEquals(2, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
    }
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<UUID> playerIds = new ArrayList<>();
        
        // Create 100 test players
        for (int i = 0; i < 100; i++) {
            playerIds.add(UUID.randomUUID());
        }
        
        // Concurrently cache mutes
        for (UUID playerId : playerIds) {
            executor.submit(() -> cache.cacheMute(playerId, mockMute));
        }
        
        // Concurrently check mutes
        for (UUID playerId : playerIds) {
            executor.submit(() -> {
                cache.isMuted(playerId);
                cache.getMute(playerId);
            });
        }
        
        // Concurrently remove some players
        for (int i = 0; i < 50; i++) {
            UUID playerId = playerIds.get(i);
            executor.submit(() -> cache.removePlayer(playerId));
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // Verify final state
        assertTrue(cache.size() <= 100);
        assertTrue(cache.size() >= 50);
    }
    
    @Test
    void testNullMuteHandling() {
        cache.cacheMute(testPlayerId, null);
        
        assertFalse(cache.isMuted(testPlayerId));
        assertNull(cache.getMute(testPlayerId));
    }
    
    @Test
    void testPermanentMute() {
        Punishment permanentMute = mock(Punishment.class);
        when(permanentMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(permanentMute.getReason()).thenReturn("Permanent mute");
        when(permanentMute.getExpires()).thenReturn(null); // No expiry date
        
        cache.cacheMute(testPlayerId, permanentMute);
        
        assertTrue(cache.isMuted(testPlayerId));
        assertEquals(permanentMute, cache.getMute(testPlayerId));
    }
    
    @Test
    void testOverwriteExistingMute() {
        Punishment firstMute = mockMute;
        Punishment secondMute = mock(Punishment.class);
        when(secondMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(secondMute.getReason()).thenReturn("Second mute");
        when(secondMute.getExpires()).thenReturn(new Date(System.currentTimeMillis() + 172800000L)); // 2 days
        
        cache.cacheMute(testPlayerId, firstMute);
        assertEquals(firstMute, cache.getMute(testPlayerId));
        
        cache.cacheMute(testPlayerId, secondMute);
        assertEquals(secondMute, cache.getMute(testPlayerId));
    }
    
    @Test
    void testRemoveNonExistentPlayer() {
        UUID nonExistentPlayer = UUID.randomUUID();
        
        // Should not throw exception
        assertDoesNotThrow(() -> cache.removePlayer(nonExistentPlayer));
        assertDoesNotThrow(() -> cache.removeMute(nonExistentPlayer));
    }
}