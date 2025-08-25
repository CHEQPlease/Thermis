# Changelog

All notable changes to the Thermis Flutter plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0] - 2024-01-XX

### 🎉 Major Features Added

#### Advanced Queue Management System
- **Per-Device Queues**: Separate queues for each printer device enabling true parallel processing
- **Queue Monitoring**: Real-time queue size tracking for all devices
- **Queue Control**: Clear all queues or specific device queues
- **Load Balancing**: Distribute print jobs across multiple printers automatically

#### Enhanced Error Handling & Retry Logic
- **Detailed Error Classification**: 10 specific error types with retryable/non-retryable classification
- **Automatic Retry**: Exponential backoff retry logic for transient errors
- **Smart Error Recovery**: Automatic handling of printer busy, network timeouts, and communication errors
- **User-Friendly Messages**: Descriptive error messages for better UX

#### Comprehensive Device Discovery
- **Flexible Discovery Methods**: Both stream-based and list-based device discovery
- **Configurable Duration**: Customizable discovery timeout (5-50 seconds)
- **Device Information**: Complete device details including name, MAC, and IP
- **Discovery Control**: Start, stop, and manage discovery processes

#### Modern API Design
- **PrintResult Class**: Detailed operation results with success status, error details, and retry information
- **PrinterConfig Flexibility**: Optional configuration with sensible defaults
- **Type Safety**: Comprehensive enum-based error handling and printer types
- **Future-Based APIs**: All operations return proper Future types for async handling

### 🔧 Technical Improvements

#### Kotlin Backend Enhancements
- **Coroutines Integration**: Full async/await support with Kotlin coroutines
- **Thread Safety**: Concurrent queue management with thread-safe operations
- **Memory Management**: Efficient resource handling and cleanup
- **Error Classification**: Detailed exception mapping to specific error types

#### Flutter Frontend Improvements
- **Null Safety**: Complete null safety compliance
- **Stream Support**: Reactive device discovery with Stream APIs
- **Error Propagation**: Detailed error information propagated to Flutter layer
- **Configuration Validation**: Input validation and error handling

### 📊 Quality & Testing

#### Comprehensive Test Suite
- **94 Test Cases**: Complete coverage of all functionality
- **Mock Platform**: Sophisticated mock implementation for testing
- **Error Scenarios**: All error conditions and edge cases covered
- **Integration Tests**: End-to-end workflow validation
- **CI/CD Ready**: Automated test execution support

#### Documentation Overhaul
- **Complete API Reference**: Detailed documentation for all public APIs
- **Usage Examples**: Real-world examples and integration patterns
- **Error Handling Guide**: Comprehensive error handling documentation
- **Migration Guide**: Easy upgrade path from previous versions

### 🚀 Performance Optimizations

#### Queue Processing
- **Parallel Execution**: Different printers work simultaneously
- **Sequential Safety**: Same printer processes jobs in order
- **Resource Optimization**: Efficient memory and CPU usage
- **Background Processing**: Non-blocking queue operations

#### Network Operations
- **Connection Pooling**: Efficient network resource management
- **Timeout Handling**: Configurable timeouts with proper cleanup
- **Retry Optimization**: Smart retry delays to avoid overwhelming devices
- **Discovery Efficiency**: Optimized device discovery algorithms

### 🛡️ Reliability Enhancements

#### Error Recovery
- **Automatic Retry**: Built-in retry logic for transient failures
- **Graceful Degradation**: Proper handling of device unavailability
- **State Recovery**: Maintain operation state across app restarts
- **Resource Cleanup**: Proper cleanup of failed operations

#### Device Management
- **Connection Monitoring**: Real-time device connection status
- **Multi-Device Support**: Handle multiple printers simultaneously
- **Device Failover**: Automatic switching between available devices
- **Configuration Persistence**: Remember device configurations

### 🔄 API Changes

#### New Methods
```dart
// Enhanced printing with detailed results
Future<PrintResult?> printReceipt(String receiptJson, {PrinterConfig? config})

// Flexible device discovery
Stream<Device> discoverPrinters({int scanDurationMs = 5000})
Future<List<Device>> getAvailableDevices({int durationMs = 50000})

// Queue management
Future<int?> getQueueSize()
Future<Map<String, int>?> getDeviceQueueSizes()
Future<bool?> clearPrintQueue()
Future<bool?> clearDeviceQueue(String deviceKey)
```

#### Updated Methods
```dart
// All printer operations now accept optional PrinterConfig
Future<bool?> openCashDrawer({PrinterConfig? config})
Future<bool?> cutPaper({PrinterConfig? config})
Future<bool?> checkPrinterConnection({PrinterConfig? config})
```

#### New Data Models
```dart
// Detailed print operation results
class PrintResult {
  final bool success;
  final PrintFailureReason? reason;
  final bool retryable;
  final String? message;
}

// Comprehensive error classification
enum PrintFailureReason {
  printerBusy, deviceInUse, networkError, communicationError, timeoutError,
  printerOffline, printerNotFound, outOfPaper, coverOpen, unknownError
}

// Enhanced device information
class Device {
  final String deviceName;
  final String mac;
  final String ip;
}
```

### 📱 Platform Support

#### Android Enhancements
- **Minimum SDK**: Android API 21+ (Android 5.0+)
- **Target SDK**: Latest Android API
- **USB Printer Support**: Enhanced ESC/POS command support
- **Network Printer Support**: Star Micronics StarIO10 SDK integration
- **Permission Handling**: Improved USB permission management

#### iOS Status
- **Current Status**: Not supported (thermal printing limitations)
- **Future Plans**: Investigating iOS thermal printing solutions

### 🔧 Developer Experience

#### Development Tools
- **Hot Reload Support**: Full Flutter hot reload compatibility
- **Debug Logging**: Comprehensive logging for troubleshooting
- **Error Messages**: Clear, actionable error messages
- **Type Hints**: Complete IDE autocomplete support

#### Integration Support
- **Plugin Architecture**: Easy integration with existing Flutter apps
- **Configuration Options**: Flexible configuration for different use cases
- **Event Handling**: Reactive programming support with Streams
- **Async/Await**: Modern async programming patterns

### 🐛 Bug Fixes

#### Queue Management
- **Fixed**: Queue overflow issues with high-volume printing
- **Fixed**: Memory leaks in long-running queue operations
- **Fixed**: Race conditions in concurrent queue access
- **Fixed**: Improper queue cleanup on app termination

#### Device Discovery
- **Fixed**: Discovery timeout issues on slow networks
- **Fixed**: Duplicate device entries in discovery results
- **Fixed**: Memory leaks in discovery stream operations
- **Fixed**: Discovery not stopping properly on app background

#### Print Operations
- **Fixed**: USB permission handling on Android 10+
- **Fixed**: Network printer connection timeouts
- **Fixed**: Improper error handling in print operations
- **Fixed**: Resource cleanup after failed print jobs

#### General Stability
- **Fixed**: App crashes on rapid consecutive print calls
- **Fixed**: Memory leaks in bitmap generation
- **Fixed**: Thread safety issues in multi-device scenarios
- **Fixed**: Improper exception handling in edge cases

### 🔄 Migration Guide

#### From v1.5.x to v1.6.0

**Breaking Changes:**
- `printCHEQReceipt()` → `printReceipt()` (method renamed)
- Return type changed from `Future<void>` to `Future<PrintResult?>`
- Configuration now passed per method call instead of global init

**Migration Steps:**

1. **Update method calls:**
```dart
// Old
await Thermis.init(config);
await Thermis.printCHEQReceipt(receiptJson);

// New
await Thermis.printReceipt(receiptJson, config: config);
```

2. **Handle detailed results:**
```dart
// Old
await Thermis.printCHEQReceipt(receiptJson);

// New
final result = await Thermis.printReceipt(receiptJson);
if (result?.success == true) {
  // Handle success
} else {
  // Handle error with detailed information
  print('Error: ${result?.reason?.displayName}');
}
```

3. **Update discovery calls:**
```dart
// Old
await Thermis.discoverPrintersWithDuration(10000);

// New
await Thermis.discoverPrinters(scanDurationMs: 10000);
// or
final devices = await Thermis.getAvailableDevices(durationMs: 10000);
```

### 📋 Deprecations

#### Removed Methods
- `init()` - Configuration now passed per method call
- `printCHEQReceipt()` - Renamed to `printReceipt()`
- `discoverPrintersWithDuration()` - Merged into `discoverPrinters()`
- `isPrinterConnected()` - Renamed to `checkPrinterConnection()`

#### Removed Classes
- Global configuration management (replaced with per-call config)

### 🔮 Future Plans

#### Upcoming Features
- **iOS Support**: Investigating iOS thermal printing solutions
- **Bluetooth Printers**: Support for Bluetooth thermal printers
- **Cloud Printing**: Integration with cloud printing services
- **Advanced Templates**: Rich receipt templating system
- **Print Scheduling**: Scheduled and delayed printing capabilities

#### Performance Improvements
- **Native Optimization**: Further native code optimizations
- **Memory Usage**: Reduced memory footprint
- **Battery Efficiency**: Optimized power consumption
- **Network Efficiency**: Improved network usage patterns

### 📞 Support & Resources

#### Documentation
- **API Reference**: Complete API documentation in `doc/API_REFERENCE.md`
- **Examples**: Real-world examples in `doc/EXAMPLES.md`
- **Test Guide**: Testing documentation in `test/README.md`
- **Troubleshooting**: Common issues and solutions in main README

#### Community
- **GitHub Issues**: Bug reports and feature requests
- **Discussions**: Community discussions and Q&A
- **Contributing**: Contribution guidelines for developers
- **Changelog**: This file for version history

---

## [1.5.0] - 2023-12-XX

### Added
- Initial release with basic printing functionality
- USB thermal printer support
- Star Micronics LAN printer support
- Basic device discovery
- Receipt generation with Receiptify integration

### Features
- Print CHEQ receipts through USB thermal printers
- Support for multiple receipt types (customer, merchant, kitchen, etc.)
- Basic printer operations (print, cash drawer, paper cut)
- Simple device discovery for Star Micronics printers

---

**Legend:**
- 🎉 Major Features
- 🔧 Technical Improvements  
- 📊 Quality & Testing
- 🚀 Performance
- 🛡️ Reliability
- 🔄 API Changes
- 📱 Platform Support
- 🐛 Bug Fixes
- 🔮 Future Plans
