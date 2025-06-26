# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-platform Minecraft moderation plugin called "modl" that provides moderation and support systems across different Minecraft server platforms (Spigot, BungeeCord, Velocity). The project is structured as a Maven multi-module build with cross-platform compatibility.

## Build Commands

- **Build all modules**: `mvn clean package`
- **Build specific module**: `mvn clean package -pl modl-core` (or other module name)
- **Install to local repository**: `mvn clean install`
- **Skip tests**: Add `-DskipTests` to any Maven command
- **Compile only**: `mvn compile`

## Project Structure

The codebase follows a multi-module Maven architecture:

- **modl-api**: Core API interfaces and data models
- **modl-core**: Shared business logic and command implementations
- **modl-platforms**: Platform-specific implementations
  - **modl-platform-spigot**: Spigot/Paper server integration
  - **modl-platform-bungee**: BungeeCord proxy integration  
  - **modl-platform-velocity**: Velocity proxy integration
- **modl-distribution**: Distribution packaging

## Key Architecture Patterns

### Platform Abstraction
The `Platform` interface (modl-core/src/main/java/com/bongbong/modl/minecraft/core/Platform.java) provides cross-platform compatibility by abstracting platform-specific functionality like command management and HTTP clients.

### HTTP Client Architecture
The `ModlHttpClient` interface (modl-api) defines async HTTP operations for player profiles, tickets, punishments, and notes. Platform implementations provide concrete HTTP clients.

### Command Framework
Uses Aikar's Command Framework (ACF) for command handling across all platforms. Commands are defined in modl-core and registered through platform-specific command registers.

## Development Environment

- **Java Version**: 17+ (configured for Java 18 in properties, compiled to Java 17)
- **Maven**: Standard Maven project structure
- **Lombok**: Used throughout for boilerplate reduction
- **Dependencies**: 
  - Protocolize for protocol manipulation
  - Cirrus for GUI/inventory management
  - ACF for command framework
  - Gson for JSON processing

## Key Dependencies

- Platform servers must have Protocolize installed (BungeeCord dependency)
- Supports Folia (spigot.yml: folia-supported: true)
- API version 1.13+ for Spigot platforms

## Plugin Configuration

Each platform has its plugin descriptor with Maven property filtering:
- Plugin name, version, description inherited from root pom.xml
- Authors: tigerbong, byteful
- Main classes follow pattern: com.bongbong.{platform}.{Platform}Plugin

## Testing

The project structure suggests testing should be done per module. Use `mvn test` for running tests, though specific test frameworks are not yet configured in the visible pom files.

## Ticket System Implementation

### Commands Available
- **`/report <player> [reason]`** - Opens a GUI with categorized report options (chat, username, skin, content, team griefing, game rules, cheating)
- **`/chatreport <player> [reason]`** - Creates finished chat reports with automatic chat log capture
- **`/bugreport <title>`** - Creates unfinished bug reports with web form completion links  
- **`/support <title>`** - Creates unfinished support requests with web form completion links

### GUI System
- Uses Cirrus framework (`dev.simplix.cirrus`) for cross-platform menu support
- Report GUI similar to HammerV2 with categorized options
- Integrated with modl-panel API for ticket creation

### API Integration
- **Finished tickets**: POST `/api/public/tickets` (requires X-Ticket-API-Key)
- **Unfinished tickets**: POST `/api/public/tickets/unfinished` (no auth required)
- Returns ticket IDs and completion URLs to players
- Supports both immediate submission and web form completion workflows

### Localization System
- **Locale files**: Located in `modl-core/src/main/resources/locale/`
- **Default locale**: `en_US.yml` with full message customization
- **Customizable elements**: 
  - All command messages and responses
  - Report GUI items, names, lore, and categories
  - Ticket priorities, tags, and form data
  - Error messages and success notifications
- **Placeholders**: Support for dynamic content like `{player}`, `{ticketId}`, `{url}`
- **Color codes**: Use `&` format (e.g., `&c` for red, `&a` for green)
- **LocaleManager**: Handles YAML loading, placeholder replacement, and color conversion