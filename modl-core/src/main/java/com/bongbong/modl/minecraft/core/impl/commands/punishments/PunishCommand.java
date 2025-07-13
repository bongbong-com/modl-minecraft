package com.bongbong.modl.minecraft.core.impl.commands.punishments;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.api.http.request.PunishmentCreateRequest;
import com.bongbong.modl.minecraft.api.http.response.PunishmentCreateResponse;
import com.bongbong.modl.minecraft.api.http.response.PunishmentTypesResponse;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.Platform;
import com.bongbong.modl.minecraft.core.util.Colors;
import com.bongbong.modl.minecraft.core.util.TimeUtil;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PunishCommand extends BaseCommand {
    private final ModlHttpClient httpClient;
    private final Platform platform;
    
    // Cache for punishment types - loaded once at startup and manually refreshed
    private volatile List<PunishmentTypesResponse.PunishmentTypeData> cachedPunishmentTypes = new ArrayList<>();
    private volatile boolean cacheInitialized = false;

    private static final Set<String> VALID_SEVERITIES = Set.of("low", "regular", "severe");

    @CommandCompletion("@players @punishment-types")
    @CommandAlias("punish")
    @Syntax("<target> <type> [reason...] [-severity low|regular|severe] [-low] [-regular] [-severe] [-alt-blocking] [-silent] [-stat-wipe]")
    @CommandPermission(Constants.PERMISSION)
    public void punish(CommandIssuer sender, @Name("target") Account target, @Name("type") String type, @Default("") String args) {
        if (target == null) {
            sender.sendMessage(Colors.translate(Constants.NOT_FOUND));
            return;
        }

        // Check if cache is initialized, if not show error
        if (!cacheInitialized || cachedPunishmentTypes.isEmpty()) {
            sender.sendMessage(Colors.translate("&cPunishment types not loaded. Try &f/punish refresh&c or contact an administrator."));
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
            sender.sendMessage(Colors.translate("&cInvalid punishment type. Available types: " + availableTypes));
            return;
        }

        final PunishmentTypesResponse.PunishmentTypeData punishmentType = punishmentTypeOpt.get();

        // Parse arguments
        PunishmentArgs punishmentArgs = parseArguments(args);
        
        // Validate severity
        if (punishmentArgs.severity != null && !VALID_SEVERITIES.contains(punishmentArgs.severity)) {
            sender.sendMessage(Colors.translate("&cInvalid severity. Valid severities: low, regular, severe"));
            return;
        }

        // Validate punishment type compatibility
        String validationError = validatePunishmentCompatibility(punishmentArgs, punishmentType);
        if (validationError != null) {
            sender.sendMessage(Colors.translate("&c" + validationError));
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
                sender.sendMessage(Colors.translate(
                    "&aSuccessfully issued " + punishmentTypeName.toLowerCase() + " punishment &a&l#" + response.getPunishmentId() + 
                    "&a for &f" + target.getUsernames().get(0).getUsername() + "&a."));
                
                if (!silentPunishment) {
                    String warningMessage = getWarningMessage(punishmentTypeName, target.getUsernames().get(0).getUsername());
                    if (warningMessage != null) {
                        platform.broadcast(warningMessage);
                    }
                }
                
                // Staff broadcast
                platform.staffBroadcast(Colors.translate(
                    "&7&o[" + issuerName + ": created " + punishmentTypeName + " #" + response.getPunishmentId() + 
                    " for " + target.getUsernames().get(0).getUsername() + "&7&o]"));
                
            } else {
                sender.sendMessage(Colors.translate("&cFailed to create punishment: " + response.getMessage()));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(Colors.translate("&cError creating punishment: " + throwable.getMessage()));
            return null;
        });
    }

    /**
     * Initialize punishment types cache - called once at startup
     */
    public void initializePunishmentTypes() {
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
    }

    /**
     * Refresh command to manually reload punishment types
     */
    @CommandAlias("punish refresh")
    @Syntax("refresh")
    @CommandPermission(Constants.PERMISSION)
    public void refresh(CommandIssuer sender) {
        sender.sendMessage(Colors.translate("&eRefreshing punishment types..."));
        
        httpClient.getPunishmentTypes().thenAccept(response -> {
            if (response.isSuccess()) {
                // Filter out manual punishment types (ordinals 0-5: kick, manual_mute, manual_ban, security_ban, linked_ban, blacklist)
                cachedPunishmentTypes = response.getData().stream()
                        .filter(pt -> pt.getOrdinal() > 5)
                        .collect(Collectors.toList());
                cacheInitialized = true;
                
                sender.sendMessage(Colors.translate("&aSuccessfully refreshed " + cachedPunishmentTypes.size() + " punishment types."));
            } else {
                sender.sendMessage(Colors.translate("&cFailed to refresh punishment types: " + response.getStatus()));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(Colors.translate("&cError refreshing punishment types: " + throwable.getMessage()));
            return null;
        });
    }

    /**
     * Get available punishment type names for tab completion
     */
    public List<String> getPunishmentTypeNames() {
        return cachedPunishmentTypes.stream()
                .map(PunishmentTypesResponse.PunishmentTypeData::getName)
                .collect(Collectors.toList());
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
            return "Cannot set severity on single-severity punishment type '" + punishmentType.getName() + "'. This punishment type has only one severity level.";
        }
        
        // Check if severity is being set on permanent until skin/name change punishments
        if (Boolean.TRUE.equals(punishmentType.getPermanentUntilSkinChange()) && args.severity != null) {
            return "Cannot set severity on '" + punishmentType.getName() + "'. This punishment is permanent until skin change.";
        }
        
        if (Boolean.TRUE.equals(punishmentType.getPermanentUntilNameChange()) && args.severity != null) {
            return "Cannot set severity on '" + punishmentType.getName() + "'. This punishment is permanent until name change.";
        }
        
        // Check if alt-blocking flag is used on punishment type that doesn't support it
        if (args.altBlocking && !Boolean.TRUE.equals(punishmentType.getCanBeAltBlocking())) {
            return "Alt-blocking is not available for punishment type '" + punishmentType.getName() + "'.";
        }
        
        // Check if stat-wiping flag is used on punishment type that doesn't support it
        if (args.statWipe && !Boolean.TRUE.equals(punishmentType.getCanBeStatWiping())) {
            return "Stat-wiping is not available for punishment type '" + punishmentType.getName() + "'.";
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