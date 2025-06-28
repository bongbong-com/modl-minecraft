package com.bongbong.modl.minecraft.core.locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LocaleManagerTest {
    /*
    
    @TempDir
    Path tempDir;
    
    private LocaleManager localeManager;
    private Path testLocaleFile;
    
    @BeforeEach
    void setUp() throws IOException {
        testLocaleFile = tempDir.resolve("test_locale.yml");
        createTestLocaleFile();
    }
    
    private void createTestLocaleFile() throws IOException {
        String localeContent = """
            messages:
              prefix: "&8[&6TEST&8]&r "
              cannot_report_self: "&cYou cannot report yourself!"
              submitting: "&eSubmitting {type}..."
              success: "&a{type} submitted successfully!"
              failed_submit: "&cFailed to submit {type}: {error}"
              
            report_gui:
              title: "&4&lReport {player}"
              inventory_size: 27
              categories:
                chat_violation:
                  item: "WRITABLE_BOOK"
                  slot: 1
                  name: "&c&lChat Violation"
                  lore:
                    - "&7Report inappropriate chat messages"
                    - "&7or language used by this player."
                    - ""
                    - "&e&lClick to report {player}"
                  report_type: "chat"
                  subject: "Chat Violation"
                username:
                  item: "NAME_TAG"
                  slot: 2
                  name: "&6&lInappropriate Username"
                  lore:
                    - "&7Report an inappropriate"
                    - "&7or offensive username."
                    - ""
                    - "&e&lClick to report {player}"
                  report_type: "player"
                  subject: "Inappropriate Username"
              close_button:
                item: "BARRIER"
                slot: 22
                name: "&c&lClose Menu"
                lore: []
              buffer_item:
                item: "GRAY_STAINED_GLASS_PANE"
                name: " "
                lore: []
                
            form_data:
              server_name: "Test Minecraft Server"
              automatic_chat_log: true
              
            tags:
              player_report: "player-report"
              chat_report: "chat-report"
              minecraft: "minecraft"
              
            priorities:
              chat: "medium"
              player: "high"
            """;
        
        try (FileWriter writer = new FileWriter(testLocaleFile.toFile())) {
            writer.write(localeContent);
        }
    }
    
    @Test
    void testLoadFromFile() {
        localeManager = new LocaleManager();
        
        assertDoesNotThrow(() -> localeManager.loadFromFile(testLocaleFile));
        
        assertEquals("&8[&6TEST&8]&r ", localeManager.getMessage("messages.prefix"));
    }
    
    @Test
    void testGetMessage() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String message = localeManager.getMessage("messages.cannot_report_self");
        assertEquals("§cYou cannot report yourself!", message);
    }
    
    @Test
    void testGetMessageWithPlaceholders() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        Map<String, String> placeholders = Map.of("type", "report");
        String message = localeManager.getMessage("messages.submitting", placeholders);
        
        assertEquals("§eSubmitting report...", message);
    }
    
    @Test
    void testGetMessageWithMultiplePlaceholders() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        Map<String, String> placeholders = Map.of(
            "type", "Bug Report",
            "error", "Network timeout"
        );
        String message = localeManager.getMessage("messages.failed_submit", placeholders);
        
        assertEquals("§cFailed to submit Bug Report: Network timeout", message);
    }
    
    @Test
    void testGetMessageMissing() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String message = localeManager.getMessage("messages.nonexistent");
        assertEquals("§cMissing locale: messages.nonexistent", message);
    }
    
    @Test
    void testGetMessageList() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        List<String> lore = localeManager.getMessageList("report_gui.categories.chat_violation.lore");
        
        assertEquals(4, lore.size());
        assertEquals("§7Report inappropriate chat messages", lore.get(0));
        assertEquals("§7or language used by this player.", lore.get(1));
        assertEquals("", lore.get(2));
        assertEquals("§e§lClick to report {player}", lore.get(3));
    }
    
    @Test
    void testGetMessageListWithPlaceholders() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        Map<String, String> placeholders = Map.of("player", "TestPlayer");
        List<String> lore = localeManager.getMessageList("report_gui.categories.chat_violation.lore", placeholders);
        
        assertEquals("§e§lClick to report TestPlayer", lore.get(3));
    }
    
    @Test
    void testGetMessageListMissing() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        List<String> lore = localeManager.getMessageList("report_gui.nonexistent.lore");
        
        assertEquals(1, lore.size());
        assertEquals("§cMissing locale list: report_gui.nonexistent.lore", lore.get(0));
    }
    
    @Test
    void testGetItemType() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String itemType = localeManager.getItemType("report_gui.categories.chat_violation.item");
        assertEquals("WRITABLE_BOOK", itemType);
    }
    
    @Test
    void testGetItemTypeDefault() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String itemType = localeManager.getItemType("report_gui.nonexistent.item");
        assertEquals("STONE", itemType);
    }
    
    @Test
    void testGetInteger() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        int slot = localeManager.getInteger("report_gui.categories.chat_violation.slot", 0);
        assertEquals(1, slot);
    }
    
    @Test
    void testGetIntegerDefault() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        int slot = localeManager.getInteger("report_gui.nonexistent.slot", 5);
        assertEquals(5, slot);
    }
    
    @Test
    void testGetIntegerFromString() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        int inventorySize = localeManager.getInteger("report_gui.inventory_size", 0);
        assertEquals(27, inventorySize);
    }
    
    @Test
    void testGetPriority() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String priority = localeManager.getPriority("chat");
        assertEquals("§medium", priority); // Note: colorized
    }
    
    @Test
    void testGetReportCategory() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        LocaleManager.ReportCategory category = localeManager.getReportCategory("chat_violation", "TestPlayer");
        
        assertEquals("WRITABLE_BOOK", category.getItemType());
        assertEquals(1, category.getSlot());
        assertEquals("§c§lChat Violation", category.getName());
        assertEquals(4, category.getLore().size());
        assertEquals("§e§lClick to report TestPlayer", category.getLore().get(3));
        assertEquals("§chat", category.getReportType());
        assertEquals("§Chat Violation", category.getSubject());
    }
    
    @Test
    void testGetCloseButton() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        LocaleManager.ReportCategory closeButton = localeManager.getCloseButton();
        
        assertEquals("BARRIER", closeButton.getItemType());
        assertEquals(22, closeButton.getSlot());
        assertEquals("§c§lClose Menu", closeButton.getName());
        assertEquals(0, closeButton.getLore().size());
        assertEquals("§close", closeButton.getReportType());
        assertEquals("§Close", closeButton.getSubject());
    }
    
    @Test
    void testGetCategoryNames() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        List<String> categoryNames = localeManager.getCategoryNames();
        
        assertEquals(2, categoryNames.size());
        assertTrue(categoryNames.contains("chat_violation"));
        assertTrue(categoryNames.contains("username"));
        // Should be sorted
        assertEquals("chat_violation", categoryNames.get(0));
        assertEquals("username", categoryNames.get(1));
    }
    
    @Test
    void testGetBufferItem() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        LocaleManager.ReportCategory bufferItem = localeManager.getBufferItem();
        
        assertEquals("GRAY_STAINED_GLASS_PANE", bufferItem.getItemType());
        assertEquals(0, bufferItem.getSlot()); // Default slot
        assertEquals("§ ", bufferItem.getName()); // Space character
        assertEquals(0, bufferItem.getLore().size());
        assertEquals("§buffer", bufferItem.getReportType());
        assertEquals("§Buffer", bufferItem.getSubject());
    }
    
    @Test
    void testCalculateMenuSize() {
        localeManager = new LocaleManager();
        
        // Test various category counts
        assertEquals(27, localeManager.calculateMenuSize(1)); // Min 3 rows
        assertEquals(27, localeManager.calculateMenuSize(5)); // 5 + 1 close = 6, need 1 row
        assertEquals(36, localeManager.calculateMenuSize(10)); // 10 + 1 close = 11, need 2 rows
        assertEquals(45, localeManager.calculateMenuSize(20)); // 20 + 1 close = 21, need 3 rows
        assertEquals(54, localeManager.calculateMenuSize(50)); // Max 6 rows
    }
    
    @Test
    void testColorizeFunction() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String colorized = localeManager.getMessage("messages.prefix");
        assertTrue(colorized.contains("§"));
        assertFalse(colorized.contains("&"));
    }
    
    @Test
    void testDefaultConstructor() {
        // This would try to load en_US.yml from resources
        assertDoesNotThrow(() -> {
            try {
                new LocaleManager();
            } catch (RuntimeException e) {
                // Expected if en_US.yml doesn't exist in test resources
                assertTrue(e.getMessage().contains("Locale file not found"));
            }
        });
    }
    
    @Test
    void testLoadNonExistentFile() {
        localeManager = new LocaleManager();
        Path nonExistentFile = tempDir.resolve("nonexistent.yml");
        
        // Should not throw exception for non-existent file
        assertDoesNotThrow(() -> localeManager.loadFromFile(nonExistentFile));
    }
    
    @Test
    void testNestedValueAccess() {
        localeManager = new LocaleManager();
        localeManager.loadFromFile(testLocaleFile);
        
        String serverName = localeManager.getMessage("form_data.server_name");
        assertEquals("§Test Minecraft Server", serverName);
    }
    
    @Test
    void testReportCategoryRecord() {
        LocaleManager.ReportCategory category = new LocaleManager.ReportCategory(
            "DIAMOND", 5, "Test Item", List.of("Line 1", "Line 2"), "test", "Test Subject"
        );
        
        assertEquals("DIAMOND", category.getItemType());
        assertEquals(5, category.getSlot());
        assertEquals("Test Item", category.getName());
        assertEquals(2, category.getLore().size());
        assertEquals("test", category.getReportType());
        assertEquals("Test Subject", category.getSubject());
    }
    */
}