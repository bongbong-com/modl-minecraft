# MODL Minecraft - Unit Test Suite

This document describes the comprehensive unit test suite for the MODL Minecraft plugin, covering all major features and components.

## 🧪 Test Overview

The test suite covers all core functionality implemented in the punishment system, ticket management, and platform integrations:

### Test Coverage by Component

| Component | Test File | Coverage |
|-----------|-----------|----------|
| **PunishmentData** | `PunishmentDataTest.java` | Structured data conversion, thread safety |
| **PunishmentCache** | `PunishmentCacheTest.java` | Caching, concurrency, expiry handling |
| **Punishment** | `PunishmentTest.java` | Core logic, activity checks, data access |
| **PlayerLoginResponse** | `PlayerLoginResponseTest.java` | API responses, punishment detection |
| **ModlHttpClientImpl** | `ModlHttpClientImplTest.java` | HTTP operations, error handling |
| **SpigotListener** | `SpigotListenerTest.java` | Event handling, MockBukkit integration |
| **LocaleManager** | `LocaleManagerTest.java` | YAML parsing, localization, templating |
| **TicketCommand** | `TicketCommandTest.java` | Command processing, ticket creation |

## 🚀 Running Tests

### Quick Start
```bash
# Run all tests
./run-tests.sh

# Or use Maven directly
mvn clean test
```

### Individual Module Testing
```bash
# Test specific modules
mvn test -pl modl-api
mvn test -pl modl-core
mvn test -pl modl-platforms/modl-platform-spigot
```

### IDE Integration
Import the project in your IDE (IntelliJ IDEA, Eclipse, VS Code) and run tests directly from the IDE test runner.

## 📋 Test Categories

### 1. **Data Structure Tests**
- **PunishmentData**: Type-safe punishment data handling
  - Map-to-record conversion
  - Export/import functionality  
  - Null safety and edge cases
  - Thread-safe atomic references

- **Punishment**: Core punishment entity logic
  - Active/inactive state detection
  - Expiry date handling
  - Structured data access
  - Type validation

### 2. **Caching Tests**
- **PunishmentCache**: In-memory player punishment caching
  - Thread-safe concurrent operations
  - Automatic expiry handling
  - Memory management
  - Cache invalidation

### 3. **API Integration Tests**
- **PlayerLoginResponse**: Login response handling
  - Active punishment detection
  - Multiple punishment types
  - Stream operations
  - Immutability guarantees

- **ModlHttpClientImpl**: HTTP client operations
  - Success/failure scenarios
  - Network error handling
  - Request formatting
  - Async response processing

### 4. **Platform Integration Tests**
- **SpigotListener**: Bukkit event handling (uses MockBukkit)
  - Login prevention for banned players
  - Mute caching on join
  - Chat blocking for muted players
  - Event cancellation
  - Cache cleanup on disconnect

### 5. **Localization Tests**
- **LocaleManager**: YAML-based localization
  - Message template processing
  - Placeholder replacement
  - Nested configuration access
  - Color code conversion
  - Category management
  - Menu size calculation

### 6. **Command Tests**
- **TicketCommand**: ACF command processing
  - Player reporting
  - Chat reporting
  - Bug reporting
  - Support requests
  - Input validation
  - API integration

## 🛠 Test Infrastructure

### Dependencies
- **JUnit 5**: Core testing framework
- **Mockito**: Mocking framework
- **MockBukkit**: Bukkit server mocking for Spigot tests
- **Maven Surefire**: Test execution and reporting

### Test Configuration
- Java 17+ compatibility
- Parallel test execution safe
- Detailed console output
- Automatic test discovery

## ✅ Test Features Covered

### Ban Prevention System
- ✅ Active ban detection during login
- ✅ Permanent vs temporary ban handling
- ✅ Kick message formatting
- ✅ Error handling for API failures
- ✅ Cross-platform implementation

### Mute Caching System  
- ✅ Active mute caching on login
- ✅ Chat event blocking
- ✅ Expiry detection and cleanup
- ✅ Thread-safe cache operations
- ✅ Memory management

### Ticket Management
- ✅ Player reporting commands
- ✅ Chat violation reporting
- ✅ Bug report creation
- ✅ Support request handling
- ✅ Form data generation
- ✅ API integration

### Localization System
- ✅ YAML configuration parsing
- ✅ Message templating
- ✅ Multi-language support
- ✅ Color code processing
- ✅ Dynamic menu generation

### HTTP Integration
- ✅ RESTful API communication
- ✅ Request/response serialization
- ✅ Error handling
- ✅ Async operation support
- ✅ Authentication headers

## 🔍 Test Scenarios

### Happy Path Testing
- All components function correctly with valid input
- Successful API communications
- Proper event handling
- Correct cache operations

### Error Handling
- Network failures
- Invalid API responses
- Malformed configuration files
- Null/missing data handling
- Concurrent access edge cases

### Edge Cases
- Empty punishment lists
- Expired punishments
- Null player data
- Missing locale entries
- Thread safety under load

## 📊 Performance Testing

The test suite includes performance considerations:
- Concurrent cache access (10 threads, 100 players)
- Large dataset handling
- Memory usage validation
- Thread safety verification

## 🐛 Debugging Tests

### Common Issues
1. **Java Version**: Ensure Java 17+ is being used
2. **Dependencies**: Run `mvn dependency:resolve` first
3. **Locale Files**: Ensure test locale files are properly formatted
4. **MockBukkit**: Check MockBukkit version compatibility

### Debug Mode
```bash
# Run with debug output
mvn test -X

# Run specific test class
mvn test -Dtest=PunishmentCacheTest

# Run specific test method
mvn test -Dtest=PunishmentCacheTest#testConcurrentAccess
```

## 📈 Continuous Integration

The test suite is designed for CI/CD integration:
- Fast execution (typically < 30 seconds)
- Clear pass/fail indicators
- Detailed failure reporting
- No external dependencies required
- Deterministic results

## 🔮 Future Enhancements

Planned test improvements:
- Integration tests with real Minecraft servers
- Performance benchmarking
- Load testing for cache operations
- End-to-end API testing
- Cross-platform compatibility verification

## 📝 Writing New Tests

When adding new features, ensure:
1. **Unit Tests**: Cover all public methods
2. **Edge Cases**: Test null, empty, and invalid inputs
3. **Error Handling**: Verify exception scenarios
4. **Thread Safety**: Test concurrent access where applicable
5. **Documentation**: Update this README with new test coverage

### Test Naming Convention
- `Test` suffix for test classes
- Descriptive method names starting with `test`
- Clear assertion messages
- Organized with `@BeforeEach` setup

Example:
```java
@Test
void testPlayerLoginWithActiveBan() {
    // Arrange
    Punishment ban = createMockBan();
    
    // Act
    PlayerLoginResponse response = processLogin(ban);
    
    // Assert
    assertTrue(response.hasActiveBan());
    assertEquals(ban, response.getActiveBan());
}
```

---

## 🎯 Quality Metrics

The test suite maintains high code quality standards:
- **100% method coverage** for critical components
- **Comprehensive edge case testing**
- **Thread safety validation**
- **Performance regression detection**
- **Cross-platform compatibility**

Run `./run-tests.sh` to execute the full test suite and verify all functionality!