# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Burp Suite extension template project built with Java and Gradle. The project is designed to create extensions for Burp Suite using the Montoya API, which provides access to all Burp Suite features for extension development.

## Key Build Commands

- **Build JAR file**: `./gradlew jar` (Unix) or `gradlew jar` (Windows)
- **Clean build**: `./gradlew clean`
- **Build output location**: `build/libs/extension-template-project.jar`

## Architecture

- **Entry point**: `src/main/java/Extension.java` - implements `BurpExtension` interface
- **Main method**: `initialize(MontoyaApi montoyaApi)` - called when extension loads
- **Target platform**: Burp Suite Professional/Community
- **Java version**: JDK 21 (source and target compatibility)
- **Dependency**: Montoya API 2025.5 (compile-only)

## Development Workflow

1. Modify `Extension.java` with your extension logic
2. Build with `./gradlew jar`
3. Load JAR into Burp Suite via Extensions > Installed > Add
4. For code changes: rebuild JAR and reload extension in Burp (Ctrl/⌘ + click "Loaded" checkbox)

## Project Configuration

- Project name configurable in `settings.gradle.kts`
- Gradle build configured for fat JAR with all dependencies included
- Uses UTF-8 encoding