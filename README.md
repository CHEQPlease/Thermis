# Thermis Flutter Plugin

[![Flutter](https://img.shields.io/badge/Flutter-3.0+-blue.svg)](https://flutter.dev)
[![Dart](https://img.shields.io/badge/Dart-3.0+-blue.svg)](https://dart.dev)
[![Android](https://img.shields.io/badge/Android-API%2021+-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

A comprehensive Flutter plugin for thermal receipt printing, specifically designed for CHEQ applications. Thermis provides seamless integration with USB and network-based thermal printers, featuring advanced queue management, error handling, and device discovery.

## ✨ Features

- 🖨️ **Multi-Printer Support**: USB Generic and Star Micronics LAN printers
- 🔄 **Advanced Queue Management**: Per-device queues with parallel processing
- 🔍 **Device Discovery**: Automatic printer detection with customizable duration
- ⚡ **Async Operations**: Non-blocking print operations with detailed results
- 🛡️ **Robust Error Handling**: Retry logic with exponential backoff
- 📊 **Queue Monitoring**: Real-time queue status and management
- 🎯 **Type Safety**: Comprehensive error classification and handling
- 🧪 **Well Tested**: 94 comprehensive tests ensuring reliability

## 📋 Table of Contents

- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [API Reference](#-api-reference)
- [Configuration](#-configuration)
- [Examples](#-examples)
- [Error Handling](#-error-handling)
- [Queue Management](#-queue-management)
- [Device Discovery](#-device-discovery)
- [Receipt Types](#-receipt-types)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

## 🚀 Installation

Add Thermis to your `pubspec.yaml`:

```yaml
dependencies:
  thermis:
    git:
      url: https://github.com/CHEQPlease/Thermis.git
      ref: release/1.5.0
```

Then run:
```bash
flutter pub get
```

## ⚡ Quick Start

### Basic Receipt Printing

```dart
import 'package:thermis/thermis.dart';

// Print to default USB printer
final result = await Thermis.printReceipt(receiptJson);
if (result?.success == true) {
  print('Receipt printed successfully!');
} else {
  print('Print failed: ${result?.reason?.displayName}');
}
```

### Network Printer Setup

```dart
import 'package:thermis/thermis.dart';

// Configure for Star Micronics LAN printer
final config = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: ['AA:BB:CC:DD:EE:FF'],
);

final result = await Thermis.printReceipt(receiptJson, config: config);
```

## 📖 API Reference

### Core Methods

#### `printReceipt(String receiptJson, {PrinterConfig? config})`

Prints a receipt with optional printer configuration.

**Parameters:**
- `receiptJson` (String): JSON string containing receipt data
- `config` (PrinterConfig?): Optional printer configuration (defaults to USB)

**Returns:** `Future<PrintResult?>` - Detailed print operation result

**Example:**
```dart
final result = await Thermis.printReceipt(
  '{"brandName": "CHEQ Diner", "orderNo": "K10"}',
  config: PrinterConfig(printerType: PrinterType.usbGeneric),
);

if (result?.success == true) {
  print('✅ Print successful');
} else if (result?.retryable == true) {
  print('⏳ Retryable error: ${result?.message}');
} else {
  print('❌ Print failed: ${result?.reason?.displayName}');
}
```

#### `openCashDrawer({PrinterConfig? config})`

Opens the cash drawer connected to the printer.

**Parameters:**
- `config` (PrinterConfig?): Optional printer configuration

**Returns:** `Future<bool?>` - Success status

**Example:**
```dart
final success = await Thermis.openCashDrawer(
  config: PrinterConfig(printerType: PrinterType.usbGeneric),
);
print(success ? 'Cash drawer opened' : 'Failed to open cash drawer');
```

#### `cutPaper({PrinterConfig? config})`

Cuts the receipt paper.

**Parameters:**
- `config` (PrinterConfig?): Optional printer configuration

**Returns:** `Future<bool?>` - Success status

**Example:**
```dart
await Thermis.cutPaper(
  config: PrinterConfig(printerType: PrinterType.usbGeneric),
);
```

#### `checkPrinterConnection({PrinterConfig? config})`

Checks if the printer is connected and ready.

**Parameters:**
- `config` (PrinterConfig?): Optional printer configuration

**Returns:** `Future<bool?>` - Connection status

**Example:**
```dart
final isConnected = await Thermis.checkPrinterConnection(
  config: PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: ['AA:BB:CC:DD:EE:FF'],
  ),
);
print('Printer ${isConnected ? 'connected' : 'disconnected'}');
```

#### `getReceiptReview(String receiptJson)`

Generates a bitmap preview of the receipt without printing.

**Parameters:**
- `receiptJson` (String): JSON string containing receipt data

**Returns:** `Future<Uint8List?>` - Receipt bitmap data

**Example:**
```dart
final bitmap = await Thermis.getReceiptReview(receiptJson);
if (bitmap != null) {
  // Display bitmap in UI
  Image.memory(bitmap);
}
```

### Device Discovery

#### `discoverPrinters({int scanDurationMs = 5000})`

Discovers available printers as a stream.

**Parameters:**
- `scanDurationMs` (int): Discovery duration in milliseconds (default: 5000)

**Returns:** `Stream<Device>` - Stream of discovered devices

**Example:**
```dart
await for (final device in Thermis.discoverPrinters(scanDurationMs: 10000)) {
  print('Found: ${device.deviceName} (${device.mac})');
}
```

#### `getAvailableDevices({int durationMs = 50000})`

Gets all available devices as a list.

**Parameters:**
- `durationMs` (int): Discovery duration in milliseconds (default: 50000)

**Returns:** `Future<List<Device>>` - List of discovered devices

**Example:**
```dart
final devices = await Thermis.getAvailableDevices(durationMs: 10000);
for (final device in devices) {
  print('Device: ${device.deviceName}');
  print('MAC: ${device.mac}');
  print('IP: ${device.ip}');
}
```

#### `stopDiscovery()`

Stops the current discovery process.

**Returns:** `Future<void>`

**Example:**
```dart
await Thermis.stopDiscovery();
```

### Queue Management

#### `getQueueSize()`

Gets the total number of jobs in all queues.

**Returns:** `Future<int?>` - Total queue size

**Example:**
```dart
final totalJobs = await Thermis.getQueueSize();
print('Total jobs in queue: $totalJobs');
```

#### `getDeviceQueueSizes()`

Gets queue sizes for each device.

**Returns:** `Future<Map<String, int>?>` - Device queue sizes

**Example:**
```dart
final deviceQueues = await Thermis.getDeviceQueueSizes();
deviceQueues?.forEach((deviceKey, queueSize) {
  print('$deviceKey: $queueSize jobs');
});
```

#### `clearPrintQueue()`

Clears all print queues.

**Returns:** `Future<bool?>` - Success status

**Example:**
```dart
final cleared = await Thermis.clearPrintQueue();
print(cleared ? 'All queues cleared' : 'Failed to clear queues');
```

#### `clearDeviceQueue(String deviceKey)`

Clears the queue for a specific device.

**Parameters:**
- `deviceKey` (String): Device identifier

**Returns:** `Future<bool?>` - Success status

**Example:**
```dart
final cleared = await Thermis.clearDeviceQueue('USB_GENERIC');
print(cleared ? 'Device queue cleared' : 'Failed to clear device queue');
```

## ⚙️ Configuration

### PrinterConfig

Configuration class for specifying printer settings.

```dart
class PrinterConfig {
  final PrinterType printerType;
  final List<String>? macAddresses;
  
  PrinterConfig({
    required this.printerType,
    this.macAddresses,
  });
}
```

### PrinterType

Enum defining supported printer types.

```dart
enum PrinterType {
  usbGeneric,  // USB thermal printers
  starMCLan,   // Star Micronics LAN printers
}
```

**Examples:**

```dart
// USB Printer
final usbConfig = PrinterConfig(
  printerType: PrinterType.usbGeneric,
);

// Single LAN Printer
final lanConfig = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: ['AA:BB:CC:DD:EE:FF'],
);

// Multiple LAN Printers
final multiLanConfig = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: [
    'AA:BB:CC:DD:EE:FF',
    '11:22:33:44:55:66',
    '99:88:77:66:55:44',
  ],
);
```

## 📝 Examples

### Complete Receipt Printing Example

```dart
import 'package:thermis/thermis.dart';

Future<void> printCompleteReceipt() async {
  const receiptJson = '''
  {
    "brandName": "CHEQ Diner",
    "orderType": "Self-Order",
    "orderSubtitle": "Kiosk-Order",
    "totalItems": "2",
    "orderNo": "K10",
    "tableNo": "234",
    "receiptType": "customer",
    "deviceType": "pos",
    "timeOfOrder": "Placed at : 01/12/2023 03:57 AM AKST",
    "isRefunded": false,
    "isReprinted": false,
    "paymentQRLink": "https://www.example.com/pay",
    "items": [
      {
        "itemName": "Salmon Fry",
        "description": "  -- Olive\\n  -- Deep Fried Salmon\\n  -- ADD Addition 1",
        "quantity": "1",
        "price": "\\$10.0",
        "strikethrough": false
      }
    ],
    "breakdown": [
      {
        "key": "Sub Total",
        "value": "\\$10.00"
      },
      {
        "key": "Tax",
        "value": "\\$1.00"
      },
      {
        "key": "GRAND TOTAL",
        "value": "\\$11.00",
        "important": true
      }
    ]
  }
  ''';

  // Print to USB printer
  final result = await Thermis.printReceipt(receiptJson);
  
  if (result?.success == true) {
    print('✅ Receipt printed successfully');
    
    // Open cash drawer after successful print
    await Thermis.openCashDrawer();
    
    // Cut paper
    await Thermis.cutPaper();
  } else {
    print('❌ Print failed: ${result?.reason?.displayName}');
    if (result?.retryable == true) {
      print('💡 This error is retryable');
    }
  }
}
```

### Network Printer Discovery and Printing

```dart
import 'package:thermis/thermis.dart';

Future<void> discoverAndPrint() async {
  print('🔍 Discovering printers...');
  
  final devices = await Thermis.getAvailableDevices(durationMs: 10000);
  
  if (devices.isEmpty) {
    print('❌ No printers found');
    return;
  }
  
  print('✅ Found ${devices.length} printer(s):');
  for (final device in devices) {
    print('  - ${device.deviceName} (${device.mac})');
  }
  
  // Use first discovered printer
  final config = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: [devices.first.mac],
  );
  
  // Check connection
  final isConnected = await Thermis.checkPrinterConnection(config: config);
  if (!isConnected) {
    print('❌ Printer not connected');
    return;
  }
  
  // Print receipt
  const receiptJson = '{"brandName": "CHEQ", "orderNo": "001"}';
  final result = await Thermis.printReceipt(receiptJson, config: config);
  
  print(result?.success == true ? '✅ Printed!' : '❌ Failed to print');
}
```

### Queue Management Example

```dart
import 'package:thermis/thermis.dart';

Future<void> manageQueues() async {
  // Check current queue status
  final totalJobs = await Thermis.getQueueSize();
  print('📊 Total jobs in queue: $totalJobs');
  
  final deviceQueues = await Thermis.getDeviceQueueSizes();
  print('📋 Device queues:');
  deviceQueues?.forEach((device, count) {
    print('  - $device: $count jobs');
  });
  
  // Add multiple print jobs
  final config = PrinterConfig(printerType: PrinterType.usbGeneric);
  
  for (int i = 1; i <= 5; i++) {
    final receiptJson = '{"orderNo": "00$i", "brandName": "CHEQ"}';
    Thermis.printReceipt(receiptJson, config: config); // Fire and forget
    print('📤 Queued job $i');
  }
  
  // Monitor queue
  await Future.delayed(Duration(seconds: 1));
  final newTotal = await Thermis.getQueueSize();
  print('📊 Queue size after adding jobs: $newTotal');
  
  // Clear specific device queue if needed
  if (newTotal! > 10) {
    await Thermis.clearDeviceQueue('USB_GENERIC');
    print('🧹 Cleared USB device queue');
  }
}
```

### Error Handling Example

```dart
import 'package:thermis/thermis.dart';

Future<void> handlePrintErrors() async {
  const receiptJson = '{"brandName": "CHEQ", "orderNo": "001"}';
  
  final result = await Thermis.printReceipt(receiptJson);
  
  if (result?.success == true) {
    print('✅ Success!');
    return;
  }
  
  // Handle different error types
  switch (result?.reason) {
    case PrintFailureReason.printerBusy:
      print('⏳ Printer is busy, will retry automatically');
      break;
    case PrintFailureReason.printerOffline:
      print('📴 Printer is offline, check connection');
      break;
    case PrintFailureReason.outOfPaper:
      print('📄 Out of paper, please refill');
      break;
    case PrintFailureReason.networkError:
      print('🌐 Network error, check connectivity');
      if (result?.retryable == true) {
        print('💡 This will be retried automatically');
      }
      break;
    default:
      print('❌ Unknown error: ${result?.message}');
  }
  
  // Check if error is retryable
  if (result?.retryable == true) {
    print('🔄 Job will be retried automatically');
  } else {
    print('🚫 Job failed permanently');
  }
}
```

## 🛡️ Error Handling

### PrintResult

The `PrintResult` class provides detailed information about print operations:

```dart
class PrintResult {
  final bool success;
  final PrintFailureReason? reason;
  final bool retryable;
  final String? message;
}
```

### PrintFailureReason

Comprehensive error classification:

| Reason | Retryable | Description |
|--------|-----------|-------------|
| `printerBusy` | ✅ | Printer is currently processing another job |
| `deviceInUse` | ✅ | Device is being used by another process |
| `networkError` | ✅ | Network connectivity issues |
| `communicationError` | ✅ | Communication protocol errors |
| `timeoutError` | ✅ | Operation timed out |
| `printerOffline` | ❌ | Printer is not responding |
| `printerNotFound` | ❌ | Printer device not found |
| `outOfPaper` | ❌ | Printer is out of paper |
| `coverOpen` | ❌ | Printer cover is open |
| `unknownError` | ❌ | Unclassified error |

### Retry Logic

Thermis automatically retries failed operations for retryable errors using exponential backoff:

- **Base Delay**: 3 seconds
- **Max Retries**: 3 attempts
- **Backoff**: Exponential with jitter

## 📊 Queue Management

### Per-Device Queues

Thermis maintains separate queues for each printer device, enabling:

- **Parallel Processing**: Different printers work simultaneously
- **Sequential Processing**: Same printer processes jobs in order
- **Load Balancing**: Distribute jobs across multiple printers

### Queue Keys

Device queues are identified by unique keys:

- **USB Generic**: `"USB_GENERIC"`
- **Star LAN**: `"STARMC_LAN_{MAC_ADDRESS}"`

Example: `"STARMC_LAN_AA:BB:CC:DD:EE:FF"`

## 🔍 Device Discovery

### Discovery Methods

1. **Stream-based**: `discoverPrinters()` - Real-time device discovery
2. **List-based**: `getAvailableDevices()` - One-shot device list

### Device Information

```dart
class Device {
  final String deviceName;  // e.g., "Star TSP100III"
  final String mac;         // e.g., "AA:BB:CC:DD:EE:FF"
  final String ip;          // e.g., "192.168.1.100"
}
```

## 📄 Receipt Types

Supported receipt types for different use cases:

| Type | Description | Use Case |
|------|-------------|----------|
| `customer` | Customer receipt | End customer copy |
| `merchant` | Merchant receipt | Business copy |
| `kitchen` | Kitchen receipt | Kitchen preparation |
| `kiosk` | Kiosk receipt | Self-service orders |
| `server_tips` | Server tips receipt | Tip distribution |
| `qr_payment` | QR payment receipt | QR code payments |

### Device Types

| Type | Description |
|------|-------------|
| `pos` | Point of Sale terminal |
| `handheld` | Mobile handheld device |

## 🧪 Testing

Thermis includes a comprehensive test suite with 94 tests covering:

- ✅ **Print Operations**: All printing scenarios
- ✅ **Error Handling**: All error conditions  
- ✅ **Queue Management**: Queue operations
- ✅ **Device Discovery**: Discovery methods
- ✅ **Configuration**: All config options
- ✅ **Integration**: End-to-end workflows

Run tests:
```bash
flutter test
```

## 🔧 Troubleshooting

### Common Issues

#### "Printer not found"
- Ensure printer is connected and powered on
- Check USB cable connection
- Verify printer drivers are installed

#### "Network error" (LAN printers)
- Check network connectivity
- Verify MAC address is correct
- Ensure printer is on the same network

#### "Printer busy"
- Wait for current job to complete
- Check if another app is using the printer
- Clear device queue if necessary

#### "Out of paper"
- Check paper roll installation
- Ensure paper is properly loaded
- Replace empty paper roll

### Debug Mode

Enable debug logging to troubleshoot issues:

```dart
// Add debug prints in your error handling
final result = await Thermis.printReceipt(receiptJson);
if (result?.success != true) {
  print('Debug - Error: ${result?.reason}');
  print('Debug - Message: ${result?.message}');
  print('Debug - Retryable: ${result?.retryable}');
}
```

### Queue Issues

If jobs are stuck in queue:

```dart
// Clear all queues
await Thermis.clearPrintQueue();

// Or clear specific device
await Thermis.clearDeviceQueue('USB_GENERIC');
```

## 📱 Platform Support

- **Android**: API 21+ (Android 5.0+)
- **iOS**: Not supported (thermal printing limitations)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For issues and questions:

1. Check [Troubleshooting](#-troubleshooting) section
2. Search existing [GitHub Issues](https://github.com/CHEQPlease/Thermis/issues)
3. Create a new issue with detailed information

## 📚 Additional Resources

- [Receiptify Library](https://github.com/CHEQPlease/Receiptify) - Receipt generation engine
- [Star Micronics Documentation](https://www.star-m.jp/eng/products/s_print/sdk/) - Star printer SDK
- [Flutter Plugin Development](https://docs.flutter.dev/development/packages-and-plugins) - Plugin development guide

---

**Made with ❤️ by the CHEQ Team**
 
