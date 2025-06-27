package com.bongbong.modl.minecraft.api.http.response;

import com.bongbong.modl.minecraft.api.Punishment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerLoginResponseTest {
    
    private Punishment mockBan;
    private Punishment mockMute;
    private List<Punishment> activePunishments;
    
    @BeforeEach
    void setUp() {
        mockBan = mock(Punishment.class);
        when(mockBan.getType()).thenReturn(Punishment.Type.BAN);
        when(mockBan.getId()).thenReturn("BAN-123");
        when(mockBan.getReason()).thenReturn("Test ban reason");
        
        mockMute = mock(Punishment.class);
        when(mockMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(mockMute.getId()).thenReturn("MUTE-456");
        when(mockMute.getReason()).thenReturn("Test mute reason");
        
        activePunishments = new ArrayList<>();
    }
    
    @Test
    void testSuccessfulResponseWithNoPunishments() {
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Login successful", activePunishments);
        
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals(0, response.getActivePunishments().size());
        assertFalse(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
        assertNull(response.getActiveBan());
        assertNull(response.getActiveMute());
    }
    
    @Test
    void testResponseWithActiveBan() {
        activePunishments.add(mockBan);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Has active ban", activePunishments);
        
        assertTrue(response.isSuccess());
        assertEquals("Has active ban", response.getMessage());
        assertEquals(1, response.getActivePunishments().size());
        assertTrue(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
        assertEquals(mockBan, response.getActiveBan());
        assertNull(response.getActiveMute());
    }
    
    @Test
    void testResponseWithActiveMute() {
        activePunishments.add(mockMute);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Has active mute", activePunishments);
        
        assertTrue(response.isSuccess());
        assertEquals("Has active mute", response.getMessage());
        assertEquals(1, response.getActivePunishments().size());
        assertFalse(response.hasActiveBan());
        assertTrue(response.hasActiveMute());
        assertNull(response.getActiveBan());
        assertEquals(mockMute, response.getActiveMute());
    }
    
    @Test
    void testResponseWithBothBanAndMute() {
        activePunishments.add(mockBan);
        activePunishments.add(mockMute);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Has both punishments", activePunishments);
        
        assertTrue(response.isSuccess());
        assertEquals("Has both punishments", response.getMessage());
        assertEquals(2, response.getActivePunishments().size());
        assertTrue(response.hasActiveBan());
        assertTrue(response.hasActiveMute());
        assertEquals(mockBan, response.getActiveBan());
        assertEquals(mockMute, response.getActiveMute());
    }
    
    @Test
    void testResponseWithMultipleBans() {
        Punishment mockBan2 = mock(Punishment.class);
        when(mockBan2.getType()).thenReturn(Punishment.Type.BAN);
        when(mockBan2.getId()).thenReturn("BAN-789");
        
        activePunishments.add(mockBan);
        activePunishments.add(mockBan2);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Multiple bans", activePunishments);
        
        assertTrue(response.hasActiveBan());
        // Should return the first ban found
        assertEquals(mockBan, response.getActiveBan());
    }
    
    @Test
    void testResponseWithMultipleMutes() {
        Punishment mockMute2 = mock(Punishment.class);
        when(mockMute2.getType()).thenReturn(Punishment.Type.MUTE);
        when(mockMute2.getId()).thenReturn("MUTE-789");
        
        activePunishments.add(mockMute);
        activePunishments.add(mockMute2);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Multiple mutes", activePunishments);
        
        assertTrue(response.hasActiveMute());
        // Should return the first mute found
        assertEquals(mockMute, response.getActiveMute());
    }
    
    @Test
    void testFailedResponse() {
        PlayerLoginResponse response = new PlayerLoginResponse(false, "Login failed", activePunishments);
        
        assertFalse(response.isSuccess());
        assertEquals("Login failed", response.getMessage());
        assertEquals(0, response.getActivePunishments().size());
        assertFalse(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
    }
    
    @Test
    void testResponseWithNullPunishments() {
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Null punishments", null);
        
        assertTrue(response.isSuccess());
        assertEquals("Null punishments", response.getMessage());
        assertEquals(0, response.getActivePunishments().size());
        assertFalse(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
        assertNull(response.getActiveBan());
        assertNull(response.getActiveMute());
    }
    
    @Test
    void testResponseImmutability() {
        activePunishments.add(mockBan);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Test", activePunishments);
        
        // Verify initial state
        assertTrue(response.hasActiveBan());
        assertEquals(1, response.getActivePunishments().size());
        
        // Modify original list
        activePunishments.add(mockMute);
        
        // Response should still have only the original punishment
        assertEquals(1, response.getActivePunishments().size());
        assertTrue(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
    }
    
    @Test
    void testResponseGettersReturnImmutableList() {
        activePunishments.add(mockBan);
        activePunishments.add(mockMute);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Test", activePunishments);
        
        List<Punishment> returnedPunishments = response.getActivePunishments();
        
        // Attempt to modify returned list should not affect the original
        assertDoesNotThrow(() -> {
            try {
                returnedPunishments.clear();
            } catch (UnsupportedOperationException e) {
                // This is expected for an immutable list
            }
        });
        
        // Original response should still have punishments
        assertEquals(2, response.getActivePunishments().size());
    }
    
    @Test
    void testStreamsWorkCorrectly() {
        activePunishments.add(mockBan);
        activePunishments.add(mockMute);
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Test", activePunishments);
        
        // Test that stream operations work (used internally by hasActiveBan/Mute methods)
        long banCount = response.getActivePunishments().stream()
            .filter(p -> p.getType() == Punishment.Type.BAN)
            .count();
        
        long muteCount = response.getActivePunishments().stream()
            .filter(p -> p.getType() == Punishment.Type.MUTE)
            .count();
        
        assertEquals(1, banCount);
        assertEquals(1, muteCount);
    }
    
    @Test
    void testResponseWithEmptyPunishmentsList() {
        PlayerLoginResponse response = new PlayerLoginResponse(true, "Empty list", new ArrayList<>());
        
        assertTrue(response.isSuccess());
        assertEquals("Empty list", response.getMessage());
        assertEquals(0, response.getActivePunishments().size());
        assertFalse(response.hasActiveBan());
        assertFalse(response.hasActiveMute());
        assertNull(response.getActiveBan());
        assertNull(response.getActiveMute());
    }
    
    @Test
    void testLombokDataAnnotationFunctionality() {
        activePunishments.add(mockBan);
        PlayerLoginResponse response1 = new PlayerLoginResponse(true, "Test", activePunishments);
        PlayerLoginResponse response2 = new PlayerLoginResponse(true, "Test", activePunishments);
        
        // Test equals (from @Data annotation)
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        
        // Test toString (from @Data annotation)
        assertNotNull(response1.toString());
        assertTrue(response1.toString().contains("PlayerLoginResponse"));
    }
}