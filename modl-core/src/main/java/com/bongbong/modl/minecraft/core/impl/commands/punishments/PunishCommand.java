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
    @Syntax("<target> <type> [-severity low|regular|severe] [reason] [-alt-blocking] [-silent] [-stat-wipe]")
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
        Optional<PunishmentTypesResponse.PunishmentTypeData> punishmentTypeOpt = punishmentTypes.stream()
                .filter(pt -> pt.getName().equalsIgnoreCase(type))
                .findFirst();

        if (!punishmentTypeOpt.isPresent()) {
            String availableTypes = punishmentTypes.stream()
                    .map(PunishmentTypesResponse.PunishmentTypeData::getName)
                    .collect(Collectors.joining(", "));
            sender.sendMessage(Colors.translate("&cInvalid punishment type. Available types: " + availableTypes));
            return;
        }

        PunishmentTypesResponse.PunishmentTypeData punishmentType = punishmentTypeOpt.get();

        // Parse arguments
        PunishmentArgs punishmentArgs = parseArguments(args);
        
        // Validate severity
        if (punishmentArgs.severity != null && !VALID_SEVERITIES.contains(punishmentArgs.severity)) {
            sender.sendMessage(Colors.translate("&cInvalid severity. Valid severities: low, regular, severe"));
            return;
        }

        // Get issuer information
        String issuerName = sender.isPlayer() ? 
            platform.getAbstractPlayer(sender.getUniqueId(), false).username() : "Console";

        // Build punishment data
        Map<String, Object> data = buildPunishmentData(punishmentArgs, punishmentType);

        // Create punishment request using the punishment type name
        PunishmentCreateRequest request = new PunishmentCreateRequest(
            target.uuid().toString(),
            issuerName,
            punishmentType.getName().toLowerCase().replace(" ", "_"),
            punishmentArgs.reason.isEmpty() ? "No reason specified" : punishmentArgs.reason,
            punishmentArgs.duration > 0 ? punishmentArgs.duration : null,
            data,
            new ArrayList<>(), // notes
            new ArrayList<>()  // attachedTicketIds
        );

        // Send punishment request
        CompletableFuture<PunishmentCreateResponse> future = httpClient.createPunishmentWithResponse(request);
        
        future.thenAccept(response -> {
            if (response.isSuccess()) {
                sender.sendMessage(Colors.translate(
                    "&aSuccessfully issued " + punishmentType.getName().toLowerCase() + " punishment &a&l#" + response.getPunishmentId() + 
                    "&a for &f" + target.username() + "&a."));
                
                if (!punishmentArgs.silent) {
                    String warningMessage = getWarningMessage(punishmentType.getName(), target.username());
                    if (warningMessage != null) {
                        platform.broadcast(warningMessage);
                    }
                }
                
                // Staff broadcast
                platform.staffBroadcast(Colors.translate(
                    "&7&o[" + issuerName + ": created " + punishmentType.getName() + " #" + response.getPunishmentId() + 
                    " for " + target.username() + "&7&o]"));
                
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
                // Filter out manual punishment types (ordinals 0-2: kick, manual_mute, manual_ban)
                cachedPunishmentTypes = response.getData().stream()
                        .filter(pt -> pt.getOrdinal() > 2)
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
                // Filter out manual punishment types (ordinals 0-2: kick, manual_mute, manual_ban)
                cachedPunishmentTypes = response.getData().stream()
                        .filter(pt -> pt.getOrdinal() > 2)
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
                result.severity = arguments[++i].toLowerCase();
            } else if (arg.equalsIgnoreCase("-alt-blocking") || arg.equalsIgnoreCase("-ab")) {
                result.altBlocking = true;
            } else if (arg.equalsIgnoreCase("-silent") || arg.equalsIgnoreCase("-s")) {
                result.silent = true;
            } else if (arg.equalsIgnoreCase("-stat-wipe") || arg.equalsIgnoreCase("-sw")) {
                result.statWipe = true;
            } else {
                // Check if this might be a duration
                long duration = TimeUtil.getDuration(arg);
                if (duration != -1L && result.duration == 0) {
                    result.duration = duration;
                } else {
                    // Add to reason
                    if (reasonBuilder.length() > 0) {
                        reasonBuilder.append(" ");
                    }
                    reasonBuilder.append(arg);
                }
            }
        }
        
        result.reason = reasonBuilder.toString().trim();
        return result;
    }

    private Map<String, Object> buildPunishmentData(PunishmentArgs args, PunishmentTypesResponse.PunishmentTypeData punishmentType) {
        Map<String, Object> data = new HashMap<>();
        
        // Universal options
        data.put("silent", args.silent);
        
        // Type-specific options based on punishment type capabilities
        if (Boolean.TRUE.equals(punishmentType.getCanBeAltBlocking()) && args.altBlocking) {
            data.put("altBlocking", true);
        }
        
        if (Boolean.TRUE.equals(punishmentType.getCanBeStatWiping()) && args.statWipe) {
            data.put("wipeAfterExpiry", true);
        }
        
        // Severity (maps to panel's severity system)
        if (args.severity != null) {
            data.put("severity", args.severity);
        } else {
            data.put("severity", "regular"); // default
        }
        
        return data;
    }

    private String getWarningMessage(String punishmentTypeName, String username) {
        return Colors.translate("&c" + username + " has been " + punishmentTypeName.toLowerCase() + ".");
    }

    private static class PunishmentArgs {
        String severity = null;
        String reason = "";
        long duration = 0;
        boolean altBlocking = false;
        boolean silent = false;
        boolean statWipe = false;
    }
}