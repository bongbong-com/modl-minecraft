package com.bongbong.modl.minecraft.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PunishmentDataTest {
    
    private Map<String, Object> testData;
    private Date testDate;
    
    @BeforeEach
    void setUp() {
        testDate = new Date();
        testData = new HashMap<>();
        testData.put("blockedName", "TestPlayer");
        testData.put("blockedSkin", "skin-hash-123");
        testData.put("linkedBanId", "ban-456");
        testData.put("linkedBanExpiry", testDate.getTime());
        testData.put("chatLog", Arrays.asList("message1", "message2", "message3"));
        testData.put("duration", 86400000L); // 1 day
        testData.put("altBlocking", true);
        testData.put("wipeAfterExpiry", true);
    }
    
    @Test
    void testFromMapWithAllFields() {
        PunishmentData data = PunishmentData.fromMap(testData);
        
        assertEquals("TestPlayer", data.blockedName());
        assertEquals("skin-hash-123", data.blockedSkin());
        assertEquals("ban-456", data.linkedBanId());
        assertEquals(testDate, data.linkedBanExpiry().get());
        assertEquals(Arrays.asList("message1", "message2", "message3"), data.chatLog());
        assertEquals(86400000L, data.duration());
        assertTrue(data.altBlocking());
        assertTrue(data.wipeAfterExpiry());
    }
    
    @Test
    void testFromMapWithEmptyMap() {
        PunishmentData data = PunishmentData.fromMap(new HashMap<>());
        
        assertNull(data.blockedName());
        assertNull(data.blockedSkin());
        assertNull(data.linkedBanId());
        assertNull(data.linkedBanExpiry().get());
        assertNull(data.chatLog());
        assertEquals(-1L, data.duration());
        assertFalse(data.altBlocking());
        assertFalse(data.wipeAfterExpiry());
    }
    
    @Test
    void testFromMapWithPartialFields() {
        Map<String, Object> partialData = new HashMap<>();
        partialData.put("blockedName", "PartialPlayer");
        partialData.put("duration", 3600000L); // 1 hour
        partialData.put("altBlocking", true);
        
        PunishmentData data = PunishmentData.fromMap(partialData);
        
        assertEquals("PartialPlayer", data.blockedName());
        assertNull(data.blockedSkin());
        assertNull(data.linkedBanId());
        assertNull(data.linkedBanExpiry().get());
        assertNull(data.chatLog());
        assertEquals(3600000L, data.duration());
        assertTrue(data.altBlocking());
        assertFalse(data.wipeAfterExpiry());
    }
    
    @Test
    void testFromMapWithDateObject() {
        Map<String, Object> dataWithDate = new HashMap<>();
        dataWithDate.put("linkedBanExpiry", testDate);
        
        PunishmentData data = PunishmentData.fromMap(dataWithDate);
        
        assertEquals(testDate, data.linkedBanExpiry().get());
    }
    
    @Test
    void testFromMapWithIntegerDuration() {
        Map<String, Object> dataWithInt = new HashMap<>();
        dataWithInt.put("duration", 7200000); // Integer instead of Long
        
        PunishmentData data = PunishmentData.fromMap(dataWithInt);
        
        assertEquals(7200000L, data.duration());
    }
    
    @Test
    void testExportWithAllFields() {
        AtomicReference<Date> linkedExpiry = new AtomicReference<>(testDate);
        List<String> chatLog = Arrays.asList("msg1", "msg2");
        
        PunishmentData data = new PunishmentData(
            "TestPlayer",
            "skin-hash",
            "ban-123",
            linkedExpiry,
            chatLog,
            86400000L,
            true,
            true
        );
        
        Map<String, Object> exported = data.export();
        
        assertEquals("TestPlayer", exported.get("blockedName"));
        assertEquals("skin-hash", exported.get("blockedSkin"));
        assertEquals("ban-123", exported.get("linkedBanId"));
        assertEquals(testDate.getTime(), exported.get("linkedBanExpiry"));
        assertEquals(chatLog, exported.get("chatLog"));
        assertEquals(86400000L, exported.get("duration"));
        assertEquals(true, exported.get("altBlocking"));
        assertEquals(true, exported.get("wipeAfterExpiry"));
    }
    
    @Test
    void testExportWithNullFields() {
        AtomicReference<Date> emptyExpiry = new AtomicReference<>();
        
        PunishmentData data = new PunishmentData(
            null,
            null,
            null,
            emptyExpiry,
            null,
            -1L, // Default duration
            false,
            false
        );
        
        Map<String, Object> exported = data.export();
        
        // Should not contain null or default values
        assertFalse(exported.containsKey("blockedName"));
        assertFalse(exported.containsKey("blockedSkin"));
        assertFalse(exported.containsKey("linkedBanId"));
        assertFalse(exported.containsKey("linkedBanExpiry"));
        assertFalse(exported.containsKey("chatLog"));
        assertFalse(exported.containsKey("duration"));
        assertFalse(exported.containsKey("altBlocking"));
        assertFalse(exported.containsKey("wipeAfterExpiry"));
    }
    
    @Test
    void testExportOnlyIncludesNonDefaultValues() {
        AtomicReference<Date> emptyExpiry = new AtomicReference<>();
        
        PunishmentData data = new PunishmentData(
            "OnlyName",
            null,
            null,
            emptyExpiry,
            null,
            -1L,
            true, // Only this boolean is true
            false
        );
        
        Map<String, Object> exported = data.export();
        
        assertEquals(2, exported.size()); // Only blockedName and altBlocking
        assertEquals("OnlyName", exported.get("blockedName"));
        assertEquals(true, exported.get("altBlocking"));
    }
    
    @Test
    void testRoundTripConversion() {
        // Test that fromMap(export()) produces equivalent data
        PunishmentData original = PunishmentData.fromMap(testData);
        Map<String, Object> exported = original.export();
        PunishmentData roundTrip = PunishmentData.fromMap(exported);
        
        assertEquals(original.blockedName(), roundTrip.blockedName());
        assertEquals(original.blockedSkin(), roundTrip.blockedSkin());
        assertEquals(original.linkedBanId(), roundTrip.linkedBanId());
        assertEquals(original.linkedBanExpiry().get(), roundTrip.linkedBanExpiry().get());
        assertEquals(original.chatLog(), roundTrip.chatLog());
        assertEquals(original.duration(), roundTrip.duration());
        assertEquals(original.altBlocking(), roundTrip.altBlocking());
        assertEquals(original.wipeAfterExpiry(), roundTrip.wipeAfterExpiry());
    }
    
    @Test
    void testLinkedBanExpiryThreadSafety() {
        AtomicReference<Date> expiry = new AtomicReference<>(testDate);
        PunishmentData data = new PunishmentData(null, null, null, expiry, null, -1L, false, false);
        
        // Test concurrent access
        Date newDate = new Date(testDate.getTime() + 1000);
        expiry.set(newDate);
        
        assertEquals(newDate, data.linkedBanExpiry().get());
    }
}