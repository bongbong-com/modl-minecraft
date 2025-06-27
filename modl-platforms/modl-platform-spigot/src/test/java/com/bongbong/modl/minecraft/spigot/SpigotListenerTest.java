package com.bongbong.modl.minecraft.spigot;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerDisconnectRequest;
import com.bongbong.modl.minecraft.api.http.request.PlayerLoginRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLoginResponse;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpigotListenerTest {
    
    private ServerMock server;
    private SpigotListener listener;
    private SpigotPlatform mockPlatform;
    
    @Mock
    private ModlHttpClient mockHttpClient;
    
    @Mock
    private Logger mockLogger;
    
    private PlayerMock testPlayer;
    private InetAddress testAddress;
    
    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        
        mockPlatform = mock(SpigotPlatform.class);
        when(mockPlatform.getHttpClient()).thenReturn(mockHttpClient);
        when(mockPlatform.getLogger()).thenReturn(mockLogger);
        
        listener = new SpigotListener(mockPlatform);
        
        testPlayer = server.addPlayer("TestPlayer");
        testAddress = InetAddress.getByName("192.168.1.100");
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    @Test
    void testPlayerLoginWithNoPunishments() throws Exception {
        PlayerLoginResponse successResponse = new PlayerLoginResponse(true, "Login allowed", null);
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(testPlayer, "localhost", testAddress);
        
        listener.onPlayerLogin(loginEvent);
        
        assertEquals(PlayerLoginEvent.Result.ALLOWED, loginEvent.getResult());
        assertNull(loginEvent.getKickMessage());
        
        verify(mockHttpClient).playerLogin(argThat(request -> 
            request.getMinecraftUuid().equals(testPlayer.getUniqueId().toString()) &&
            request.getUsername().equals(testPlayer.getName()) &&
            request.getIpAddress().equals(testAddress.getHostAddress())
        ));
    }
    
    @Test
    void testPlayerLoginWithActiveBan() throws Exception {
        Punishment mockBan = mock(Punishment.class);
        when(mockBan.getType()).thenReturn(Punishment.Type.BAN);
        when(mockBan.getReason()).thenReturn("Cheating detected");
        when(mockBan.getExpires()).thenReturn(null); // Permanent ban
        
        PlayerLoginResponse banResponse = new PlayerLoginResponse(false, "Player is banned", Arrays.asList(mockBan));
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(banResponse));
        
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(testPlayer, "localhost", testAddress);
        
        listener.onPlayerLogin(loginEvent);
        
        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, loginEvent.getResult());
        assertNotNull(loginEvent.getKickMessage());
        assertTrue(loginEvent.getKickMessage().contains("You are banned from this server!"));
        assertTrue(loginEvent.getKickMessage().contains("Cheating detected"));
        assertTrue(loginEvent.getKickMessage().contains("permanent"));
    }
    
    @Test
    void testPlayerLoginWithTemporaryBan() throws Exception {
        Punishment mockBan = mock(Punishment.class);
        when(mockBan.getType()).thenReturn(Punishment.Type.BAN);
        when(mockBan.getReason()).thenReturn("Temporary timeout");
        when(mockBan.getExpires()).thenReturn(new java.util.Date(System.currentTimeMillis() + 86400000L)); // 1 day
        
        PlayerLoginResponse banResponse = new PlayerLoginResponse(false, "Player is banned", Arrays.asList(mockBan));
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(banResponse));
        
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(testPlayer, "localhost", testAddress);
        
        listener.onPlayerLogin(loginEvent);
        
        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, loginEvent.getResult());
        assertNotNull(loginEvent.getKickMessage());
        assertTrue(loginEvent.getKickMessage().contains("Temporary timeout"));
        assertTrue(loginEvent.getKickMessage().contains("Expires:"));
    }
    
    @Test
    void testPlayerLoginWithHttpError() throws Exception {
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));
        
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(testPlayer, "localhost", testAddress);
        
        listener.onPlayerLogin(loginEvent);
        
        // Should allow login on error to prevent false kicks
        assertEquals(PlayerLoginEvent.Result.ALLOWED, loginEvent.getResult());
        assertNull(loginEvent.getKickMessage());
        
        verify(mockLogger).severe(contains("Failed to check punishments for TestPlayer"));
    }
    
    @Test
    void testPlayerJoinCachesMute() throws Exception {
        Punishment mockMute = mock(Punishment.class);
        when(mockMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(mockMute.getReason()).thenReturn("Inappropriate language");
        
        PlayerLoginResponse muteResponse = new PlayerLoginResponse(true, "Has mute", Arrays.asList(mockMute));
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(muteResponse));
        
        PlayerJoinEvent joinEvent = new PlayerJoinEvent(testPlayer, "TestPlayer joined the game");
        
        listener.onPlayerJoin(joinEvent);
        
        // Verify the mute was cached
        assertTrue(listener.getPunishmentCache().isMuted(testPlayer.getUniqueId()));
        assertEquals(mockMute, listener.getPunishmentCache().getMute(testPlayer.getUniqueId()));
        
        verify(mockHttpClient).playerLogin(any(PlayerLoginRequest.class));
    }
    
    @Test
    void testPlayerJoinHandlesHttpError() throws Exception {
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Cache error")));
        
        PlayerJoinEvent joinEvent = new PlayerJoinEvent(testPlayer, "TestPlayer joined the game");
        
        assertDoesNotThrow(() -> listener.onPlayerJoin(joinEvent));
        
        verify(mockLogger).severe(contains("Failed to cache mute for TestPlayer"));
    }
    
    @Test
    void testPlayerChatWithMute() throws Exception {
        // First cache a mute
        Punishment mockMute = mock(Punishment.class);
        when(mockMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(mockMute.getReason()).thenReturn("Spam");
        when(mockMute.getExpires()).thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000L)); // 1 hour
        
        listener.getPunishmentCache().cacheMute(testPlayer.getUniqueId(), mockMute);
        
        // Create a chat event
        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(
            false, testPlayer, "Hello world!", server.getOnlinePlayers()
        );
        
        listener.onPlayerChat(chatEvent);
        
        assertTrue(chatEvent.isCancelled());
        
        // Player should have received a mute message (this would normally be sent via player.sendMessage)
        // We can't easily verify the message was sent in MockBukkit, but we can verify the event was cancelled
    }
    
    @Test
    void testPlayerChatWithoutMute() throws Exception {
        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(
            false, testPlayer, "Hello world!", server.getOnlinePlayers()
        );
        
        listener.onPlayerChat(chatEvent);
        
        assertFalse(chatEvent.isCancelled());
    }
    
    @Test
    void testPlayerChatWithExpiredMute() throws Exception {
        // Cache an expired mute
        Punishment expiredMute = mock(Punishment.class);
        when(expiredMute.getType()).thenReturn(Punishment.Type.MUTE);
        when(expiredMute.getReason()).thenReturn("Old spam");
        when(expiredMute.getExpires()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000L)); // 1 hour ago
        
        listener.getPunishmentCache().cacheMute(testPlayer.getUniqueId(), expiredMute);
        
        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(
            false, testPlayer, "Hello world!", server.getOnlinePlayers()
        );
        
        listener.onPlayerChat(chatEvent);
        
        // Chat should be allowed as mute is expired
        assertFalse(chatEvent.isCancelled());
        
        // Mute should be removed from cache
        assertFalse(listener.getPunishmentCache().isMuted(testPlayer.getUniqueId()));
    }
    
    @Test
    void testPlayerQuitRemovesFromCache() throws Exception {
        // Cache a mute for the player
        Punishment mockMute = mock(Punishment.class);
        when(mockMute.getType()).thenReturn(Punishment.Type.MUTE);
        listener.getPunishmentCache().cacheMute(testPlayer.getUniqueId(), mockMute);
        
        assertTrue(listener.getPunishmentCache().isMuted(testPlayer.getUniqueId()));
        
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(testPlayer, "TestPlayer left the game");
        
        listener.onPlayerQuit(quitEvent);
        
        // Player should be removed from cache
        assertFalse(listener.getPunishmentCache().isMuted(testPlayer.getUniqueId()));
        
        verify(mockHttpClient).playerDisconnect(argThat(request -> 
            request.getPlayerUuid().equals(testPlayer.getUniqueId().toString())
        ));
    }
    
    @Test
    void testFormatBanMessageWithPermanentBan() {
        // Test the private formatBanMessage method through reflection or by observing behavior
        Punishment mockBan = mock(Punishment.class);
        when(mockBan.getReason()).thenReturn("Hacking");
        when(mockBan.getExpires()).thenReturn(null);
        
        PlayerLoginResponse banResponse = new PlayerLoginResponse(false, "Banned", Arrays.asList(mockBan));
        when(mockHttpClient.playerLogin(any(PlayerLoginRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(banResponse));
        
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(testPlayer, "localhost", testAddress);
        listener.onPlayerLogin(loginEvent);
        
        String kickMessage = loginEvent.getKickMessage();
        assertNotNull(kickMessage);
        assertTrue(kickMessage.contains("You are banned"));
        assertTrue(kickMessage.contains("Hacking"));
        assertTrue(kickMessage.contains("permanent"));
    }
    
    @Test
    void testPunishmentCacheInitialization() {
        assertNotNull(listener.getPunishmentCache());
        assertEquals(0, listener.getPunishmentCache().size());
    }
}