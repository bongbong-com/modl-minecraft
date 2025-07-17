package com.bongbong.modl.minecraft.core.impl.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PlayerLookupRequest;
import com.bongbong.modl.minecraft.api.http.response.PlayerLookupResponse;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.util.Colors;
import lombok.RequiredArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PlayerLookupCommand extends BaseCommand {
    private final ModlHttpClient httpClient;
    private final Platform platform;
    private final Cache cache;
    private final LocaleManager localeManager;

    @CommandCompletion("@players")
    @CommandAlias("lookup|look|check|info")
    @Syntax("<player>")
    @Description("Look up detailed information about a player (online or offline)")
    public void lookup(CommandIssuer sender, @Name("player") String playerQuery) {
        // Check if user is staff member - use staff permissions cache which is more reliable
        if (sender.isPlayer()) {
            UUID senderUuid = sender.getUniqueId();
            boolean isStaffMember = cache.isStaffMemberByPermissions(senderUuid);
            
            
            if (!isStaffMember) {
                sender.sendMessage(Colors.translate("&cYou must be a staff member to use this command."));
                return;
            }
        }
        // Console can always use the command

        sender.sendMessage(Colors.translate("&7Looking up player &e" + playerQuery + "&7..."));

        PlayerLookupRequest request = new PlayerLookupRequest(playerQuery);
        
        CompletableFuture<PlayerLookupResponse> future = httpClient.lookupPlayer(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess() && response.getData() != null) {
                displayPlayerInfo(sender, response.getData());
            } else {
                sender.sendMessage(Colors.translate("&cPlayer not found: &e" + playerQuery));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(Colors.translate("&cError looking up player: " + throwable.getMessage()));
            return null;
        });
    }

    private void displayPlayerInfo(CommandIssuer sender, PlayerLookupResponse.PlayerData data) {
        // Header
        sender.sendMessage(Colors.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(Colors.translate("&6&lPlayer Information: &e" + data.getCurrentUsername()));
        sender.sendMessage(Colors.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        // Basic Info
        sender.sendMessage(Colors.translate("&7UUID: &f" + data.getMinecraftUuid()));
        
        if (data.getPreviousUsernames() != null && !data.getPreviousUsernames().isEmpty()) {
            String prevNames = String.join(", ", data.getPreviousUsernames());
            if (prevNames.length() > 50) {
                prevNames = prevNames.substring(0, 47) + "...";
            }
            sender.sendMessage(Colors.translate("&7Previous names: &f" + prevNames));
        }

        // Online Status
        String onlineStatus = data.isOnline() ? "&aOnline" : "&cOffline";
        sender.sendMessage(Colors.translate("&7Status: " + onlineStatus));
        
        if (data.isOnline() && data.getCurrentServer() != null) {
            sender.sendMessage(Colors.translate("&7Current server: &f" + data.getCurrentServer()));
        }

        // Dates
        if (data.getFirstSeen() != null) {
            sender.sendMessage(Colors.translate("&7First seen: &f" + formatDate(data.getFirstSeen())));
        }
        if (data.getLastSeen() != null) {
            sender.sendMessage(Colors.translate("&7Last seen: &f" + formatDate(data.getLastSeen())));
        }

        // Location Info
        if (data.getCountry() != null) {
            sender.sendMessage(Colors.translate("&7Country: &f" + data.getCountry()));
        }

        // Punishment Statistics
        if (data.getPunishmentStats() != null) {
            PlayerLookupResponse.PunishmentStats stats = data.getPunishmentStats();
            sender.sendMessage(Colors.translate(""));
            sender.sendMessage(Colors.translate("&c&lPunishment Statistics:"));
            
            String statusColor = getStatusColor(stats.getStatus());
            sender.sendMessage(Colors.translate("&7Status: " + statusColor + stats.getStatus().toUpperCase() + " &7(&f" + stats.getPoints() + " points&7)"));
            
            sender.sendMessage(Colors.translate("&7Total punishments: &f" + stats.getTotalPunishments()));
            sender.sendMessage(Colors.translate("&7Active punishments: &f" + stats.getActivePunishments()));
            
            if (stats.getBans() > 0) {
                sender.sendMessage(Colors.translate("&7Bans: &f" + stats.getBans()));
            }
            if (stats.getMutes() > 0) {
                sender.sendMessage(Colors.translate("&7Mutes: &f" + stats.getMutes()));
            }
            if (stats.getKicks() > 0) {
                sender.sendMessage(Colors.translate("&7Kicks: &f" + stats.getKicks()));
            }
            if (stats.getWarnings() > 0) {
                sender.sendMessage(Colors.translate("&7Warnings: &f" + stats.getWarnings()));
            }
        }

        // Recent Punishments
        if (data.getRecentPunishments() != null && !data.getRecentPunishments().isEmpty()) {
            sender.sendMessage(Colors.translate(""));
            sender.sendMessage(Colors.translate("&c&lRecent Punishments:"));
            
            int count = 0;
            for (PlayerLookupResponse.RecentPunishment punishment : data.getRecentPunishments()) {
                if (count >= 3) break; // Limit to 3 recent punishments
                
                String activeIndicator = punishment.isActive() ? "&c●" : "&8●";
                String typeColor = getPunishmentTypeColor(punishment.getType());
                
                sender.sendMessage(Colors.translate(String.format("  %s %s%s &7by &f%s &7- %s", 
                    activeIndicator, 
                    typeColor, 
                    punishment.getType() != null ? punishment.getType() : "Unknown",
                    punishment.getIssuer() != null ? punishment.getIssuer() : "System",
                    formatDate(punishment.getIssuedAt()))));
                
                count++;
            }
        }

        // Recent Tickets
        if (data.getRecentTickets() != null && !data.getRecentTickets().isEmpty()) {
            sender.sendMessage(Colors.translate(""));
            sender.sendMessage(Colors.translate("&9&lRecent Tickets:"));
            
            int count = 0;
            for (PlayerLookupResponse.RecentTicket ticket : data.getRecentTickets()) {
                if (count >= 2) break; // Limit to 2 recent tickets
                
                String statusColor = getTicketStatusColor(ticket.getStatus());
                String title = ticket.getTitle() != null ? ticket.getTitle() : "No title";
                String status = ticket.getStatus() != null ? ticket.getStatus() : "Open";
                
                sender.sendMessage(Colors.translate(String.format("  &7#%s %s%s &7- %s", 
                    ticket.getId(),
                    statusColor,
                    status.toUpperCase(),
                    formatDate(ticket.getCreatedAt()))));
                
                if (title.length() > 40) {
                    sender.sendMessage(Colors.translate("    &8" + title.substring(0, 37) + "..."));
                } else {
                    sender.sendMessage(Colors.translate("    &8" + title));
                }
                count++;
            }
        }

        // Profile Links
        sender.sendMessage(Colors.translate(""));
        if (data.getProfileUrl() != null) {
            // Create clickable JSON message for profile link
            String profileMessage = String.format(
                "{\"text\":\"\",\"extra\":[" +
                "{\"text\":\"📋 \",\"color\":\"gold\"}," +
                "{\"text\":\"View Full Profile\",\"color\":\"aqua\",\"underlined\":true," +
                "\"clickEvent\":{\"action\":\"open_url\",\"value\":\"%s\"}," +
                "\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click to view %s's profile\"}}]}",
                data.getProfileUrl(), data.getCurrentUsername()
            );
            
            if (sender.isPlayer()) {
                UUID senderUuid = sender.getUniqueId();
                platform.runOnMainThread(() -> {
                    platform.sendJsonMessage(senderUuid, profileMessage);
                });
            } else {
                sender.sendMessage(Colors.translate("&6Profile: &b" + data.getProfileUrl()));
            }
        }

        sender.sendMessage(Colors.translate("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private String formatDate(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(isoDate);
            return new SimpleDateFormat("MMM dd, yyyy HH:mm").format(date);
        } catch (Exception e) {
            return isoDate; // Fallback to original string
        }
    }

    private String getStatusColor(String status) {
        if (status == null) return "&7";
        switch (status.toLowerCase()) {
            case "low": return "&a";
            case "medium": return "&e";
            case "habitual": return "&c";
            default: return "&7";
        }
    }

    private String getPunishmentTypeColor(String type) {
        if (type == null) return "&7";
        String lower = type.toLowerCase();
        if (lower.contains("ban")) return "&4";
        if (lower.contains("mute")) return "&6";
        if (lower.contains("kick")) return "&e";
        if (lower.contains("warn")) return "&c";
        return "&7";
    }

    private String getTicketStatusColor(String status) {
        if (status == null) return "&7";
        switch (status.toLowerCase()) {
            case "open": return "&a";
            case "pending": return "&e";
            case "closed": return "&8";
            case "resolved": return "&2";
            default: return "&7";
        }
    }
}