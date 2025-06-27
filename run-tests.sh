#!/bin/bash

# MODL Minecraft Test Runner
# Runs all unit tests across the project

echo "🧪 Running MODL Minecraft Unit Tests"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

print_status "Starting test execution..."

# Run tests with Maven
print_status "Executing Maven test phase..."

# Use the correct Java version if specified
if [ ! -z "$JAVA_HOME" ]; then
    print_status "Using Java from JAVA_HOME: $JAVA_HOME"
fi

# Run tests with detailed output
mvn clean test \
    -Dmaven.compiler.source=17 \
    -Dmaven.compiler.target=17 \
    -Dsurefire.printSummary=true \
    -Dsurefire.reportFormat=plain \
    -Dsurefire.useFile=false

TEST_EXIT_CODE=$?

echo ""
echo "=================================="

if [ $TEST_EXIT_CODE -eq 0 ]; then
    print_success "All tests passed! ✅"
    echo ""
    print_status "Test Coverage Summary:"
    echo "• PunishmentData: Unit tests for structured punishment data"
    echo "• PunishmentCache: Thread-safe caching tests"
    echo "• Punishment: Core punishment logic tests"
    echo "• PlayerLoginResponse: API response handling tests"
    echo "• ModlHttpClientImpl: HTTP client integration tests"
    echo "• SpigotListener: Platform-specific event handling tests (MockBukkit)"
    echo "• LocaleManager: Localization and YAML parsing tests"
    echo "• TicketCommand: Command processing and API integration tests"
    echo ""
    print_status "Test Features Covered:"
    echo "• Ban prevention during login"
    echo "• Mute caching and chat prevention"
    echo "• Ticket creation and management"
    echo "• Localization and message formatting"
    echo "• HTTP API integration"
    echo "• Platform-specific implementations"
    echo "• Error handling and edge cases"
    echo "• Thread safety and concurrency"
else
    print_error "Some tests failed! ❌"
    echo ""
    print_warning "Check the output above for details on failed tests."
    print_status "Common issues:"
    echo "• Missing test dependencies (check pom.xml)"
    echo "• Java version compatibility (requires Java 17+)"
    echo "• Network issues for HTTP client tests"
    echo "• Missing locale files for LocaleManager tests"
fi

echo ""
print_status "Test execution completed with exit code: $TEST_EXIT_CODE"

exit $TEST_EXIT_CODE