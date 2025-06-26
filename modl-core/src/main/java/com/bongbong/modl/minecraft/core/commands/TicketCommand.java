package com.bongbong.modl.minecraft.core.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.CreateTicketRequest;
import com.bongbong.modl.minecraft.api.http.response.CreateTicketResponse;
import com.bongbong.modl.minecraft.core.gui.ReportMenu;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CommandAlias("report")
@RequiredArgsConstructor
public class TicketCommand extends BaseCommand {
    
    private final ModlHttpClient httpClient;
    private final String panelUrl; // e.g., "https://myserver.modl.gg"
    private final LocaleManager locale;
    
    @Default
    @CommandPermission("modl.report")
    @Description("Report a player - opens a GUI with customizable options")
    @Syntax("<player> [reason]")
    public void report(CommandIssuer sender, String targetPlayer, @Optional String reason) {
        String senderName = getSenderName(sender);
        
        if (targetPlayer.equalsIgnoreCase(senderName)) {
            sendMessage(sender, locale.getMessage("messages.cannot_report_self"));
            return;
        }
        
        // Open the report GUI
        ReportMenu reportMenu = new ReportMenu(sender, targetPlayer, reason, httpClient, panelUrl, locale);
        reportMenu.display(sender.getIssuer());
    }
    
    @CommandAlias("chatreport")
    @CommandPermission("modl.chatreport")
    @Description("Report a player for inappropriate chat and save chat logs")
    @Syntax("<player> [reason]")
    public void chatReport(CommandIssuer sender, String targetPlayer, @Optional String reason) {
        String senderUuid = getSenderUuid(sender);
        String senderName = getSenderName(sender);
        
        if (targetPlayer.equalsIgnoreCase(senderName)) {
            sendMessage(sender, locale.getMessage("messages.cannot_report_self"));
            return;
        }
        
        // Save recent chat logs for the target player
        List<String> chatLogs = getChatLogs(targetPlayer);
        
        JsonObject formData = new JsonObject();
        formData.addProperty("reportedPlayer", targetPlayer);
        formData.addProperty("reason", reason != null ? reason : locale.getMessage("chat_logs.auto_reason"));
        formData.addProperty("server", locale.getMessage("form_data.server_name"));
        formData.addProperty("automaticChatLog", locale.getInteger("form_data.automatic_chat_log", 1) == 1);
        
        CreateTicketRequest request = new CreateTicketRequest(
            senderUuid,                                         // creatorUuid
            senderName,                                         // creatorName
            "chat",                                            // type
            locale.getMessage("report_gui.categories.chat_violation.subject", Map.of("player", targetPlayer)), // subject
            "Chat report: " + (reason != null ? reason : locale.getMessage("chat_logs.auto_reason")), // description
            "unknown-uuid",                                    // reportedPlayerUuid
            targetPlayer,                                      // reportedPlayerName
            chatLogs,                                          // chatMessages
            formData,                                          // formData
            Arrays.asList(
                locale.getMessage("tags.chat_report"),
                locale.getMessage("tags.minecraft"), 
                locale.getMessage("tags.auto_logs")
            ), // tags
            locale.getPriority("chat")                         // priority
        );
        
        submitFinishedTicket(sender, request, "Chat report");
    }
    
    @CommandAlias("bugreport")
    @CommandPermission("modl.bugreport")
    @Description("Report a bug - creates an unfinished report with a form link")
    @Syntax("<title>")
    public void bugReport(CommandIssuer sender, String title) {
        String senderUuid = getSenderUuid(sender);
        String senderName = getSenderName(sender);
        
        CreateTicketRequest request = new CreateTicketRequest(
            senderUuid,                                         // creatorUuid
            senderName,                                         // creatorName
            "bug",                                             // type
            locale.getMessage("commands.bugreport.description") + ": " + title, // subject
            null,                                              // description (filled in form)
            null,                                              // reportedPlayerUuid
            null,                                              // reportedPlayerName
            null,                                              // chatMessages
            null,                                              // formData (filled in form)
            Arrays.asList(
                locale.getMessage("tags.bug_report"), 
                locale.getMessage("tags.minecraft")
            ), // tags
            locale.getPriority("bug")                          // priority
        );
        
        submitUnfinishedTicket(sender, request, "Bug report", title);
    }
    
    @CommandAlias("support")
    @CommandPermission("modl.support")
    @Description("Request support - creates an unfinished request with a form link")
    @Syntax("<title>")
    public void supportRequest(CommandIssuer sender, String title) {
        String senderUuid = getSenderUuid(sender);
        String senderName = getSenderName(sender);
        
        CreateTicketRequest request = new CreateTicketRequest(
            senderUuid,                                         // creatorUuid
            senderName,                                         // creatorName
            "support",                                          // type
            locale.getMessage("commands.support.description") + ": " + title, // subject
            null,                                              // description (filled in form)
            null,                                              // reportedPlayerUuid
            null,                                              // reportedPlayerName
            null,                                              // chatMessages
            null,                                              // formData (filled in form)
            Arrays.asList(
                locale.getMessage("tags.support"), 
                locale.getMessage("tags.minecraft")
            ), // tags
            locale.getPriority("support")                      // priority
        );
        
        submitUnfinishedTicket(sender, request, "Support request", title);
    }
    
    private void submitFinishedTicket(CommandIssuer sender, CreateTicketRequest request, String ticketType) {
        sendMessage(sender, locale.getMessage("messages.submitting", Map.of("type", ticketType.toLowerCase())));
        
        CompletableFuture<CreateTicketResponse> future = httpClient.createTicket(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess() && response.getTicketId() != null) {
                sendMessage(sender, locale.getMessage("messages.success", Map.of("type", ticketType)));
                sendMessage(sender, locale.getMessage("messages.ticket_id", Map.of("ticketId", response.getTicketId())));
                
                String ticketUrl = panelUrl + "/tickets/" + response.getTicketId();
                sendMessage(sender, locale.getMessage("messages.view_ticket", Map.of("url", ticketUrl)));
                sendMessage(sender, locale.getMessage("messages.evidence_note"));
            } else {
                String error = response.getMessage() != null ? response.getMessage() : locale.getMessage("messages.unknown_error");
                sendMessage(sender, locale.getMessage("messages.failed_submit", Map.of("type", ticketType.toLowerCase(), "error", error)));
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, locale.getMessage("messages.failed_submit", Map.of("type", ticketType.toLowerCase(), "error", throwable.getMessage())));
            return null;
        });
    }
    
    private void submitUnfinishedTicket(CommandIssuer sender, CreateTicketRequest request, String ticketType, String title) {
        sendMessage(sender, locale.getMessage("messages.creating", Map.of("type", ticketType.toLowerCase())));
        
        CompletableFuture<CreateTicketResponse> future = httpClient.createUnfinishedTicket(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess() && response.getTicketId() != null) {
                sendMessage(sender, locale.getMessage("messages.created", Map.of("type", ticketType)));
                sendMessage(sender, locale.getMessage("messages.ticket_id", Map.of("ticketId", response.getTicketId())));
                
                String formUrl = panelUrl + "/player-ticket/" + response.getTicketId();
                sendMessage(sender, locale.getMessage("messages.complete_form", Map.of("type", ticketType.toLowerCase(), "url", formUrl)));
                sendMessage(sender, locale.getMessage("messages.form_title_note", Map.of("title", title)));
            } else {
                String error = response.getMessage() != null ? response.getMessage() : locale.getMessage("messages.unknown_error");
                sendMessage(sender, locale.getMessage("messages.failed_create", Map.of("type", ticketType.toLowerCase(), "error", error)));
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, locale.getMessage("messages.failed_create", Map.of("type", ticketType.toLowerCase(), "error", throwable.getMessage())));
            return null;
        });
    }
    
    private List<String> getChatLogs(String targetPlayer) {
        // TODO: Implement chat log retrieval system
        // This should integrate with your chat logging system
        // For now, return a placeholder
        return Arrays.asList(
            "[" + getCurrentTime() + "] " + targetPlayer + ": " + locale.getMessage("chat_logs.placeholder"),
            "[" + getCurrentTime() + "] System: " + locale.getMessage("chat_logs.system_note")
        );
    }
    
    private String getCurrentTime() {
        return java.time.LocalTime.now().toString().substring(0, 8);
    }
    
    private String getSenderUuid(CommandIssuer sender) {
        if (sender.getUniqueId() != null) {
            return sender.getUniqueId().toString();
        }
        return UUID.randomUUID().toString();
    }
    
    private String getSenderName(CommandIssuer sender) {
        if (sender.getIssuer() != null) {
            return sender.getIssuer().toString();
        }
        return "Unknown";
    }
    
    private void sendMessage(CommandIssuer sender, String message) {
        sender.sendMessage(message);
    }
}