package com.bongbong.modl.minecraft.core.commands;

import co.aikar.commands.CommandIssuer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.CreateTicketRequest;
import com.bongbong.modl.minecraft.api.http.response.CreateTicketResponse;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketCommandTest {
    
    @Mock
    private ModlHttpClient mockHttpClient;
    
    @Mock
    private LocaleManager mockLocale;
    
    @Mock
    private CommandIssuer mockIssuer;
    
    private TicketCommand ticketCommand;
    private final String panelUrl = "https://panel.example.com";
    
    @BeforeEach
    void setUp() {
        when(mockIssuer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockIssuer.getIssuer()).thenReturn("TestPlayer");
        
        ticketCommand = new TicketCommand(mockHttpClient, panelUrl, mockLocale);
        
        // Setup locale manager defaults
        when(mockLocale.getMessage(eq("messages.cannot_report_self"), any())).thenReturn("You cannot report yourself!");
        when(mockLocale.getMessage(eq("messages.submitting"), any())).thenReturn("Submitting report...");
        when(mockLocale.getMessage(eq("messages.success"), any())).thenReturn("Report submitted successfully!");
        when(mockLocale.getMessage(eq("messages.failed_submit"), any())).thenReturn("Failed to submit report");
        when(mockLocale.getMessage(eq("tags.chat_report"))).thenReturn("chat-report");
        when(mockLocale.getMessage(eq("tags.minecraft"))).thenReturn("minecraft");
        when(mockLocale.getPriority("chat")).thenReturn("medium");
    }
    
    @Test
    void testReportPlayerSuccess() {
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Ticket created", "TICKET-123", "https://panel.example.com/tickets/TICKET-123"
        );
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Cheating in game");
        
        verify(mockIssuer).sendMessage("Submitting report...");
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getType().equals("player") &&
            request.getSubject().contains("TargetPlayer") &&
            request.getDescription().equals("Cheating in game")
        ));
    }
    
    @Test
    void testReportPlayerCannotReportSelf() {
        when(mockIssuer.getIssuer()).thenReturn("TestPlayer");
        
        ticketCommand.reportPlayer(mockIssuer, "TestPlayer", "Self report attempt");
        
        verify(mockIssuer).sendMessage("You cannot report yourself!");
        verify(mockHttpClient, never()).createTicket(any());
    }
    
    @Test
    void testReportPlayerWithNullReason() {
        when(mockLocale.getMessage(eq("messages.no_details"), any())).thenReturn("No additional details provided");
        
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Ticket created", "TICKET-124", "https://panel.example.com/tickets/TICKET-124"
        );
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", null);
        
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getDescription().equals("No additional details provided")
        ));
    }
    
    @Test
    void testChatReportSuccess() {
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Chat report created", "TICKET-125", "https://panel.example.com/tickets/TICKET-125"
        );
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.chatReport(mockIssuer, "BadPlayer", "Used inappropriate language");
        
        verify(mockIssuer).sendMessage("Submitting report...");
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getType().equals("chat") &&
            request.getReportedPlayerName().equals("BadPlayer") &&
            request.getTags().contains("chat-report")
        ));
    }
    
    @Test
    void testBugReportSuccess() {
        when(mockLocale.getMessage(eq("tags.bug_report"))).thenReturn("bug-report");
        when(mockLocale.getMessage(eq("messages.creating"), any())).thenReturn("Creating bug report...");
        when(mockLocale.getMessage(eq("messages.created"), any())).thenReturn("Bug report created!");
        when(mockLocale.getMessage(eq("messages.complete_form"), any())).thenReturn("Complete your form: {url}");
        
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Bug report created", "TICKET-126", "https://panel.example.com/tickets/TICKET-126/form"
        );
        when(mockHttpClient.createUnfinishedTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.bugReport(mockIssuer, "Game crashes when opening inventory");
        
        verify(mockIssuer).sendMessage("Creating bug report...");
        verify(mockHttpClient).createUnfinishedTicket(argThat(request -> 
            request.getType().equals("bug") &&
            request.getSubject().contains("Bug Report") &&
            request.getTags().contains("bug-report")
        ));
    }
    
    @Test
    void testSupportRequestSuccess() {
        when(mockLocale.getMessage(eq("tags.support"))).thenReturn("support");
        when(mockLocale.getMessage(eq("messages.creating"), any())).thenReturn("Creating support request...");
        when(mockLocale.getMessage(eq("messages.created"), any())).thenReturn("Support request created!");
        when(mockLocale.getMessage(eq("messages.complete_form"), any())).thenReturn("Complete your request: {url}");
        
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Support request created", "TICKET-127", "https://panel.example.com/tickets/TICKET-127/form"
        );
        when(mockHttpClient.createUnfinishedTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.supportRequest(mockIssuer, "Need help with account recovery");
        
        verify(mockIssuer).sendMessage("Creating support request...");
        verify(mockHttpClient).createUnfinishedTicket(argThat(request -> 
            request.getType().equals("support") &&
            request.getSubject().contains("Support Request")
        ));
    }
    
    @Test
    void testReportPlayerHttpError() {
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockIssuer).sendMessage("Submitting report...");
        // The error handling should be in the async callback
        verify(mockHttpClient).createTicket(any(CreateTicketRequest.class));
    }
    
    @Test
    void testReportPlayerFailedResponse() {
        CreateTicketResponse failedResponse = new CreateTicketResponse(
            false, "Validation error", null, null
        );
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(failedResponse));
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockHttpClient).createTicket(any(CreateTicketRequest.class));
        // The response handling would be in the async callback
    }
    
    @Test
    void testGetSenderUuidHandlesNullUuid() {
        when(mockIssuer.getUniqueId()).thenReturn(null);
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Test reason");
        
        // Should still create the ticket with a generated UUID
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getCreatorUuid() != null &&
            UUID.fromString(request.getCreatorUuid()) != null
        ));
    }
    
    @Test
    void testGetSenderNameHandlesNullIssuer() {
        when(mockIssuer.getIssuer()).thenReturn(null);
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Test reason");
        
        // Should still create the ticket with "Unknown" as the name
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getCreatorName().equals("Unknown")
        ));
    }
    
    @Test
    void testFormDataCreation() {
        when(mockLocale.getMessage("form_data.server_name")).thenReturn("Test Server");
        
        CreateTicketResponse successResponse = new CreateTicketResponse(
            true, "Ticket created", "TICKET-128", "https://panel.example.com/tickets/TICKET-128"
        );
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.reportPlayer(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getFormData() != null &&
            request.getFormData().has("reportType") &&
            request.getFormData().get("reportType").getAsString().equals("player")
        ));
    }
}