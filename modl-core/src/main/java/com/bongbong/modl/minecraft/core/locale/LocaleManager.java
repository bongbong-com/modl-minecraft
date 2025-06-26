package com.bongbong.modl.minecraft.core.locale;

import org.yaml.snakeyaml.Yaml;
import lombok.Getter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}