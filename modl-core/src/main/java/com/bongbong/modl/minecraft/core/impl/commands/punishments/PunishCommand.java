package com.bongbong.modl.minecraft.core.impl.commands.punishments;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PunishmentCreateRequest;
import com.bongbong.modl.minecraft.api.http.response.PunishmentCreateResponse;
import com.bongbong.modl.minecraft.api.http.response.PunishmentTypesResponse;
import com.bongbong.modl.minecraft.api.http.response.StaffPermissionsResponse;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.locale.LocaleManager;
import com.bongbong.modl.minecraft.core.util.Colors;
import com.bongbong.modl.minecraft.core.util.PermissionUtil;
import com.bongbong.modl.minecraft.core.util.TimeUtil;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PunishCommand extends BaseCommand {
    private final ModlHttpClient httpClient;
    private final Platform platform;
    private final Cache cache;
    private final LocaleManager localeManager;
    
    // Cache for punishment types - loaded once at startup and manually refreshed
    private volatile List<PunishmentTypesResponse.PunishmentTypeData> cachedPunishmentTypes = new ArrayList<>();
    private volatile boolean cacheInitialized = false;

    private static final Set<String> VALID_SEVERITIES = Set.of("low", "regular", "severe");

    @CommandCompletion("@players @punishment-types")
    @CommandAlias("punish")
    @Syntax("<target> <type> [reason...] [-severity low|regular|severe] [-low] [-regular] [-severe] [-alt-blocking] [-silent] [-stat-wipe]")
    @Description("Issue a punishment to a player. Multi-word punishment types like 'Chat Abuse' are supported without quotes.")
    public void punish(CommandIssuer sender, @Name("target") Account target, @Single @Name("type") String type, @Optional @Name("reason") String[] reasonArgs) {
        if (target == null) {
            sender.sendMessage(localeManager.getPunishmentMessage("general.player_not_found", Map.of()));
            return;
        }

        // Check if cache is initialized, if not show error
        if (!cacheInitialized || cachedPunishmentTypes.isEmpty()) {
            sender.sendMessage(localeManager.getPunishmentMessage("general.punishment_types_not_loaded", Map.of()));
            return;
        }

        // Use cached punishment types
        List<PunishmentTypesResponse.PunishmentTypeData> punishmentTypes = cachedPunishmentTypes;
        
        // Find the punishment type by name
        java.util.Optional<PunishmentTypesResponse.PunishmentTypeData> punishmentTypeOpt = punishmentTypes.stream()
                .filter(pt -> pt.getName().equalsIgnoreCase(type))
                .findFirst();

        if (!punishmentTypeOpt.isPresent()) {
            String availableTypes = punishmentTypes.stream()
                    .map(PunishmentTypesResponse.PunishmentTypeData::getName)
                    .collect(Collectors.joining(", "));
            sender.sendMessage(localeManager.getPunishmentMessage("general.invalid_punishment_type", 
                Map.of("types", availableTypes)));
            return;
        }

        final PunishmentTypesResponse.PunishmentTypeData punishmentType = punishmentTypeOpt.get();

        // Check permission for this specific punishment type
        String punishmentPermission = PermissionUtil.formatPunishmentPermission(punishmentType.getName());
        if (!PermissionUtil.hasPermission(sender, cache, punishmentPermission)) {
            sender.sendMessage(localeManager.getPunishmentMessage("general.no_permission_punishment", 
                Map.of("type", punishmentType.getName())));
            return;
        }

        // Parse arguments
        String combinedArgs = reasonArgs != null ? String.join(" ", reasonArgs) : "";
        PunishmentArgs punishmentArgs = parseArguments(combinedArgs);
        
        // Validate severity
        if (punishmentArgs.severity != null && !VALID_SEVERITIES.contains(punishmentArgs.severity)) {
            sender.sendMessage(Colors.translate("&cInvalid severity. Valid severities: low, regular, severe"));
            return;
        }

        // Validate punishment type compatibility
        String validationError = validatePunishmentCompatibility(punishmentArgs, punishmentType);
        if (validationError != null) {
            sender.sendMessage(validationError);
            return;
        }

        // Set default values if not specified (matching panel logic)
        if (punishmentArgs.severity == null) {
            punishmentArgs.severity = "regular"; // Default severity
        }

        // Calculate offense level automatically based on player status (matching panel AI logic)
        String calculatedOffenseLevel = calculateOffenseLevel(target, punishmentType);
        punishmentArgs.offenseLevel = calculatedOffenseLevel;

        // Get issuer information
        final String issuerName = sender.isPlayer() ? 
            platform.getAbstractPlayer(sender.getUniqueId(), false).username() : "Console";

        // Build punishment data (matching panel logic)
        Map<String, Object> data = buildPunishmentData(punishmentArgs, punishmentType);

        // Create notes list (matching panel logic)
        List<String> notes = new ArrayList<>();
        if (!punishmentArgs.reason.isEmpty()) {
            notes.add(punishmentArgs.reason);
        }

        // Create punishment request matching panel API structure
        PunishmentCreateRequest request = new PunishmentCreateRequest(
            target.getMinecraftUuid().toString(),
            issuerName,
            punishmentType.getOrdinal(), // Use integer ordinal
            punishmentArgs.reason.isEmpty() ? "No reason specified" : punishmentArgs.reason,
            punishmentArgs.duration > 0 ? punishmentArgs.duration : null,
            data,
            notes,
            new ArrayList<>(), // attachedTicketIds
            punishmentArgs.severity, // severity for punishment calculation
            punishmentArgs.offenseLevel // status (offense level) for punishment calculation
        );

        // Make copies for lambda usage
        final String punishmentTypeName = punishmentType.getName();
        final boolean silentPunishment = punishmentArgs.silent;

        // Send punishment request
        CompletableFuture<PunishmentCreateResponse> future = httpClient.createPunishmentWithResponse(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess()) {
                String targetName = target.getUsernames().get(0).getUsername();
                
                // Success message to issuer
                sender.sendMessage(localeManager.punishment()
                    .type(punishmentTypeName)
                    .target(targetName)
                    .punishmentId(response.getPunishmentId())
                    .get("general.punishment_issued"));
                
                if (!silentPunishment) {
                    // Public notification - try punishment type specific first, then fall back to default
                    String publicMessage = getPublicNotificationMessage(punishmentTypeName, targetName, 0, punishmentType.getOrdinal());
                    if (publicMessage != null) {
                        platform.broadcast(publicMessage);
                    }
                }
                
                // Staff notification
                String staffMessage = localeManager.punishment()
                    .issuer(issuerName)
                    .type(punishmentTypeName)
                    .target(targetName)
                    .punishmentId(response.getPunishmentId())
                    .get("general.staff_notification");
                platform.staffBroadcast(staffMessage);
                
            } else {
                sender.sendMessage(localeManager.getPunishmentMessage("general.punishment_error", 
                    Map.of("error", response.getMessage())));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(localeManager.getPunishmentMessage("general.punishment_error", 
                Map.of("error", throwable.getMessage())));
            return null;
        });
    }

    /**
     * Initialize punishment types cache - called once at startup
     */
    public void initializePunishmentTypes() {
        // Load punishment types
        httpClient.getPunishmentTypes().thenAccept(response -> {
            if (response.isSuccess()) {
                // Filter out manual punishment types (ordinals 0-5: kick, manual_mute, manual_ban, security_ban, linked_ban, blacklist)
                cachedPunishmentTypes = response.getData().stream()
                        .filter(pt -> pt.getOrdinal() > 5)
                        .collect(Collectors.toList());
                cacheInitialized = true;
                platform.runOnMainThread(() -> {
                    // Log successful initialization
                    System.out.println("[MODL] Loaded " + cachedPunishmentTypes.size() + " punishment types from API");
                });
            } else {
                platform.runOnMainThread(() -> {
                    System.err.println("[MODL] Failed to load punishment types from API: " + response.getStatus());
                });
            }
        }).exceptionally(throwable -> {
            platform.runOnMainThread(() -> {
                System.err.println("[MODL] Error loading punishment types: " + throwable.getMessage());
            });
            return null;
        });
        
        // Load staff permissions
        loadStaffPermissions();
    }
    
    /**
     * Load staff permissions into cache
     */
    private void loadStaffPermissions() {
        httpClient.getStaffPermissions().thenAccept(response -> {
            cache.clearStaffPermissions();
            
            for (var staffMember : response.getData().getStaff()) {
                if (staffMember.getMinecraftUuid() != null) {
                    try {
                        UUID uuid = UUID.fromString(staffMember.getMinecraftUuid());
                        cache.cacheStaffPermissions(uuid, staffMember.getStaffRole(), staffMember.getPermissions());
                    } catch (IllegalArgumentException e) {
                        platform.runOnMainThread(() -> {
                            System.out.println("[MODL] Invalid UUID for staff member " + staffMember.getStaffUsername() + ": " + staffMember.getMinecraftUuid());
                        });
                    }
                }
            }
            
            platform.runOnMainThread(() -> {
                System.out.println("[MODL] Loaded permissions for " + response.getData().getStaff().size() + " staff members");
            });
        }).exceptionally(throwable -> {
            platform.runOnMainThread(() -> {
                System.err.println("[MODL] Error loading staff permissions: " + throwable.getMessage());
            });
            return null;
        });
    }

    /**
     * Refresh command to manually reload punishment types
     */
    @CommandAlias("modl reload")
    @Syntax("reload")
    public void reload(CommandIssuer sender) {
        // Check if user has admin permissions to refresh cache
        if (!PermissionUtil.hasAnyPermission(sender, cache, "admin.settings.view", "admin.settings.modify")) {
            sender.sendMessage(localeManager.getPunishmentMessage("general.no_permission_reload", Map.of()));
            return;
        }
        sender.sendMessage(localeManager.getPunishmentMessage("general.reloading", Map.of()));
        
        // Refresh punishment types
        httpClient.getPunishmentTypes().thenAccept(response -> {
            if (response.isSuccess()) {
                // Filter out manual punishment types (ordinals 0-5: kick, manual_mute, manual_ban, security_ban, linked_ban, blacklist)
                cachedPunishmentTypes = response.getData().stream()
                        .filter(pt -> pt.getOrdinal() > 5)
                        .collect(Collectors.toList());
                cacheInitialized = true;
                
                sender.sendMessage(localeManager.getPunishmentMessage("general.reload_success_types", 
                    Map.of("count", String.valueOf(cachedPunishmentTypes.size()))));
            } else {
                sender.sendMessage(localeManager.getPunishmentMessage("general.reload_error", 
                    Map.of("error", "Status: " + response.getStatus())));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(localeManager.getPunishmentMessage("general.reload_error", 
                Map.of("error", throwable.getMessage())));
            return null;
        });
        
        // Refresh staff permissions
        loadStaffPermissions();
    }

    /**
     * Get available punishment type names for tab completion
     */
    public List<String> getPunishmentTypeNames() {
        return cachedPunishmentTypes.stream()
                .map(PunishmentTypesResponse.PunishmentTypeData::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Get public notification message for punishment
     */
    private String getPublicNotificationMessage(String punishmentTypeName, String targetName, long duration, int ordinal) {
        // Determine if this is a temporary or permanent punishment
        boolean isTemporary = duration > 0;
        
        Map<String, String> variables = Map.of(
            "target", targetName,
            "duration", localeManager.formatDuration(duration)
        );
        
        // Try punishment-type-specific message first using ordinal
        String messagePath = isTemporary ? "public_notification.temporary" : "public_notification.permanent";
        String specificMessage = localeManager.getPunishmentTypeMessage(ordinal, messagePath, variables);
        
        // If no specific message found, use default based on punishment category
        if (specificMessage.contains("Missing locale:")) {
            String category = getBasicPunishmentCategory(punishmentTypeName);
            String defaultPath = isTemporary ? "public_notifications.temporary." : "public_notifications.permanent.";
            defaultPath += category;
            return localeManager.getPunishmentMessage(defaultPath, variables);
        }
        
        return specificMessage;
    }
    
    /**
     * Map punishment type name to basic category for default messages
     */
    private String getBasicPunishmentCategory(String punishmentTypeName) {
        String lower = punishmentTypeName.toLowerCase();
        if (lower.contains("kick")) return "kick";
        if (lower.contains("mute")) return "mute";
        if (lower.contains("ban")) return "ban";
        if (lower.contains("blacklist")) return "blacklist";
        
        // Default to ban for unknown types
        return "ban";
    }

    private PunishmentArgs parseArguments(String args) {
        String[] arguments = args.split(" ");
        PunishmentArgs result = new PunishmentArgs();
        
        StringBuilder reasonBuilder = new StringBuilder();
        
        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            
            if (arg.equalsIgnoreCase("-severity") && i + 1 < arguments.length) {
                String severityInput = arguments[++i].toLowerCase();
                // Map UI severity names to API severity names (matching panel logic)
                switch (severityInput) {
                    case "lenient":
                        result.severity = "low";
                        break;
                    case "regular":
                        result.severity = "regular";
                        break;
                    case "aggravated":
                    case "severe":
                        result.severity = "severe";
                        break;
                    default:
                        result.severity = severityInput; // Use as-is for low/regular/severe
                }
            } else if (arg.equalsIgnoreCase("-low")) {
                result.severity = "low";
            } else if (arg.equalsIgnoreCase("-regular")) {
                result.severity = "regular";
            } else if (arg.equalsIgnoreCase("-severe")) {
                result.severity = "severe";
            // Removed manual offense level - now calculated automatically
            } else if (arg.equalsIgnoreCase("-alt-blocking") || arg.equalsIgnoreCase("-ab")) {
                result.altBlocking = true;
            } else if (arg.equalsIgnoreCase("-silent") || arg.equalsIgnoreCase("-s")) {
                result.silent = true;
            } else if (arg.equalsIgnoreCase("-stat-wipe") || arg.equalsIgnoreCase("-sw")) {
                result.statWipe = true;
            } else {
                // For dynamic punishments, durations are calculated automatically based on severity and offense level
                // Any non-flag argument is treated as part of the reason
                if (reasonBuilder.length() > 0) {
                    reasonBuilder.append(" ");
                }
                reasonBuilder.append(arg);
            }
        }
        
        result.reason = reasonBuilder.toString().trim();
        return result;
    }

    private Map<String, Object> buildPunishmentData(PunishmentArgs args, PunishmentTypesResponse.PunishmentTypeData punishmentType) {
        Map<String, Object> data = new HashMap<>();
        
        // Initialize default fields (matching panel backend logic)
        data.put("duration", 0L);
        data.put("blockedName", null);
        data.put("blockedSkin", null);
        data.put("linkedBanId", null);
        data.put("linkedBanExpiry", null);
        data.put("chatLog", null);
        data.put("altBlocking", false);
        data.put("wipeAfterExpiry", false);
        
        // Universal options
        data.put("silent", args.silent);
        
        // Calculate duration based on punishment type configuration (matching panel logic)
        long calculatedDuration = calculateDuration(args, punishmentType);
        if (calculatedDuration > 0) {
            data.put("duration", calculatedDuration);
        }
        
        // Type-specific options based on punishment type capabilities
        if (Boolean.TRUE.equals(punishmentType.getCanBeAltBlocking()) && args.altBlocking) {
            data.put("altBlocking", true);
        }
        
        if (Boolean.TRUE.equals(punishmentType.getCanBeStatWiping()) && args.statWipe) {
            data.put("wipeAfterExpiry", true);
        }
        
        return data;
    }
    
    private long calculateDuration(PunishmentArgs args, PunishmentTypesResponse.PunishmentTypeData punishmentType) {
        // If manual duration specified (for administrative punishments), use that
        if (args.duration > 0) {
            return args.duration;
        }
        
        // For configured punishments, calculate based on severity and offense level
        // This requires the durations configuration from the punishment type
        // Since we don't have the full duration configuration in our API response,
        // we'll return 0 for now and let the server handle duration calculation
        // based on the severity and status fields we send
        
        return 0;
    }

    /**
     * Validate punishment type compatibility with provided arguments
     */
    private String validatePunishmentCompatibility(PunishmentArgs args, PunishmentTypesResponse.PunishmentTypeData punishmentType) {
        // Check if severity is being set on single-severity punishment
        if (Boolean.TRUE.equals(punishmentType.getSingleSeverityPunishment()) && args.severity != null) {
            return localeManager.getPunishmentMessage("validation.single_severity_error", 
                Map.of("type", punishmentType.getName()));
        }
        
        // Check if severity is being set on permanent until skin/name change punishments
        if (Boolean.TRUE.equals(punishmentType.getPermanentUntilSkinChange()) && args.severity != null) {
            return localeManager.getPunishmentMessage("validation.permanent_skin_change_error", 
                Map.of("type", punishmentType.getName()));
        }
        
        if (Boolean.TRUE.equals(punishmentType.getPermanentUntilNameChange()) && args.severity != null) {
            return localeManager.getPunishmentMessage("validation.permanent_name_change_error", 
                Map.of("type", punishmentType.getName()));
        }
        
        // Check if alt-blocking flag is used on punishment type that doesn't support it
        if (args.altBlocking && !Boolean.TRUE.equals(punishmentType.getCanBeAltBlocking())) {
            return localeManager.getPunishmentMessage("validation.alt_blocking_not_supported", 
                Map.of("type", punishmentType.getName()));
        }
        
        // Check if stat-wiping flag is used on punishment type that doesn't support it
        if (args.statWipe && !Boolean.TRUE.equals(punishmentType.getCanBeStatWiping())) {
            return localeManager.getPunishmentMessage("validation.stat_wiping_not_supported", 
                Map.of("type", punishmentType.getName()));
        }
        
        return null; // No validation errors
    }

    /**
     * Calculate offense level automatically based on player status (matching panel AI logic)
     */
    private String calculateOffenseLevel(Account target, PunishmentTypesResponse.PunishmentTypeData punishmentType) {
        // Calculate player status based on active punishment points (matching panel logic)
        PlayerStatus status = calculatePlayerStatus(target, punishmentType.getCategory());
        
        // Map player status to offense level (matching panel punishment-service.ts logic)
        switch (status) {
            case LOW:
                return "low";
            case MEDIUM:
                return "medium";
            case HABITUAL:
                return "habitual";
            default:
                return "low"; // Default fallback
        }
    }
    
    /**
     * Calculate player status based on active punishment points (matching panel player-status-calculator.ts)
     */
    private PlayerStatus calculatePlayerStatus(Account target, String category) {
        // Get active punishments and calculate points
        int totalPoints = 0;
        
        for (var punishment : target.getPunishments()) {
            if (isActivePunishment(punishment)) {
                // Add points for active punishments (simplified - in real implementation would need punishment type config)
                totalPoints += getPunishmentPoints(punishment);
            }
        }
        
        // Apply thresholds based on category (matching panel defaults)
        if ("Social".equalsIgnoreCase(category)) {
            // Social thresholds: Medium ≥ 4, Habitual ≥ 8
            if (totalPoints >= 8) return PlayerStatus.HABITUAL;
            if (totalPoints >= 4) return PlayerStatus.MEDIUM;
            return PlayerStatus.LOW;
        } else {
            // Gameplay thresholds: Medium ≥ 5, Habitual ≥ 10  
            if (totalPoints >= 10) return PlayerStatus.HABITUAL;
            if (totalPoints >= 5) return PlayerStatus.MEDIUM;
            return PlayerStatus.LOW;
        }
    }
    
    /**
     * Check if a punishment is currently active (matching panel logic)
     */
    private boolean isActivePunishment(Object punishment) {
        // Simplified check - in real implementation would check:
        // - If punishment has started
        // - If punishment has not expired
        // - If punishment has not been pardoned
        // For now, return true to count all punishments
        return true;
    }
    
    /**
     * Get points for a punishment (simplified)
     */
    private int getPunishmentPoints(Object punishment) {
        // Simplified point calculation - in real implementation would:
        // - Get punishment type configuration
        // - Use configured point values
        // - Consider severity and other factors
        return 1; // Default 1 point per punishment
    }
    
    private enum PlayerStatus {
        LOW, MEDIUM, HABITUAL
    }

    private String getWarningMessage(String punishmentTypeName, String username) {
        return Colors.translate("&c" + username + " has been " + punishmentTypeName.toLowerCase() + ".");
    }

    private static class PunishmentArgs {
        String severity = null;
        String offenseLevel = null;
        String reason = "";
        long duration = 0;
        boolean altBlocking = false;
        boolean silent = false;
        boolean statWipe = false;
    }
}