package com.bongbong.modl.minecraft.core.impl.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.bongbong.modl.minecraft.api.AbstractPlayer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.CreateTicketRequest;
import com.bongbong.modl.minecraft.api.http.response.CreateTicketResponse;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.impl.menus.ReportMenu;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.service.ChatMessageCache;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CommandAlias("report")
@RequiredArgsConstructor
public class TicketCommands extends BaseCommand {
    private final Platform platform;
    private final ModlHttpClient httpClient;
    private final String panelUrl; // e.g., "https://myserver.modl.gg"
    private final LocaleManager locale;
    private final ChatMessageCache chatMessageCache;
    
    @Default
    @Description("Report a player - opens a GUI with customizable options")
    @Syntax("<player> [reason]")
    public void report(CommandIssuer sender, AbstractPlayer targetPlayer, @Optional String reason) {
        AbstractPlayer hero = platform.getAbstractPlayer(sender.getUniqueId(), false);
        
        if (targetPlayer.username().equalsIgnoreCase(hero.username())) {
            sendMessage(sender, locale.getMessage("messages.cannot_report_self"));
            return;
        }

        // Open the report GUI
        ReportMenu reportMenu = new ReportMenu(platform.getAbstractPlayer(sender.getUniqueId(), false), targetPlayer, reason, httpClient, locale, platform);
        reportMenu.display(sender.getIssuer());
    }
    
    @CommandAlias("chatreport")
    @Description("Report a player for inappropriate chat and save chat logs")
    @Syntax("<player> [reason]")
    public void chatReport(CommandIssuer sender, AbstractPlayer targetPlayer, @Optional String reason) {
        AbstractPlayer hero = platform.getAbstractPlayer(sender.getUniqueId(), false);
        
        if (targetPlayer.username().equalsIgnoreCase(hero.username())) {
            sendMessage(sender, locale.getMessage("messages.cannot_report_self"));
            return;
        }
        
        // Get the last 30 chat messages from the server where the reporter is located
        List<String> chatLogs = chatMessageCache.getRecentMessages(hero.uuid().toString(), 30);
        
        // If no messages are cached, show error and return
        if (chatLogs.isEmpty()) {
            sendMessage(sender, locale.getMessage("messages.no_chat_logs_available", Map.of("player", targetPlayer.username())));
            return;
        }

        CreateTicketRequest request = new CreateTicketRequest(
            hero.uuid().toString(),                                         // creatorUuid
            hero.username(),                                         // creatorName
            "chat",                                            // type
            locale.getMessage("report_gui.categories.chat_violation.subject", Map.of("player", targetPlayer.username())), // subject
            "Chat report: " + (reason != null ? reason : locale.getMessage("chat_logs.auto_reason")), // description
            targetPlayer.uuid().toString(),                    // reportedPlayerUuid
            targetPlayer.username(),                                      // reportedPlayerName
            chatLogs,                                          // chatMessages
            List.of(), // tags
            locale.getPriority("chat")                         // priority
        );
        
        submitFinishedTicket(sender, request, "Chat report");
    }
    
    @CommandAlias("bugreport")
    @Description("Report a bug - creates an unfinished report with a form link")
    @Syntax("<title>")
    public void bugReport(CommandIssuer sender, String title) {
        AbstractPlayer hero = platform.getAbstractPlayer(sender.getUniqueId(), false);

        CreateTicketRequest request = new CreateTicketRequest(
                hero.uuid().toString(),                                         // creatorUuid
                hero.username(),                                         // creatorName
            "bug",                                             // type
                hero.username() + ": " + title, // subject
            null,                                              // description (filled in form)
            null,                                              // reportedPlayerUuid
            null,                                              // reportedPlayerName
            null,                                              // chatMessages
            List.of(), // tags
            "normal"                          // priority
        );
        
        submitUnfinishedTicket(sender, request, "Bug report", title);
    }
    
    @CommandAlias("support")
    @Description("Request support - creates an unfinished request with a form link")
    @Syntax("<title>")
    public void supportRequest(CommandIssuer sender, String title) {
        AbstractPlayer hero = platform.getAbstractPlayer(sender.getUniqueId(), false);
        
        CreateTicketRequest request = new CreateTicketRequest(
            hero.uuid().toString(),                                         // creatorUuid
            hero.username(),                                         // creatorName
            "support",                                          // type
            hero.username() + ": " + title, // subject
            null,                                              // description (filled in form)
            null,                                              // reportedPlayerUuid
            null,                                              // reportedPlayerName
            null,                                              // chatMessages
            List.of(), // tags
            "normal"                     // priority
        );
        
        submitUnfinishedTicket(sender, request, "Support request", title);
    }
    
    @CommandAlias("apply")
    @Description("Submit a staff application - creates an unfinished application with a form link")
    @Syntax("<position>")
    public void staffApplication(CommandIssuer sender, String position) {
        AbstractPlayer hero = platform.getAbstractPlayer(sender.getUniqueId(), false);
        
        CreateTicketRequest request = new CreateTicketRequest(
            hero.uuid().toString(),                                         // creatorUuid
            hero.username(),                                         // creatorName
            "application",                                          // type
            hero.username() + ": " + position + " Application", // subject
            null,                                              // description (filled in form)
            null,                                              // reportedPlayerUuid
            null,                                              // reportedPlayerName
            null,                                              // chatMessages
            List.of("application", "staff"), // tags
            "normal"                     // priority
        );
        
        submitUnfinishedTicket(sender, request, "Staff application", position);
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
                
                String formUrl = panelUrl + "/ticket/" + response.getTicketId();
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


    private void sendMessage(CommandIssuer sender, String message) {
        sender.sendMessage(message);
    }
}