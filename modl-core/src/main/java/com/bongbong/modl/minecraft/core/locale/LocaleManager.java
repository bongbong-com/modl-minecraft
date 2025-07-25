package com.bongbong.modl.minecraft.core.locale;

import org.yaml.snakeyaml.Yaml;
import lombok.Getter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LocaleManager {
    
    @Getter
    private Map<String, Object> messages;
    private final String currentLocale;
    
    public LocaleManager(String locale) {
        this.currentLocale = locale;
        this.messages = new HashMap<>();
        loadLocale(locale);
    }
    
    public LocaleManager() {
        this("en_US");
    }
    
    private void loadLocale(String locale) {
        try {
            // Try to load from resources first
            String resourcePath = "/locale/" + locale + ".yml";
            InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
            
            if (resourceStream != null) {
                Yaml yaml = new Yaml();
                this.messages = yaml.load(resourceStream);
                resourceStream.close();
            } else {
                throw new RuntimeException("Locale file not found: " + resourcePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load locale: " + locale, e);
        }
    }
    
    public void loadFromFile(Path localeFile) {
        try {
            if (Files.exists(localeFile)) {
                Yaml yaml = new Yaml();
                this.messages = yaml.load(Files.newInputStream(localeFile));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load locale from file: " + localeFile, e);
        }
    }
    
    public String getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        Object value = getNestedValue(messages, path);
        if (value instanceof String) {
            String message = (String) value;
            
            // Replace placeholders
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            // Convert color codes
            return colorize(message);
        }
        return "&cMissing locale: " + path;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getMessageList(String path) {
        return getMessageList(path, new HashMap<>());
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getMessageList(String path, Map<String, String> placeholders) {
        Object value = getNestedValue(messages, path);
        if (value instanceof List) {
            List<String> list = (List<String>) value;
            return list.stream()
                    .map(line -> {
                        // Replace placeholders
                        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                            line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                        return colorize(line);
                    })
                    .toList();
        }
        return List.of("&cMissing locale list: " + path);
    }
    
    public String getItemType(String path) {
        Object value = getNestedValue(messages, path);
        return value instanceof String ? (String) value : "STONE";
    }
    
    public int getInteger(String path, int defaultValue) {
        Object value = getNestedValue(messages, path);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
    
    public String getPriority(String reportType) {
        return getMessage("priorities." + reportType, Map.of("type", reportType));
    }
    
    public ReportCategory getReportCategory(String categoryName, String targetPlayer) {
        Map<String, String> placeholders = Map.of("player", targetPlayer);
        
        String basePath = "report_gui.categories." + categoryName;
        
        return new ReportCategory(
            getItemType(basePath + ".item"),
            getInteger(basePath + ".slot", 0),
            getMessage(basePath + ".name", placeholders),
            getMessageList(basePath + ".lore", placeholders),
            getMessage(basePath + ".report_type"),
            getMessage(basePath + ".subject", placeholders)
        );
    }
    
    public ReportCategory getCloseButton() {
        return new ReportCategory(
            getItemType("report_gui.close_button.item"),
            getInteger("report_gui.close_button.slot", 22),
            getMessage("report_gui.close_button.name"),
            getMessageList("report_gui.close_button.lore"),
            "close",
            "Close"
        );
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getCategoryNames() {
        Object categoriesObj = getNestedValue(messages, "report_gui.categories");
        if (categoriesObj instanceof Map) {
            Map<String, Object> categories = (Map<String, Object>) categoriesObj;
            return categories.keySet().stream().sorted().toList();
        }
        return List.of();
    }
    
    public ReportCategory getBufferItem() {
        return new ReportCategory(
            getItemType("report_gui.buffer_item.item"),
            0, // slot will be set dynamically
            getMessage("report_gui.buffer_item.name"),
            getMessageList("report_gui.buffer_item.lore"),
            "buffer",
            "Buffer"
        );
    }
    
    public int calculateMenuSize(int categoryCount) {
        // Calculate minimum rows needed (categories + close button + buffer)
        int minSlots = categoryCount + 1; // +1 for close button
        
        // Round up to nearest multiple of 9 (full rows)
        int rows = (int) Math.ceil(minSlots / 9.0);
        
        // Minimum 3 rows, maximum 6 rows
        rows = Math.max(3, Math.min(6, rows));
        
        return rows * 9;
    }
    
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] keys = path.split("\\.");
        Object current = map;
        
        for (String key : keys) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private String colorize(String message) {
        return message.replace("&", "§");
    }
    
    // ==================== PUNISHMENT SYSTEM METHODS ====================
    
    /**
     * Get a punishment message with variable replacement
     */
    public String getPunishmentMessage(String path, Map<String, String> variables) {
        Map<String, String> allVariables = new HashMap<>(variables);
        
        // Add config variables
        allVariables.putIfAbsent("appeal_url", getMessage("config.appeal_url"));
        allVariables.putIfAbsent("default_reason", getMessage("config.default_reason"));
        
        return getMessage(path, allVariables);
    }
    
    /**
     * Get a punishment message with builder pattern for common variables
     */
    public PunishmentMessageBuilder punishment() {
        return new PunishmentMessageBuilder(this);
    }
    
    /**
     * Get punishment type specific message by ordinal, falling back to default
     */
    public String getPunishmentTypeMessage(int ordinal, String messagePath, Map<String, String> variables) {
        // Try punishment type specific message first using ordinal
        String specificPath = "punishment_types.ordinal_" + ordinal + "." + messagePath;
        Object specificValue = getNestedValue(messages, specificPath);
        
        if (specificValue instanceof String) {
            return getMessage(specificPath, variables);
        }
        
        // Fall back to default punishment message
        return getPunishmentMessage(messagePath, variables);
    }
    
    /**
     * Get punishment type specific message by name (deprecated - use ordinal version)
     * @deprecated Use getPunishmentTypeMessage(int ordinal, String messagePath, Map<String, String> variables) instead
     */
    @Deprecated
    public String getPunishmentTypeMessage(String punishmentTypeName, String messagePath, Map<String, String> variables) {
        String normalizedTypeName = normalizeTypeName(punishmentTypeName);
        
        // Try punishment type specific message first
        String specificPath = "punishment_types." + normalizedTypeName + "." + messagePath;
        Object specificValue = getNestedValue(messages, specificPath);
        
        if (specificValue instanceof String) {
            return getMessage(specificPath, variables);
        }
        
        // Fall back to default punishment message
        return getPunishmentMessage(messagePath, variables);
    }
    
    /**
     * Get public notification message for punishment type by ordinal
     */
    public String getPublicNotificationMessage(int ordinal, Map<String, String> variables) {
        String path = "punishment_types.ordinal_" + ordinal + ".public_notification";
        Object value = getNestedValue(messages, path);
        
        if (value instanceof String) {
            return getMessage(path, variables);
        }
        
        // Fallback to default if no specific message found
        return getMessage("punishments.public_notifications.default", variables);
    }
    
    /**
     * Get player notification message for punishment type by ordinal (returns joined lines)
     */
    public String getPlayerNotificationMessage(int ordinal, Map<String, String> variables) {
        String path = "punishment_types.ordinal_" + ordinal + ".player_notification";
        Object value = getNestedValue(messages, path);
        
        if (value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) value;
            
            // Replace variables in each line and join with newlines
            return lines.stream()
                    .map(line -> {
                        // Replace placeholders
                        for (Map.Entry<String, String> entry : variables.entrySet()) {
                            line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                        return colorize(line);
                    })
                    .collect(Collectors.joining("\n"));
        } else if (value instanceof String) {
            // Handle legacy single string format
            return getMessage(path, variables);
        }
        
        // Fallback to default if no specific message found
        return getMessage("punishments.player_notifications.default", variables);
    }
    
    /**
     * Get player notification message lines as separate strings (for multi-line sending)
     */
    public List<String> getPlayerNotificationLines(int ordinal, Map<String, String> variables) {
        String path = "punishment_types.ordinal_" + ordinal + ".player_notification";
        Object value = getNestedValue(messages, path);
        
        if (value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) value;
            
            // Replace variables in each line
            return lines.stream()
                    .map(line -> {
                        // Replace placeholders
                        for (Map.Entry<String, String> entry : variables.entrySet()) {
                            line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                        return colorize(line);
                    })
                    .collect(Collectors.toList());
        } else if (value instanceof String) {
            // Handle legacy single string format
            return List.of(getMessage(path, variables));
        }
        
        // Fallback to default if no specific message found
        return List.of(getMessage("punishments.player_notifications.default", variables));
    }
    
    /**
     * Format duration in a human-readable way
     */
    public String formatDuration(long durationMs) {
        if (durationMs <= 0) {
            return getMessage("config.duration_format.permanent");
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(durationMs);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
        
        StringBuilder duration = new StringBuilder();
        
        if (days > 0) {
            duration.append(getMessage("config.duration_format.days", Map.of("days", String.valueOf(days))));
        }
        if (hours > 0) {
            if (duration.length() > 0) duration.append(" ");
            duration.append(getMessage("config.duration_format.hours", Map.of("hours", String.valueOf(hours))));
        }
        if (minutes > 0) {
            if (duration.length() > 0) duration.append(" ");
            duration.append(getMessage("config.duration_format.minutes", Map.of("minutes", String.valueOf(minutes))));
        }
        if (seconds > 0 && duration.length() == 0) {
            duration.append(getMessage("config.duration_format.seconds", Map.of("seconds", String.valueOf(seconds))));
        }
        
        return duration.toString();
    }
    
    /**
     * Format expiration date
     */
    public String formatExpiration(Date expiration) {
        String format = getMessage("config.date_format");
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.format(expiration);
        } catch (Exception e) {
            return expiration.toString();
        }
    }
    
    /**
     * Normalize punishment type name for config lookup
     */
    private String normalizeTypeName(String typeName) {
        return typeName.toLowerCase().replace(" ", "_").replace("-", "_");
    }
    
    /**
     * Builder class for punishment messages
     */
    public static class PunishmentMessageBuilder {
        private final LocaleManager localeManager;
        private final Map<String, String> variables;
        
        public PunishmentMessageBuilder(LocaleManager localeManager) {
            this.localeManager = localeManager;
            this.variables = new HashMap<>();
        }
        
        public PunishmentMessageBuilder target(String target) {
            variables.put("target", target);
            return this;
        }
        
        public PunishmentMessageBuilder player(String player) {
            variables.put("player", player);
            return this;
        }
        
        public PunishmentMessageBuilder issuer(String issuer) {
            variables.put("issuer", issuer);
            return this;
        }
        
        public PunishmentMessageBuilder reason(String reason) {
            variables.put("reason", reason);
            return this;
        }
        
        public PunishmentMessageBuilder type(String type) {
            variables.put("type", type);
            variables.put("punishment_type", type);
            return this;
        }
        
        public PunishmentMessageBuilder duration(long durationMs) {
            variables.put("duration", localeManager.formatDuration(durationMs));
            return this;
        }
        
        public PunishmentMessageBuilder expiration(Date expiration) {
            variables.put("expiration", localeManager.formatExpiration(expiration));
            return this;
        }
        
        public PunishmentMessageBuilder punishmentId(String punishmentId) {
            variables.put("punishment_id", punishmentId);
            return this;
        }
        
        public PunishmentMessageBuilder server(String server) {
            variables.put("server", server);
            return this;
        }
        
        public PunishmentMessageBuilder variable(String key, String value) {
            variables.put(key, value);
            return this;
        }
        
        public String get(String path) {
            return localeManager.getPunishmentMessage(path, variables);
        }
        
        public String getForType(String punishmentTypeName, String path) {
            return localeManager.getPunishmentTypeMessage(punishmentTypeName, path, variables);
        }
        
        public String getForOrdinal(int ordinal, String path) {
            return localeManager.getPunishmentTypeMessage(ordinal, path, variables);
        }
    }
    
    public static class ReportCategory {
        @Getter private final String itemType;
        @Getter private final int slot;
        @Getter private final String name;
        @Getter private final List<String> lore;
        @Getter private final String reportType;
        @Getter private final String subject;
        
        public ReportCategory(String itemType, int slot, String name, List<String> lore, String reportType, String subject) {
            this.itemType = itemType;
            this.slot = slot;
            this.name = name;
            this.lore = lore;
            this.reportType = reportType;
            this.subject = subject;
        }
    }
    
    /**
     * Get the current locale name
     */
    public String getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Reload the locale from file
     */
    public void reloadLocale() {
        loadLocale(currentLocale);
    }
}