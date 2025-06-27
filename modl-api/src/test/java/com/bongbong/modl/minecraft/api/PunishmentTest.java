package com.bongbong.modl.minecraft.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PunishmentTest {
    
    private Map<String, Object> testDataMap;
    private Date testIssueDate;
    private Date testStartDate;
    private Date testExpiryDate;
    private List<Modification> testModifications;
    private List<Note> testNotes;
    private List<String> testTicketIds;
    
    @BeforeEach
    void setUp() {
        testIssueDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        testStartDate = new Date(System.currentTimeMillis() - 3600000L); // 1 hour ago
        testExpiryDate = new Date(System.currentTimeMillis() + 86400000L); // 1 day from now
        
        testModifications = new ArrayList<>();
        testNotes = new ArrayList<>();
        testTicketIds = Arrays.asList("ticket-123", "ticket-456");
        
        testDataMap = new HashMap<>();
        testDataMap.put("reason", "Test punishment reason");
        testDataMap.put("duration", 172800000L); // 2 days in milliseconds
        testDataMap.put("expires", testExpiryDate);
        testDataMap.put("active", true);
        testDataMap.put("evidence", "Test evidence");
    }
    
    @Test
    void testPunishmentCreation() {
        Punishment punishment = new Punishment(
            "PUN-123",
            "TestModerator",
            testIssueDate,
            testStartDate,
            Punishment.Type.MUTE,
            testModifications,
            testNotes,
            testTicketIds,
            testDataMap
        );
        
        assertEquals("PUN-123", punishment.getId());
        assertEquals("TestModerator", punishment.getIssuerName());
        assertEquals(testIssueDate, punishment.getIssued());
        assertEquals(testStartDate, punishment.getStarted());
        assertEquals(Punishment.Type.MUTE, punishment.getType());
        assertEquals(testModifications, punishment.getModifications());
        assertEquals(testNotes, punishment.getNotes());
        assertEquals(testTicketIds, punishment.getAttachedTicketIds());
        assertEquals(testDataMap, punishment.getDataMap());
    }
    
    @Test
    void testGetReason() {
        Punishment punishment = createTestPunishment();
        
        assertEquals("Test punishment reason", punishment.getReason());
    }
    
    @Test
    void testGetReasonWithNullData() {
        Map<String, Object> emptyDataMap = new HashMap<>();
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, emptyDataMap
        );
        
        assertNull(punishment.getReason());
    }
    
    @Test
    void testGetReasonWithNonStringData() {
        Map<String, Object> invalidDataMap = new HashMap<>();
        invalidDataMap.put("reason", 12345); // Non-string value
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, invalidDataMap
        );
        
        assertNull(punishment.getReason());
    }
    
    @Test
    void testGetExpires() {
        Punishment punishment = createTestPunishment();
        
        assertEquals(testExpiryDate, punishment.getExpires());
    }
    
    @Test
    void testGetExpiresWithLongValue() {
        Map<String, Object> dataMapWithLong = new HashMap<>(testDataMap);
        dataMapWithLong.put("expires", testExpiryDate.getTime());
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, dataMapWithLong
        );
        
        assertEquals(testExpiryDate, punishment.getExpires());
    }
    
    @Test
    void testGetExpiresWithNullValue() {
        Map<String, Object> dataMapWithoutExpiry = new HashMap<>(testDataMap);
        dataMapWithoutExpiry.remove("expires");
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, dataMapWithoutExpiry
        );
        
        assertNull(punishment.getExpires());
    }
    
    @Test
    void testIsActiveWithActivePunishment() {
        Punishment punishment = createTestPunishment();
        
        assertTrue(punishment.isActive());
    }
    
    @Test
    void testIsActiveWithInactiveFlagFalse() {
        Map<String, Object> inactiveDataMap = new HashMap<>(testDataMap);
        inactiveDataMap.put("active", false);
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, inactiveDataMap
        );
        
        assertFalse(punishment.isActive());
    }
    
    @Test
    void testIsActiveWithExpiredPunishment() {
        Map<String, Object> expiredDataMap = new HashMap<>(testDataMap);
        Date pastDate = new Date(System.currentTimeMillis() - 86400000L); // 1 day ago
        expiredDataMap.put("expires", pastDate);
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, expiredDataMap
        );
        
        assertFalse(punishment.isActive());
    }
    
    @Test
    void testIsActiveWithUnstartedBan() {
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, null, // No start date
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, testDataMap
        );
        
        assertFalse(punishment.isActive());
    }
    
    @Test
    void testIsActiveWithUnstartedMute() {
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, null, // No start date
            Punishment.Type.MUTE, testModifications, testNotes, testTicketIds, testDataMap
        );
        
        assertFalse(punishment.isActive());
    }
    
    @Test
    void testIsActiveWithPermanentPunishment() {
        Map<String, Object> permanentDataMap = new HashMap<>(testDataMap);
        permanentDataMap.remove("expires"); // No expiry = permanent
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, permanentDataMap
        );
        
        assertTrue(punishment.isActive());
    }
    
    @Test
    void testGetDataReturnsStructuredData() {
        Punishment punishment = createTestPunishment();
        PunishmentData data = punishment.getData();
        
        assertNotNull(data);
        // The data should be created from the dataMap
        assertEquals(-1L, data.duration()); // No duration in our test data
        assertFalse(data.altBlocking()); // No altBlocking in our test data
    }
    
    @Test
    void testGetDataWithStructuredFields() {
        Map<String, Object> structuredDataMap = new HashMap<>(testDataMap);
        structuredDataMap.put("blockedName", "BadPlayer");
        structuredDataMap.put("duration", 86400000L);
        structuredDataMap.put("altBlocking", true);
        
        Punishment punishment = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.BAN, testModifications, testNotes, testTicketIds, structuredDataMap
        );
        
        PunishmentData data = punishment.getData();
        
        assertEquals("BadPlayer", data.blockedName());
        assertEquals(86400000L, data.duration());
        assertTrue(data.altBlocking());
    }
    
    @Test
    void testPunishmentTypes() {
        assertEquals(2, Punishment.Type.values().length);
        assertTrue(Arrays.asList(Punishment.Type.values()).contains(Punishment.Type.BAN));
        assertTrue(Arrays.asList(Punishment.Type.values()).contains(Punishment.Type.MUTE));
    }
    
    @Test
    void testPunishmentEquality() {
        Punishment punishment1 = createTestPunishment();
        Punishment punishment2 = new Punishment(
            "PUN-123", "TestModerator", testIssueDate, testStartDate,
            Punishment.Type.MUTE, testModifications, testNotes, testTicketIds, testDataMap
        );
        
        // Test that objects with same data are equal (if equals is implemented)
        assertEquals(punishment1.getId(), punishment2.getId());
        assertEquals(punishment1.getType(), punishment2.getType());
        assertEquals(punishment1.getReason(), punishment2.getReason());
    }
    
    @Test
    void testDataMapImmutability() {
        Punishment punishment = createTestPunishment();
        Map<String, Object> retrievedDataMap = punishment.getDataMap();
        
        // Verify we can read the original data
        assertEquals("Test punishment reason", retrievedDataMap.get("reason"));
        
        // The dataMap should be the same reference (not a copy)
        assertSame(testDataMap, retrievedDataMap);
    }
    
    @Test
    void testLazyDataInitialization() {
        Punishment punishment = createTestPunishment();
        
        // First call should create the PunishmentData
        PunishmentData data1 = punishment.getData();
        // Second call should return the same instance (cached)
        PunishmentData data2 = punishment.getData();
        
        assertSame(data1, data2);
    }
    
    private Punishment createTestPunishment() {
        return new Punishment(
            "PUN-123",
            "TestModerator",
            testIssueDate,
            testStartDate,
            Punishment.Type.MUTE,
            testModifications,
            testNotes,
            testTicketIds,
            testDataMap
        );
    }
}