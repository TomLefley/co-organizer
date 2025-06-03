# Co-Organizer

A Burp Suite extension that enables secure, group-based sharing of HTTP request/response items between security researchers and penetration testers with optional end-to-end encryption.

[![Build Status](https://github.com/TomLefley/co-organizer/actions/workflows/ci.yml/badge.svg)](https://github.com/TomLefley/co-organizer/actions/workflows/ci.yml)
[![CodeQL](https://github.com/TomLefley/co-organizer/actions/workflows/codeql.yml/badge.svg)](https://github.com/TomLefley/co-organizer/actions/workflows/codeql.yml)
[![Dependency Review](https://github.com/TomLefley/co-organizer/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/TomLefley/co-organizer/actions/workflows/dependency-review.yml)
[![Java Version](https://img.shields.io/badge/java-21-blue)](build.gradle.kts)
[![License](https://img.shields.io/badge/license-GPL--3-blue)](LICENSE)

## 🎯 Why Co-Organizer?

**Stop copy-pasting HTTP requests and responses!** Co-Organizer streamlines collaboration between security professionals by enabling secure, encrypted sharing of Burp Suite items with group-based access control.

Perfect for:
- **👥 Team Collaboration**: Share findings securely within your team only
- **🎓 Training & Education**: Create secure groups for students and instructors  
- **🔍 Bug Bounty**: Collaborate on complex vulnerabilities with trusted researchers
- **📋 Client Projects**: Share findings within project teams with encryption
- **🔄 Cross-Testing**: Organize testing groups with automatic access control
- **🏢 Enterprise Security**: Multiple team isolation with cryptographic security

## ✨ Key Features

### 🚀 Flexible Sharing Options
- **Share items with public links** that anyone can import
- **Share items with private links** that only specific groups can import
- **Automatic link generation** and clipboard copy

### 👥 Group Management
- **Create collaboration groups** with simple names
- **Generate secure invite codes** for team members
- **Join groups instantly** by pasting invite codes
- **Leave groups** when projects end

### 🔐 Encryption
- **AES-256-GCM encryption** for group-shared items
- **Unique keys per group** with cryptographic isolation
- **Random IV generation** prevents pattern analysis
- **Automatic key management** - no manual key handling

### 📥 Intelligent Import
- **Paste shared links** in proxied browser
- **Automatic decryption** for authorized groups
- **Items automatically appear** in Burp's Organizer

## 📋 Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Group Management](#group-management)
- [Data Security](#data-security)
- [Data Privacy](#data-privacy)
- [Contributing](#contributing)

## 🚀 Installation

### Option 1: BApp Store (Recommended)

1. In Burp Suite: **Extensions > BApp Store**
2. Search for "Co-Organizer"
3. Click **Install** and the extension loads automatically ✅

### Option 2: Download Release

1. Download `co-organizer.jar` from [Releases](../../releases)
2. In Burp Suite: **Extensions > Installed > Add**
3. Select the JAR file and click **Next**

### Option 3: Build from Source

```bash
git clone https://github.com/TomLefley/co-organizer.git
cd co-organizer
./gradlew jar
# Load: build/libs/extension-template-project.jar
```

## ⚡ Quick Start

### 1. Share an HTTP Item

1. **Select one or more request/responses** in HTTP history
2. **Right-click** → **Extensions** → **Co-Organizer** → "Share" (or "Share with..." → [Group Name])
3. **Link copied** to clipboard automatically
4. **Send link** to colleague

### 2. Import Shared Item

1. **Paste link** in proxied browser
2. **Items appear** in Burp's Organizer tab
3. **Ready to analyze** immediately

## 👥 Group Management

### Creating Groups
1. **Co-Organizer Groups tab** → **Toolbar (top right)** → "Create a new collaboration group"
2. **Enter group name**
3. **Generate invite code** to share with team members

### Joining Groups
1. **Obtain invite code** from group creator
2. **Co-Organizer Groups tab** → **Toolbar (top right)** → "Join an existing group using an invite code"
3. **Paste full invite or base64 invite code** → Group appears in table

### Leaving Groups
1. **Co-Organizer Groups tab** → **Select group in table**
2. **Toolbar** → "Leave selected group"

### Sharing with Groups
- **Right-click HTTP item** → **Extensions** → **Co-Organizer** → **Share with...** → **[Group Name]**
- Items shared with groups are **end-to-end encrypted**
- Only group members can decrypt and view content

## 🔒 Data Security

### Encryption
- **All items shared with groups** are end-to-end encrypted using AES-256-GCM
- **Unique encryption keys per group** with cryptographic isolation
- **All items publicly shared** are encrypted at rest when using the PortSwigger Co-Organizer server

### Data Retention
- **PortSwigger Co-Organizer server** retains data for a maximum of **7 days**
- **Automatic deletion** after retention period
- **No permanent storage** of shared items

## 🔐 Data Privacy

### Using Custom Servers
For enhanced privacy, you can host your own sharing server:

1. **Fork this repository**
2. **Edit `ServerConfiguration.java`** to point to your server:
   ```java
   public static final String HOST = "your-server.com";
   public static final int PORT = 8080;
   ```
3. **Use the included OpenAPI specification** ([`openapi.yaml`](openapi.yaml)) to implement your server
4. **Rebuild the extension**: `./gradlew jar`

**Note**: The PortSwigger Co-Organizer server decorates the import endpoint flow to make it more aesthetically pleasing, but the OpenAPI definition outlines the basic requirements for compatibility.

### Debug ID Privacy
Co-Organizer includes a debug ID header for troubleshooting:

- **Automatic Generation**: A unique UUID is generated on first startup
- **Optional Transmission**: Sent as `X-Debug-Id` header only in sharing requests
- **Privacy Control**: Users can disable by setting the `co-organizer.debug-id` preference to empty
- **No Regeneration**: Once cleared, stays cleared until manually regenerated

## 🤝 Contributing

We welcome contributions to Co-Organizer! 

- **🐛 Report bugs**: [Open an issue](../../issues/new?template=bug_report.md)
- **✨ Request features**: [Open an issue](../../issues/new?template=feature_request.md)
- **📖 Improve documentation**: Submit pull requests
- **🧪 Add tests**: Help expand test coverage

---

## 📄 License

GPL-3 License - see [LICENSE](LICENSE) for details.

---

**🚀 Ready to streamline your security testing workflow? Install Co-Organizer today!**