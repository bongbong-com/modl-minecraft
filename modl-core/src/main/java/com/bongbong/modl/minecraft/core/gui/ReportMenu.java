package com.bongbong.modl.minecraft.core.gui;

import co.aikar.commands.CommandIssuer;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.CreateTicketRequest;
import com.bongbong.modl.minecraft.api.http.response.CreateTicketResponse;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.google.gson.JsonObject;
import dev.simplix.cirrus.item.CirrusItem;
import dev.simplix.cirrus.menus.SimpleMenu;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class ReportMenu extends SimpleMenu {
    
    private final CommandIssuer reporter;
    private final String targetPlayer;
    private final String reason;
    private final ModlHttpClient httpClient;
    private final String panelUrl;
    private final LocaleManager locale;
    
    public ReportMenu(CommandIssuer reporter, String targetPlayer, String reason, ModlHttpClient httpClient, String panelUrl, LocaleManager locale) {
        super();
        this.reporter = reporter;
        this.targetPlayer = targetPlayer;
        this.reason = reason;
        this.httpClient = httpClient;
        this.panelUrl = panelUrl;
        this.locale = locale;
        
        title(locale.getMessage("report_gui.title", Map.of("player", targetPlayer)));
        type(InventoryType.GENERIC_9X3); // Fixed size from locale
        buildMenu();
    }
    
    private void buildMenu() {
        // Create report category items from locale
        createCategoryItem("chat_violation", "chat_report");
        createCategoryItem("username", "username_report");
        createCategoryItem("skin", "skin_report");
        createCategoryItem("content", "content_report");
        createCategoryItem("team_griefing", "team_report");
        createCategoryItem("game_rules", "game_report");
        createCategoryItem("cheating", "cheating_report");
        
        // Close button
        LocaleManager.ReportCategory closeButton = locale.getCloseButton();
        List<ChatElement<?>> closeLore = closeButton.getLore().stream()
            .map(ChatElement::ofLegacyText)
            .collect(Collectors.toList());
        set(CirrusItem.of(
                ItemType.valueOf(closeButton.getItemType()),
                ChatElement.ofLegacyText(closeButton.getName()),
                closeLore
            )
            .slot(closeButton.getSlot())
            .actionHandler("close"));
    }
    
    private void createCategoryItem(String categoryName, String actionHandler) {
        LocaleManager.ReportCategory category = locale.getReportCategory(categoryName, targetPlayer);
        
        List<ChatElement<?>> lore = category.getLore().stream()
            .map(ChatElement::ofLegacyText)
            .collect(Collectors.toList());
        set(CirrusItem.of(
                ItemType.valueOf(category.getItemType()),
                ChatElement.ofLegacyText(category.getName()),
                lore
            )
            .slot(category.getSlot())
            .actionHandler(actionHandler));
    }
    
    @Override
    protected void registerActionHandlers() {
        registerActionHandler("chat_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("chat_violation", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("username_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("username", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("skin_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("skin", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("content_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("content", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("team_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("team_griefing", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("game_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("game_rules", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("cheating_report", click -> {
            click.clickedMenu().close();
            LocaleManager.ReportCategory category = locale.getReportCategory("cheating", targetPlayer);
            submitReport(category.getReportType(), category.getSubject());
        });
        
        registerActionHandler("close", click -> {
            click.clickedMenu().close();
        });
    }
    
    private void submitReport(String type, String subject) {
        String senderUuid = getSenderUuid();
        String senderName = getSenderName();
        
        CreateTicketRequest request = new CreateTicketRequest(
            senderUuid,                                         // creatorUuid
            senderName,                                         // creatorName
            type,                                               // type
            subject + ": " + targetPlayer,                     // subject
            reason != null ? reason : locale.getMessage("messages.no_details", Map.of()), // description
            "unknown-uuid",                                     // reportedPlayerUuid
            targetPlayer,                                       // reportedPlayerName
            null,                                              // chatMessages
            createReportFormData(type),                        // formData
            Arrays.asList(
                locale.getMessage("tags.player_report"),
                locale.getMessage("tags.minecraft"),
                type
            ), // tags
            locale.getPriority(type)                           // priority
        );
        
        sendMessage(locale.getMessage("messages.submitting", Map.of("type", "report")));
        
        CompletableFuture<CreateTicketResponse> future = httpClient.createTicket(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess() && response.getTicketId() != null) {
                sendMessage(locale.getMessage("messages.success", Map.of("type", "Report")));
                sendMessage(locale.getMessage("messages.ticket_id", Map.of("ticketId", response.getTicketId())));
                
                String ticketUrl = panelUrl + "/tickets/" + response.getTicketId();
                sendMessage(locale.getMessage("messages.view_ticket", Map.of("url", ticketUrl)));
                sendMessage(locale.getMessage("messages.evidence_note"));
            } else {
                String error = response.getMessage() != null ? response.getMessage() : locale.getMessage("messages.unknown_error");
                sendMessage(locale.getMessage("messages.failed_submit", Map.of("type", "report", "error", error)));
            }
        }).exceptionally(throwable -> {
            sendMessage(locale.getMessage("messages.failed_submit", Map.of("type", "report", "error", throwable.getMessage())));
            return null;
        });
    }
    
    private JsonObject createReportFormData(String reportType) {
        JsonObject formData = new JsonObject();
        formData.addProperty("reportType", reportType);
        formData.addProperty("reportedPlayer", targetPlayer);
        formData.addProperty("reason", reason);
        formData.addProperty("reporterName", getSenderName());
        formData.addProperty("reporterUuid", getSenderUuid());
        formData.addProperty("server", locale.getMessage("form_data.server_name"));
        return formData;
    }
    
    private String getSenderUuid() {
        if (reporter.getUniqueId() != null) {
            return reporter.getUniqueId().toString();
        }
        return UUID.randomUUID().toString();
    }
    
    private String getSenderName() {
        if (reporter.getIssuer() != null) {
            return reporter.getIssuer().toString();
        }
        return "Unknown";
    }
    
    private void sendMessage(String message) {
        reporter.sendMessage(message);
    }
}