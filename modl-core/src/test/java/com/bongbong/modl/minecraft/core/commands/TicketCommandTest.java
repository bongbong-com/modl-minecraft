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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
public class TicketCommandTest {
    
    @Mock
    private ModlHttpClient mockHttpClient;
    
    @Mock
    private LocaleManager mockLocale;
    
    @Mock
    private CommandIssuer mockIssuer;
    
    private TicketCommand ticketCommand;
    private final String panelUrl = "https://panel.example.com";
    
    /*
    @BeforeEach
    void setUp() {
        when(mockIssuer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockIssuer.getIssuer()).thenReturn("TestPlayer");
        
        ticketCommand = new TicketCommand(mockHttpClient, panelUrl, mockLocale);
        
        // Setup locale manager defaults
        when(mockLocale.getMessage(eq("messages.cannot_report_self"))).thenReturn("You cannot report yourself!");
        when(mockLocale.getMessage(eq("messages.submitting"), any(Map.class))).thenReturn("Submitting report...");
        when(mockLocale.getMessage(eq("messages.success"), any(Map.class))).thenReturn("Report submitted successfully!");
        when(mockLocale.getMessage(eq("messages.failed_submit"), any(Map.class))).thenReturn("Failed to submit report");
        when(mockLocale.getMessage(eq("messages.creating"), any(Map.class))).thenReturn("Creating ticket...");
        when(mockLocale.getMessage(eq("messages.created"), any(Map.class))).thenReturn("Ticket created!");
        when(mockLocale.getMessage(eq("messages.complete_form"), any(Map.class))).thenReturn("Complete your form: {url}");
        when(mockLocale.getMessage(eq("messages.ticket_id"), any(Map.class))).thenReturn("Ticket ID: {ticketId}");
        when(mockLocale.getMessage(eq("messages.view_ticket"), any(Map.class))).thenReturn("View ticket: {url}");
        when(mockLocale.getMessage(eq("messages.evidence_note"))).thenReturn("Please provide evidence.");
        when(mockLocale.getMessage(eq("tags.chat_report"))).thenReturn("chat-report");
        when(mockLocale.getMessage(eq("tags.minecraft"))).thenReturn("minecraft");
        when(mockLocale.getMessage(eq("tags.auto_logs"))).thenReturn("auto-logs");
        when(mockLocale.getMessage(eq("tags.bug_report"))).thenReturn("bug-report");
        when(mockLocale.getMessage(eq("tags.support"))).thenReturn("support");
        when(mockLocale.getMessage(eq("commands.bugreport.description"))).thenReturn("Bug Report");
        when(mockLocale.getMessage(eq("commands.support.description"))).thenReturn("Support Request");
        when(mockLocale.getMessage(eq("chat_logs.auto_reason"))).thenReturn("Inappropriate chat");
        when(mockLocale.getMessage(eq("form_data.server_name"))).thenReturn("Test Server");
        when(mockLocale.getPriority("chat")).thenReturn("medium");
        when(mockLocale.getPriority("bug")).thenReturn("low");
        when(mockLocale.getPriority("support")).thenReturn("low");
    }
    
    @Test
    void testReportCannotReportSelf() {
        ticketCommand.report(mockIssuer, "TestPlayer", "Self report attempt");
        
        verify(mockIssuer).sendMessage("You cannot report yourself!");
        verify(mockHttpClient, never()).createTicket(any());
    }
    
    @Test
    void testChatReportSuccess() {
        CreateTicketResponse successResponse = new CreateTicketResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Ticket created");
        successResponse.setTicketId("TICKET-123");
        
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.chatReport(mockIssuer, "BadPlayer", "Used inappropriate language");
        
        verify(mockIssuer).sendMessage("Submitting report...");
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getType().equals("chat") &&
            request.getReportedPlayerName().equals("BadPlayer")
        ));
    }
    
    @Test
    void testChatReportCannotReportSelf() {
        ticketCommand.chatReport(mockIssuer, "TestPlayer", "Self report attempt");
        
        verify(mockIssuer).sendMessage("You cannot report yourself!");
        verify(mockHttpClient, never()).createTicket(any());
    }
    
    @Test
    void testBugReportSuccess() {
        CreateTicketResponse successResponse = new CreateTicketResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Bug report created");
        successResponse.setTicketId("TICKET-124");
        
        when(mockHttpClient.createUnfinishedTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.bugReport(mockIssuer, "Game crashes when opening inventory");
        
        verify(mockIssuer).sendMessage("Creating ticket...");
        verify(mockHttpClient).createUnfinishedTicket(argThat(request -> 
            request.getType().equals("bug") &&
            request.getSubject().contains("Bug Report") &&
            request.getTags().contains("bug-report")
        ));
    }
    
    @Test
    void testSupportRequestSuccess() {
        CreateTicketResponse successResponse = new CreateTicketResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Support request created");
        successResponse.setTicketId("TICKET-125");
        
        when(mockHttpClient.createUnfinishedTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.supportRequest(mockIssuer, "Need help with account recovery");
        
        verify(mockIssuer).sendMessage("Creating ticket...");
        verify(mockHttpClient).createUnfinishedTicket(argThat(request -> 
            request.getType().equals("support") &&
            request.getSubject().contains("Support Request") &&
            request.getTags().contains("support")
        ));
    }
    
    @Test
    void testChatReportHttpError() {
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
        
        ticketCommand.chatReport(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockIssuer).sendMessage("Submitting report...");
        verify(mockHttpClient).createTicket(any(CreateTicketRequest.class));
    }
    
    @Test
    void testChatReportFailedResponse() {
        CreateTicketResponse failedResponse = new CreateTicketResponse();
        failedResponse.setSuccess(false);
        failedResponse.setMessage("Validation error");
        
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(failedResponse));
        
        ticketCommand.chatReport(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockHttpClient).createTicket(any(CreateTicketRequest.class));
    }
    
    @Test
    void testGetSenderUuidHandlesNullUuid() {
        when(mockIssuer.getUniqueId()).thenReturn(null);
        
        ticketCommand.chatReport(mockIssuer, "TargetPlayer", "Test reason");
        
        // Should still create the ticket with a generated UUID
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getCreatorUuid() != null
        ));
    }
    
    @Test
    void testGetSenderNameHandlesNullIssuer() {
        when(mockIssuer.getIssuer()).thenReturn(null);
        
        ticketCommand.chatReport(mockIssuer, "TargetPlayer", "Test reason");
        
        // Should still create the ticket with "Unknown" as the name
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getCreatorName().equals("Unknown")
        ));
    }
    
    @Test
    void testFormDataCreation() {
        when(mockLocale.getMessage("form_data.server_name")).thenReturn("Test Server");
        
        CreateTicketResponse successResponse = new CreateTicketResponse();
        successResponse.setSuccess(true);
        successResponse.setMessage("Ticket created");
        successResponse.setTicketId("TICKET-126");
        
        when(mockHttpClient.createTicket(any(CreateTicketRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        ticketCommand.chatReport(mockIssuer, "TargetPlayer", "Test reason");
        
        verify(mockHttpClient).createTicket(argThat(request -> 
            request.getFormData() != null &&
            request.getFormData().has("reportedPlayer") &&
            request.getFormData().get("reportedPlayer").getAsString().equals("TargetPlayer")
        ));
    }
    */
}