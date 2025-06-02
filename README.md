# Co-Organizer

A Burp Suite extension that enables secure, group-based sharing of HTTP request/response items between security researchers and penetration testers with end-to-end encryption.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](build.gradle.kts)
[![Java Version](https://img.shields.io/badge/java-21-blue)](build.gradle.kts)
[![License](https://img.shields.io/badge/license-GPL--3-blue)](LICENSE)

## 🎯 Why Co-Organizer?

**Stop copy-pasting HTTP requests and responses!** Co-Organizer revolutionizes collaboration between security professionals by enabling secure, encrypted sharing of Burp Suite items with group-based access control.

Perfect for:
- **👥 Team Collaboration**: Share findings securely within your team only
- **🎓 Training & Education**: Create secure groups for students and instructors  
- **🔍 Bug Bounty**: Collaborate on complex vulnerabilities with trusted researchers
- **📋 Client Projects**: Share findings within project teams with encryption
- **🔄 Cross-Testing**: Organize testing groups with automatic access control
- **🏢 Enterprise Security**: Multiple team isolation with cryptographic security

## ✨ Key Features

### 🚀 Flexible Sharing Options
- **Right-click any HTTP item** → "Share" for unencrypted sharing
- **"Share with..."** submenu → Select specific groups for encrypted sharing
- **Automatic link generation** and clipboard copy
- **No manual export/import** required

### 👥 Group Management
- **Create collaboration groups** with simple names
- **Generate secure invite codes** for team members
- **Join groups instantly** by pasting invite codes
- **Leave groups** when projects end
- **Automatic access control** - no access after leaving

### 🔐 Military-Grade Encryption
- **AES-256-GCM encryption** for group-shared items
- **Unique keys per group** with cryptographic isolation
- **Random IV generation** prevents pattern analysis  
- **Authentication tags** detect tampering attempts
- **Automatic key management** - no manual key handling

### 📥 Intelligent Import  
- **Paste shared links** in browser
- **Automatic decryption** for authorized groups
- **401 Unauthorized** for groups you're not in
- **Items automatically appear** in Burp's Organizer
- **Mixed encrypted/unencrypted** support

### 🎨 Seamless Integration
- **Native Burp UI** integration with tabs and menus
- **Smart toast notifications** with success/error icons
- **Background processing** keeps Burp responsive
- **Group list in sharing order** for easy selection

## 📋 Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Group Management](#group-management)
- [Sharing & Encryption](#sharing--encryption)
- [Detailed Usage](#detailed-usage)
- [Server Setup](#server-setup)
- [API Specification](#api-specification)
- [Security Features](#security-features)
- [Privacy & Debug ID](#privacy--debug-id)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Support](#support)

## 🚀 Installation

### Option 1: Download Release (Recommended)

1. Download `co-organizer.jar` from [Releases](../../releases)
2. In Burp Suite: **Extensions > Installed > Add**
3. Select the JAR file and click **Next**
4. Extension loads automatically ✅

### Option 2: Build from Source

```bash
git clone https://github.com/TomLefley/co-organizer.git
cd co-organizer
./gradlew jar
# Load: build/libs/extension-template-project.jar
```

### Requirements
- **Burp Suite** Professional or Community (2023.1+)
- **Java 21+** (already included with modern Burp)

## ⚡ Quick Start

### 1. Share an HTTP Item

![Share Demo](docs/share-demo.gif)

1. **Select** request/response in HTTP history
2. **Right-click** → "Share item" (or "Share selected items")  
3. **Link copied** to clipboard automatically
4. **Send link** to colleague

### 2. Import Shared Item

![Import Demo](docs/import-demo.gif)

1. **Paste link** in browser
2. **Items appear** in Burp's Organizer tab
3. **Ready to analyze** immediately

That's it! No configuration needed.

## 📖 Detailed Usage

### Sharing Multiple Items

Select multiple requests/responses (Ctrl/Cmd+click) and use "Share selected items":

```
✅ Single item    → "Share item"
✅ Multiple items → "Share selected items"
✅ Mixed content  → All preserved
✅ Large payloads → Handled efficiently
```

### What Gets Shared

**Complete HTTP data:**
- 📝 Request method, URL, headers, body
- 📊 Response status, headers, body  
- 🔗 Exact timing and metadata
- 🎯 All parameter values and cookies

**Data integrity:**
- ✅ Binary data preserved
- ✅ Unicode characters supported
- ✅ Special headers maintained
- ✅ Authentication tokens included

### Organizing Imported Items

Imported items appear in **Organizer > Co-Organizer** with:
- 📅 Import timestamp
- 👤 Source information (if available)
- 🏷️ Automatic categorization
- 🔍 Full search capability

## 🖥️ Server Setup

Co-Organizer requires a local sharing server. You can use any server that:

- **Accepts POST** requests to `/share`
- **Returns JSON** with `{"url": "download-link"}`
- **Serves downloads** with Base64-encoded data

### 📋 API Specification

See [`openapi.yaml`](openapi.yaml) for the complete API specification. This documents:
- Exact request/response formats
- Required headers and status codes  
- Data schemas for encrypted vs unencrypted sharing
- Example requests and responses

Use the OpenAPI spec to generate server stubs in your preferred language or validate your implementation.

### 🔧 Custom Server Address

By default, Co-Organizer connects to `http://localhost:3000`. To use a different server:

1. **Fork the repository**
2. **Edit `ServerConfiguration.java`**:
   ```java
   public static final String HOST = "your-server.com";  // Change this
   public static final int PORT = 8080;                  // Change this
   ```
3. **Rebuild the extension**: `./gradlew jar`

This single change updates all server connections throughout the extension.

### Example Server URLs
```
✅ http://localhost:3000/abc123/import
✅ http://localhost:3000/items/xyz789/import  
✅ http://localhost:3000/share/def456/import
❌ http://example.com:3000/import (wrong host)
❌ http://localhost:8080/import (wrong port)
❌ http://localhost:3000/upload (wrong endpoint)
```

### Quick Server Setup

**Node.js Example:**
```javascript
const express = require('express');
const app = express();

app.post('/share', (req, res) => {
  const id = generateId();
  storeData(id, req.body);
  res.json({ url: `http://localhost:3000/${id}/import` });
});

app.get('/:id/import', (req, res) => {
  const data = getData(req.params.id);
  res.send(data);
});

app.listen(3000);
```

## 🛠️ Troubleshooting

### Common Issues

**❌ "Share failed: No response from server"**
- ✅ Check server is running on `localhost:3000`
- ✅ Verify `/share` endpoint accepts POST requests
- ✅ Check firewall settings

**❌ Items not importing automatically**
- ✅ Ensure Burp proxy is active
- ✅ Check URL format: `localhost:3000/*/import`
- ✅ Verify response contains Base64 data

**❌ "Failed to decode base64 response"**
- ✅ Server should return raw Base64 (not JSON-wrapped)
- ✅ Check server encoding matches extension expectation

### Debug Information

Enable detailed logging in **Extensions > Co-Organizer > Output**:
```
✅ "Response matches shared item import pattern"
✅ "Successfully decoded base64 response"  
✅ "Successfully sent X items to organizer"
```

### Getting Help

1. **Check logs** in Extensions tab
2. **Try minimal test** with single request
3. **Verify server** with curl/Postman
4. **Report issues** with logs and steps to reproduce

## 🔧 Advanced Features

### Batch Operations
- Share up to **100 items** simultaneously
- **Automatic compression** for large datasets
- **Progress indicators** for bulk operations

### Security Features
- **Local-only** sharing by default
- **No external dependencies** 
- **Data validation** on import
- **Safe encoding** prevents injection
- **Privacy-respecting debug ID** for optional troubleshooting

### Performance
- **Background processing** keeps Burp responsive
- **Streaming uploads** for large items
- **Efficient serialization** minimizes memory usage
- **Smart caching** reduces server load

## 🔒 Privacy & Debug ID

Co-Organizer includes a **privacy-respecting debug system** to help with troubleshooting while protecting user privacy.

### How Debug ID Works

- **Automatic Generation**: A unique UUID is generated on first startup
- **Optional Transmission**: Sent as `X-Debug-Id` header only in sharing requests
- **Privacy Control**: Users can disable by clearing the ID
- **No Regeneration**: Once cleared, stays cleared until manually regenerated

### Debug ID States

```
🆔 Generated    → UUID sent in requests (default)
🚫 Cleared      → No header sent (privacy mode)
🔄 Regenerated  → New UUID generated and sent
```

### Privacy Options

**For privacy-conscious users:**

1. **View current debug ID**: Check Burp extension logs on startup
2. **Disable debug ID**: Clear the `co-organizer.debug-id` preference
3. **Keep disabled**: Once cleared, it won't regenerate automatically

**Technical details:**
- Stored in Burp's preference system as `co-organizer.debug-id`
- Empty string = intentionally disabled (respects user choice)
- `null` = not set yet (will generate new UUID)
- Valid UUID = enabled and will be sent in requests

### Server Implementation

Your sharing server will receive the debug ID (if enabled) as:
```http
X-Debug-Id: 12345678-1234-1234-1234-123456789012
```

Use this for:
- Troubleshooting user-specific issues
- Correlating related requests
- Usage analytics (if permitted)

⚠️ **Privacy Note**: The debug ID is purely for troubleshooting. Users who clear it expect complete anonymity.

## 🏗️ Extension Architecture

Co-Organizer uses a **clean, modular design** following SOLID principles:

```
📦 Services     → Sharing, notifications
🎯 Handlers     → Proxy response processing  
🔍 Matchers     → URL pattern recognition
📊 Serializers  → Data encoding/decoding
🎨 UI           → Context menus, notifications
📋 Models       → Data structures
```

This architecture ensures:
- **Reliable** operation under load
- **Maintainable** codebase for contributors
- **Extensible** for future features
- **Testable** with comprehensive test suite

## 🛠️ Development

### Quick Development Setup
```bash
git clone https://github.com/TomLefley/co-organizer.git
cd co-organizer
./gradlew test  # Run tests
./gradlew jar   # Build extension
```

### Custom Server Configuration

To develop with a different server address:

1. **Edit server configuration** in `src/main/java/dev/lefley/coorganizer/config/ServerConfiguration.java`
2. **Update constants** as needed:
   - `HOST` - Server hostname or IP
   - `PORT` - Server port number
   - `SHARE_ENDPOINT` - Upload endpoint path
   - `IMPORT_ENDPOINT_SUFFIX` - Download endpoint suffix
3. **Rebuild and test**: `./gradlew clean test jar`

### Contributing
We welcome contributions! See [Contributing Guidelines](CONTRIBUTING.md) for:
- 🐛 Bug reports and fixes
- ✨ Feature requests and implementations  
- 📖 Documentation improvements
- 🧪 Test coverage enhancements

## 📞 Support & Community

### Getting Support
- 📋 **Issues**: [GitHub Issues](../../issues) for bugs and features
- 📖 **Documentation**: [CLAUDE.md](CLAUDE.md) for technical details
- 💬 **Community**: Security research forums and Discord

### Feedback & Suggestions
We'd love to hear how you're using Co-Organizer:
- ⭐ **Star the repo** if you find it useful
- 🐛 **Report bugs** to help improve reliability  
- 💡 **Suggest features** for future versions
- 📝 **Share use cases** to inspire others

---

## 📄 License

GPL-3 License - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- **PortSwigger** for the excellent Burp Suite platform
- **Security research community** for inspiring collaborative tools
- **Contributors** who help make Co-Organizer better

---

**🚀 Ready to streamline your security testing workflow? Install Co-Organizer today!**