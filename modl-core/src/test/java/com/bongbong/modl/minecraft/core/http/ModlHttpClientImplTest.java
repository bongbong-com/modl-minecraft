package com.bongbong.modl.minecraft.core.http;

import com.bongbong.modl.minecraft.api.http.request.*;
import com.bongbong.modl.minecraft.api.http.response.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModlHttpClientImplTest {
    
    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockHttpResponse;
    
    private ModlHttpClientImpl httpClientImpl;
    private final String baseUrl = "https://api.example.com";
    private final String apiKey = "test-api-key";
    private final Gson gson = new Gson();
    
    @BeforeEach
    void setUp() {
        // Create a spy to allow partial mocking
        httpClientImpl = spy(new ModlHttpClientImpl(baseUrl, apiKey));
        
        // Replace the internal HTTP client with our mock
        try {
            java.lang.reflect.Field httpClientField = ModlHttpClientImpl.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(httpClientImpl, mockHttpClient);
        } catch (Exception e) {
            fail("Failed to inject mock HTTP client: " + e.getMessage());
        }
    }
    
    @Test
    void testGetPlayerProfileSuccess() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        PlayerProfileResponse expectedResponse = new PlayerProfileResponse(
            playerUuid.toString(), "TestPlayer", "test@example.com", 
            null, null, null, null
        );
        
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(gson.toJson(expectedResponse));
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<PlayerProfileResponse> future = httpClientImpl.getPlayerProfile(playerUuid);
        PlayerProfileResponse response = future.get();
        
        assertNotNull(response);
        assertEquals(expectedResponse.getUuid(), response.getUuid());
        assertEquals(expectedResponse.getUsername(), response.getUsername());
        
        verify(mockHttpClient).sendAsync(argThat(request -> 
            request.uri().toString().contains("/players/" + playerUuid) &&
            request.headers().firstValue("X-API-Key").orElse("").equals(apiKey)
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testGetPlayerProfileNotFound() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        
        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpResponse.body()).thenReturn("Player not found");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<PlayerProfileResponse> future = httpClientImpl.getPlayerProfile(playerUuid);
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertTrue(exception.getCause().getMessage().contains("Request failed with status code 404"));
    }
    
    @Test
    void testPlayerLoginSuccess() throws Exception {
        PlayerLoginRequest request = new PlayerLoginRequest(
            UUID.randomUUID().toString(),
            "TestPlayer",
            "192.168.1.1",
            null,
            null
        );
        
        PlayerLoginResponse expectedResponse = new PlayerLoginResponse(true, "Login successful", null);
        
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(gson.toJson(expectedResponse));
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<PlayerLoginResponse> future = httpClientImpl.playerLogin(request);
        PlayerLoginResponse response = future.get();
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        
        verify(mockHttpClient).sendAsync(argThat(httpRequest -> 
            httpRequest.uri().toString().contains("/players/login") &&
            httpRequest.headers().firstValue("X-API-Key").orElse("").equals(apiKey) &&
            httpRequest.headers().firstValue("Content-Type").orElse("").equals("application/json")
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testPlayerLoginWithPunishments() throws Exception {
        PlayerLoginRequest request = new PlayerLoginRequest(
            UUID.randomUUID().toString(),
            "BannedPlayer",
            "192.168.1.1",
            null,
            null
        );
        
        PlayerLoginResponse expectedResponse = new PlayerLoginResponse(false, "Player is banned", null);
        
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(gson.toJson(expectedResponse));
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<PlayerLoginResponse> future = httpClientImpl.playerLogin(request);
        PlayerLoginResponse response = future.get();
        
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("Player is banned", response.getMessage());
    }
    
    @Test
    void testCreateTicketSuccess() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
            UUID.randomUUID().toString(),
            "TestPlayer",
            "player",
            "Test Report",
            "This is a test report",
            null,
            null,
            null,
            null,
            null,
            "medium"
        );
        
        CreateTicketResponse expectedResponse = new CreateTicketResponse(
            true, "Ticket created", "TICKET-123", "https://panel.example.com/tickets/TICKET-123"
        );
        
        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn(gson.toJson(expectedResponse));
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<CreateTicketResponse> future = httpClientImpl.createTicket(request);
        CreateTicketResponse response = future.get();
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("TICKET-123", response.getTicketId());
        assertEquals("https://panel.example.com/tickets/TICKET-123", response.getTicketUrl());
        
        verify(mockHttpClient).sendAsync(argThat(httpRequest -> 
            httpRequest.uri().toString().contains("/api/public/tickets") &&
            httpRequest.headers().firstValue("X-Ticket-API-Key").orElse("").equals(apiKey)
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testCreateUnfinishedTicketSuccess() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
            UUID.randomUUID().toString(),
            "TestPlayer",
            "support",
            "Need Help",
            "I need help with something",
            null,
            null,
            null,
            null,
            null,
            "low"
        );
        
        CreateTicketResponse expectedResponse = new CreateTicketResponse(
            true, "Unfinished ticket created", "TICKET-456", "https://panel.example.com/tickets/TICKET-456/form"
        );
        
        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn(gson.toJson(expectedResponse));
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<CreateTicketResponse> future = httpClientImpl.createUnfinishedTicket(request);
        CreateTicketResponse response = future.get();
        
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("TICKET-456", response.getTicketId());
        assertTrue(response.getTicketUrl().contains("/form"));
        
        verify(mockHttpClient).sendAsync(argThat(httpRequest -> 
            httpRequest.uri().toString().contains("/api/public/tickets/unfinished")
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testPlayerDisconnectSuccess() throws Exception {
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(UUID.randomUUID().toString());
        
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<Void> future = httpClientImpl.playerDisconnect(request);
        
        assertDoesNotThrow(future::get);
        
        verify(mockHttpClient).sendAsync(argThat(httpRequest -> 
            httpRequest.uri().toString().contains("/players/disconnect") &&
            httpRequest.headers().firstValue("X-API-Key").orElse("").equals(apiKey)
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testCreatePunishmentSuccess() throws Exception {
        CreatePunishmentRequest request = new CreatePunishmentRequest(
            UUID.randomUUID().toString(),
            "BAN",
            "Cheating",
            null,
            86400000L,
            null,
            "TestModerator"
        );
        
        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpResponse.body()).thenReturn("{}");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<Void> future = httpClientImpl.createPunishment(request);
        
        assertDoesNotThrow(future::get);
        
        verify(mockHttpClient).sendAsync(argThat(httpRequest -> 
            httpRequest.uri().toString().contains("/punishments") &&
            httpRequest.headers().firstValue("X-API-Key").orElse("").equals(apiKey)
        ), any(HttpResponse.BodyHandler.class));
    }
    
    @Test
    void testHttpClientErrorHandling() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("Internal server error");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<PlayerProfileResponse> future = httpClientImpl.getPlayerProfile(playerUuid);
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertTrue(exception.getCause().getMessage().contains("Request failed with status code 500"));
    }
    
    @Test
    void testNetworkErrorHandling() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.failedFuture(new IOException("Network error")));
        
        CompletableFuture<PlayerProfileResponse> future = httpClientImpl.getPlayerProfile(playerUuid);
        
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Network error", exception.getCause().getMessage());
    }
    
    @Test
    void testVoidResponseHandling() throws Exception {
        PlayerDisconnectRequest request = new PlayerDisconnectRequest(UUID.randomUUID().toString());
        
        when(mockHttpResponse.statusCode()).thenReturn(204); // No content
        when(mockHttpResponse.body()).thenReturn("");
        when(mockHttpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        
        CompletableFuture<Void> future = httpClientImpl.playerDisconnect(request);
        Void result = future.get();
        
        assertNull(result);
    }
}